package br.edu.iftm.irrigacao.persistencia;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import jakarta.persistence.LockModeType;

public interface ProjetoRepository extends JpaRepository<Projeto, Long> {

    boolean existsByCodigo(String codigo);

    /** Projeto com propriedade, produtor e responsável já carregados, para a tela de detalhe. */
    @Query("""
            select p from Projeto p
            join fetch p.propriedade pr
            join fetch pr.produtor
            join fetch p.responsavel
            where p.id = :id
            """)
    Optional<Projeto> buscarComPropriedadeEProdutor(@Param("id") Long id);

    /**
     * Lista de projetos para tela, sem N+1 em propriedade, produtor e responsável, dos mais novos
     * para os mais antigos.
     *
     * @param situacoes situações aceitas; todas, quando a tela não filtra
     * @param busca     padrão do LIKE sobre o código e o nome do produtor, em minúsculas, como
     *                  {@code %prj-2026%}; {@code %} traz todos
     */
    @Query("""
            select p from Projeto p
            join fetch p.propriedade pr
            join fetch pr.produtor pd
            join fetch p.responsavel
            where p.situacao in :situacoes
              and (lower(p.codigo) like :busca or lower(pd.nome) like :busca)
            order by p.dataAbertura desc, p.id desc
            """)
    List<Projeto> pesquisar(@Param("situacoes") Collection<SituacaoProjeto> situacoes,
            @Param("busca") String busca);

    /**
     * Maior código com o prefixo, como {@code PRJ-2026-%}, para gerar o próximo (RF01). Como o
     * número tem sempre 4 dígitos, a ordem do texto é a ordem do número.
     *
     * @return o maior código, ou {@code null} se ainda não há projeto com o prefixo
     */
    @Query("select max(p.codigo) from Projeto p where p.codigo like :prefixo")
    String ultimoCodigo(@Param("prefixo") String prefixo);

    /**
     * Projeto com a linha travada até o fim da transação ({@code select ... for update}), para que
     * duas mudanças de situação ao mesmo tempo não partam da mesma situação (RF21).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Projeto p where p.id = :id")
    Optional<Projeto> buscarParaAlterarSituacao(@Param("id") Long id);
}
