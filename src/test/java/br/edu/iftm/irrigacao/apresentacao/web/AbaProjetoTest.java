package br.edu.iftm.irrigacao.apresentacao.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

/**
 * Aba em que a página do projeto abre e o estado de cada aba, conforme a fase do projeto.
 */
class AbaProjetoTest {

    private static Projeto projeto(SituacaoProjeto situacao) {
        Projeto projeto = new Projeto();
        projeto.setSituacao(situacao);
        return projeto;
    }

    private static Projeto comSobrasTratadas() {
        Projeto projeto = projeto(SituacaoProjeto.FINALIZANDO_EXECUCAO);
        projeto.setSobrasDevolvidas(true);
        return projeto;
    }

    @Test
    @DisplayName("Cada situação abre a aba da sua fase")
    void testAbaDeCadaSituacao() {
        Map<SituacaoProjeto, AbaProjeto> esperado = new EnumMap<>(SituacaoProjeto.class);
        esperado.put(SituacaoProjeto.ORCANDO, AbaProjeto.PROPOSTA);
        esperado.put(SituacaoProjeto.AGUARDANDO_RESPOSTA, AbaProjeto.PROPOSTA);
        esperado.put(SituacaoProjeto.CANCELADO, AbaProjeto.PROPOSTA);
        esperado.put(SituacaoProjeto.PREPARANDO_MATERIAL, AbaProjeto.MATERIAL);
        esperado.put(SituacaoProjeto.PROGRAMANDO_EXECUCAO, AbaProjeto.EXECUCAO);
        esperado.put(SituacaoProjeto.EXECUTANDO_INSTALACAO, AbaProjeto.EXECUCAO);
        esperado.put(SituacaoProjeto.VALIDANDO_MEDICAO, AbaProjeto.EXECUCAO);
        esperado.put(SituacaoProjeto.CORRIGINDO_INSTALACAO, AbaProjeto.EXECUCAO);
        esperado.put(SituacaoProjeto.COLHENDO_ACEITE, AbaProjeto.EXECUCAO);
        esperado.put(SituacaoProjeto.TRATANDO_PENDENCIA, AbaProjeto.EXECUCAO);
        esperado.put(SituacaoProjeto.FINALIZANDO_EXECUCAO, AbaProjeto.EXECUCAO);
        esperado.put(SituacaoProjeto.ENCERRADO, AbaProjeto.ENCERRAMENTO);

        esperado.forEach((situacao, aba) -> {
            assertThat(AbaProjeto.daFase(projeto(situacao), false)).as(situacao.name()).isEqualTo(aba);
            assertThat(AbaProjeto.daFase(projeto(situacao), true)).as(situacao.name()).isEqualTo(aba);
        });
        // Só falta EM_LEVANTAMENTO, que depende do levantamento
        assertThat(esperado).hasSize(SituacaoProjeto.values().length - 1);
    }

    @Test
    @DisplayName("Em levantamento: incompleto abre o Levantamento; completo, a Lista técnica do dimensionamento")
    void testEmLevantamento() {
        assertThat(AbaProjeto.daFase(projeto(SituacaoProjeto.EM_LEVANTAMENTO), false)).isEqualTo(AbaProjeto.LEVANTAMENTO);
        assertThat(AbaProjeto.daFase(projeto(SituacaoProjeto.EM_LEVANTAMENTO), true)).isEqualTo(AbaProjeto.LISTA);
    }

    @Test
    @DisplayName("Finalizando com as sobras tratadas, falta liberar o faturamento: abre o Encerramento")
    void testFinalizandoComSobrasTratadas() {
        assertThat(AbaProjeto.daFase(comSobrasTratadas(), true)).isEqualTo(AbaProjeto.ENCERRAMENTO);
    }

    /** Estados das seis abas, na ordem da página: C concluída, A atual, P pendente. */
    private static String estados(Projeto projeto, boolean levantamentoCompleto) {
        StringBuilder texto = new StringBuilder();
        for (AbaProjeto.ComEstado item : AbaProjeto.comEstados(projeto, levantamentoCompleto)) {
            texto.append(item.estado().name().charAt(0));
        }
        return texto.toString();
    }

    private static String estados(SituacaoProjeto situacao, boolean levantamentoCompleto) {
        return estados(projeto(situacao), levantamentoCompleto);
    }

    @Test
    @DisplayName("Estado de cada aba: concluída, a da fase atual ou pendente, sempre na ordem do fluxo")
    void testEstados() {
        assertThat(estados(SituacaoProjeto.EM_LEVANTAMENTO, false)).isEqualTo("APPPPP");
        assertThat(estados(SituacaoProjeto.EM_LEVANTAMENTO, true)).isEqualTo("CAPPPP");
        assertThat(estados(SituacaoProjeto.ORCANDO, true)).isEqualTo("CCAPPP");
        assertThat(estados(SituacaoProjeto.AGUARDANDO_RESPOSTA, true)).isEqualTo("CCAPPP");
        assertThat(estados(SituacaoProjeto.PREPARANDO_MATERIAL, true)).isEqualTo("CCCAPP");
        for (SituacaoProjeto situacao : List.of(SituacaoProjeto.PROGRAMANDO_EXECUCAO,
                SituacaoProjeto.EXECUTANDO_INSTALACAO, SituacaoProjeto.VALIDANDO_MEDICAO,
                SituacaoProjeto.CORRIGINDO_INSTALACAO, SituacaoProjeto.COLHENDO_ACEITE,
                SituacaoProjeto.TRATANDO_PENDENCIA, SituacaoProjeto.FINALIZANDO_EXECUCAO)) {
            assertThat(estados(situacao, true)).as(situacao.name()).isEqualTo("CCCCAP");
        }
        assertThat(estados(comSobrasTratadas(), true)).isEqualTo("CCCCCA");
        // Encerrado ou cancelado, não há fase atual
        assertThat(estados(SituacaoProjeto.ENCERRADO, true)).isEqualTo("CCCCCC");
        assertThat(estados(SituacaoProjeto.CANCELADO, true)).isEqualTo("CCPPPP");
    }

    @Test
    @DisplayName("O nome de cada aba, usado no fim do endereço, é único")
    void testNomes() {
        assertThat(AbaProjeto.values()).extracting(AbaProjeto::getNome)
                .containsExactly("levantamento", "lista", "proposta", "material", "execucao", "encerramento");
    }
}
