package br.edu.iftm.irrigacao.dominio.comercial.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioLogado;
import br.edu.iftm.irrigacao.dominio.comercial.Orcamento;
import br.edu.iftm.irrigacao.dominio.comercial.Orcamento.Linha;
import br.edu.iftm.irrigacao.dominio.comercial.ParametrosComerciais;
import br.edu.iftm.irrigacao.dominio.comercial.Proposta;
import br.edu.iftm.irrigacao.dominio.comercial.PropostaService;
import br.edu.iftm.irrigacao.dominio.comercial.SituacaoProposta;
import br.edu.iftm.irrigacao.dominio.estoque.Cotacao;
import br.edu.iftm.irrigacao.dominio.estoque.CotacaoService;
import br.edu.iftm.irrigacao.dominio.estoque.DisponibilidadeItem;
import br.edu.iftm.irrigacao.dominio.estoque.EstoqueService;
import br.edu.iftm.irrigacao.dominio.estoque.Material;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.PropostaRepository;
import br.edu.iftm.irrigacao.persistencia.UsuarioRepository;

/**
 * Orçamento e proposta (RF09), alçada (RF10, G2), envio e validade (RF11) e resposta do cliente
 * (RF12, G3).
 */
@Service
public class PropostaServiceImpl implements PropostaService {

    /** Propostas que ainda estão com a empresa: no máximo uma por projeto. */
    private static final Set<SituacaoProposta> ABERTAS = EnumSet.of(SituacaoProposta.EM_ELABORACAO,
            SituacaoProposta.AGUARDANDO_DIRETORIA);

    private static final Set<SituacaoProposta> EM_ANDAMENTO = EnumSet.of(SituacaoProposta.EM_ELABORACAO,
            SituacaoProposta.AGUARDANDO_DIRETORIA, SituacaoProposta.ENVIADA);

    private static final BigDecimal HORAS_MAXIMAS = new BigDecimal("999999.99");
    private static final BigDecimal CEM = new BigDecimal("100");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PropostaRepository propostaRepository;
    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstoqueService estoqueService;
    private final CotacaoService cotacaoService;
    private final ProjetoService projetoService;
    private final UsuarioLogado usuarioLogado;
    private final ParametrosComerciais parametros;
    private final Clock clock;

    public PropostaServiceImpl(PropostaRepository propostaRepository, ProjetoRepository projetoRepository,
            UsuarioRepository usuarioRepository, EstoqueService estoqueService, CotacaoService cotacaoService,
            ProjetoService projetoService, UsuarioLogado usuarioLogado, ParametrosComerciais parametros, Clock clock) {
        this.propostaRepository = propostaRepository;
        this.projetoRepository = projetoRepository;
        this.usuarioRepository = usuarioRepository;
        this.estoqueService = estoqueService;
        this.cotacaoService = cotacaoService;
        this.projetoService = projetoService;
        this.usuarioLogado = usuarioLogado;
        this.parametros = parametros;
        this.clock = clock;
    }

    // ---------- Orçamento (RF09) ----------

    @Override
    @Transactional(readOnly = true)
    public Orcamento simular(Long projetoId, BigDecimal horasPrevistas, BigDecimal percentualDesconto) {
        buscarProjeto(projetoId);
        exigirHoras(horasPrevistas);
        exigirDesconto(percentualDesconto);
        return montar(projetoId, horasPrevistas, percentualDesconto, false);
    }

    @Override
    @Transactional
    public Proposta gerar(Long projetoId, BigDecimal horasPrevistas, BigDecimal percentualDesconto) {
        Projeto projeto = buscarProjeto(projetoId);
        if (projeto.getSituacao() != SituacaoProjeto.ORCANDO) {
            throw new RegraNegocioException("O orçamento é gerado com o projeto em \"" + SituacaoProjeto.ORCANDO.getRotulo()
                    + "\". O projeto " + projeto.getCodigo() + " está em \"" + projeto.getSituacao().getRotulo() + "\".");
        }
        if (propostaRepository.existsByProjetoIdAndSituacaoIn(projetoId, ABERTAS)) {
            throw new RegraNegocioException("O projeto " + projeto.getCodigo()
                    + " já tem uma proposta em elaboração ou na diretoria. Altere essa proposta em vez de gerar outra.");
        }
        return novaVersao(projeto, horasPrevistas, percentualDesconto);
    }

    @Override
    @Transactional(readOnly = true)
    public Orcamento detalhar(Long propostaId) {
        Proposta proposta = buscarPorId(propostaId);
        return montar(proposta.getProjeto().getId(), proposta.getHorasPrevistas(), proposta.getPercentualDesconto(), true);
    }

    // ---------- Consultas ----------

    @Override
    @Transactional(readOnly = true)
    public Proposta buscarPorId(Long id) {
        return propostaRepository.buscarCompleta(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Proposta não encontrada."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Proposta> listarDoProjeto(Long projetoId) {
        return propostaRepository.listarDoProjeto(projetoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Proposta> listarEmAndamento() {
        return propostaRepository.listarPorSituacoes(EM_ANDAMENTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Proposta> pendentesDaDiretoria() {
        return propostaRepository.listarPorSituacoes(EnumSet.of(SituacaoProposta.AGUARDANDO_DIRETORIA));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Proposta> abertaDoProjeto(Long projetoId) {
        return propostaRepository.findFirstByProjetoIdOrderByNumeroDesc(projetoId)
                .filter(proposta -> ABERTAS.contains(proposta.getSituacao()));
    }

    // ---------- Alçada (RF10, G2) ----------

    @Override
    public boolean dentroDaAlcada(BigDecimal percentualDesconto) {
        return percentualDesconto.compareTo(parametros.getAlcadaVendedor()) <= 0;
    }

    @Override
    public boolean podeEnviar(Proposta proposta) {
        return proposta.getSituacao() == SituacaoProposta.EM_ELABORACAO
                && (dentroDaAlcada(proposta.getPercentualDesconto()) || proposta.getAprovadoPor() != null);
    }

    @Override
    @Transactional
    public Proposta alterar(Long propostaId, BigDecimal horasPrevistas, BigDecimal percentualDesconto) {
        Proposta proposta = buscarNaSituacao(propostaId, SituacaoProposta.EM_ELABORACAO, "alterada");
        exigirHoras(horasPrevistas);
        exigirDesconto(percentualDesconto);
        if (proposta.getPercentualDesconto().compareTo(percentualDesconto) != 0) {
            // A aprovação e a resposta da diretoria valiam para o desconto anterior
            proposta.setAprovadoPor(null);
            proposta.setObservacaoDiretoria(null);
        }
        Long projetoId = proposta.getProjeto().getId();
        proposta.setHorasPrevistas(escala(horasPrevistas));
        proposta.setPercentualDesconto(escala(percentualDesconto));
        proposta.setValorTotal(montar(projetoId, horasPrevistas, percentualDesconto, true).getValorTotal());
        proposta.getProjeto().setCustoOrcado(montar(projetoId, horasPrevistas, percentualDesconto, false).getCustoOrcado());
        return proposta;
    }

    @Override
    @Transactional
    public Proposta pedirAprovacao(Long propostaId) {
        Proposta proposta = buscarNaSituacao(propostaId, SituacaoProposta.EM_ELABORACAO, "enviada à diretoria");
        if (dentroDaAlcada(proposta.getPercentualDesconto())) {
            throw new RegraNegocioException("O desconto de " + percentual(proposta.getPercentualDesconto())
                    + " está dentro da alçada de " + percentual(parametros.getAlcadaVendedor())
                    + ": a proposta pode ser enviada direto ao cliente.");
        }
        if (proposta.getAprovadoPor() != null) {
            throw new RegraNegocioException("O desconto desta proposta já foi aprovado pela diretoria.");
        }
        proposta.setObservacaoDiretoria(null);
        proposta.setSituacao(SituacaoProposta.AGUARDANDO_DIRETORIA);
        return proposta;
    }

    @Override
    @Transactional
    public Proposta aprovarDesconto(Long propostaId, String observacao) {
        Proposta proposta = buscarNaSituacao(propostaId, SituacaoProposta.AGUARDANDO_DIRETORIA, "aprovada pela diretoria");
        Long diretor = usuarioLogado.idAtual()
                .orElseThrow(() -> new RegraNegocioException("Entre no sistema para aprovar o desconto."));
        proposta.setAprovadoPor(usuarioRepository.getReferenceById(diretor));
        proposta.setObservacaoDiretoria(texto(observacao, 500, "A observação"));
        proposta.setSituacao(SituacaoProposta.EM_ELABORACAO);
        return proposta;
    }

    @Override
    @Transactional
    public Proposta negarDesconto(Long propostaId, String motivo) {
        Proposta proposta = buscarNaSituacao(propostaId, SituacaoProposta.AGUARDANDO_DIRETORIA, "negada pela diretoria");
        String texto = texto(motivo, 500, "O motivo");
        if (texto == null) {
            throw new RegraNegocioException("Informe o motivo da negativa, para o vendedor refazer o desconto.");
        }
        proposta.setAprovadoPor(null);
        proposta.setObservacaoDiretoria(texto);
        proposta.setSituacao(SituacaoProposta.EM_ELABORACAO);
        return proposta;
    }

    // ---------- Envio e validade (RF11) ----------

    @Override
    @Transactional
    public Proposta enviar(Long propostaId) {
        Proposta proposta = buscarNaSituacao(propostaId, SituacaoProposta.EM_ELABORACAO, "enviada");
        if (!podeEnviar(proposta)) {
            throw new RegraNegocioException("O desconto de " + percentual(proposta.getPercentualDesconto())
                    + " passa da alçada de " + percentual(parametros.getAlcadaVendedor())
                    + " e não foi aprovado pela diretoria. Peça a aprovação antes de enviar.");
        }
        proposta.setDataEnvio(LocalDate.now(clock));
        if (proposta.getValidadeDias() == null) {
            proposta.setValidadeDias(parametros.getValidadeDias());
        }
        proposta.setSituacao(SituacaoProposta.ENVIADA);
        projetoService.transitar(proposta.getProjeto().getId(), SituacaoProjeto.AGUARDANDO_RESPOSTA,
                "Proposta " + proposta.getNumero() + " enviada ao cliente, válida até "
                        + DATA.format(proposta.getDataValidade()) + ".");
        return proposta;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> enviadasVencidas() {
        LocalDate hoje = LocalDate.now(clock);
        return propostaRepository.findBySituacao(SituacaoProposta.ENVIADA).stream()
                .filter(proposta -> vencida(proposta, hoje))
                .map(Proposta::getId)
                .toList();
    }

    @Override
    @Transactional
    public Optional<Proposta> vencer(Long propostaId) {
        Proposta proposta = propostaRepository.findById(propostaId).orElse(null);
        if (proposta == null || proposta.getSituacao() != SituacaoProposta.ENVIADA
                || !vencida(proposta, LocalDate.now(clock))) {
            return Optional.empty();
        }
        proposta.setSituacao(SituacaoProposta.VENCIDA);
        Long projetoId = proposta.getProjeto().getId();
        projetoService.transitar(projetoId, SituacaoProjeto.ORCANDO, "Proposta " + proposta.getNumero()
                + " venceu: valia até " + DATA.format(proposta.getDataValidade())
                + ". Nova versão gerada com os preços atuais.");
        // recalcularPrecos() da Figura 9: mesma mão de obra e mesmo desconto, preços de hoje
        return Optional.of(novaVersao(buscarProjeto(projetoId), proposta.getHorasPrevistas(),
                proposta.getPercentualDesconto()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Proposta> alertarVencimento(Long usuarioId) {
        LocalDate hoje = LocalDate.now(clock);
        return propostaRepository.listarDoResponsavel(usuarioId, SituacaoProposta.ENVIADA).stream()
                .filter(proposta -> entraNoAlerta(proposta.getDataValidade(), hoje))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarAlertasDeVencimento(Long usuarioId) {
        LocalDate hoje = LocalDate.now(clock);
        return propostaRepository.contarDoResponsavelPorValidade(usuarioId, SituacaoProposta.ENVIADA).stream()
                .filter(grupo -> entraNoAlerta(grupo.getDataEnvio().plusDays(grupo.getValidadeDias()), hoje))
                .mapToLong(PropostaRepository.TotalPorValidade::getTotal)
                .sum();
    }

    /** Ainda vale e vence em até {@code irrigacao.proposta.dias-alerta} dias, contando hoje. */
    private boolean entraNoAlerta(LocalDate dataValidade, LocalDate hoje) {
        return !hoje.isAfter(dataValidade) && !dataValidade.isAfter(hoje.plusDays(parametros.getDiasAlerta()));
    }

    // ---------- Resposta do cliente (RF12, G3) ----------

    @Override
    @Transactional
    public Proposta registrarAprovacao(Long propostaId) {
        Proposta proposta = buscarNaSituacao(propostaId, SituacaoProposta.ENVIADA, "aprovada pelo cliente");
        if (vencida(proposta, LocalDate.now(clock))) {
            throw new RegraNegocioException("A proposta " + proposta.getNumero() + " venceu: valia até "
                    + DATA.format(proposta.getDataValidade()) + ". A aprovação depois do vencimento não é "
                    + "registrada; a proposta é refeita com os preços atuais.");
        }
        proposta.setSituacao(SituacaoProposta.APROVADA);
        Long projetoId = proposta.getProjeto().getId();
        projetoService.transitar(projetoId, SituacaoProjeto.PREPARANDO_MATERIAL,
                "Proposta " + proposta.getNumero() + " aprovada pelo cliente.");
        reservarODisponivel(projetoId);
        return proposta;
    }

    @Override
    @Transactional
    public Proposta registrarRecusa(Long propostaId, String motivo) {
        Proposta proposta = buscarNaSituacao(propostaId, SituacaoProposta.ENVIADA, "recusada pelo cliente");
        String texto = texto(motivo, 400, "O motivo");
        if (texto == null) {
            throw new RegraNegocioException("Informe o motivo da recusa do cliente.");
        }
        proposta.setSituacao(SituacaoProposta.RECUSADA);
        projetoService.transitar(proposta.getProjeto().getId(), SituacaoProjeto.CANCELADO,
                "Proposta " + proposta.getNumero() + " recusada pelo cliente: " + texto);
        return proposta;
    }

    // ---------- Apoio ----------

    /**
     * Linhas do orçamento a partir da disponibilidade da lista técnica. O custo de um item faltante
     * é o da cotação mais barata, quando há; se não, o custo unitário do cadastro.
     *
     * @param precoCongelado {@code true} usa o preço congelado no item; {@code false}, o preço de
     *                       venda atual do material
     */
    private Orcamento montar(Long projetoId, BigDecimal horas, BigDecimal desconto, boolean precoCongelado) {
        Map<Long, Cotacao> cotacoes = cotacaoService.maisBaratas(projetoId);
        List<DisponibilidadeItem> itens = estoqueService.disponibilidade(projetoId);
        if (itens.isEmpty()) {
            throw new RegraNegocioException("A lista técnica do projeto está vazia.");
        }
        List<Linha> linhas = itens.stream().map(disponibilidade -> {
            Material material = disponibilidade.getMaterial();
            BigDecimal preco = precoCongelado && disponibilidade.getItem().getPrecoUnitario() != null
                    ? disponibilidade.getItem().getPrecoUnitario()
                    : material.getPrecoVenda();
            if (preco == null) {
                throw new RegraNegocioException("O material " + material.getCodigo() + " está sem preço de venda no cadastro.");
            }
            Cotacao cotacao = cotacoes.get(material.getId());
            boolean faltante = disponibilidade.getFaltante().signum() > 0;
            if (faltante && cotacao != null) {
                return new Linha(disponibilidade.getItem(), preco, cotacao.getPrecoUnitario(),
                        "cotação de " + cotacao.getFornecedor(), disponibilidade.getFaltante());
            }
            if (material.getCustoUnitario() == null) {
                throw new RegraNegocioException("O material " + material.getCodigo() + " está sem custo unitário no cadastro.");
            }
            return new Linha(disponibilidade.getItem(), preco, material.getCustoUnitario(), "cadastro",
                    disponibilidade.getFaltante());
        }).toList();
        return new Orcamento(linhas, horas, parametros.getPrecoHora(), parametros.getCustoHora(), desconto);
    }

    /** Proposta de número seguinte em EM_ELABORACAO, com os preços de hoje congelados nos itens. */
    private Proposta novaVersao(Projeto projeto, BigDecimal horas, BigDecimal desconto) {
        exigirHoras(horas);
        exigirDesconto(desconto);
        Orcamento orcamento = montar(projeto.getId(), horas, desconto, false);
        orcamento.getLinhas().forEach(linha -> linha.getItem().setPrecoUnitario(linha.getPrecoUnitario()));
        projeto.setCustoOrcado(orcamento.getCustoOrcado());
        int numero = propostaRepository.findFirstByProjetoIdOrderByNumeroDesc(projeto.getId())
                .map(anterior -> anterior.getNumero() + 1)
                .orElse(1);
        return propostaRepository.save(new Proposta(null, projeto, numero, orcamento.getValorTotal(), escala(desconto),
                null, parametros.getValidadeDias(), SituacaoProposta.EM_ELABORACAO, escala(horas), null, null));
    }

    /** Reserva, para cada item, o que ainda não foi reservado, até o disponível (G3). */
    private void reservarODisponivel(Long projetoId) {
        for (DisponibilidadeItem item : estoqueService.disponibilidade(projetoId)) {
            BigDecimal aReservar = item.getPrevista().subtract(item.getReservadaParaProjeto()).min(item.getDisponivel());
            if (aReservar.signum() > 0) {
                estoqueService.reservar(item.getMaterial().getId(), projetoId, aReservar);
            }
        }
    }

    private static boolean vencida(Proposta proposta, LocalDate hoje) {
        return proposta.getDataValidade() != null && hoje.isAfter(proposta.getDataValidade());
    }

    private Proposta buscarNaSituacao(Long propostaId, SituacaoProposta situacao, String acao) {
        Proposta proposta = propostaRepository.findById(propostaId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Proposta não encontrada."));
        if (proposta.getSituacao() != situacao) {
            throw new RegraNegocioException("A proposta " + proposta.getNumero() + " está \""
                    + proposta.getSituacao().getRotulo() + "\" e não pode ser " + acao + ". Isso só vale para "
                    + "proposta \"" + situacao.getRotulo() + "\".");
        }
        return proposta;
    }

    private Projeto buscarProjeto(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
    }

    private static void exigirHoras(BigDecimal horas) {
        if (horas == null || horas.signum() < 0 || horas.compareTo(HORAS_MAXIMAS) > 0
                || horas.stripTrailingZeros().scale() > 2) {
            throw new RegraNegocioException("Informe as horas previstas de mão de obra, zero ou mais, com até 2 casas decimais.");
        }
    }

    private static void exigirDesconto(BigDecimal desconto) {
        if (desconto == null || desconto.signum() < 0 || desconto.compareTo(CEM) > 0
                || desconto.stripTrailingZeros().scale() > 2) {
            throw new RegraNegocioException("Informe o desconto, de 0 a 100%, com até 2 casas decimais.");
        }
    }

    private static BigDecimal escala(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    /** Texto opcional sem espaços nas pontas; vazio vira {@code null}. */
    private static String texto(String valor, int tamanho, String nome) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpo = valor.strip();
        if (limpo.length() > tamanho) {
            throw new RegraNegocioException(nome + " pode ter até " + tamanho + " caracteres.");
        }
        return limpo;
    }

    private static String percentual(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.of("pt", "BR"));
        formato.setMaximumFractionDigits(2);
        return formato.format(valor) + "%";
    }
}
