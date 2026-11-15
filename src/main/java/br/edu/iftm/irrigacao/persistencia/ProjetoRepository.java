package br.edu.iftm.irrigacao.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

public interface ProjetoRepository extends JpaRepository<Projeto, Long> {

    boolean existsByCodigo(String codigo);

    /** Projeto com propriedade, produtor e responsável já carregados, para a tela de detalhe. */
    @Query("""
            select p from Projeto p
            join fetch p.propriedade pr
            join fetch pr.produtor
            join fetch p.responsavel
            where p.id = :id
            """)
    Optional<Projeto> buscarComPropriedadeEProdutor(@Param("id") Long id);

    /** Lista de projetos para tela, sem N+1 em propriedade, produtor e responsável. */
    @Query("""
            select p from Projeto p
            join fetch p.propriedade pr
            join fetch pr.produtor
            join fetch p.responsavel
            order by p.dataAbertura desc, p.id desc
            """)
    List<Projeto> listarComPropriedadeEProdutor();

    /** A mesma lista, só com os projetos de uma situação. */
    @Query("""
            select p from Projeto p
            join fetch p.propriedade pr
            join fetch pr.produtor
            join fetch p.responsavel
            where p.situacao = :situacao
            order by p.dataAbertura desc, p.id desc
            """)
    List<Projeto> listarPorSituacaoComPropriedadeEProdutor(@Param("situacao") SituacaoProjeto situacao);
}
