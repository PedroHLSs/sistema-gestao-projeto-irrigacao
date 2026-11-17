package br.edu.iftm.irrigacao.dominio.execucao;

import java.math.BigDecimal;

/**
 * Medição com o desvio em % de cada valor em relação ao parâmetro de projeto (RF17), para as telas.
 * O desvio é negativo quando a medida ficou abaixo do parâmetro.
 */
public class MedicaoAvaliada {

    private final Medicao medicao;
    private final BigDecimal desvioPressao;
    private final BigDecimal desvioVazao;

    public MedicaoAvaliada(Medicao medicao, BigDecimal desvioPressao, BigDecimal desvioVazao) {
        this.medicao = medicao;
        this.desvioPressao = desvioPressao;
        this.desvioVazao = desvioVazao;
    }

    public Medicao getMedicao() {
        return medicao;
    }

    public BigDecimal getDesvioPressao() {
        return desvioPressao;
    }

    public BigDecimal getDesvioVazao() {
        return desvioVazao;
    }
}
