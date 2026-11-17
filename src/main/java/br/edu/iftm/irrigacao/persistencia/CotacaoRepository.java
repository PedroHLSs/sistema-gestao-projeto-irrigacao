package br.edu.iftm.irrigacao.persistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.estoque.Cotacao;
import br.edu.iftm.irrigacao.dominio.estoque.SituacaoCotacao;

public interface CotacaoRepository extends JpaRepository<Cotacao, Long> {

    /** Cotações de um projeto, com o material já carregado (RF08, RF13). */
    @Query("""
            select c from Cotacao c
            join fetch c.material m
            where c.projeto.id = :projetoId
            order by m.descricao, c.precoUnitario
            """)
    List<Cotacao> listarPorProjetoComMaterial(@Param("projetoId") Long projetoId);

    /**
     * Cotações que ainda valem para o projeto (fora a situação informada, que é a DESCARTADA), da
     * mais barata para a mais cara dentro de cada material (RF08, RF09).
     */
    @Query("""
            select c from Cotacao c
            join fetch c.material m
            where c.projeto.id = :projetoId and c.situacao <> :descartada
            order by m.id, c.precoUnitario, c.id
            """)
    List<Cotacao> listarValidas(@Param("projetoId") Long projetoId, @Param("descartada") SituacaoCotacao descartada);

    /** Cotações de um material no projeto, para descartar as que não foram escolhidas (RF13). */
    List<Cotacao> findByProjetoIdAndMaterialId(Long projetoId, Long materialId);

    /** Cotações numa situação, com projeto e material carregados, como as requisições abertas (RF13). */
    @Query("""
            select c from Cotacao c
            join fetch c.projeto p
            join fetch c.material m
            where c.situacao = :situacao
            order by p.codigo, m.descricao
            """)
    List<Cotacao> listarPorSituacao(@Param("situacao") SituacaoCotacao situacao);
}
