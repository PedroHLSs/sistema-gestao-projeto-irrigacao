package br.edu.iftm.irrigacao.persistencia;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

public interface LevantamentoRepository extends JpaRepository<Levantamento, Long> {

    Optional<Levantamento> findByProjetoId(Long projetoId);

    /** Levantamentos de vários projetos numa consulta, para a lista de projetos. */
    @Query("select l from Levantamento l where l.projeto.id in :projetoIds")
    List<Levantamento> listarDosProjetos(@Param("projetoIds") Collection<Long> projetoIds);

    /**
     * Visitas agendadas no intervalo {@code [inicio, fim)}, com projeto, propriedade e produtor já
     * carregados, em ordem de data e hora (RF02).
     */
    @Query("""
            select l from Levantamento l
            join fetch l.projeto p
            join fetch p.propriedade pr
            join fetch pr.produtor
            where l.dataAgendada >= :inicio and l.dataAgendada < :fim
            order by l.dataAgendada, p.codigo
            """)
    List<Levantamento> listarAgendadosEntre(@Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    /**
     * Levantamentos completos dos projetos numa situação: os mesmos campos que
     * {@code LevantamentoService.validarCamposObrigatorios} exige no G1 (RF04).
     */
    @Query("""
            select count(l) from Levantamento l
            where l.projeto.situacao = :situacao
              and l.dataVisita is not null
              and l.cultura is not null and trim(l.cultura) <> ''
              and l.areaIrrigadaHa is not null
              and l.fonteAgua is not null
              and l.vazaoFonte is not null
              and l.topografia is not null and trim(l.topografia) <> ''
              and l.latitude is not null
              and l.longitude is not null
              and l.fotos is not empty
            """)
    long contarCompletosNaSituacao(@Param("situacao") SituacaoProjeto situacao);

    /**
     * Os levantamentos que {@link #contarCompletosNaSituacao} conta, com projeto, propriedade e
     * produtor carregados, dos projetos abertos há mais tempo para os mais novos (RF04, RF05).
     */
    @Query("""
            select l from Levantamento l
            join fetch l.projeto p
            join fetch p.propriedade pr
            join fetch pr.produtor
            where p.situacao = :situacao
              and l.dataVisita is not null
              and l.cultura is not null and trim(l.cultura) <> ''
              and l.areaIrrigadaHa is not null
              and l.fonteAgua is not null
              and l.vazaoFonte is not null
              and l.topografia is not null and trim(l.topografia) <> ''
              and l.latitude is not null
              and l.longitude is not null
              and l.fotos is not empty
            order by p.dataAbertura, p.id
            """)
    List<Levantamento> listarCompletosNaSituacao(@Param("situacao") SituacaoProjeto situacao);
}
