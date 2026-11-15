package br.edu.iftm.irrigacao.persistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.estoque.MovimentacaoEstoque;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    /** Extrato de um material, da movimentação mais nova para a mais antiga (RF07). */
    @Query("""
            select m from MovimentacaoEstoque m
            left join fetch m.projeto
            left join fetch m.usuario
            where m.material.id = :materialId
            order by m.dataHora desc, m.id desc
            """)
    List<MovimentacaoEstoque> listarPorMaterial(@Param("materialId") Long materialId);

    /** Movimentações de um projeto, com o material já carregado. */
    @Query("""
            select m from MovimentacaoEstoque m
            join fetch m.material
            where m.projeto.id = :projetoId
            order by m.dataHora, m.id
            """)
    List<MovimentacaoEstoque> listarPorProjetoComMaterial(@Param("projetoId") Long projetoId);
}
