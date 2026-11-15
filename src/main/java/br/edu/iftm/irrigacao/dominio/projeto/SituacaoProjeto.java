package br.edu.iftm.irrigacao.dominio.projeto;

/**
 * Situação do projeto, com os 13 valores da Figura 10. As transições permitidas estão em
 * docs/modelo.md (seção 4) e são validadas por {@code ProjetoService.transitar(...)}.
 * {@code ENCERRADO} e {@code CANCELADO} são finais.
 */
public enum SituacaoProjeto {
    EM_LEVANTAMENTO,
    ORCANDO,
    AGUARDANDO_RESPOSTA,
    PREPARANDO_MATERIAL,
    PROGRAMANDO_EXECUCAO,
    EXECUTANDO_INSTALACAO,
    VALIDANDO_MEDICAO,
    CORRIGINDO_INSTALACAO,
    COLHENDO_ACEITE,
    TRATANDO_PENDENCIA,
    FINALIZANDO_EXECUCAO,
    ENCERRADO,
    CANCELADO
}
