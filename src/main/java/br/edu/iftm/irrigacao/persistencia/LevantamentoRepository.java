package br.edu.iftm.irrigacao.persistencia;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;

public interface LevantamentoRepository extends JpaRepository<Levantamento, Long> {

    Optional<Levantamento> findByProjetoId(Long projetoId);
}
