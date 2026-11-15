package br.edu.iftm.irrigacao.dominio.acesso;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Usuário da sessão, montado no login a partir do {@code Usuario} (RNF04).
 *
 * <p>É o {@code User} do Spring Security com o id, o nome e o perfil a mais. Com o id guardado
 * aqui, a auditoria sabe quem está logado sem consultar o banco a cada gravação (RNF03). A única
 * authority é o nome do {@link Perfil}.
 */
public class UsuarioAutenticado extends User {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String nome;
    private final Perfil perfil;

    public UsuarioAutenticado(Usuario usuario) {
        super(usuario.getLogin(), usuario.getSenhaHash(),
                List.of(new SimpleGrantedAuthority(usuario.getPerfil().name())));
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.perfil = usuario.getPerfil();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Perfil getPerfil() {
        return perfil;
    }
}
