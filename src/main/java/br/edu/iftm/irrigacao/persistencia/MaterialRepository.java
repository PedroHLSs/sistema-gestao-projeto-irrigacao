package br.edu.iftm.irrigacao.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.iftm.irrigacao.dominio.estoque.Material;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    Optional<Material> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Material> findAllByOrderByDescricao();
}
