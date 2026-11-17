package br.edu.iftm.irrigacao.apresentacao.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Formulário de cotação de um material faltante com um fornecedor (RF08).
 */
public class CotacaoForm {

    @NotNull(message = "Escolha o material.")
    private Long materialId;

    @NotBlank(message = "Informe o fornecedor.")
    @Size(max = 120, message = "O fornecedor pode ter até 120 caracteres.")
    private String fornecedor;

    @NotNull(message = "Informe o preço unitário.")
    @Positive(message = "O preço deve ser maior que zero.")
    @Digits(integer = 12, fraction = 2, message = "Use até 12 dígitos e 2 casas decimais.")
    private BigDecimal precoUnitario;

    @PositiveOrZero(message = "O prazo não pode ser negativo.")
    private Integer prazoDias;

    @NotNull(message = "Informe a quantidade.")
    @Positive(message = "A quantidade deve ser maior que zero.")
    @Digits(integer = 11, fraction = 3, message = "Use até 11 dígitos e 3 casas decimais.")
    private BigDecimal quantidade;

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(String fornecedor) {
        this.fornecedor = fornecedor;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public Integer getPrazoDias() {
        return prazoDias;
    }

    public void setPrazoDias(Integer prazoDias) {
        this.prazoDias = prazoDias;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }
}
