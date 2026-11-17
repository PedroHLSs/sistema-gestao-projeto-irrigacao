package br.edu.iftm.irrigacao.apresentacao.web;

import java.math.BigDecimal;

import br.edu.iftm.irrigacao.dominio.comercial.Proposta;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Horas de mão de obra e desconto do orçamento (RF09, RF10), usados na prévia, ao gerar e ao
 * alterar a proposta.
 */
public class OrcamentoForm {

    @NotNull(message = "Informe as horas previstas.")
    @PositiveOrZero(message = "As horas não podem ser negativas.")
    @Digits(integer = 6, fraction = 2, message = "Use até 6 dígitos e 2 casas decimais.")
    private BigDecimal horasPrevistas = BigDecimal.ZERO;

    @NotNull(message = "Informe o desconto.")
    @PositiveOrZero(message = "O desconto não pode ser negativo.")
    @DecimalMax(value = "100", message = "O desconto vai até 100%.")
    @Digits(integer = 3, fraction = 2, message = "Use até 2 casas decimais.")
    private BigDecimal percentualDesconto = BigDecimal.ZERO;

    public OrcamentoForm() {
    }

    public OrcamentoForm(Proposta proposta) {
        this.horasPrevistas = proposta.getHorasPrevistas();
        this.percentualDesconto = proposta.getPercentualDesconto();
    }

    public BigDecimal getHorasPrevistas() {
        return horasPrevistas;
    }

    public void setHorasPrevistas(BigDecimal horasPrevistas) {
        this.horasPrevistas = horasPrevistas;
    }

    public BigDecimal getPercentualDesconto() {
        return percentualDesconto;
    }

    public void setPercentualDesconto(BigDecimal percentualDesconto) {
        this.percentualDesconto = percentualDesconto;
    }
}
