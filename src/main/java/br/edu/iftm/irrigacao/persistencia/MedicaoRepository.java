package br.edu.iftm.irrigacao.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.iftm.irrigacao.dominio.execucao.Medicao;

public interface MedicaoRepository extends JpaRepository<Medicao, Long> {

    List<Medicao> findByProjetoIdOrderByDataHoraDesc(Long projetoId);

    /** Medições do projeto com o usuário, da mais nova para a mais antiga, desempatando pela ordem de registro (RF17). */
    @EntityGraph(attributePaths = "usuario")
    List<Medicao> findByProjetoIdOrderByDataHoraDescIdDesc(Long projetoId);

    /** A última medição do projeto (RF17). */
    Optional<Medicao> findFirstByProjetoIdOrderByDataHoraDesc(Long projetoId);
}
