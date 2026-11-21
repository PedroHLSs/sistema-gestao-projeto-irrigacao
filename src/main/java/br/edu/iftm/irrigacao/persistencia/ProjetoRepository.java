package br.edu.iftm.irrigacao.persistencia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.comercial.SituacaoProposta;
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

    // ---------- Orçado x realizado (RF22) ----------

    /** Linha de {@link #listarEncerradosComCustos}. */
    interface EncerradoComCustos {
        Long getId();

        String getCodigo();

        String getProdutor();

        LocalDate getDataLiberacao();

        BigDecimal getCustoOrcado();

        BigDecimal getValorTotal();

        BigDecimal getCustoMateriais();

        BigDecimal getCustoHoras();
    }

    /**
     * Projetos encerrados com o faturamento liberado em {@code [de, ate]}, com os valores somados no
     * banco, numa consulta só.
     */
    @Query("""
            select p.id as id, p.codigo as codigo, pd.nome as produtor,
                   p.dataLiberacaoFaturamento as dataLiberacao, p.custoOrcado as custoOrcado,
                   (select max(pp.valorTotal) from Proposta pp
                    where pp.projeto.id = p.id and pp.situacao = :aprovada) as valorTotal,
                   (select coalesce(sum(am.quantidade * m.custoUnitario), 0) from ApontamentoMaterial am
                    join am.material m
                    where am.ordemServico.projeto.id = p.id) as custoMateriais,
                   (select coalesce(sum(ah.horas * ah.valorHora), 0) from ApontamentoHoras ah
                    where ah.ordemServico.projeto.id = p.id) as custoHoras
            from Projeto p
            join p.propriedade pr
            join pr.produtor pd
            where p.situacao = :situacao
              and p.dataLiberacaoFaturamento >= :de and p.dataLiberacaoFaturamento <= :ate
            order by p.dataLiberacaoFaturamento desc, p.id desc
            """)
    List<EncerradoComCustos> listarEncerradosComCustos(@Param("situacao") SituacaoProjeto situacao,
            @Param("aprovada") SituacaoProposta aprovada, @Param("de") LocalDate de, @Param("ate") LocalDate ate);

    /** Resultado de {@link #totalOrcadoNaSituacao}. */
    interface TotalOrcado {
        long getProjetos();

        BigDecimal getCustoOrcado();
    }

    @Query("""
            select count(p) as projetos, coalesce(sum(p.custoOrcado), 0) as custoOrcado
            from Projeto p
            where p.situacao = :situacao
            """)
    TotalOrcado totalOrcadoNaSituacao(@Param("situacao") SituacaoProjeto situacao);

    // ---------- Painel inicial ----------

    /** Linha de {@link #contarDoResponsavelPorSituacao}. */
    interface TotalPorSituacao {
        SituacaoProjeto getSituacao();

        long getTotal();
    }

    @Query("""
            select p.situacao as situacao, count(p) as total
            from Projeto p
            where p.responsavel.id = :usuarioId
            group by p.situacao
            """)
    List<TotalPorSituacao> contarDoResponsavelPorSituacao(@Param("usuarioId") Long usuarioId);

    @Query("select count(p) from Projeto p where p.situacao = :situacao")
    long contarNaSituacao(@Param("situacao") SituacaoProjeto situacao);

    @Query("select count(p) from Projeto p where p.situacao = :situacao and p.sobrasDevolvidas = :devolvidas")
    long contarPorSobras(@Param("situacao") SituacaoProjeto situacao, @Param("devolvidas") boolean devolvidas);
}
