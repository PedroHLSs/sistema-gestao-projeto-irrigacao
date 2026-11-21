package br.edu.iftm.irrigacao.dominio.projeto.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.comercial.PropostaService;
import br.edu.iftm.irrigacao.dominio.comercial.SituacaoProposta;
import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.ProgressoSeparacao;
import br.edu.iftm.irrigacao.dominio.estoque.SituacaoCotacao;
import br.edu.iftm.irrigacao.dominio.projeto.FilaEngenharia;
import br.edu.iftm.irrigacao.dominio.projeto.FilaEstoque;
import br.edu.iftm.irrigacao.dominio.projeto.FilaEstoque.SeparacaoNaFila;
import br.edu.iftm.irrigacao.dominio.projeto.OrcadoRealizadoService;
import br.edu.iftm.irrigacao.dominio.projeto.PainelInicial;
import br.edu.iftm.irrigacao.dominio.projeto.PainelInicial.ProjetosNaSituacao;
import br.edu.iftm.irrigacao.dominio.projeto.PainelService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.CotacaoRepository;
import br.edu.iftm.irrigacao.persistencia.ItemProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.LevantamentoRepository;
import br.edu.iftm.irrigacao.persistencia.OrdemServicoRepository;
import br.edu.iftm.irrigacao.persistencia.PendenciaRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.PropostaRepository;

/**
 * Painel inicial por perfil.
 */
@Service
public class PainelServiceImpl implements PainelService {

    private final ProjetoRepository projetoRepository;
    private final PropostaRepository propostaRepository;
    private final PendenciaRepository pendenciaRepository;
    private final LevantamentoRepository levantamentoRepository;
    private final CotacaoRepository cotacaoRepository;
    private final OrdemServicoRepository ordemRepository;
    private final ItemProjetoRepository itemProjetoRepository;
    private final PropostaService propostaService;
    private final OrcadoRealizadoService orcadoRealizadoService;
    private final Clock clock;

    public PainelServiceImpl(ProjetoRepository projetoRepository, PropostaRepository propostaRepository,
            PendenciaRepository pendenciaRepository, LevantamentoRepository levantamentoRepository,
            CotacaoRepository cotacaoRepository, OrdemServicoRepository ordemRepository,
            ItemProjetoRepository itemProjetoRepository, PropostaService propostaService, OrcadoRealizadoService orcadoRealizadoService, Clock clock) {
        this.projetoRepository = projetoRepository;
        this.propostaRepository = propostaRepository;
        this.pendenciaRepository = pendenciaRepository;
        this.levantamentoRepository = levantamentoRepository;
        this.cotacaoRepository = cotacaoRepository;
        this.ordemRepository = ordemRepository;
        this.itemProjetoRepository = itemProjetoRepository;
        this.propostaService = propostaService;
        this.orcadoRealizadoService = orcadoRealizadoService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PainelInicial montar(Perfil perfil, Long usuarioId) {
        if (perfil == null) {
            return new PainelInicial(null, null, null, null, null, null);
        }
        return switch (perfil) {
            case VENDEDOR -> new PainelInicial(vendedor(usuarioId), null, null, null, null, null);
            case PROJETISTA -> new PainelInicial(null, projetista(), null, null, null, null);
            case ALMOXARIFE, COMPRADOR -> new PainelInicial(null, null, estoque(), null, null, null);
            case TECNICO_CAMPO -> new PainelInicial(null, null, null, tecnico(usuarioId), null, null);
            case FINANCEIRO -> new PainelInicial(null, null, null, null, financeiro(), null);
            case DIRETORIA -> new PainelInicial(null, null, null, null, null, diretoria());
            case ADMINISTRADOR -> new PainelInicial(null, projetista(), estoque(), null, financeiro(), diretoria());
        };
    }

    @Override
    @Transactional(readOnly = true)
    public FilaEngenharia filaEngenharia() {
        return new FilaEngenharia(levantamentoRepository.listarCompletosNaSituacao(SituacaoProjeto.EM_LEVANTAMENTO),
                projetoRepository.pesquisar(EnumSet.of(SituacaoProjeto.PROGRAMANDO_EXECUCAO), "%"),
                projetoRepository.pesquisar(EnumSet.of(SituacaoProjeto.EXECUTANDO_INSTALACAO), "%"));
    }

    @Override
    @Transactional(readOnly = true)
    public FilaEstoque filaEstoque() {
        List<Projeto> aSeparar = projetoRepository.pesquisar(EnumSet.of(SituacaoProjeto.PREPARANDO_MATERIAL), "%");
        // Execução liberada e antes do aceite: a separação continua aberta (separação parcial, G5)
        EnumSet<SituacaoProjeto> emExecucao = EnumSet.noneOf(SituacaoProjeto.class);
        for (SituacaoProjeto situacao : SituacaoProjeto.values()) {
            if (situacao.isMaterialEmAndamento() && situacao != SituacaoProjeto.PREPARANDO_MATERIAL) {
                emExecucao.add(situacao);
            }
        }
        List<Projeto> liberados = projetoRepository.pesquisar(emExecucao, "%");
        Map<Long, List<ItemProjeto>> itens = itensDosProjetos(Stream.concat(aSeparar.stream(), liberados.stream())
                .map(Projeto::getId)
                .toList());
        List<SeparacaoNaFila> aContinuar = liberados.stream()
                .map(projeto -> separacao(projeto, itens))
                .filter(separacao -> !separacao.progresso().isCompleta())
                .toList();
        List<Projeto> sobras = projetoRepository.pesquisar(EnumSet.of(SituacaoProjeto.FINALIZANDO_EXECUCAO), "%").stream()
                .filter(projeto -> !projeto.isSobrasDevolvidas())
                .toList();
        return new FilaEstoque(aSeparar.stream().map(projeto -> separacao(projeto, itens)).toList(), aContinuar, sobras);
    }

    private Map<Long, List<ItemProjeto>> itensDosProjetos(List<Long> projetoIds) {
        if (projetoIds.isEmpty()) {
            return Map.of();
        }
        return itemProjetoRepository.listarDosProjetos(projetoIds).stream()
                .collect(Collectors.groupingBy(item -> item.getProjeto().getId()));
    }

    private static SeparacaoNaFila separacao(Projeto projeto, Map<Long, List<ItemProjeto>> itens) {
        return new SeparacaoNaFila(projeto, new ProgressoSeparacao(itens.getOrDefault(projeto.getId(), List.of())));
    }

    private PainelInicial.Vendedor vendedor(Long usuarioId) {
        if (usuarioId == null) {
            return new PainelInicial.Vendedor(List.of(), 0, 0);
        }
        List<ProjetosNaSituacao> porSituacao = projetoRepository.contarDoResponsavelPorSituacao(usuarioId).stream()
                .map(total -> new ProjetosNaSituacao(total.getSituacao(), total.getTotal()))
                .sorted(Comparator.comparing(ProjetosNaSituacao::situacao))
                .toList();
        return new PainelInicial.Vendedor(porSituacao, propostaService.contarAlertasDeVencimento(usuarioId),
                pendenciaRepository.contarAbertasDoResponsavel(usuarioId));
    }

    private PainelInicial.Projetista projetista() {
        return new PainelInicial.Projetista(
                levantamentoRepository.contarCompletosNaSituacao(SituacaoProjeto.EM_LEVANTAMENTO),
                projetoRepository.contarNaSituacao(SituacaoProjeto.PROGRAMANDO_EXECUCAO));
    }

    private PainelInicial.Estoque estoque() {
        FilaEstoque fila = filaEstoque();
        return new PainelInicial.Estoque(cotacaoRepository.contarNaSituacao(SituacaoCotacao.REQUISITADA),
                fila.separacoesPendentes(), fila.sobrasAReceber().size());
    }

    private PainelInicial.Tecnico tecnico(Long usuarioId) {
        return new PainelInicial.Tecnico(
                usuarioId == null ? 0 : ordemRepository.contarAbertasDoTecnicoNoDia(usuarioId, LocalDate.now(clock)));
    }

    private PainelInicial.Financeiro financeiro() {
        return new PainelInicial.Financeiro(projetoRepository.contarPorSobras(SituacaoProjeto.FINALIZANDO_EXECUCAO, true),
                projetoRepository.contarPorSobras(SituacaoProjeto.FINALIZANDO_EXECUCAO, false));
    }

    private PainelInicial.Diretoria diretoria() {
        return new PainelInicial.Diretoria(propostaRepository.contarNaSituacao(SituacaoProposta.AGUARDANDO_DIRETORIA),
                orcadoRealizadoService.resumo());
    }
}
