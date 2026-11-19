package br.edu.iftm.irrigacao.persistencia;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoHoras;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

public interface ApontamentoHorasRepository extends JpaRepository<ApontamentoHoras, Long> {

    /** Horas apontadas em todas as ordens de serviço do projeto, com ordem e usuário (RF21, RF22). */
    @Query("""
            select a from ApontamentoHoras a
            join fetch a.ordemServico o
            left join fetch a.usuario
            where o.projeto.id = :projetoId
            order by a.dataHora, a.id
            """)
    List<ApontamentoHoras> listarPorProjeto(@Param("projetoId") Long projetoId);

    /** Horas apontadas numa ordem de serviço, em ordem de lançamento (RF16). */
    List<ApontamentoHoras> findByOrdemServicoIdOrderByDataHoraAscIdAsc(Long ordemServicoId);

    /** Resultado de {@link #totalDoProjeto}. */
    interface TotalDeHoras {
        BigDecimal getHoras();

        BigDecimal getCusto();
    }

    /** Horas apontadas no projeto e o custo delas, pelo valor da hora de cada apontamento (RF22). */
    @Query("""
            select coalesce(sum(a.horas), 0) as horas, coalesce(sum(a.horas * a.valorHora), 0) as custo
            from ApontamentoHoras a
            where a.ordemServico.projeto.id = :projetoId
            """)
    TotalDeHoras totalDoProjeto(@Param("projetoId") Long projetoId);

    /** Custo das horas apontadas, somado nos projetos de uma situação (RF22). */
    @Query("""
            select coalesce(sum(a.horas * a.valorHora), 0)
            from ApontamentoHoras a
            where a.ordemServico.projeto.situacao = :situacao
            """)
    BigDecimal somarCustoDosProjetos(@Param("situacao") SituacaoProjeto situacao);
}
