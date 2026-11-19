package br.edu.iftm.irrigacao.persistencia;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

public interface LevantamentoRepository extends JpaRepository<Levantamento, Long> {

    Optional<Levantamento> findByProjetoId(Long projetoId);

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
}
