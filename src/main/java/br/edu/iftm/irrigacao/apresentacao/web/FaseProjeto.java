package br.edu.iftm.irrigacao.apresentacao.web;

import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.AGUARDANDO_RESPOSTA;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.CANCELADO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.COLHENDO_ACEITE;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.CORRIGINDO_INSTALACAO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.EM_LEVANTAMENTO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.ENCERRADO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.EXECUTANDO_INSTALACAO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.FINALIZANDO_EXECUCAO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.ORCANDO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.PREPARANDO_MATERIAL;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.PROGRAMANDO_EXECUCAO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.TRATANDO_PENDENCIA;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.VALIDANDO_MEDICAO;

import java.util.List;

import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

/**
 * As 13 situações do projeto agrupadas pela fase, na ordem do fluxo: é como o filtro da lista de
 * projetos as mostra, e a cor da etiqueta de cada situação segue o mesmo agrupamento (app.css).
 */
public enum FaseProjeto {
    LEVANTAMENTO("Levantamento", List.of(EM_LEVANTAMENTO)),
    PROPOSTA("Proposta", List.of(ORCANDO, AGUARDANDO_RESPOSTA)),
    MATERIAL("Material", List.of(PREPARANDO_MATERIAL)),
    EXECUCAO("Execução", List.of(PROGRAMANDO_EXECUCAO, EXECUTANDO_INSTALACAO, VALIDANDO_MEDICAO,
            CORRIGINDO_INSTALACAO, COLHENDO_ACEITE, TRATANDO_PENDENCIA, FINALIZANDO_EXECUCAO)),
    TERMINADOS("Encerrados e cancelados", List.of(ENCERRADO, CANCELADO));

    private final String rotulo;
    private final List<SituacaoProjeto> situacoes;

    FaseProjeto(String rotulo, List<SituacaoProjeto> situacoes) {
        this.rotulo = rotulo;
        this.situacoes = situacoes;
    }

    public String getRotulo() {
        return rotulo;
    }

    public List<SituacaoProjeto> getSituacoes() {
        return situacoes;
    }
}
