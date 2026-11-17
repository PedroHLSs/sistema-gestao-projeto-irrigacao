package br.edu.iftm.irrigacao.apresentacao.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * Material consumido apontado na ordem de serviço (RF16). Que o material é da lista técnica, quem
 * confere é o {@code ApontamentoService}.
 */
public class ApontamentoMaterialForm {

    @NotNull(message = "Escolha o material.")
    private Long materialId;

    @NotNull(message = "Informe a quantidade.")
    @DecimalMin(value = "0.001", message = "A quantidade deve ser maior que zero.")
    @Digits(integer = 11, fraction = 3, message = "Use até 3 casas decimais.")
    private BigDecimal quantidade;

    public ApontamentoMaterialForm() {
    }

    public ApontamentoMaterialForm(Long materialId, BigDecimal quantidade) {
        this.materialId = materialId;
        this.quantidade = quantidade;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }
}
