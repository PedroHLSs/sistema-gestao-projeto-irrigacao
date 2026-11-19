package br.edu.iftm.irrigacao.apresentacao.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.AssertTrue;

/**
 * Período do orçado x realizado (RF22), com as duas datas inclusive. As duas são opcionais.
 */
public class PeriodoFiltro {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate de;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate ate;

    /** O erro aparece junto do campo {@code ate} na tela, pelo nome {@code periodoValido}. */
    @AssertTrue(message = "A data final não pode ser anterior à inicial.")
    public boolean isPeriodoValido() {
        return de == null || ate == null || !ate.isBefore(de);
    }

    public LocalDate getDe() {
        return de;
    }

    public void setDe(LocalDate de) {
        this.de = de;
    }

    public LocalDate getAte() {
        return ate;
    }

    public void setAte(LocalDate ate) {
        this.ate = ate;
    }
}
