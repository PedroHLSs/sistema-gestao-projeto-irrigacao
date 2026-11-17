package br.edu.iftm.irrigacao.dominio.comercial;

import java.util.Locale;

/**
 * Conferência e formatação do CPF ou CNPJ do produtor pelo dígito verificador (RF01).
 *
 * <p>O valor é gravado normalizado: só letras maiúsculas e dígitos, sem ponto, barra ou traço. Com
 * 11 caracteres é CPF; com 14 é CNPJ. O CNPJ aceita também o formato alfanumérico da Receita
 * Federal (IN RFB 2.229/2024, emitido desde julho de 2026): os 12 primeiros caracteres podem ser
 * letras, e cada caractere vale o seu código ASCII menos 48 no cálculo do dígito; os dois dígitos
 * verificadores continuam numéricos. O CNPJ só de números é um caso particular do mesmo cálculo.
 */
public final class CpfCnpj {

    private static final int TAMANHO_CPF = 11;
    private static final int TAMANHO_CNPJ = 14;

    private CpfCnpj() {
    }

    /** Tira pontuação e espaços e passa para maiúsculas. {@code null} vira texto vazio. */
    public static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.toUpperCase(Locale.ROOT).replaceAll("[^0-9A-Z]", "");
    }

    /** CPF ou CNPJ com os dígitos verificadores corretos, com ou sem pontuação. */
    public static boolean valido(String valor) {
        String documento = normalizar(valor);
        return switch (documento.length()) {
            case TAMANHO_CPF -> cpfValido(documento);
            case TAMANHO_CNPJ -> cnpjValido(documento);
            default -> false;
        };
    }

    /**
     * CPF no formato 000.000.000-00 e CNPJ no formato 00.000.000/0000-00. Valor com outro tamanho
     * volta como veio.
     */
    public static String formatar(String valor) {
        String d = normalizar(valor);
        if (d.length() == TAMANHO_CPF) {
            return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
        }
        if (d.length() == TAMANHO_CNPJ) {
            return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8) + "/"
                    + d.substring(8, 12) + "-" + d.substring(12);
        }
        return valor;
    }

    private static boolean cpfValido(String cpf) {
        if (!cpf.matches("\\d{11}") || todosIguais(cpf)) {
            return false;
        }
        return digitoCpf(cpf, 9) == valor(cpf.charAt(9)) && digitoCpf(cpf, 10) == valor(cpf.charAt(10));
    }

    /** Pesos decrescentes de {@code tamanho + 1} até 2 sobre os primeiros {@code tamanho} dígitos. */
    private static int digitoCpf(String cpf, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += valor(cpf.charAt(i)) * (tamanho + 1 - i);
        }
        return digito(soma);
    }

    private static boolean cnpjValido(String cnpj) {
        if (!cnpj.matches("[0-9A-Z]{12}\\d{2}") || todosIguais(cnpj)) {
            return false;
        }
        return digitoCnpj(cnpj, 12) == valor(cnpj.charAt(12)) && digitoCnpj(cnpj, 13) == valor(cnpj.charAt(13));
    }

    /** Pesos de 2 a 9, da direita para a esquerda, recomeçando em 2 depois do 9. */
    private static int digitoCnpj(String cnpj, int tamanho) {
        int soma = 0;
        int peso = 2;
        for (int i = tamanho - 1; i >= 0; i--) {
            soma += valor(cnpj.charAt(i)) * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        return digito(soma);
    }

    private static int digito(int soma) {
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** Dígito vale o próprio número; letra vale o código ASCII menos 48 (A = 17, B = 18...). */
    private static int valor(char caractere) {
        return caractere - '0';
    }

    /** 000.000.000-00, 111.111.111-11 e semelhantes passam no cálculo, mas não existem. */
    private static boolean todosIguais(String documento) {
        return documento.chars().allMatch(c -> c == documento.charAt(0));
    }
}
