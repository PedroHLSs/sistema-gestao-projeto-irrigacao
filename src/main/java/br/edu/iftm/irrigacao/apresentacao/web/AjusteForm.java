package br.edu.iftm.irrigacao.apresentacao.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Formulário do ajuste de saldo (RF07): o saldo contado no almoxarifado. A diferença para o saldo
 * do sistema é calculada pelo {@code EstoqueService}.
 */
public class AjusteForm {

    @NotNull(message = "Informe o saldo contado.")
    @PositiveOrZero(message = "O saldo não pode ser negativo.")
    @Digits(integer = 11, fraction = 3, message = "Use até 11 dígitos e 3 casas decimais.")
    private BigDecimal novoSaldo;

    public AjusteForm() {
    }

    public AjusteForm(BigDecimal novoSaldo) {
        this.novoSaldo = novoSaldo;
    }

    public BigDecimal getNovoSaldo() {
        return novoSaldo;
    }

    public void setNovoSaldo(BigDecimal novoSaldo) {
        this.novoSaldo = novoSaldo;
    }
}
