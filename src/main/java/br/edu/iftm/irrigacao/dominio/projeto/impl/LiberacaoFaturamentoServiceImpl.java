package br.edu.iftm.irrigacao.dominio.projeto.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.projeto.Garantia;
import br.edu.iftm.irrigacao.dominio.projeto.LiberacaoFaturamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoParaLiberar;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.AceiteRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;

/**
 * Liberação do faturamento (RF20) e garantia (RF21).
 */
@Service
public class LiberacaoFaturamentoServiceImpl implements LiberacaoFaturamentoService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProjetoRepository projetoRepository;
    private final AceiteRepository aceiteRepository;
    private final ProjetoService projetoService;
    private final Clock clock;

    public LiberacaoFaturamentoServiceImpl(ProjetoRepository projetoRepository, AceiteRepository aceiteRepository,
            ProjetoService projetoService, Clock clock) {
        this.projetoRepository = projetoRepository;
        this.aceiteRepository = aceiteRepository;
        this.projetoService = projetoService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjetoParaLiberar> projetosParaLiberar() {
        return projetoRepository.pesquisar(EnumSet.of(SituacaoProjeto.FINALIZANDO_EXECUCAO), "%").stream()
                .map(projeto -> new ProjetoParaLiberar(projeto, motivoDoBloqueio(projeto).orElse(null)))
                .toList();
    }

    @Override
    public Optional<String> motivoDoBloqueio(Projeto projeto) {
        if (projeto.getSituacao() != SituacaoProjeto.FINALIZANDO_EXECUCAO) {
            return Optional.of("O faturamento é liberado com o projeto em \""
                    + SituacaoProjeto.FINALIZANDO_EXECUCAO.getRotulo() + "\". O projeto " + projeto.getCodigo()
                    + " está em \"" + projeto.getSituacao().getRotulo() + "\".");
        }
        if (!projeto.isSobrasDevolvidas()) {
            return Optional.of("As sobras do projeto " + projeto.getCodigo()
                    + " ainda não foram resolvidas pelo almoxarifado.");
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDate> dataDoAceite(Long projetoId) {
        return aceiteRepository.findByProjetoId(projetoId).map(aceite -> aceite.getDataHora().toLocalDate());
    }

    @Override
    @Transactional
    public Projeto liberar(Long projetoId, LocalDate dataInicioGarantia, Integer prazoGarantiaMeses) {
        // Linha travada antes de ler a situação: dois envios juntos não liberam o projeto duas vezes
        Projeto projeto = projetoRepository.buscarParaAlterarSituacao(projetoId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
        motivoDoBloqueio(projeto).ifPresent(motivo -> {
            throw new RegraNegocioException(motivo);
        });
        LocalDate hoje = LocalDate.now(clock);
        exigirGarantiaValida(projetoId, dataInicioGarantia, prazoGarantiaMeses, hoje);

        projeto.setDataLiberacaoFaturamento(hoje);
        projeto.setDataInicioGarantia(dataInicioGarantia);
        projeto.setPrazoGarantiaMeses(prazoGarantiaMeses);
        LocalDate fim = dataInicioGarantia.plusMonths(prazoGarantiaMeses);
        return projetoService.transitar(projetoId, SituacaoProjeto.ENCERRADO, "Faturamento liberado. Garantia de "
                + prazoGarantiaMeses + (prazoGarantiaMeses == 1 ? " mês" : " meses") + ", de "
                + DATA.format(dataInicioGarantia) + " até " + DATA.format(fim) + ".");
    }

    @Override
    public Optional<Garantia> garantia(Projeto projeto) {
        return Garantia.doProjeto(projeto, LocalDate.now(clock));
    }

    private void exigirGarantiaValida(Long projetoId, LocalDate inicio, Integer prazoMeses, LocalDate hoje) {
        if (inicio == null) {
            throw new RegraNegocioException("Informe a data de início da garantia.");
        }
        if (prazoMeses == null || prazoMeses < 1 || prazoMeses > PRAZO_MAXIMO_MESES) {
            throw new RegraNegocioException("Informe o prazo da garantia, de 1 a " + PRAZO_MAXIMO_MESES + " meses.");
        }
        if (inicio.isAfter(hoje)) {
            throw new RegraNegocioException("O início da garantia não pode ser depois de hoje.");
        }
        dataDoAceite(projetoId).filter(inicio::isBefore).ifPresent(aceite -> {
            throw new RegraNegocioException("O início da garantia não pode ser antes do aceite, assinado em "
                    + DATA.format(aceite) + ".");
        });
    }
}
