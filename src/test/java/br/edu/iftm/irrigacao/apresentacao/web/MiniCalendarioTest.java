package br.edu.iftm.irrigacao.apresentacao.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.iftm.irrigacao.apresentacao.web.MiniCalendario.Dia;
import br.edu.iftm.irrigacao.apresentacao.web.MiniCalendario.Semana;

/**
 * Calendário do mês ao lado da agenda de visitas.
 */
class MiniCalendarioTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 19);

    @Test
    @DisplayName("Seis semanas de segunda a domingo, com os dias de fora do mês, hoje e a semana aberta marcados")
    void testNovembroDe2026() {
        MiniCalendario mini = MiniCalendario.de(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 18), HOJE);

        List<Semana> semanas = mini.getSemanas();
        assertThat(semanas).hasSize(6).allSatisfy(semana -> {
            assertThat(semana.dias()).hasSize(7);
            assertThat(semana.inicio().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        });
        // 01/11/2026 é domingo: a primeira semana começa em 26/10
        assertThat(semanas.get(0).inicio()).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(semanas.get(0).dias()).extracting(Dia::doMes)
                .containsExactly(false, false, false, false, false, false, true);
        assertThat(semanas.get(5).dias().get(6).data()).isEqualTo(LocalDate.of(2026, 12, 6));

        assertThat(semanas).filteredOn(Semana::aberta).extracting(Semana::inicio)
                .containsExactly(LocalDate.of(2026, 11, 16));
        assertThat(semanas.stream().flatMap(semana -> semana.dias().stream()).filter(Dia::hoje))
                .extracting(Dia::data).containsExactly(HOJE);

        assertThat(mini.getPrimeiroDia()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(mini.getMesAnterior()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(mini.getProximoMes()).isEqualTo(YearMonth.of(2026, 12));
    }

    @Test
    @DisplayName("Em outro mês, a semana aberta e hoje não aparecem marcados")
    void testOutroMes() {
        MiniCalendario mini = MiniCalendario.de(YearMonth.of(2027, 2), LocalDate.of(2026, 11, 18), HOJE);

        assertThat(mini.getSemanas()).noneMatch(Semana::aberta);
        assertThat(mini.getSemanas()).flatExtracting(Semana::dias).noneMatch(Dia::hoje);
        assertThat(mini.getProximoMes()).isEqualTo(YearMonth.of(2027, 3));
    }

    @Test
    @DisplayName("O mês da semana é o da quinta-feira dela, também na virada do ano")
    void testMesDaSemana() {
        assertThat(MiniCalendario.mesDaSemana(LocalDate.of(2026, 11, 18))).isEqualTo(YearMonth.of(2026, 11));
        // 30/11 a 06/12: a quinta-feira é 03/12
        assertThat(MiniCalendario.mesDaSemana(LocalDate.of(2026, 11, 30))).isEqualTo(YearMonth.of(2026, 12));
        // 28/12/2026 a 03/01/2027: a quinta-feira é 31/12
        assertThat(MiniCalendario.mesDaSemana(LocalDate.of(2027, 1, 2))).isEqualTo(YearMonth.of(2026, 12));
    }
}
