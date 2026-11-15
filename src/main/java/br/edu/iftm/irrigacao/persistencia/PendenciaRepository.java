package br.edu.iftm.irrigacao.persistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.iftm.irrigacao.dominio.execucao.Pendencia;

public interface PendenciaRepository extends JpaRepository<Pendencia, Long> {

    List<Pendencia> findByProjetoIdOrderByDataRegistroDesc(Long projetoId);

    /** Pendências em aberto do projeto (RF18). */
    List<Pendencia> findByProjetoIdAndResolvidaFalse(Long projetoId);
}
