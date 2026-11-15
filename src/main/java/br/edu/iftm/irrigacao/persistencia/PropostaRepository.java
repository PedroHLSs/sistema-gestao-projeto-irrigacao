package br.edu.iftm.irrigacao.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.iftm.irrigacao.dominio.comercial.Proposta;
import br.edu.iftm.irrigacao.dominio.comercial.SituacaoProposta;

public interface PropostaRepository extends JpaRepository<Proposta, Long> {

    /** Todas as versões do orçamento de um projeto, da mais nova para a mais antiga. */
    List<Proposta> findByProjetoIdOrderByNumeroDesc(Long projetoId);

    /** A proposta vigente do projeto, que é a de maior número. */
    Optional<Proposta> findFirstByProjetoIdOrderByNumeroDesc(Long projetoId);

    /** Usado na verificação de vencimento das propostas enviadas (RF11). */
    List<Proposta> findBySituacao(SituacaoProposta situacao);
}
