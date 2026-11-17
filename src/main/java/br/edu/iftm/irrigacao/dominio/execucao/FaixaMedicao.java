package br.edu.iftm.irrigacao.dominio.execucao;

import java.math.BigDecimal;

/**
 * Faixa aceita na medição (RF17, G6): os parâmetros de projeto mais ou menos a tolerância, mostrada
 * ao técnico antes de medir. Os limites contam como dentro.
 */
public class FaixaMedicao {

    private final BigDecimal pressaoMinima;
    private final BigDecimal pressaoMaxima;
    private final BigDecimal vazaoMinima;
    private final BigDecimal vazaoMaxima;
    private final BigDecimal toleranciaPercentual;

    public FaixaMedicao(BigDecimal pressaoMinima, BigDecimal pressaoMaxima, BigDecimal vazaoMinima,
            BigDecimal vazaoMaxima, BigDecimal toleranciaPercentual) {
        this.pressaoMinima = pressaoMinima;
        this.pressaoMaxima = pressaoMaxima;
        this.vazaoMinima = vazaoMinima;
        this.vazaoMaxima = vazaoMaxima;
        this.toleranciaPercentual = toleranciaPercentual;
    }

    public BigDecimal getPressaoMinima() {
        return pressaoMinima;
    }

    public BigDecimal getPressaoMaxima() {
        return pressaoMaxima;
    }

    public BigDecimal getVazaoMinima() {
        return vazaoMinima;
    }

    public BigDecimal getVazaoMaxima() {
        return vazaoMaxima;
    }

    public BigDecimal getToleranciaPercentual() {
        return toleranciaPercentual;
    }
}
