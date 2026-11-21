package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;
import java.util.Map;

import br.edu.iftm.irrigacao.dominio.projeto.Projeto;

/**
 * Separação do material no almoxarifado e liberação da execução (RF14, G5).
 *
 * <p>Em projeto grande não dá para separar tudo antes de a equipe ir a campo. Por isso a obra começa
 * com parte do material: o almoxarifado libera a execução com pelo menos um item separado
 * ({@link #liberarParaExecucao}), e o restante continua sendo separado durante a instalação, até o
 * aceite do cliente. Antes da liberação a separação fica no almoxarifado e pode ser corrigida; depois
 * dela, cada separação já sai do estoque para o projeto.
 */
public interface SeparacaoService {

    /** O item só é separado quando a reserva do projeto e o disponível em estoque cobrem o previsto (G4). */
    boolean podeSeparar(DisponibilidadeItem item);

    /** A separação fica aberta da aprovação da proposta até o aceite ({@code SituacaoProjeto.isMaterialEmAndamento()}). */
    boolean separacaoAberta(Projeto projeto);

    /**
     * Separação aos poucos (RF14): soma a quantidade separada agora ao que já foi separado de cada
     * item ({@code ItemProjeto.quantidadeSeparada}), para o almoxarife gravar em quantas vezes
     * precisar. Vazio ou zero deixa o item como está. Na primeira vez que o item é separado, o que
     * ainda faltava reservar dele é reservado para o projeto (G4). Tudo ou nada: com um item recusado,
     * nenhum muda.
     *
     * <p>Antes da liberação da execução, quantidade negativa tira do separado, para corrigir um engano.
     * Depois dela, a separação só soma, e o que foi separado agora sai do estoque para o projeto
     * (SAIDA_PROJETO), menos o que o técnico já apontou além do separado, que saiu no apontamento.
     *
     * @param separadasAgora id do {@code ItemProjeto} para a quantidade separada agora
     * @return o andamento da separação depois da gravação
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException se o item ainda não está todo
     *                                                             reservado, se o total passa da
     *                                                             prevista ou fica abaixo de zero,
     *                                                             se tira do separado depois da
     *                                                             liberação ou com a separação fechada
     */
    ProgressoSeparacao separar(Long projetoId, Map<Long, BigDecimal> separadasAgora);

    /**
     * G5: libera a execução com o que já foi separado. Exige pelo menos um item com quantidade
     * separada; o separado de cada item sai do estoque (SAIDA_PROJETO, baixando a reserva), o projeto
     * fica com {@code separacaoConferida} e passa de PREPARANDO_MATERIAL para PROGRAMANDO_EXECUCAO
     * (transição 7). O que não foi separado continua reservado para o projeto.
     *
     * @return o andamento da separação no momento da liberação
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException sem item separado, fora de
     *                                                             PREPARANDO_MATERIAL ou com a
     *                                                             execução já liberada
     */
    ProgressoSeparacao liberarParaExecucao(Long projetoId);
}
