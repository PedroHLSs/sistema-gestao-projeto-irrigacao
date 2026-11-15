package br.edu.iftm.irrigacao.persistencia;

import java.util.List;
import java.util.Optional;

import org.hibernate.jpa.HibernateHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import jakarta.persistence.QueryHint;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLogin(String login);

    /** Usado no login: o usuário inativo não é encontrado (RNF04). */
    Optional<Usuario> findByLoginAndAtivoTrue(String login);

    /**
     * Id do usuário de um login, para a auditoria (RNF03). A consulta roda sem o flush automático
     * porque a auditoria a chama de dentro de um flush, quando uma entidade é alterada.
     */
    @Query("select u.id from Usuario u where u.login = :login")
    @QueryHints(@QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    Optional<Long> findIdByLogin(@Param("login") String login);

    boolean existsByLogin(String login);

    /** Login usado por outro usuário que não o do id informado (RF23). */
    boolean existsByLoginAndIdNot(String login, Long id);

    boolean existsByPerfil(Perfil perfil);

    /** Tela de gestão de usuários (RF23). */
    List<Usuario> findAllByOrderByNome();

    /** Usuários ativos de um perfil, como os técnicos que podem entrar numa ordem de serviço (RF15). */
    List<Usuario> findByPerfilAndAtivoTrueOrderByNome(Perfil perfil);
}
