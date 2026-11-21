package br.edu.iftm.irrigacao.apresentacao.web;

import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.AGUARDANDO_RESPOSTA;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.CANCELADO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.EM_LEVANTAMENTO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.ENCERRADO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.FINALIZANDO_EXECUCAO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.ORCANDO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.PREPARANDO_MATERIAL;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

/**
 * Abas da página do projeto (projetos/detalheProjeto.html), uma por fase, na ordem do fluxo. A
 * página abre na aba da fase em que o projeto está: as ações que mudam a situação voltam todas para
 * a página do projeto, e assim o usuário cai direto no próximo passo em vez de sempre no Levantamento.
 *
 * <p>O nome é o que vai no fim do endereço quando o usuário troca de aba ({@code /projetos/7#proposta}).
 * Com ele, recarregar a página ou voltar de outra tela mantém a aba que estava aberta.
 *
 * <p>Cada aba também mostra o seu {@link Estado}: concluída, a da fase atual ou ainda pendente.
 */
public enum AbaProjeto {
    LEVANTAMENTO("levantamento", "Levantamento"),
    LISTA("lista", "Lista técnica"),
    PROPOSTA("proposta", "Proposta"),
    MATERIAL("material", "Material"),
    EXECUCAO("execucao", "Execução"),
    ENCERRAMENTO("encerramento", "Encerramento");

    /** Situações em que a proposta ainda não foi aprovada pelo cliente e o material não foi reservado. */
    private static final Set<SituacaoProjeto> ANTES_DA_APROVACAO = EnumSet.of(EM_LEVANTAMENTO, ORCANDO,
            AGUARDANDO_RESPOSTA, CANCELADO);

    private final String nome;
    private final String rotulo;

    AbaProjeto(String nome, String rotulo) {
        this.nome = nome;
        this.rotulo = rotulo;
    }

    public String getNome() {
        return nome;
    }

    public String getRotulo() {
        return rotulo;
    }

    public enum Estado {
        CONCLUIDA, ATUAL, PENDENTE
    }

    /** A aba com o seu estado, para a página desenhar a marca de cada uma. */
    public record ComEstado(AbaProjeto aba, Estado estado) {

        public boolean isConcluida() {
            return estado == Estado.CONCLUIDA;
        }

        public boolean isAtual() {
            return estado == Estado.ATUAL;
        }
    }

    /**
     * @param levantamentoCompleto o levantamento não tem mais campo faltando: em levantamento, o
     *                             próximo passo passa a ser o dimensionamento, na aba Lista técnica
     */
    public static AbaProjeto daFase(Projeto projeto, boolean levantamentoCompleto) {
        return switch (projeto.getSituacao()) {
            case EM_LEVANTAMENTO -> levantamentoCompleto ? LISTA : LEVANTAMENTO;
            // CANCELADO só vem da recusa da proposta
            case ORCANDO, AGUARDANDO_RESPOSTA, CANCELADO -> PROPOSTA;
            case PREPARANDO_MATERIAL -> MATERIAL;
            case PROGRAMANDO_EXECUCAO, EXECUTANDO_INSTALACAO, VALIDANDO_MEDICAO, CORRIGINDO_INSTALACAO,
                    COLHENDO_ACEITE, TRATANDO_PENDENCIA -> EXECUCAO;
            // Com as sobras tratadas, falta só liberar o faturamento, que fica na aba Encerramento
            case FINALIZANDO_EXECUCAO -> projeto.isSobrasDevolvidas() ? ENCERRAMENTO : EXECUCAO;
            case ENCERRADO -> ENCERRAMENTO;
        };
    }

    /** Todas as abas, na ordem da página, com o estado de cada uma. */
    public static List<ComEstado> comEstados(Projeto projeto, boolean levantamentoCompleto) {
        return Arrays.stream(values())
                .map(aba -> new ComEstado(aba, aba.estadoNa(projeto, levantamentoCompleto)))
                .toList();
    }

    /** Encerrado ou cancelado, o projeto não tem fase atual. */
    public Estado estadoNa(Projeto projeto, boolean levantamentoCompleto) {
        SituacaoProjeto situacao = projeto.getSituacao();
        boolean semFaseAtual = situacao == ENCERRADO || situacao == CANCELADO;
        if (!semFaseAtual && this == daFase(projeto, levantamentoCompleto)) {
            return Estado.ATUAL;
        }
        return concluidaNa(projeto, levantamentoCompleto) ? Estado.CONCLUIDA : Estado.PENDENTE;
    }

    private boolean concluidaNa(Projeto projeto, boolean levantamentoCompleto) {
        SituacaoProjeto situacao = projeto.getSituacao();
        return switch (this) {
            case LEVANTAMENTO -> situacao != EM_LEVANTAMENTO || levantamentoCompleto;
            case LISTA -> situacao != EM_LEVANTAMENTO;
            case PROPOSTA -> !ANTES_DA_APROVACAO.contains(situacao);
            case MATERIAL -> !ANTES_DA_APROVACAO.contains(situacao) && situacao != PREPARANDO_MATERIAL;
            case EXECUCAO -> situacao == ENCERRADO
                    || (situacao == FINALIZANDO_EXECUCAO && projeto.isSobrasDevolvidas());
            case ENCERRAMENTO -> situacao == ENCERRADO;
        };
    }
}
