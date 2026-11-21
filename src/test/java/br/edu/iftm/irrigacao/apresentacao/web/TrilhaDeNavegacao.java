package br.edu.iftm.irrigacao.apresentacao.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Confere a trilha de navegação (fragments/trilha.html) da página devolvida pelo MockMvc.
 */
public final class TrilhaDeNavegacao {

    private static final String INICIO = "<nav class=\"trilha\" aria-label=\"Trilha de navegação\">\\s*"
            + "<ol class=\"breadcrumb\">\\s*";

    private TrilhaDeNavegacao() {
    }

    /** Projetos › código do projeto, com o link para ele › página atual. */
    public static ResultMatcher doProjeto(Long projetoId, String pagina) {
        return trilha(INICIO
                + item("/projetos", "Projetos")
                + item("/projetos/" + projetoId, "PRJ-\\d{4}-\\d{4}")
                + atual(Pattern.quote(pagina)));
    }

    /** Na página do próprio projeto: Projetos › código do projeto, como item atual. */
    public static ResultMatcher doProprioProjeto() {
        return trilha(INICIO + item("/projetos", "Projetos") + atual("PRJ-\\d{4}-\\d{4}"));
    }

    /** Lista de outra seção › página atual. */
    public static ResultMatcher daSecao(String endereco, String rotulo, String pagina) {
        return trilha(INICIO + item(endereco, Pattern.quote(rotulo)) + atual(Pattern.quote(pagina)));
    }

    private static String item(String endereco, String texto) {
        return "<li class=\"breadcrumb-item\"><a href=\"" + Pattern.quote(endereco) + "\">" + texto + "</a></li>\\s*";
    }

    private static String atual(String texto) {
        return "<li class=\"breadcrumb-item active\" aria-current=\"page\">" + texto + "</li>\\s*</ol>";
    }

    private static ResultMatcher trilha(String padrao) {
        return resultado -> assertThat(resultado.getResponse().getContentAsString())
                .as("trilha de navegação")
                .containsPattern(padrao);
    }
}
