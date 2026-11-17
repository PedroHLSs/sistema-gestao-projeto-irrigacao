package br.edu.iftm.irrigacao.dominio.projeto;

import java.time.LocalDate;
import java.util.List;

/**
 * Visitas técnicas de uma semana, de segunda a domingo, separadas por dia (RF02). Os sete dias
 * sempre aparecem, mesmo os que não têm visita.
 */
public class AgendaSemanal {

    private final LocalDate inicio;
    private final List<DiaDaAgenda> dias;

    /**
     * @param inicio a segunda-feira da semana
     * @param dias   os sete dias, de segunda a domingo
     */
    public AgendaSemanal(LocalDate inicio, List<DiaDaAgenda> dias) {
        this.inicio = inicio;
        this.dias = List.copyOf(dias);
    }

    /** Segunda-feira da semana. */
    public LocalDate getInicio() {
        return inicio;
    }

    /** Domingo da semana. */
    public LocalDate getFim() {
        return inicio.plusDays(6);
    }

    public LocalDate getSemanaAnterior() {
        return inicio.minusWeeks(1);
    }

    public LocalDate getProximaSemana() {
        return inicio.plusWeeks(1);
    }

    public List<DiaDaAgenda> getDias() {
        return dias;
    }

    public int getTotalDeVisitas() {
        return dias.stream().mapToInt(dia -> dia.getVisitas().size()).sum();
    }

    /** Um dia da agenda e as visitas marcadas nele, em ordem de horário. */
    public static class DiaDaAgenda {

        private final LocalDate data;
        private final boolean hoje;
        private final List<Levantamento> visitas;

        public DiaDaAgenda(LocalDate data, boolean hoje, List<Levantamento> visitas) {
            this.data = data;
            this.hoje = hoje;
            this.visitas = List.copyOf(visitas);
        }

        public LocalDate getData() {
            return data;
        }

        public boolean isHoje() {
            return hoje;
        }

        public List<Levantamento> getVisitas() {
            return visitas;
        }
    }
}
