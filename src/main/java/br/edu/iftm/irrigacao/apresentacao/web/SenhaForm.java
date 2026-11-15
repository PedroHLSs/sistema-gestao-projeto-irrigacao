package br.edu.iftm.irrigacao.apresentacao.web;

import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulário de redefinição de senha pelo administrador (RF23).
 */
public class SenhaForm {

    static final String MENSAGEM_TAMANHO = "A senha deve ter de " + UsuarioService.SENHA_TAMANHO_MINIMO + " a "
            + UsuarioService.SENHA_TAMANHO_MAXIMO + " caracteres.";

    @NotNull(message = MENSAGEM_TAMANHO)
    @Size(min = UsuarioService.SENHA_TAMANHO_MINIMO, max = UsuarioService.SENHA_TAMANHO_MAXIMO,
            message = MENSAGEM_TAMANHO)
    private String novaSenha;

    public String getNovaSenha() {
        return novaSenha;
    }

    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }
}
