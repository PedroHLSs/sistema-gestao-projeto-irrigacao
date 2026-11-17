package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import br.edu.iftm.irrigacao.dominio.projeto.Projeto;

/**
 * Devolução das sobras de material ao estoque (RF19, G8), pela Figura 8, com o projeto em
 * FINALIZANDO_EXECUCAO.
 */
public interface SobrasService {

    /** Para cada item da lista técnica: separada, apontada, sobra e o que já foi devolvido. */
    List<SobraItem> sobras(Long projetoId);

    /** Em FINALIZANDO_EXECUCAO e com as sobras ainda não tratadas. */
    boolean podeDevolver(Projeto projeto);

    /**
     * O almoxarife confirma o que voltou de cada item com sobra, de zero até a sobra (pode voltar
     * menos). Cada quantidade maior que zero entra no estoque por {@link EstoqueService#devolver}.
     * Sem nenhum item com sobra, a chamada com o mapa vazio confirma "sem sobras". Nos dois casos o
     * projeto fica com {@code sobrasDevolvidas = true}.
     *
     * @param devolvidas quantidade devolvida por id do item da lista técnica; obrigatória para cada
     *                   item com sobra
     */
    void devolver(Long projetoId, Map<Long, BigDecimal> devolvidas);
}
