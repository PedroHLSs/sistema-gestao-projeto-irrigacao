package br.edu.iftm.irrigacao.dominio.projeto.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.comercial.PropostaService;
import br.edu.iftm.irrigacao.dominio.comercial.SituacaoProposta;
import br.edu.iftm.irrigacao.dominio.estoque.SituacaoCotacao;
import br.edu.iftm.irrigacao.dominio.projeto.OrcadoRealizadoService;
import br.edu.iftm.irrigacao.dominio.projeto.PainelInicial;
import br.edu.iftm.irrigacao.dominio.projeto.PainelInicial.ProjetosNaSituacao;
import br.edu.iftm.irrigacao.dominio.projeto.PainelService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.CotacaoRepository;
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
    private final PropostaService propostaService;
    private final OrcadoRealizadoService orcadoRealizadoService;
    private final Clock clock;

    public PainelServiceImpl(ProjetoRepository projetoRepository, PropostaRepository propostaRepository,
            PendenciaRepository pendenciaRepository, LevantamentoRepository levantamentoRepository,
            CotacaoRepository cotacaoRepository, OrdemServicoRepository ordemRepository,
            PropostaService propostaService, OrcadoRealizadoService orcadoRealizadoService, Clock clock) {
        this.projetoRepository = projetoRepository;
        this.propostaRepository = propostaRepository;
        this.pendenciaRepository = pendenciaRepository;
        this.levantamentoRepository = levantamentoRepository;
        this.cotacaoRepository = cotacaoRepository;
        this.ordemRepository = ordemRepository;
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
        return new PainelInicial.Estoque(cotacaoRepository.contarNaSituacao(SituacaoCotacao.REQUISITADA),
                projetoRepository.contarSemSeparacaoConferida(SituacaoProjeto.PREPARANDO_MATERIAL),
                projetoRepository.contarPorSobras(SituacaoProjeto.FINALIZANDO_EXECUCAO, false));
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
