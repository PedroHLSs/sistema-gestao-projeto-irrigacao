package br.edu.iftm.irrigacao.persistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.comercial.Produtor;

public interface ProdutorRepository extends JpaRepository<Produtor, Long> {

    boolean existsByCpfCnpj(String cpfCnpj);

    /** CPF ou CNPJ usado por outro produtor que não o do id informado (RF01). */
    boolean existsByCpfCnpjAndIdNot(String cpfCnpj, Long id);

    List<Produtor> findAllByOrderByNome();

    /**
     * Busca da tela de produtores (RF01).
     *
     * @param nome      padrão do LIKE sobre o nome em minúsculas, como {@code %joao%}
     * @param documento padrão do LIKE sobre o CPF/CNPJ normalizado, como {@code %52998%}; texto
     *                  vazio quando a busca não tem número
     */
    @Query("""
            select p from Produtor p
            where lower(p.nome) like :nome or p.cpfCnpj like :documento
            order by p.nome
            """)
    List<Produtor> pesquisar(@Param("nome") String nome, @Param("documento") String documento);
}
