package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Quantidade de material, que tem até 3 casas decimais (BigDecimal 14,3), escrita como no Brasil:
 * {@code 1.250,5} e não {@code 1250.500}. Usada nas mensagens dos services e nas telas de estoque
 * (RF07).
 */
public final class Quantidade {

    /** Casas decimais da coluna de quantidade e de saldo. */
    public static final int CASAS_DECIMAIS = 3;

    /** Dígitos antes da vírgula que cabem na coluna (14 - 3). */
    public static final int DIGITOS_INTEIROS = 11;

    private static final Locale BRASIL = Locale.of("pt", "BR");

    private Quantidade() {
    }

    /** {@code 1250.500} vira {@code 1.250,5}; {@code null} vira texto vazio. */
    public static String formatar(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        NumberFormat formato = NumberFormat.getNumberInstance(BRASIL);
        formato.setMaximumFractionDigits(CASAS_DECIMAIS);
        return formato.format(valor);
    }

    /** Como {@link #formatar}, com o sinal na frente: {@code +10}, {@code -2,5}. Zero vira texto vazio. */
    public static String comSinal(BigDecimal valor) {
        if (valor == null || valor.signum() == 0) {
            return "";
        }
        return (valor.signum() > 0 ? "+" : "-") + formatar(valor.abs());
    }

    /** Cabe na coluna: até 3 casas decimais e até 11 dígitos antes da vírgula. */
    public static boolean cabeNaColuna(BigDecimal valor) {
        BigDecimal limpo = valor.stripTrailingZeros();
        return Math.max(limpo.scale(), 0) <= CASAS_DECIMAIS
                && limpo.precision() - limpo.scale() <= DIGITOS_INTEIROS;
    }
}
