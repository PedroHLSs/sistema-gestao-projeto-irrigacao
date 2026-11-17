package br.edu.iftm.irrigacao.persistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoMaterial;

public interface ApontamentoMaterialRepository extends JpaRepository<ApontamentoMaterial, Long> {

    /** Materiais apontados em todas as ordens de serviço do projeto, com o material já carregado (RF22). */
    @Query("""
            select a from ApontamentoMaterial a
            join fetch a.material
            where a.ordemServico.projeto.id = :projetoId
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
}
