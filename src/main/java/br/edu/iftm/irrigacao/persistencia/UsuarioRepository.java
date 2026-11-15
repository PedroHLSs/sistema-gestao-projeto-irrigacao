package br.edu.iftm.irrigacao.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Usado no login (RNF04). */
    Optional<Usuario> findByLogin(String login);

    boolean existsByLogin(String login);

    /** Usuários ativos de um perfil, como os técnicos que podem entrar numa ordem de serviço (RF15). */
    List<Usuario> findByPerfilAndAtivoTrueOrderByNome(Perfil perfil);
}
