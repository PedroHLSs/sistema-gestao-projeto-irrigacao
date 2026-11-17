package br.edu.iftm.irrigacao.dominio.projeto;

import java.util.List;

/**
 * Abertura do projeto (RF01), consulta e mudança de situação com o histórico (RF21).
 *
 * <p>Toda mudança de {@code Projeto.situacao} passa por {@link #transitar}, que valida a transição
 * pelo mapa de {@link TransicoesProjeto} e grava o {@link HistoricoProjeto} na mesma transação.
 */
public interface ProjetoService {

    /**
     * Abre o projeto (RF01, transição 1): gera o código {@code PRJ-AAAA-NNNN}, sequencial dentro do
     * ano da abertura; a data de abertura é hoje; o responsável é o usuário logado; a situação é
     * {@code EM_LEVANTAMENTO}. Grava o histórico de abertura, com a situação anterior vazia.
     *
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException se a propriedade não for do
     *                                                             produtor ou não houver usuário logado
     */
    Projeto abrir(Long produtorId, Long propriedadeId);

    /**
     * Muda a situação do projeto, se o mapa de {@link TransicoesProjeto} permitir, e grava o
     * {@link HistoricoProjeto} na mesma transação (RF21). As condições de negócio da transição são
     * conferidas antes por quem pede a mudança.
     *
     * @param observacao texto livre do histórico, opcional (até 500 caracteres)
     * @return o projeto já na nova situação
     * @throws TransicaoInvalidaException se o mapa não permitir; nada é alterado
     */
    Projeto transitar(Long projetoId, SituacaoProjeto novaSituacao, String observacao);

    /**
     * Projeto com propriedade, produtor e responsável carregados.
     *
     * @throws br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException se o id não existir
     */
    Projeto buscarPorId(Long id);

    /**
     * Projetos dos mais novos para os mais antigos.
     *
     * @param situacao só os desta situação; {@code null} traz todas
     * @param busca    trecho do código ou do nome do produtor; vazio traz todos
     */
    List<Projeto> pesquisar(SituacaoProjeto situacao, String busca);

    /** Linha do tempo do projeto, da abertura até a última mudança (RF21). */
    List<HistoricoProjeto> historico(Long projetoId);
}
