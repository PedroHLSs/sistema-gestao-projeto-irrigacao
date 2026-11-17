package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cadastro de materiais e preços (RF06). O cadastro não mexe no saldo: material novo nasce com
 * saldo zero, e o saldo só muda pelo {@link EstoqueService} (RF07, RNF05).
 *
 * <p>O código é gravado sem espaços nas pontas e em maiúsculas.
 */
public interface MaterialService {

    /** Quantos resultados a busca da lista técnica devolve, no máximo. */
    int LIMITE_BUSCA = 20;

    /** Materiais cujo código ou descrição contém o texto, em ordem de código. Busca vazia traz todos. */
    List<Material> pesquisar(String busca);

    /**
     * Até {@value #LIMITE_BUSCA} materiais cujo código ou descrição contém o texto, para a busca da
     * lista técnica. Busca vazia não traz nenhum.
     */
    List<Material> buscarParaListaTecnica(String busca);

    /** @throws br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException se o id não existir */
    Material buscarPorId(Long id);

    /**
     * @param idIgnorado material que está sendo editado, para o código dele mesmo não contar;
     *                   {@code null} na inclusão
     */
    boolean codigoEmUso(String codigo, Long idIgnorado);

    /** Cria o material com saldo e reservado zerados. */
    Material criar(String codigo, String descricao, String unidade, BigDecimal precoVenda, BigDecimal custoUnitario);

    /** Altera o cadastro. O saldo e o reservado ficam como estão no banco. */
    Material atualizar(Long id, String codigo, String descricao, String unidade, BigDecimal precoVenda,
            BigDecimal custoUnitario);
}
