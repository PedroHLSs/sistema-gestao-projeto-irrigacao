package br.edu.iftm.irrigacao.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.TipoMovimentacao;

public interface ItemProjetoRepository extends JpaRepository<ItemProjeto, Long> {

    /** Lista técnica de um projeto, com o material já carregado (RF05). */
    @Query("""
            select i from ItemProjeto i
            join fetch i.material m
            where i.projeto.id = :projetoId
            order by m.descricao
            """)
    List<ItemProjeto> listarPorProjetoComMaterial(@Param("projetoId") Long projetoId);

    /**
     * Disponibilidade da lista técnica de um projeto numa consulta só (RF07): cada linha traz o
     * item, com o material já carregado, e na segunda posição a soma das reservas daquele material
     * para o projeto (zero quando não há).
     *
     * @param reserva sempre {@code TipoMovimentacao.RESERVA}
     */
    @Query("""
            select i,
                   (select coalesce(sum(mv.quantidade), 0) from MovimentacaoEstoque mv
                    where mv.projeto.id = :projetoId and mv.material.id = m.id and mv.tipo = :reserva)
            from ItemProjeto i
            join fetch i.material m
            where i.projeto.id = :projetoId
            order by m.descricao
            """)
    List<Object[]> disponibilidade(@Param("projetoId") Long projetoId, @Param("reserva") TipoMovimentacao reserva);

    Optional<ItemProjeto> findByProjetoIdAndMaterialId(Long projetoId, Long materialId);

    /** O projeto precisa de pelo menos um item para sair de EM_LEVANTAMENTO. */
    boolean existsByProjetoId(Long projetoId);

    /** Quantos itens a lista técnica do projeto tem (RF05). */
    long countByProjetoId(Long projetoId);
}
