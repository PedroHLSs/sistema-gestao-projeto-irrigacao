package br.edu.iftm.irrigacao.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;

public interface ItemProjetoRepository extends JpaRepository<ItemProjeto, Long> {

    /** Lista técnica de um projeto, com o material já carregado (RF05). */
    @Query("""
            select i from ItemProjeto i
            join fetch i.material m
            where i.projeto.id = :projetoId
            order by m.descricao
            """)
    List<ItemProjeto> listarPorProjetoComMaterial(@Param("projetoId") Long projetoId);

    Optional<ItemProjeto> findByProjetoIdAndMaterialId(Long projetoId, Long materialId);

    /** O projeto precisa de pelo menos um item para sair de EM_LEVANTAMENTO. */
    boolean existsByProjetoId(Long projetoId);
}
