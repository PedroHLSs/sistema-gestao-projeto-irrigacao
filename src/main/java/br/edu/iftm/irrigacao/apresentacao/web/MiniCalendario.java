package br.edu.iftm.irrigacao.apresentacao.web;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * O calendário do mês ao lado da agenda de visitas: semanas de segunda a domingo, com os dias de
 * fora do mês, o dia de hoje e a semana aberta na agenda marcados. São sempre seis semanas, para o
 * quadro não mudar de altura de um mês para o outro.
 */
public class MiniCalendario {

    private static final int SEMANAS = 6;

    private final YearMonth mes;
    private final List<Semana> semanas;

    private MiniCalendario(YearMonth mes, List<Semana> semanas) {
        this.mes = mes;
        this.semanas = List.copyOf(semanas);
    }

    /**
     * @param mes          o mês mostrado
     * @param semanaAberta qualquer dia da semana aberta na agenda
     * @param hoje         a data de hoje
     */
    public static MiniCalendario de(YearMonth mes, LocalDate semanaAberta, LocalDate hoje) {
        LocalDate primeiraSegunda = mes.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate segundaAberta = semanaAberta.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<Semana> semanas = new ArrayList<>();
        for (int i = 0; i < SEMANAS; i++) {
            LocalDate segunda = primeiraSegunda.plusWeeks(i);
            List<Dia> dias = new ArrayList<>();
            for (int d = 0; d < 7; d++) {
                LocalDate data = segunda.plusDays(d);
                dias.add(new Dia(data, YearMonth.from(data).equals(mes), data.equals(hoje)));
            }
            semanas.add(new Semana(segunda, segunda.equals(segundaAberta), dias));
        }
        return new MiniCalendario(mes, semanas);
    }

    /**
     * O mês que o calendário mostra quando ninguém escolheu outro: o da quinta-feira da semana, que
     * é o mês com mais dias dela (a semana de 30/11 a 06/12 fica em dezembro).
     */
    public static YearMonth mesDaSemana(LocalDate semanaAberta) {
        return YearMonth.from(semanaAberta.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(3));
    }

    /** Primeiro dia do mês mostrado, para o título. */
    public LocalDate getPrimeiroDia() {
        return mes.atDay(1);
    }

    public YearMonth getMesAnterior() {
        return mes.minusMonths(1);
    }

    public YearMonth getProximoMes() {
        return mes.plusMonths(1);
    }

    public List<Semana> getSemanas() {
        return semanas;
    }

    /**
     * @param inicio a segunda-feira
     * @param aberta se é a semana aberta na agenda
     */
    public record Semana(LocalDate inicio, boolean aberta, List<Dia> dias) {
    }

    /**
     * @param doMes se o dia é do mês mostrado; os outros completam a primeira e a última semana
     */
    public record Dia(LocalDate data, boolean doMes, boolean hoje) {
    }
}
