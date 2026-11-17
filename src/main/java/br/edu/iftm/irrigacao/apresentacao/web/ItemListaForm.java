package br.edu.iftm.irrigacao.apresentacao.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Formulário de inclusão de item na lista técnica (RF05). O material é escolhido na busca, que
 * preenche {@code materialId}.
 */
public class ItemListaForm {

    @NotNull(message = "Escolha o material na busca.")
    private Long materialId;

    /** Código e descrição do material escolhido, só para a tela voltar com ele preenchido. */
    private String materialRotulo;

    @NotNull(message = "Informe a quantidade prevista.")
    @Positive(message = "A quantidade deve ser maior que zero.")
    @Digits(integer = 11, fraction = 3, message = "Use até 11 dígitos e 3 casas decimais.")
    private BigDecimal quantidadePrevista;

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getMaterialRotulo() {
        return materialRotulo;
    }

    public void setMaterialRotulo(String materialRotulo) {
        this.materialRotulo = materialRotulo;
    }

    public BigDecimal getQuantidadePrevista() {
        return quantidadePrevista;
    }

    public void setQuantidadePrevista(BigDecimal quantidadePrevista) {
        this.quantidadePrevista = quantidadePrevista;
    }
}
