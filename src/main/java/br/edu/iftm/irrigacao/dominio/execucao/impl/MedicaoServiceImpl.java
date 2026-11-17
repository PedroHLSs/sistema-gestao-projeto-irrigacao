package br.edu.iftm.irrigacao.dominio.execucao.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioLogado;
import br.edu.iftm.irrigacao.dominio.execucao.FaixaMedicao;
import br.edu.iftm.irrigacao.dominio.execucao.Medicao;
import br.edu.iftm.irrigacao.dominio.execucao.MedicaoAvaliada;
import br.edu.iftm.irrigacao.dominio.execucao.MedicaoService;
import br.edu.iftm.irrigacao.dominio.projeto.ArmazenamentoDeFotos;
import br.edu.iftm.irrigacao.dominio.projeto.FotoArmazenada;
import br.edu.iftm.irrigacao.dominio.projeto.FotoEnviada;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.TipoFoto;
import br.edu.iftm.irrigacao.persistencia.MedicaoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.UsuarioRepository;

/**
 * Medição e validação (RF17, G6). As contas da tolerância são feitas em decimal exato a partir do
 * valor digitado, para que 30,6 com limite de 30 + 2% conte como dentro, e não como fora por um
 * arredondamento de {@code double}.
 */
@Service
public class MedicaoServiceImpl implements MedicaoService {

    private static final BigDecimal CEM = new BigDecimal("100");

    private final MedicaoRepository medicaoRepository;
    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioLogado usuarioLogado;
    private final ProjetoService projetoService;
    private final ArmazenamentoDeFotos armazenamento;
    private final Clock clock;

    public MedicaoServiceImpl(MedicaoRepository medicaoRepository, ProjetoRepository projetoRepository,
            UsuarioRepository usuarioRepository, UsuarioLogado usuarioLogado, ProjetoService projetoService,
            ArmazenamentoDeFotos armazenamento, Clock clock) {
        this.medicaoRepository = medicaoRepository;
        this.projetoRepository = projetoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioLogado = usuarioLogado;
        this.projetoService = projetoService;
        this.armazenamento = armazenamento;
        this.clock = clock;
    }

    @Override
    public FaixaMedicao faixa(Projeto projeto) {
        exigirParametros(projeto);
        BigDecimal pressao = BigDecimal.valueOf(projeto.getPressaoProjetoMca());
        BigDecimal vazao = BigDecimal.valueOf(projeto.getVazaoProjetoM3h());
        BigDecimal tolerancia = BigDecimal.valueOf(projeto.getToleranciaPercentual());
        BigDecimal margemPressao = margem(pressao, tolerancia);
        BigDecimal margemVazao = margem(vazao, tolerancia);
        return new FaixaMedicao(pressao.subtract(margemPressao), pressao.add(margemPressao),
                vazao.subtract(margemVazao), vazao.add(margemVazao), tolerancia);
    }

    @Override
    @Transactional
    public Medicao registrar(Long projetoId, Double pressaoMedidaMca, Double vazaoMedidaM3h, FotoEnviada foto,
            Double latitude, Double longitude) {
        // Linha travada antes de ler a situação: dois envios juntos não gravam duas medições
        Projeto projeto = projetoRepository.buscarParaAlterarSituacao(projetoId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
        ExecucaoRegras.exigirSituacao(projeto, SituacaoProjeto.VALIDANDO_MEDICAO, "A medição é registrada");
        exigirParametros(projeto);
        if (pressaoMedidaMca == null || pressaoMedidaMca <= 0) {
            throw new RegraNegocioException("Informe a pressão medida, maior que zero.");
        }
        if (vazaoMedidaM3h == null || vazaoMedidaM3h <= 0) {
            throw new RegraNegocioException("Informe a vazão medida, maior que zero.");
        }
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new RegraNegocioException("A latitude vai de -90 a 90.");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new RegraNegocioException("A longitude vai de -180 a 180.");
        }
        TipoFoto tipo = foto == null ? null : armazenamento.conferir(foto);

        double tolerancia = projeto.getToleranciaPercentual();
        boolean aprovada = dentroDaTolerancia(pressaoMedidaMca, projeto.getPressaoProjetoMca(), tolerancia)
                && dentroDaTolerancia(vazaoMedidaM3h, projeto.getVazaoProjetoM3h(), tolerancia);
        String caminho = foto == null ? null : armazenamento.gravar("medicao/" + projetoId, foto, tipo);
        Medicao medicao = medicaoRepository.save(new Medicao(null, projeto, pressaoMedidaMca, vazaoMedidaM3h, caminho,
                latitude, longitude, LocalDateTime.now(clock), aprovada, usuarioAtual()));

        String resumo = "pressão " + numero(pressaoMedidaMca) + " mca ("
                + sinal(desvioPercentual(pressaoMedidaMca, projeto.getPressaoProjetoMca())) + "), vazão "
                + numero(vazaoMedidaM3h) + " m³/h (" + sinal(desvioPercentual(vazaoMedidaM3h, projeto.getVazaoProjetoM3h()))
                + "), tolerância de " + numero(tolerancia) + "%.";
        if (aprovada) {
            projetoService.transitar(projetoId, SituacaoProjeto.COLHENDO_ACEITE, "Medição dentro do parâmetro: " + resumo);
        } else {
            projetoService.transitar(projetoId, SituacaoProjeto.CORRIGINDO_INSTALACAO,
                    "Medição fora do parâmetro: " + resumo);
        }
        return medicao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicaoAvaliada> doProjeto(Long projetoId) {
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
        return medicaoRepository.findByProjetoIdOrderByDataHoraDescIdDesc(projetoId).stream()
                .map(m -> new MedicaoAvaliada(m,
                        projeto.getPressaoProjetoMca() == null ? null
                                : desvioPercentual(m.getPressaoMedidaMca(), projeto.getPressaoProjetoMca()),
                        projeto.getVazaoProjetoM3h() == null ? null
                                : desvioPercentual(m.getVazaoMedidaM3h(), projeto.getVazaoProjetoM3h())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FotoArmazenada foto(Long projetoId, Long medicaoId) {
        Medicao medicao = medicaoRepository.findById(medicaoId)
                .filter(m -> m.getProjeto().getId().equals(projetoId) && m.getFoto() != null)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Foto não encontrada."));
        return armazenamento.abrir(medicao.getFoto());
    }

    @Override
    public boolean dentroDaTolerancia(double medida, double referencia, double toleranciaPercentual) {
        BigDecimal ref = BigDecimal.valueOf(referencia);
        BigDecimal diferenca = BigDecimal.valueOf(medida).subtract(ref).abs();
        return diferenca.compareTo(margem(ref, BigDecimal.valueOf(toleranciaPercentual))) <= 0;
    }

    @Override
    public BigDecimal desvioPercentual(double medida, double referencia) {
        BigDecimal ref = BigDecimal.valueOf(referencia);
        return BigDecimal.valueOf(medida).subtract(ref).multiply(CEM).divide(ref, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal margem(BigDecimal referencia, BigDecimal toleranciaPercentual) {
        return referencia.multiply(toleranciaPercentual).divide(CEM);
    }

    private static void exigirParametros(Projeto projeto) {
        if (projeto.getPressaoProjetoMca() == null || projeto.getVazaoProjetoM3h() == null
                || projeto.getToleranciaPercentual() == null) {
            throw new RegraNegocioException("O projeto " + projeto.getCodigo()
                    + " está sem pressão ou vazão de projeto: a medição não tem com o que comparar.");
        }
    }

    private Usuario usuarioAtual() {
        return usuarioLogado.idAtual().map(usuarioRepository::getReferenceById).orElse(null);
    }

    private static String numero(double valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.of("pt", "BR"));
        formato.setMaximumFractionDigits(3);
        return formato.format(valor);
    }

    private static String sinal(BigDecimal desvio) {
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.of("pt", "BR"));
        formato.setMaximumFractionDigits(2);
        return (desvio.signum() > 0 ? "+" : "") + formato.format(desvio) + "%";
    }
}
