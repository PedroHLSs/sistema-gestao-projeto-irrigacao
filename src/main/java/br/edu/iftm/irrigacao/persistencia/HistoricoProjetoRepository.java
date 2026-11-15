package br.edu.iftm.irrigacao.persistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.projeto.HistoricoProjeto;

public interface HistoricoProjetoRepository extends JpaRepository<HistoricoProjeto, Long> {

    /** Histórico de um projeto em ordem cronológica, com o usuário já carregado (RF21). */
    @Query("""
            select h from HistoricoProjeto h
            left join fetch h.usuario
            where h.projeto.id = :projetoId
            order by h.dataHora, h.id
            """)
    List<HistoricoProjeto> listarPorProjeto(@Param("projetoId") Long projetoId);
}
