package br.edu.iftm.irrigacao.dominio.execucao.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioLogado;
import br.edu.iftm.irrigacao.dominio.comercial.ParametrosComerciais;
import br.edu.iftm.irrigacao.dominio.estoque.EstoqueService;
import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.Material;
import br.edu.iftm.irrigacao.dominio.estoque.Quantidade;
import br.edu.iftm.irrigacao.dominio.estoque.SaldoInsuficienteException;
import br.edu.iftm.irrigacao.dominio.execucao.Apontamento;
import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoHoras;
import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoMaterial;
import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoService;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServico;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServicoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.ApontamentoHorasRepository;
import br.edu.iftm.irrigacao.persistencia.ApontamentoMaterialRepository;
import br.edu.iftm.irrigacao.persistencia.ApontamentoRepository;
import br.edu.iftm.irrigacao.persistencia.ItemProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.UsuarioRepository;

/**
 * Apontamento de horas e materiais (RF16). O material apontado além do separado sai do estoque no
 * mesmo lançamento (RF07); o separado já saiu na conferência da separação.
 */
@Service
public class ApontamentoServiceImpl implements ApontamentoService {

    private static final BigDecimal HORAS_MAXIMAS = new BigDecimal("999999.99");

    private final OrdemServicoService ordemServicoService;
    private final ApontamentoRepository apontamentoRepository;
    private final ApontamentoHorasRepository horasRepository;
    private final ApontamentoMaterialRepository materialRepository;
    private final ItemProjetoRepository itemProjetoRepository;
    private final EstoqueService estoqueService;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioLogado usuarioLogado;
    private final ParametrosComerciais parametros;
    private final Clock clock;

    public ApontamentoServiceImpl(OrdemServicoService ordemServicoService, ApontamentoRepository apontamentoRepository,
            ApontamentoHorasRepository horasRepository, ApontamentoMaterialRepository materialRepository,
            ItemProjetoRepository itemProjetoRepository, EstoqueService estoqueService,
            UsuarioRepository usuarioRepository, UsuarioLogado usuarioLogado, ParametrosComerciais parametros,
            Clock clock) {
        this.ordemServicoService = ordemServicoService;
        this.apontamentoRepository = apontamentoRepository;
        this.horasRepository = horasRepository;
        this.materialRepository = materialRepository;
        this.itemProjetoRepository = itemProjetoRepository;
        this.estoqueService = estoqueService;
        this.usuarioRepository = usuarioRepository;
        this.usuarioLogado = usuarioLogado;
        this.parametros = parametros;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ApontamentoHoras apontarHoras(Long ordemId, BigDecimal horas) {
        OrdemServico ordem = ordemEmExecucao(ordemId);
        if (horas == null || horas.signum() <= 0 || horas.compareTo(HORAS_MAXIMAS) > 0
                || horas.stripTrailingZeros().scale() > 2) {
            throw new RegraNegocioException("Informe as horas, maiores que zero e com até 2 casas decimais.");
        }
        return horasRepository.save(new ApontamentoHoras(null, ordem, LocalDateTime.now(clock), usuarioAtual(), horas,
                parametros.getCustoHora()));
    }

    @Override
    @Transactional
    public ApontamentoMaterial apontarMaterial(Long ordemId, Long materialId, BigDecimal quantidade) {
        OrdemServico ordem = ordemEmExecucao(ordemId);
        Long projetoId = ordem.getProjeto().getId();
        ItemProjeto item = itemProjetoRepository.buscarParaApontar(projetoId, materialId)
                .orElseThrow(() -> new RegraNegocioException("Esse material não está na lista técnica do projeto "
                        + ordem.getProjeto().getCodigo() + ". Só material da lista técnica pode ser apontado."));
        if (quantidade == null || quantidade.signum() <= 0 || !Quantidade.cabeNaColuna(quantidade)) {
            throw new RegraNegocioException("Informe a quantidade, maior que zero e com até 3 casas decimais.");
        }
        item.setQuantidadeApontada(item.getQuantidadeApontada().add(quantidade));
        // A baixa no estoque limpa o contexto de persistência: o material fica carregado antes dela
        Material material = item.getMaterial();
        Hibernate.initialize(material);
        ApontamentoMaterial apontamento = materialRepository.save(new ApontamentoMaterial(null, ordem,
                LocalDateTime.now(clock), usuarioAtual(), material, quantidade));
        acertarEstoque(item, projetoId);
        return apontamento;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> avisoDeExcesso(Long projetoId, Long materialId) {
        return itemProjetoRepository.findByProjetoIdAndMaterialId(projetoId, materialId)
                .filter(item -> item.getQuantidadeApontada().compareTo(item.getQuantidadeSeparada()) > 0)
                .map(item -> "Atenção: o apontado de " + item.getMaterial().getCodigo() + " ("
                        + Quantidade.formatar(item.getQuantidadeApontada()) + " " + item.getMaterial().getUnidade()
                        + ") passou do separado (" + Quantidade.formatar(item.getQuantidadeSeparada()) + "). "
                        + "O que passou, " + Quantidade.formatar(item.getQuantidadeApontada()
                                .subtract(item.getQuantidadeSeparada())) + " " + item.getMaterial().getUnidade()
                        + ", foi baixado do estoque. Confira se o lançamento está certo.");
    }

    @Override
    @Transactional
    public Apontamento excluirUltimo(Long ordemId) {
        OrdemServico ordem = ordemEmExecucao(ordemId);
        Apontamento ultimo = apontamentoRepository.findFirstByOrdemServicoIdOrderByDataHoraDescIdDesc(ordemId)
                .orElseThrow(() -> new RegraNegocioException("A ordem de serviço " + ordem.getNumero()
                        + " não tem lançamento para excluir."));
        if (ultimo instanceof ApontamentoMaterial lancamento) {
            Long projetoId = ordem.getProjeto().getId();
            // A volta ao estoque limpa o contexto de persistência: o material fica carregado antes dela
            Hibernate.initialize(lancamento.getMaterial());
            ItemProjeto item = itemProjetoRepository.buscarParaApontar(projetoId, lancamento.getMaterial().getId())
                    .orElse(null);
            apontamentoRepository.delete(ultimo);
            if (item != null) {
                item.setQuantidadeApontada(
                        item.getQuantidadeApontada().subtract(lancamento.getQuantidade()).max(BigDecimal.ZERO));
                acertarEstoque(item, projetoId);
            }
            return ultimo;
        }
        apontamentoRepository.delete(ultimo);
        return ultimo;
    }

    /**
     * O que o técnico usou além do separado sai do estoque na hora; se um lançamento é excluído, a
     * diferença volta (RF16, RF07).
     */
    private void acertarEstoque(ItemProjeto item, Long projetoId) {
        Material material = item.getMaterial();
        BigDecimal apontada = item.getQuantidadeApontada();
        BigDecimal separada = item.getQuantidadeSeparada();
        try {
            estoqueService.acertarBaixaAlemDoSeparado(material.getId(), projetoId, apontada, separada);
        } catch (SaldoInsuficienteException e) {
            throw new RegraNegocioException("O apontado de " + material.getCodigo() + " passa do separado em "
                    + Quantidade.formatar(apontada.subtract(separada)) + " " + material.getUnidade()
                    + " e o estoque só tem " + Quantidade.formatar(e.getDisponivel()) + " " + material.getUnidade()
                    + " disponível para baixar. Confira o lançamento ou peça ao almoxarifado para conferir o estoque.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApontamentoHoras> horasDaOrdem(Long ordemId) {
        return horasRepository.findByOrdemServicoIdOrderByDataHoraAscIdAsc(ordemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApontamentoMaterial> materiaisDaOrdem(Long ordemId) {
        return materialRepository.listarPorOrdemComMaterial(ordemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApontamentoHoras> horasDoProjeto(Long projetoId) {
        return horasRepository.listarPorProjeto(projetoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApontamentoMaterial> materiaisDoProjeto(Long projetoId) {
        return materialRepository.listarPorProjetoComMaterial(projetoId);
    }

    /**
     * A ordem, aberta e com a instalação dela em andamento, com o projeto em EXECUTANDO_INSTALACAO
     * (Figura 5: "apontar horas e materiais").
     */
    private OrdemServico ordemEmExecucao(Long ordemId) {
        OrdemServico ordem = ordemServicoService.buscarParaUsuarioLogado(ordemId);
        if (ordem.getDataConclusao() != null) {
            throw new RegraNegocioException("A ordem de serviço " + ordem.getNumero() + " já foi concluída.");
        }
        ExecucaoRegras.exigirSituacao(ordem.getProjeto(), SituacaoProjeto.EXECUTANDO_INSTALACAO, "O apontamento é feito");
        if (ordem.getInstalacaoConcluidaEm() != null) {
            throw new RegraNegocioException("A instalação da ordem de serviço " + ordem.getNumero()
                    + " já foi concluída: os lançamentos dela estão fechados.");
        }
        return ordem;
    }

    private Usuario usuarioAtual() {
        return usuarioLogado.idAtual().map(usuarioRepository::getReferenceById).orElse(null);
    }
}
