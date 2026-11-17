package br.edu.iftm.irrigacao.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.iftm.irrigacao.dominio.execucao.Apontamento;

/**
 * Repositório da classe base. Devolve os apontamentos de horas e de material juntos, cada um
 * na sua subclasse (RF16).
 */
public interface ApontamentoRepository extends JpaRepository<Apontamento, Long> {

    List<Apontamento> findByOrdemServicoIdOrderByDataHora(Long ordemServicoId);

    /** O último lançamento da ordem, de horas ou de material, para a exclusão (RF16). */
    Optional<Apontamento> findFirstByOrdemServicoIdOrderByDataHoraDescIdDesc(Long ordemServicoId);
}
