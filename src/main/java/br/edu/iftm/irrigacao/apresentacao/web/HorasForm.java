package br.edu.iftm.irrigacao.apresentacao.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** Horas trabalhadas apontadas na ordem de serviço (RF16). */
public class HorasForm {

    @NotNull(message = "Informe as horas.")
    @DecimalMin(value = "0.01", message = "As horas devem ser maiores que zero.")
    @Digits(integer = 6, fraction = 2, message = "Use até 2 casas decimais.")
    private BigDecimal horas;

    public HorasForm() {
    }

    public HorasForm(BigDecimal horas) {
        this.horas = horas;
    }

    public BigDecimal getHoras() {
        return horas;
    }

    public void setHoras(BigDecimal horas) {
        this.horas = horas;
    }
}
