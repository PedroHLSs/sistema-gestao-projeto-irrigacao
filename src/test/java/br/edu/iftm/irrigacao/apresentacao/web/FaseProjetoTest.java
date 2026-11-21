package br.edu.iftm.irrigacao.apresentacao.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

/**
 * Agrupamento das situações por fase, usado no filtro da lista de projetos.
 */
class FaseProjetoTest {

    @Test
    @DisplayName("Cada situação está em uma fase só, e as fases seguem a ordem do fluxo")
    void testCadaSituacaoEmUmaFase() {
        assertThat(Arrays.stream(FaseProjeto.values()).flatMap(fase -> fase.getSituacoes().stream()))
                .containsExactly(SituacaoProjeto.values());
        assertThat(FaseProjeto.values()).extracting(FaseProjeto::getRotulo)
                .containsExactly("Levantamento", "Proposta", "Material", "Execução", "Encerrados e cancelados");
    }
}
