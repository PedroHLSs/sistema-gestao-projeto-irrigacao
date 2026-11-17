package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
     * Registra a cotação em COTADA. Só com o projeto em ORCANDO e para material da lista técnica.
     *
     * @param prazoDias prazo de entrega em dias, opcional
     */
    Cotacao registrar(Long projetoId, Long materialId, String fornecedor, BigDecimal precoUnitario, Integer prazoDias,
            BigDecimal quantidade);
}
