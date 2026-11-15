package br.edu.iftm.irrigacao.persistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.estoque.Cotacao;

public interface CotacaoRepository extends JpaRepository<Cotacao, Long> {

    /** Cotações de um projeto, com o material já carregado (RF08, RF13). */
    @Query("""
            select c from Cotacao c
            join fetch c.material m
            where c.projeto.id = :projetoId
            order by m.descricao, c.precoUnitario
            """)
    List<Cotacao> listarPorProjetoComMaterial(@Param("projetoId") Long projetoId);
}
