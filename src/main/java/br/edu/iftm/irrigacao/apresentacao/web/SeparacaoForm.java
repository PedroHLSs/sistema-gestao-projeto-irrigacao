package br.edu.iftm.irrigacao.apresentacao.web;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Quantidades separadas agora pelo almoxarife, por item da lista técnica (RF14). Os campos da tela
 * se chamam {@code quantidades[id do item]} e cada valor é somado ao que já foi separado do item;
 * negativo tira, vazio deixa o item como está. A conferência dos valores é feita pelo
 * {@code SeparacaoService}.
 */
public class SeparacaoForm {

    private Map<Long, BigDecimal> quantidades = new HashMap<>();

    public Map<Long, BigDecimal> getQuantidades() {
        return quantidades;
    }

    public void setQuantidades(Map<Long, BigDecimal> quantidades) {
        this.quantidades = quantidades;
    }
}
