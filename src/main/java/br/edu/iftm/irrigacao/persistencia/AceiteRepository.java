package br.edu.iftm.irrigacao.persistencia;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.iftm.irrigacao.dominio.execucao.Aceite;

public interface AceiteRepository extends JpaRepository<Aceite, Long> {

    @EntityGraph(attributePaths = "usuario")
    Optional<Aceite> findByProjetoId(Long projetoId);
}
