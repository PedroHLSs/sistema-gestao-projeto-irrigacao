package br.edu.iftm.irrigacao.persistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;

public interface PropriedadeRepository extends JpaRepository<Propriedade, Long> {

    List<Propriedade> findByProdutorIdOrderByNome(Long produtorId);
}
