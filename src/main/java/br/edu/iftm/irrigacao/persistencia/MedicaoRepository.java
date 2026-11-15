package br.edu.iftm.irrigacao.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.iftm.irrigacao.dominio.execucao.Medicao;

public interface MedicaoRepository extends JpaRepository<Medicao, Long> {

    List<Medicao> findByProjetoIdOrderByDataHoraDesc(Long projetoId);

    /** A última medição do projeto (RF17). */
    Optional<Medicao> findFirstByProjetoIdOrderByDataHoraDesc(Long projetoId);
}
