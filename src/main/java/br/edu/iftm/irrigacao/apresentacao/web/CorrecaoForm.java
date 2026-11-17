package br.edu.iftm.irrigacao.apresentacao.web;

import jakarta.validation.constraints.Size;

/** O que foi corrigido depois da medição reprovada (Figura 5, transição 12). Vai para o histórico. */
public class CorrecaoForm {

    @Size(max = 450, message = "Use até 450 caracteres.")
    private String descricao;

    public CorrecaoForm() {
    }

    public CorrecaoForm(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
