package br.edu.iftm.irrigacao.dominio.projeto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Mapa de transições da situação do projeto contra a tabela da seção 4 do docs/modelo.md (RF21).
 */
class TransicoesProjetoTest {

    @ParameterizedTest(name = "transição {0}: {1} -> {2}")
    @MethodSource("br.edu.iftm.irrigacao.dominio.projeto.TabelaDoModelo#todas")
    @DisplayName("As 17 transições da tabela são permitidas")
    void testTransicoesDaTabela(int numero, SituacaoProjeto de, SituacaoProjeto para) {
        assertThat(TransicoesProjeto.permite(de, para)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("br.edu.iftm.irrigacao.dominio.projeto.TabelaDoModelo#proibidas")
    @DisplayName("Nenhum outro par de situações é permitido")
    void testParesForaDaTabela(SituacaoProjeto de, SituacaoProjeto para) {
        assertThat(TransicoesProjeto.permite(de, para)).isFalse();
    }

    @Test
    @DisplayName("O mapa tem exatamente 16 mudanças, mais a abertura")
    void testQuantidadeDeTransicoes() {
        long mudancas = Arrays.stream(SituacaoProjeto.values())
                .mapToLong(s -> TransicoesProjeto.seguintes(s).size())
                .sum();
        assertThat(mudancas).isEqualTo(16);
    }

    @ParameterizedTest(name = "abertura em {0}")
    @EnumSource(value = SituacaoProjeto.class, names = "EM_LEVANTAMENTO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("A abertura só leva a EM_LEVANTAMENTO")
    void testAberturaSoEmLevantamento(SituacaoProjeto situacao) {
        assertThat(TransicoesProjeto.SITUACAO_INICIAL).isEqualTo(SituacaoProjeto.EM_LEVANTAMENTO);
        assertThat(TransicoesProjeto.permite(null, situacao)).isFalse();
    }

    @Test
    @DisplayName("EM_LEVANTAMENTO não vai direto para ENCERRADO")
    void testLevantamentoNaoVaiParaEncerrado() {
        assertThat(TransicoesProjeto.permite(SituacaoProjeto.EM_LEVANTAMENTO, SituacaoProjeto.ENCERRADO)).isFalse();
        assertThat(TransicoesProjeto.seguintes(SituacaoProjeto.EM_LEVANTAMENTO))
                .containsExactly(SituacaoProjeto.ORCANDO);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("br.edu.iftm.irrigacao.dominio.projeto.TabelaDoModelo#saidasDasFinais")
    @DisplayName("Nada sai de ENCERRADO nem de CANCELADO")
    void testFinaisNaoTemSaida(SituacaoProjeto fim, SituacaoProjeto para) {
        assertThat(TransicoesProjeto.permite(fim, para)).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(SituacaoProjeto.class)
    @DisplayName("Só ENCERRADO e CANCELADO são finais")
    void testSituacoesFinais(SituacaoProjeto situacao) {
        boolean esperado = situacao == SituacaoProjeto.ENCERRADO || situacao == SituacaoProjeto.CANCELADO;
        assertThat(TransicoesProjeto.ehFinal(situacao)).isEqualTo(esperado);
        assertThat(TransicoesProjeto.seguintes(situacao).isEmpty()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Situação nova vazia nunca é permitida")
    void testNovaVazia() {
        assertThat(TransicoesProjeto.permite(SituacaoProjeto.EM_LEVANTAMENTO, null)).isFalse();
        assertThat(TransicoesProjeto.permite(null, null)).isFalse();
    }

    @Test
    @DisplayName("O mapa não pode ser alterado de fora")
    void testMapaImutavel() {
        assertThatThrownBy(() -> TransicoesProjeto.seguintes(SituacaoProjeto.ENCERRADO).add(SituacaoProjeto.ORCANDO))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
