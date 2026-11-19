package br.edu.iftm.irrigacao.dominio.projeto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Comparação entre o previsto e o realizado (RF22).
 *
 * <pre>
 * margem prevista = valorTotal - custoOrcado
 * margem real     = valorTotal - custoRealizado
 * percentual      = margem / valorTotal x 100
 * </pre>
 *
 * <p>Sem proposta aprovada ou sem custo orçado, a margem correspondente fica vazia. Com valor total
 * zero (desconto de 100%), o percentual fica vazio.
 *
 * @param valorTotal     valor da proposta aprovada
 * @param custoOrcado    custo previsto, gravado no projeto
 * @param custoRealizado materiais e horas apontados, em centavos
 */
public record Apuracao(BigDecimal valorTotal, BigDecimal custoOrcado, BigDecimal custoRealizado) {

    private static final BigDecimal CEM = new BigDecimal("100");

    /** Arredonda em centavos só a soma final, como o orçamento faz. */
    public static Apuracao de(BigDecimal valorTotal, BigDecimal custoOrcado, BigDecimal custoMateriais,
            BigDecimal custoHoras) {
        return new Apuracao(valorTotal, custoOrcado, custoMateriais.add(custoHoras).setScale(2, RoundingMode.HALF_UP));
    }

    public BigDecimal getMargemPrevista() {
        return margem(custoOrcado);
    }

    public BigDecimal getMargemPrevistaPercentual() {
        return percentual(getMargemPrevista());
    }

    public BigDecimal getMargemReal() {
        return margem(custoRealizado);
    }

    public BigDecimal getMargemRealPercentual() {
        return percentual(getMargemReal());
    }

    private BigDecimal margem(BigDecimal custo) {
        return valorTotal == null || custo == null ? null : valorTotal.subtract(custo);
    }

    private BigDecimal percentual(BigDecimal margem) {
        if (margem == null || valorTotal.signum() == 0) {
            return null;
        }
        return margem.multiply(CEM).divide(valorTotal, 2, RoundingMode.HALF_UP);
    }
}
