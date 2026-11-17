package br.edu.iftm.irrigacao.apresentacao.web;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Quantidade devolvida ao estoque por item da lista técnica (RF19). Os campos da tela se chamam
 * {@code devolvidas[id do item]}; os limites de cada um são conferidos pelo {@code SobrasService}.
 */
public class SobrasForm {

    private Map<Long, BigDecimal> devolvidas = new HashMap<>();

    public Map<Long, BigDecimal> getDevolvidas() {
        return devolvidas;
    }

    public void setDevolvidas(Map<Long, BigDecimal> devolvidas) {
        this.devolvidas = devolvidas;
    }
}
