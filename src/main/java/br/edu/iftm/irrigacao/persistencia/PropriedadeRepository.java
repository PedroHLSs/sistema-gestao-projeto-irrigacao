package br.edu.iftm.irrigacao.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;

public interface PropriedadeRepository extends JpaRepository<Propriedade, Long> {

    List<Propriedade> findByProdutorIdOrderByNome(Long produtorId);

    /** Propriedade só se ela for do produtor informado (RF01). */
    Optional<Propriedade> findByIdAndProdutorId(Long id, Long produtorId);

    /** Todas as propriedades com o produtor já carregado, para a abertura do projeto (RF01). */
    @Query("""
            select pr from Propriedade pr
            join fetch pr.produtor p
            order by p.nome, pr.nome
            """)
    List<Propriedade> listarComProdutor();
}
