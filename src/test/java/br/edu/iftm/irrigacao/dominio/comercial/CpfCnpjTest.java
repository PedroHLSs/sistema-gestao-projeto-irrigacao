package br.edu.iftm.irrigacao.dominio.comercial;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Conferência do CPF e do CNPJ pelo dígito verificador (RF01).
 */
class CpfCnpjTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "529.982.247-25", "52998224725", "111.444.777-35", " 529 982 247 25 " })
    @DisplayName("CPF com os dígitos certos, com ou sem pontuação")
    void testCpfValido(String cpf) {
        assertThat(CpfCnpj.valido(cpf)).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "529.982.247-24", // segundo dígito errado
            "529.982.247-35", // primeiro dígito errado
            "111.111.111-11", // todos iguais: passa no cálculo, mas não existe
            "000.000.000-00",
            "529.982.247-2", // 10 dígitos
            "529.982.247-251", // 12 dígitos
            "52998224A25" // letra no CPF
    })
    @DisplayName("CPF com dígito errado, repetido ou com tamanho errado")
    void testCpfInvalido(String cpf) {
        assertThat(CpfCnpj.valido(cpf)).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "11.222.333/0001-81", "11222333000181", "11.444.777/0001-61" })
    @DisplayName("CNPJ numérico com os dígitos certos")
    void testCnpjValido(String cnpj) {
        assertThat(CpfCnpj.valido(cnpj)).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "12.ABC.345/01DE-35", "12abc34501de35" })
    @DisplayName("CNPJ alfanumérico (IN RFB 2.229/2024) com os dígitos certos, em maiúsculas ou minúsculas")
    void testCnpjAlfanumericoValido(String cnpj) {
        assertThat(CpfCnpj.valido(cnpj)).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "11.222.333/0001-82", // segundo dígito errado
            "11.222.333/0001-91", // primeiro dígito errado
            "00.000.000/0000-00", // todos iguais
            "12.ABC.345/01DE-36", // alfanumérico com dígito errado
            "12.ABC.345/01DE-3A", // dígito verificador com letra
            "11.222.333/0001-8" // 13 caracteres
    })
    @DisplayName("CNPJ com dígito errado, repetido ou com tamanho errado")
    void testCnpjInvalido(String cnpj) {
        assertThat(CpfCnpj.valido(cnpj)).isFalse();
    }

    @ParameterizedTest(name = "\"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "abc", "1234" })
    @DisplayName("Vazio ou sem tamanho de documento não é válido")
    void testVazio(String valor) {
        assertThat(CpfCnpj.valido(valor)).isFalse();
    }

    @Test
    @DisplayName("Normaliza tirando a pontuação e passando para maiúsculas")
    void testNormalizar() {
        assertThat(CpfCnpj.normalizar("529.982.247-25")).isEqualTo("52998224725");
        assertThat(CpfCnpj.normalizar("12.abc.345/01de-35")).isEqualTo("12ABC34501DE35");
        assertThat(CpfCnpj.normalizar(null)).isEmpty();
    }

    @Test
    @DisplayName("Formata CPF e CNPJ com a pontuação de costume")
    void testFormatar() {
        assertThat(CpfCnpj.formatar("52998224725")).isEqualTo("529.982.247-25");
        assertThat(CpfCnpj.formatar("11222333000181")).isEqualTo("11.222.333/0001-81");
        assertThat(CpfCnpj.formatar("12ABC34501DE35")).isEqualTo("12.ABC.345/01DE-35");
        assertThat(CpfCnpj.formatar("123")).isEqualTo("123");
    }
}
