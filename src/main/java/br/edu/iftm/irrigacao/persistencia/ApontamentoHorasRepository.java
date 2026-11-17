package br.edu.iftm.irrigacao.persistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoHoras;

public interface ApontamentoHorasRepository extends JpaRepository<ApontamentoHoras, Long> {

    /** Horas apontadas em todas as ordens de serviço do projeto, para o custo realizado (RF22). */
    @Query("""
            select a from ApontamentoHoras a
            where a.ordemServico.projeto.id = :projetoId
            order by a.dataHora, a.id
            """)
    List<ApontamentoHoras> listarPorProjeto(@Param("projetoId") Long projetoId);

    /** Horas apontadas numa ordem de serviço, em ordem de lançamento (RF16). */
    List<ApontamentoHoras> findByOrdemServicoIdOrderByDataHoraAscIdAsc(Long ordemServicoId);
}
