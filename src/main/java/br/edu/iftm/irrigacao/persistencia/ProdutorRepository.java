package br.edu.iftm.irrigacao.persistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.iftm.irrigacao.dominio.comercial.Produtor;

public interface ProdutorRepository extends JpaRepository<Produtor, Long> {

    boolean existsByCpfCnpj(String cpfCnpj);

    List<Produtor> findAllByOrderByNome();
}
