package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

/**
 * Cotação dos materiais faltantes com fornecedores (RF08, atividade 7). Cada cotação nasce em
 * COTADA. A mais barata de cada material entra no custo orçado do projeto (RF09).
 */
public interface CotacaoService {

    /** Cotações do projeto com o material carregado, em ordem de material e de preço. */
    List<Cotacao> listar(Long projetoId);

    /**
     * A cotação mais barata de cada material do projeto, sem contar as descartadas. Empate de
     * preço fica com a registrada primeiro.
     *
     * @return id do material para a cotação
     */
    Map<Long, Cotacao> maisBaratas(Long projetoId);

    /**
     * A cotação é registrada no orçamento (ORCANDO) e também enquanto o material do projeto está em
     * andamento ({@link SituacaoProjeto#isMaterialEmAndamento()}), para o que passou a faltar depois do
     * orçamento e precisa de requisição de compra (G4, RF13). Em projeto que começou com a separação
     * parcial, a compra do que falta continua durante a instalação.
     */
    boolean podeCotar(Projeto projeto);

    /**
     * Registra a cotação em COTADA, com o projeto numa situação de {@link #podeCotar(Projeto)} e para
     * material da lista técnica.
     *
     * @param prazoDias prazo de entrega em dias, opcional
     */
    Cotacao registrar(Long projetoId, Long materialId, String fornecedor, BigDecimal precoUnitario, Integer prazoDias,
            BigDecimal quantidade);

    /**
     * Quanto ainda falta comprar de cada material do projeto: o faltante da lista técnica menos o
     * que já está em requisição aberta (REQUISITADA). Nunca negativo.
     *
     * @return id do material para a quantidade
     */
    Map<Long, BigDecimal> quantidadesAComprar(Long projetoId);

    /**
     * Requisição de compra (RF13, G4): a cotação escolhida vai para REQUISITADA. O mesmo material
     * pode ter mais de uma requisição no projeto enquanto faltar comprar (por exemplo, parte de um
     * fornecedor e parte de outro). Quando as requisições cobrem o faltante, as outras cotações do
     * item que ainda estavam em COTADA vão para DESCARTADA. Só com o material do projeto em andamento
     * ({@link SituacaoProjeto#isMaterialEmAndamento()}), para cotação COTADA de material que ainda tem o
     * que comprar.
     */
    Cotacao requisitar(Long projetoId, Long cotacaoId);

    /** Requisições abertas: cotações em REQUISITADA, com projeto e material carregados (RF13). */
    List<Cotacao> requisicoesAbertas();

    /**
     * Recebimento da requisição, numa transação só: a cotação vai para RECEBIDA, a quantidade entra
     * no estoque ({@code entradaCompra}) e é reservada para o projeto até o que o item ainda precisa.
     * O que chegar a mais fica disponível.
     *
     * @return a cotação recebida
     */
    Cotacao receber(Long cotacaoId);
}
