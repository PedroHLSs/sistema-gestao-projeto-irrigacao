package br.edu.iftm.irrigacao.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.iftm.irrigacao.dominio.execucao.OrdemServico;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {

    List<OrdemServico> findByProjetoIdOrderByNumero(Long projetoId);

    /** A ordem de serviço aberta do projeto é a que ainda não tem data de conclusão (RF15). */
    Optional<OrdemServico> findFirstByProjetoIdAndDataConclusaoIsNullOrderByNumeroDesc(Long projetoId);
}
