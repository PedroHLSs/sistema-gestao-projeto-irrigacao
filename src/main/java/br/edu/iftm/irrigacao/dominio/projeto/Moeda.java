package br.edu.iftm.irrigacao.dominio.projeto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Valores do orçado x realizado escritos como no Brasil (RF22). Valor que não pôde ser apurado
 * aparece como travessão.
 */
public final class Moeda {

    private static final String SEM_VALOR = "—";

    private Moeda() {
    }

    /** {@code 1234.5} vira {@code R$ 1.234,50}. */
    public static String formatar(BigDecimal valor) {
        return valor == null ? SEM_VALOR : "R$ " + numero(valor);
    }

    /** {@code 37.5} vira {@code 37,50%}. */
    public static String percentual(BigDecimal valor) {
        return valor == null ? SEM_VALOR : numero(valor) + "%";
    }

    private static String numero(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.of("pt", "BR")));
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return formato.format(valor);
    }
}
