package br.edu.iftm.irrigacao.apresentacao.web;

import jakarta.validation.constraints.NotNull;

/**
 * Formulário de abertura do projeto (RF01): o produtor e a propriedade onde o sistema será
 * instalado. Código, data, responsável e situação são definidos pelo {@code ProjetoService}.
 */
public class AberturaProjetoForm {

    @NotNull(message = "Escolha o produtor.")
    private Long produtorId;

    @NotNull(message = "Escolha a propriedade.")
    private Long propriedadeId;

    public Long getProdutorId() {
        return produtorId;
    }

    public void setProdutorId(Long produtorId) {
        this.produtorId = produtorId;
    }

    public Long getPropriedadeId() {
        return propriedadeId;
    }

    public void setPropriedadeId(Long propriedadeId) {
        this.propriedadeId = propriedadeId;
    }
}
