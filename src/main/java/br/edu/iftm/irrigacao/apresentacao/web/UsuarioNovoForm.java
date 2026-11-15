package br.edu.iftm.irrigacao.apresentacao.web;

import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulário de inclusão do usuário: os campos da edição mais a senha inicial (RF23).
 */
public class UsuarioNovoForm extends UsuarioForm {

    @NotNull(message = SenhaForm.MENSAGEM_TAMANHO)
    @Size(min = UsuarioService.SENHA_TAMANHO_MINIMO, max = UsuarioService.SENHA_TAMANHO_MAXIMO,
            message = SenhaForm.MENSAGEM_TAMANHO)
    private String senha;

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}
