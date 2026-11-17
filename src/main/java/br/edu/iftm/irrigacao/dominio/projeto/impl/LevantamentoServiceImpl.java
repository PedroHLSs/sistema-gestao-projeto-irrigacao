package br.edu.iftm.irrigacao.dominio.projeto.impl;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.projeto.AgendaSemanal;
import br.edu.iftm.irrigacao.dominio.projeto.AgendaSemanal.DiaDaAgenda;
import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.LevantamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.LevantamentoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;

/**
 * Agendamento da visita técnica e agenda semanal (RF02).
 */
@Service
public class LevantamentoServiceImpl implements LevantamentoService {

    private final LevantamentoRepository levantamentoRepository;
    private final ProjetoRepository projetoRepository;
    private final Clock clock;

    public LevantamentoServiceImpl(LevantamentoRepository levantamentoRepository, ProjetoRepository projetoRepository,
            Clock clock) {
        this.levantamentoRepository = levantamentoRepository;
        this.projetoRepository = projetoRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Levantamento> buscarPorProjeto(Long projetoId) {
        return levantamentoRepository.findByProjetoId(projetoId);
    }

    @Override
    public boolean podeAgendarVisita(Projeto projeto, Levantamento levantamento) {
        return projeto.getSituacao() == SituacaoProjeto.EM_LEVANTAMENTO
                && (levantamento == null || levantamento.getDataVisita() == null);
    }

    @Override
    @Transactional
    public Levantamento agendarVisita(Long projetoId, LocalDateTime dataAgendada) {
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
        Levantamento levantamento = levantamentoRepository.findByProjetoId(projetoId).orElse(null);
        if (projeto.getSituacao() != SituacaoProjeto.EM_LEVANTAMENTO) {
            throw new RegraNegocioException("A visita só pode ser agendada com o projeto em \""
                    + SituacaoProjeto.EM_LEVANTAMENTO.getRotulo() + "\". O projeto " + projeto.getCodigo()
                    + " está em \"" + projeto.getSituacao().getRotulo() + "\".");
        }
        if (!podeAgendarVisita(projeto, levantamento)) {
            throw new RegraNegocioException("A visita do projeto " + projeto.getCodigo()
                    + " já foi realizada; o agendamento não muda mais.");
        }
        if (dataAgendada == null) {
            throw new RegraNegocioException("Informe a data e a hora da visita.");
        }
        LocalDateTime data = dataAgendada.truncatedTo(ChronoUnit.MINUTES);
        if (data.isBefore(LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES))) {
            throw new RegraNegocioException("A visita não pode ser agendada para uma data e hora que já passaram.");
        }
        if (levantamento == null) {
            levantamento = new Levantamento();
            levantamento.setProjeto(projeto);
        }
        levantamento.setDataAgendada(data);
        return levantamentoRepository.save(levantamento);
    }

    @Override
    @Transactional(readOnly = true)
    public AgendaSemanal agendaDaSemana(LocalDate dia) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate inicio = (dia == null ? hoje : dia).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate proximaSegunda = inicio.plusWeeks(1);

        Map<LocalDate, List<Levantamento>> porDia = new LinkedHashMap<>();
        for (LocalDate data = inicio; data.isBefore(proximaSegunda); data = data.plusDays(1)) {
            porDia.put(data, new ArrayList<>());
        }
        for (Levantamento visita : levantamentoRepository.listarAgendadosEntre(inicio.atStartOfDay(),
                proximaSegunda.atStartOfDay())) {
            porDia.get(visita.getDataAgendada().toLocalDate()).add(visita);
        }

        List<DiaDaAgenda> dias = porDia.entrySet().stream()
                .map(e -> new DiaDaAgenda(e.getKey(), e.getKey().equals(hoje), e.getValue()))
                .toList();
        return new AgendaSemanal(inicio, dias);
    }
}
