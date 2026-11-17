package br.edu.iftm.irrigacao.dominio.projeto.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.projeto.AgendaSemanal;
import br.edu.iftm.irrigacao.dominio.projeto.AgendaSemanal.DiaDaAgenda;
import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.LevantamentoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;

/**
 * Agendamento da visita e agenda semanal (RF02), com o relógio parado na quarta-feira 18/11/2026
 * às 10:00.
 */
@ExtendWith(MockitoExtension.class)
class LevantamentoServiceImplTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 18, 10, 0);

    @Mock
    private LevantamentoRepository levantamentoRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    private LevantamentoServiceImpl service;

    @BeforeEach
    void criarService() {
        ZoneId zona = ZoneId.systemDefault();
        service = new LevantamentoServiceImpl(levantamentoRepository, projetoRepository,
                Clock.fixed(AGORA.atZone(zona).toInstant(), zona));
    }

    private Projeto projetoEm(SituacaoProjeto situacao) {
        Projeto projeto = new Projeto();
        projeto.setId(1L);
        projeto.setCodigo("PRJ-2026-0001");
        projeto.setSituacao(situacao);
        return projeto;
    }

    private Levantamento levantamento(Projeto projeto, LocalDateTime dataAgendada) {
        Levantamento levantamento = new Levantamento();
        levantamento.setId(9L);
        levantamento.setProjeto(projeto);
        levantamento.setDataAgendada(dataAgendada);
        return levantamento;
    }

    // ---------- Agendamento ----------

    @Test
    @DisplayName("O agendamento cria o levantamento só com a data agendada, sem os segundos")
    void testAgendarCriaLevantamento() {
        Projeto projeto = projetoEm(SituacaoProjeto.EM_LEVANTAMENTO);
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projeto));
        when(levantamentoRepository.findByProjetoId(1L)).thenReturn(Optional.empty());
        when(levantamentoRepository.save(any(Levantamento.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Levantamento criado = service.agendarVisita(1L, LocalDateTime.of(2026, 11, 20, 8, 30, 45));

        assertThat(criado.getProjeto()).isSameAs(projeto);
        assertThat(criado.getDataAgendada()).isEqualTo(LocalDateTime.of(2026, 11, 20, 8, 30));
        assertThat(criado.getDataVisita()).isNull();
        assertThat(criado.getCultura()).isNull();
        assertThat(criado.getAreaIrrigadaHa()).isNull();
        assertThat(criado.getFonteAgua()).isNull();
        assertThat(criado.getVazaoFonte()).isNull();
        assertThat(criado.getFotos()).isEmpty();
        assertThat(criado.getLatitude()).isNull();
        assertThat(criado.getLongitude()).isNull();
    }

    @Test
    @DisplayName("Antes da visita, agendar de novo só troca a data do mesmo levantamento")
    void testReagendar() {
        Projeto projeto = projetoEm(SituacaoProjeto.EM_LEVANTAMENTO);
        Levantamento existente = levantamento(projeto, LocalDateTime.of(2026, 11, 19, 14, 0));
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projeto));
        when(levantamentoRepository.findByProjetoId(1L)).thenReturn(Optional.of(existente));
        when(levantamentoRepository.save(existente)).thenReturn(existente);

        Levantamento salvo = service.agendarVisita(1L, LocalDateTime.of(2026, 11, 23, 7, 0));

        assertThat(salvo).isSameAs(existente);
        assertThat(salvo.getId()).isEqualTo(9L);
        assertThat(salvo.getDataAgendada()).isEqualTo(LocalDateTime.of(2026, 11, 23, 7, 0));
    }

    @Test
    @DisplayName("Agendar para o minuto atual é aceito")
    void testAgendarParaAgora() {
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projetoEm(SituacaoProjeto.EM_LEVANTAMENTO)));
        when(levantamentoRepository.findByProjetoId(1L)).thenReturn(Optional.empty());
        when(levantamentoRepository.save(any(Levantamento.class))).thenAnswer(chamada -> chamada.getArgument(0));

        assertThat(service.agendarVisita(1L, AGORA).getDataAgendada()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("Data que já passou é recusada")
    void testAgendarNoPassado() {
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projetoEm(SituacaoProjeto.EM_LEVANTAMENTO)));
        when(levantamentoRepository.findByProjetoId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.agendarVisita(1L, AGORA.minusMinutes(1)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A visita não pode ser agendada para uma data e hora que já passaram.");
        verify(levantamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sem data o agendamento é recusado")
    void testAgendarSemData() {
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projetoEm(SituacaoProjeto.EM_LEVANTAMENTO)));
        when(levantamentoRepository.findByProjetoId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.agendarVisita(1L, null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Informe a data e a hora da visita.");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SituacaoProjeto.class, names = "EM_LEVANTAMENTO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Fora de EM_LEVANTAMENTO a visita não é agendada")
    void testAgendarForaDoLevantamento(SituacaoProjeto situacao) {
        Projeto projeto = projetoEm(situacao);
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projeto));
        when(levantamentoRepository.findByProjetoId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.agendarVisita(1L, AGORA.plusDays(1)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageStartingWith("A visita só pode ser agendada com o projeto em \"Em levantamento\".");
        assertThat(service.podeAgendarVisita(projeto, null)).isFalse();
        verify(levantamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Depois da visita realizada o agendamento não muda mais")
    void testAgendarDepoisDaVisita() {
        Projeto projeto = projetoEm(SituacaoProjeto.EM_LEVANTAMENTO);
        Levantamento realizado = levantamento(projeto, LocalDateTime.of(2026, 11, 17, 8, 0));
        realizado.setDataVisita(LocalDateTime.of(2026, 11, 17, 8, 20));
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projeto));
        when(levantamentoRepository.findByProjetoId(1L)).thenReturn(Optional.of(realizado));

        assertThatThrownBy(() -> service.agendarVisita(1L, AGORA.plusDays(1)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A visita do projeto PRJ-2026-0001 já foi realizada; o agendamento não muda mais.");
        assertThat(service.podeAgendarVisita(projeto, realizado)).isFalse();
    }

    @Test
    @DisplayName("Em EM_LEVANTAMENTO, sem visita realizada, o agendamento é permitido")
    void testPodeAgendar() {
        Projeto projeto = projetoEm(SituacaoProjeto.EM_LEVANTAMENTO);

        assertThat(service.podeAgendarVisita(projeto, null)).isTrue();
        assertThat(service.podeAgendarVisita(projeto, levantamento(projeto, AGORA))).isTrue();
    }

    // ---------- Agenda ----------

    @Test
    @DisplayName("A agenda vai de segunda a domingo, com as visitas separadas por dia e o dia de hoje marcado")
    void testAgendaDaSemana() {
        Projeto projeto = projetoEm(SituacaoProjeto.EM_LEVANTAMENTO);
        Levantamento quartaCedo = levantamento(projeto, LocalDateTime.of(2026, 11, 18, 7, 0));
        Levantamento quartaTarde = levantamento(projeto, LocalDateTime.of(2026, 11, 18, 15, 0));
        Levantamento domingo = levantamento(projeto, LocalDateTime.of(2026, 11, 22, 23, 59));
        when(levantamentoRepository.listarAgendadosEntre(LocalDateTime.of(2026, 11, 16, 0, 0),
                LocalDateTime.of(2026, 11, 23, 0, 0))).thenReturn(List.of(quartaCedo, quartaTarde, domingo));

        // Qualquer dia da semana leva à mesma agenda: aqui, a sexta-feira
        AgendaSemanal agenda = service.agendaDaSemana(LocalDate.of(2026, 11, 20));

        assertThat(agenda.getInicio()).isEqualTo(LocalDate.of(2026, 11, 16));
        assertThat(agenda.getFim()).isEqualTo(LocalDate.of(2026, 11, 22));
        assertThat(agenda.getSemanaAnterior()).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(agenda.getProximaSemana()).isEqualTo(LocalDate.of(2026, 11, 23));
        assertThat(agenda.getDias()).extracting(DiaDaAgenda::getData)
                .containsExactly(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 18),
                        LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 21),
                        LocalDate.of(2026, 11, 22));
        assertThat(agenda.getDias().get(2).getVisitas()).containsExactly(quartaCedo, quartaTarde);
        assertThat(agenda.getDias().get(6).getVisitas()).containsExactly(domingo);
        assertThat(agenda.getDias().get(0).getVisitas()).isEmpty();
        assertThat(agenda.getDias()).extracting(DiaDaAgenda::isHoje)
                .containsExactly(false, false, true, false, false, false, false);
        assertThat(agenda.getTotalDeVisitas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Sem dia informado a agenda é a da semana de hoje")
    void testAgendaDeHoje() {
        when(levantamentoRepository.listarAgendadosEntre(LocalDateTime.of(2026, 11, 16, 0, 0),
                LocalDateTime.of(2026, 11, 23, 0, 0))).thenReturn(List.of());

        AgendaSemanal agenda = service.agendaDaSemana(null);

        assertThat(agenda.getInicio()).isEqualTo(LocalDate.of(2026, 11, 16));
        assertThat(agenda.getTotalDeVisitas()).isZero();
    }

    @Test
    @DisplayName("Semana que atravessa a virada do ano")
    void testAgendaNaViradaDoAno() {
        when(levantamentoRepository.listarAgendadosEntre(LocalDateTime.of(2026, 12, 28, 0, 0),
                LocalDateTime.of(2027, 1, 4, 0, 0))).thenReturn(List.of());

        AgendaSemanal agenda = service.agendaDaSemana(LocalDate.of(2027, 1, 1));

        assertThat(agenda.getInicio()).isEqualTo(LocalDate.of(2026, 12, 28));
        assertThat(agenda.getFim()).isEqualTo(LocalDate.of(2027, 1, 3));
        assertThat(agenda.getDias()).noneMatch(DiaDaAgenda::isHoje);
    }
}
