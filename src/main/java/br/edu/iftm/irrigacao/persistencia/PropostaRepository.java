package br.edu.iftm.irrigacao.persistencia;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.comercial.Proposta;
import br.edu.iftm.irrigacao.dominio.comercial.SituacaoProposta;

public interface PropostaRepository extends JpaRepository<Proposta, Long> {

    /** Todas as versões do orçamento de um projeto, da mais nova para a mais antiga. */
    List<Proposta> findByProjetoIdOrderByNumeroDesc(Long projetoId);

    /** A proposta vigente do projeto, que é a de maior número. */
    Optional<Proposta> findFirstByProjetoIdOrderByNumeroDesc(Long projetoId);

    /** Usado na verificação de vencimento das propostas enviadas (RF11). */
    List<Proposta> findBySituacao(SituacaoProposta situacao);

    /** O projeto já tem uma proposta numa destas situações (RF09). */
    boolean existsByProjetoIdAndSituacaoIn(Long projetoId, Collection<SituacaoProposta> situacoes);

    /** Proposta com projeto, propriedade, produtor, responsável e aprovador carregados, para as telas. */
    @Query("""
            select p from Proposta p
            join fetch p.projeto pj
            join fetch pj.propriedade pr
            join fetch pr.produtor
            join fetch pj.responsavel
            left join fetch p.aprovadoPor
            where p.id = :id
            """)
    Optional<Proposta> buscarCompleta(@Param("id") Long id);

    /** Versões do orçamento de um projeto, da mais nova para a mais antiga, com o aprovador (RF09). */
    @Query("""
            select p from Proposta p
            left join fetch p.aprovadoPor
            where p.projeto.id = :projetoId
            order by p.numero desc
            """)
    List<Proposta> listarDoProjeto(@Param("projetoId") Long projetoId);

    /** Propostas nas situações informadas, com projeto e produtor, para as listas (RF10, RF11). */
    @Query("""
            select p from Proposta p
            join fetch p.projeto pj
            join fetch pj.propriedade pr
            join fetch pr.produtor
            join fetch pj.responsavel
            left join fetch p.aprovadoPor
            where p.situacao in :situacoes
            order by pj.codigo, p.numero desc
            """)
    List<Proposta> listarPorSituacoes(@Param("situacoes") Collection<SituacaoProposta> situacoes);

    /** Propostas numa situação dos projetos de um responsável, para o alerta de vencimento (RF11). */
    @Query("""
            select p from Proposta p
            join fetch p.projeto pj
            where pj.responsavel.id = :usuarioId and p.situacao = :situacao
            """)
    List<Proposta> listarDoResponsavel(@Param("usuarioId") Long usuarioId,
            @Param("situacao") SituacaoProposta situacao);
}
