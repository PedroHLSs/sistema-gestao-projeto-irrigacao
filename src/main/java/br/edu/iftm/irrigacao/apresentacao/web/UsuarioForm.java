package br.edu.iftm.irrigacao.apresentacao.web;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Formulário de edição do usuário: nome, login e perfil (RF23). A senha não entra aqui; ela é
 * definida na inclusão ({@link UsuarioNovoForm}) e trocada pelo {@link SenhaForm}.
 */
@LoginUnico
public class UsuarioForm {

    /** Vazio na inclusão. Na edição vem do {@code {id}} da URL. */
    private Long id;

    @NotBlank(message = "Informe o nome.")
    @Size(max = 120, message = "O nome pode ter até 120 caracteres.")
    private String nome;

    @NotBlank(message = "Informe o login.")
    @Pattern(regexp = "|[a-z0-9._-]{3,60}",
            message = "Use de 3 a 60 caracteres: letras minúsculas sem acento, números, ponto, hífen ou sublinhado.")
    private String login;

    @NotNull(message = "Escolha o perfil.")
    private Perfil perfil;

    public UsuarioForm() {
    }

    public UsuarioForm(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.login = usuario.getLogin();
        this.perfil = usuario.getPerfil();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }
}
