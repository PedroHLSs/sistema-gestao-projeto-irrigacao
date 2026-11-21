package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;

/**
 * Tipo de movimentação de estoque (Figura 10) e o efeito de cada uma no saldo e no reservado do
 * material (RF07, RNF05).
 *
 * <pre>
 * ENTRADA_COMPRA  saldo + q
 * AJUSTE          saldo + q   (q é a diferença e pode ser negativa)
 * RESERVA         reservado + q   (q negativa: reserva que não saiu e voltou ao disponível)
 * SAIDA_PROJETO   saldo - q e reservado - q
 * DEVOLUCAO       saldo + q
 * </pre>
 */
public enum TipoMovimentacao {
    ENTRADA_COMPRA("Entrada de compra"),
    RESERVA("Reserva"),
    SAIDA_PROJETO("Saída para projeto"),
    DEVOLUCAO("Devolução"),
    AJUSTE("Ajuste");

    private final String rotulo;

    TipoMovimentacao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Quanto a movimentação de quantidade {@code q} muda o saldo do material. */
    public BigDecimal efeitoNoSaldo(BigDecimal q) {
        return switch (this) {
            case ENTRADA_COMPRA, AJUSTE, DEVOLUCAO -> q;
            case SAIDA_PROJETO -> q.negate();
            case RESERVA -> BigDecimal.ZERO;
        };
    }

    /** Quanto a movimentação de quantidade {@code q} muda o saldo reservado do material. */
    public BigDecimal efeitoNoReservado(BigDecimal q) {
        return switch (this) {
            case RESERVA -> q;
            case SAIDA_PROJETO -> q.negate();
            case ENTRADA_COMPRA, AJUSTE, DEVOLUCAO -> BigDecimal.ZERO;
        };
    }
}
