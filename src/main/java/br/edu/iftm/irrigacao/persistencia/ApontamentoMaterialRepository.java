package br.edu.iftm.irrigacao.persistencia;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoMaterial;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

public interface ApontamentoMaterialRepository extends JpaRepository<ApontamentoMaterial, Long> {

    /** Materiais apontados em todas as ordens de serviço do projeto, com material, ordem e usuário (RF21, RF22). */
    @Query("""
            select a from ApontamentoMaterial a
            join fetch a.material
            join fetch a.ordemServico o
            left join fetch a.usuario
            where o.projeto.id = :projetoId
            order by a.dataHora, a.id
            """)
    List<ApontamentoMaterial> listarPorProjetoComMaterial(@Param("projetoId") Long projetoId);

    /** Materiais apontados numa ordem de serviço, com o material já carregado (RF16). */
    @Query("""
            select a from ApontamentoMaterial a
            join fetch a.material
            where a.ordemServico.id = :ordemId
            order by a.dataHora, a.id
            """)
    List<ApontamentoMaterial> listarPorOrdemComMaterial(@Param("ordemId") Long ordemId);

    /** Linha de {@link #somarPorMaterial}. */
    interface SomaPorMaterial {
        Long getMaterialId();

        BigDecimal getQuantidade();
    }

    /** Quantidade apontada de cada material no projeto (RF22). */
    @Query("""
            select a.material.id as materialId, sum(a.quantidade) as quantidade
            from ApontamentoMaterial a
            where a.ordemServico.projeto.id = :projetoId
            group by a.material.id
            """)
    List<SomaPorMaterial> somarPorMaterial(@Param("projetoId") Long projetoId);

    /** Quantidade apontada x custo unitário atual, somada nos projetos de uma situação (RF22). */
    @Query("""
            select coalesce(sum(a.quantidade * m.custoUnitario), 0)
            from ApontamentoMaterial a
            join a.material m
            where a.ordemServico.projeto.situacao = :situacao
            """)
    BigDecimal somarCustoDosProjetos(@Param("situacao") SituacaoProjeto situacao);
}
