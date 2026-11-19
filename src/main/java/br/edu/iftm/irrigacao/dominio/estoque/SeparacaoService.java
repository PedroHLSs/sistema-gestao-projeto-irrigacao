package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Separação do material no almoxarifado e conferência contra a lista técnica (RF14, G5), com o
 * projeto em PREPARANDO_MATERIAL.
 */
public interface SeparacaoService {

    /** O item só é separado quando a reserva do projeto e o disponível em estoque cobrem o previsto (G4). */
    boolean podeSeparar(DisponibilidadeItem item);

    /**
     * Grava a quantidade separada de cada item informado ({@code ItemProjeto.quantidadeSeparada}) e
     * reserva para o projeto o que ainda faltava reservar do item. Valor vazio deixa o item como está.
     *
     * @param quantidadesPorItem id do {@code ItemProjeto} para a quantidade separada
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException se falta material em estoque para
     *                                                             algum item, fora de
     *                                                             PREPARANDO_MATERIAL ou com a
     *                                                             separação já conferida
     */
    void registrar(Long projetoId, Map<Long, BigDecimal> quantidadesPorItem);

    /**
     * G5: compara separada com prevista, item a item. Com divergência, devolve os itens com a
     * diferença e não muda nada. Conferida, baixa a reserva de cada item (SAIDA_PROJETO), marca
     * {@code separacaoConferida} e passa o projeto de PREPARANDO_MATERIAL para PROGRAMANDO_EXECUCAO
     * (transição 7).
     *
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException se a separação já foi conferida
     */
    ResultadoConferencia conferir(Long projetoId);
}
