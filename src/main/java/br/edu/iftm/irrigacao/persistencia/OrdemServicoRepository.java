package br.edu.iftm.irrigacao.persistencia;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.execucao.OrdemServico;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {

    List<OrdemServico> findByProjetoIdOrderByNumero(Long projetoId);

    /** A ordem de serviço aberta do projeto é a que ainda não tem data de conclusão (RF15). */
    Optional<OrdemServico> findFirstByProjetoIdAndDataConclusaoIsNullOrderByNumeroDesc(Long projetoId);

    /** Só o id do projeto, para travar a linha dele antes de carregar a ordem. */
    @Query("select o.projeto.id from OrdemServico o where o.id = :id")
    Optional<Long> projetoDaOrdem(@Param("id") Long id);

    /** Ordens ainda abertas do projeto, para concluir no aceite (RF18). */
    List<OrdemServico> findByProjetoIdAndDataConclusaoIsNull(Long projetoId);

    /** Ordens abertas do projeto cuja equipe ainda não concluiu a instalação, com os técnicos (RF16). */
    @Query("""
            select distinct o from OrdemServico o
            left join fetch o.tecnicos
            where o.projeto.id = :projetoId and o.dataConclusao is null and o.instalacaoConcluidaEm is null
            order by o.numero
            """)
    List<OrdemServico> instalacaoPendente(@Param("projetoId") Long projetoId);

    /**
     * Ordens de todos os técnicos, com projeto, produtor e equipe, para quem acompanha a execução (RF15).
     *
     * @param incluirConcluidas {@code false} traz só as abertas
     */
    @Query("""
            select distinct o from OrdemServico o
            left join fetch o.tecnicos
            join fetch o.projeto p
            join fetch p.propriedade pr
            join fetch pr.produtor
            where :incluirConcluidas = true or o.dataConclusao is null
            order by o.dataProgramada, o.id
            """)
    List<OrdemServico> listarParaAcompanhamento(@Param("incluirConcluidas") boolean incluirConcluidas);

    /** Ordem com projeto, propriedade, produtor e técnicos carregados, para a tela da ordem (RF15). */
    @Query("""
            select o from OrdemServico o
            join fetch o.projeto p
            join fetch p.propriedade pr
            join fetch pr.produtor
            left join fetch o.tecnicos
            where o.id = :id
            """)
    Optional<OrdemServico> buscarCompleta(@Param("id") Long id);

    /** Ordens de vários projetos com os técnicos numa consulta, para a lista de projetos. */
    @Query("""
            select distinct o from OrdemServico o
            left join fetch o.tecnicos
            where o.projeto.id in :projetoIds
            order by o.numero
            """)
    List<OrdemServico> listarDosProjetos(@Param("projetoIds") Collection<Long> projetoIds);

    /** Ordens do projeto com os técnicos, em ordem de número (RF15). */
    @Query("""
            select distinct o from OrdemServico o
            left join fetch o.tecnicos
            where o.projeto.id = :projetoId
            order by o.numero
            """)
    List<OrdemServico> listarDoProjeto(@Param("projetoId") Long projetoId);

    /** Ordens abertas em que o usuário é técnico, das mais próximas para as mais distantes (RF15). */
    @Query("""
            select distinct o from OrdemServico o
            join o.tecnicos t
            join fetch o.projeto p
            join fetch p.propriedade pr
            join fetch pr.produtor
            where t.id = :usuarioId and o.dataConclusao is null
            order by o.dataProgramada, o.id
            """)
    List<OrdemServico> abertasDoTecnico(@Param("usuarioId") Long usuarioId);

    /** Ordens abertas do técnico programadas para o dia, para o painel inicial. */
    @Query("""
            select count(distinct o) from OrdemServico o
            join o.tecnicos t
            where t.id = :usuarioId and o.dataProgramada = :dia and o.dataConclusao is null
            """)
    long contarAbertasDoTecnicoNoDia(@Param("usuarioId") Long usuarioId, @Param("dia") LocalDate dia);
}
