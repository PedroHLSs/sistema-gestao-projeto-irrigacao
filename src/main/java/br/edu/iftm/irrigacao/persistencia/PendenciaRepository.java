package br.edu.iftm.irrigacao.persistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.execucao.Pendencia;

public interface PendenciaRepository extends JpaRepository<Pendencia, Long> {

    List<Pendencia> findByProjetoIdOrderByDataRegistroDesc(Long projetoId);

    /** Pendências em aberto do projeto (RF18). */
    List<Pendencia> findByProjetoIdAndResolvidaFalse(Long projetoId);

    /** Pendências em aberto dos projetos de um responsável, para o painel do vendedor. */
    @Query("select count(p) from Pendencia p where p.projeto.responsavel.id = :usuarioId and p.resolvida = false")
    long contarAbertasDoResponsavel(@Param("usuarioId") Long usuarioId);
}
