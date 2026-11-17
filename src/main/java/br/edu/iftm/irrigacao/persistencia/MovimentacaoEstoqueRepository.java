package br.edu.iftm.irrigacao.persistencia;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.estoque.MovimentacaoEstoque;
import br.edu.iftm.irrigacao.dominio.estoque.TipoMovimentacao;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    /**
     * Extrato de um material no período {@code [inicio, fim)}, da movimentação mais nova para a mais
     * antiga, paginado (RF07). O projeto e o usuário vêm junto pelo {@code @EntityGraph}, que vale
     * só para a consulta da página e não para a contagem.
     *
     * @param projetoId só as movimentações deste projeto; {@code null} traz todas
     */
    @EntityGraph(attributePaths = { "projeto", "usuario" })
    @Query("""
            select m from MovimentacaoEstoque m
            where m.material.id = :materialId
              and m.dataHora >= :inicio and m.dataHora < :fim
              and (:projetoId is null or m.projeto.id = :projetoId)
            order by m.dataHora desc, m.id desc
            """)
    Page<MovimentacaoEstoque> extrato(@Param("materialId") Long materialId, @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim, @Param("projetoId") Long projetoId, Pageable pageable);

    /** Projetos que já movimentaram o material, para o filtro do extrato. */
    @Query("""
            select distinct p from MovimentacaoEstoque m
            join m.projeto p
            where m.material.id = :materialId
            order by p.codigo
            """)
    List<Projeto> projetosDoMaterial(@Param("materialId") Long materialId);

    /** Soma das movimentações de um tipo para um projeto e um material; zero quando não há nenhuma. */
    @Query("""
            select coalesce(sum(m.quantidade), 0) from MovimentacaoEstoque m
            where m.projeto.id = :projetoId and m.material.id = :materialId and m.tipo = :tipo
            """)
    BigDecimal somar(@Param("projetoId") Long projetoId, @Param("materialId") Long materialId,
            @Param("tipo") TipoMovimentacao tipo);

    /** Movimentações de um projeto, com o material já carregado. */
    @Query("""
            select m from MovimentacaoEstoque m
            join fetch m.material
            where m.projeto.id = :projetoId
            order by m.dataHora, m.id
            """)
    List<MovimentacaoEstoque> listarPorProjetoComMaterial(@Param("projetoId") Long projetoId);
}
