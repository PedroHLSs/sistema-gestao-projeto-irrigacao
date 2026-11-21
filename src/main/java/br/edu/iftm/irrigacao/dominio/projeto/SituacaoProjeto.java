package br.edu.iftm.irrigacao.dominio.projeto;

/**
 * Situação do projeto, com os 13 valores da Figura 10. As transições permitidas estão em
 * {@link TransicoesProjeto}, igual à tabela da seção 4 do docs/modelo.md, e são validadas por
 * {@code ProjetoService.transitar(...)}. {@code ENCERRADO} e {@code CANCELADO} são finais.
 *
 * <p>O nome do valor é o que vai para o banco. O rótulo é só o texto que aparece nas telas.
 */
public enum SituacaoProjeto {
    EM_LEVANTAMENTO("Em levantamento"),
    ORCANDO("Orçando"),
    AGUARDANDO_RESPOSTA("Aguardando resposta"),
    PREPARANDO_MATERIAL("Preparando material"),
    PROGRAMANDO_EXECUCAO("Programando execução"),
    EXECUTANDO_INSTALACAO("Executando instalação"),
    VALIDANDO_MEDICAO("Validando medição"),
    CORRIGINDO_INSTALACAO("Corrigindo instalação"),
    COLHENDO_ACEITE("Colhendo aceite"),
    TRATANDO_PENDENCIA("Tratando pendência"),
    FINALIZANDO_EXECUCAO("Finalizando execução"),
    ENCERRADO("Encerrado"),
    CANCELADO("Cancelado");

    private final String rotulo;

    SituacaoProjeto(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /**
     * Da aprovação da proposta até o aceite do cliente o material do projeto continua sendo comprado,
     * recebido e separado (RF13, RF14). Em projeto grande a obra começa com parte do material: depois
     * que o almoxarifado libera a execução, o restante é separado enquanto a equipe instala.
     */
    public boolean isMaterialEmAndamento() {
        return switch (this) {
            case PREPARANDO_MATERIAL, PROGRAMANDO_EXECUCAO, EXECUTANDO_INSTALACAO, VALIDANDO_MEDICAO,
                    CORRIGINDO_INSTALACAO, COLHENDO_ACEITE, TRATANDO_PENDENCIA -> true;
            case EM_LEVANTAMENTO, ORCANDO, AGUARDANDO_RESPOSTA, FINALIZANDO_EXECUCAO, ENCERRADO, CANCELADO -> false;
        };
    }
}
