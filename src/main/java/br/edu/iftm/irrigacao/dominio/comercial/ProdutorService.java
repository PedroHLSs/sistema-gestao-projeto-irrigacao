package br.edu.iftm.irrigacao.dominio.comercial;

import java.util.List;

/**
 * Cadastro do produtor e das propriedades dele (RF01). Uma propriedade pertence a um produtor.
 *
 * <p>O CPF/CNPJ é recebido com ou sem pontuação, conferido pelo dígito verificador e gravado
 * normalizado (ver {@link CpfCnpj}).
 */
public interface ProdutorService {

    /**
     * Produtores cujo nome contém o texto ou cujo CPF/CNPJ contém os números digitados, em ordem de
     * nome. Busca vazia traz todos.
     */
    List<Produtor> pesquisar(String busca);

    /** @throws br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException se o id não existir */
    Produtor buscarPorId(Long id);

    /**
     * @param cpfCnpj    com ou sem pontuação
     * @param idIgnorado produtor que está sendo editado, para o documento dele mesmo não contar;
     *                   {@code null} na inclusão
     */
    boolean cpfCnpjEmUso(String cpfCnpj, Long idIgnorado);

    /** @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException com CPF/CNPJ inválido ou repetido */
    Produtor criar(String nome, String cpfCnpj, String telefone, String email);

    /** @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException com CPF/CNPJ inválido ou repetido */
    Produtor atualizar(Long id, String nome, String cpfCnpj, String telefone, String email);

    /** Propriedades de um produtor, em ordem de nome. */
    List<Propriedade> listarPropriedades(Long produtorId);

    /** Todas as propriedades, com o produtor carregado, em ordem de produtor e de nome. */
    List<Propriedade> listarPropriedadesComProdutor();

    /**
     * @throws br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException se a propriedade não
     *                                                                     existir ou for de outro
     *                                                                     produtor
     */
    Propriedade buscarPropriedade(Long produtorId, Long propriedadeId);

    Propriedade criarPropriedade(Long produtorId, String nome, String municipio, Double latitude,
            Double longitude, Double areaTotalHa);

    Propriedade atualizarPropriedade(Long produtorId, Long propriedadeId, String nome, String municipio,
            Double latitude, Double longitude, Double areaTotalHa);
}
