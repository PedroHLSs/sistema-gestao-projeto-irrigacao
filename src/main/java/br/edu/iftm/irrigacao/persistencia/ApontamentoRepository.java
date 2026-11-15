package br.edu.iftm.irrigacao.persistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.iftm.irrigacao.dominio.execucao.Apontamento;

/**
 * Repositório da classe base. Devolve os apontamentos de horas e de material juntos, cada um
 * na sua subclasse (RF16).
 */
public interface ApontamentoRepository extends JpaRepository<Apontamento, Long> {

    List<Apontamento> findByOrdemServicoIdOrderByDataHora(Long ordemServicoId);
}
