package br.edu.iftm.irrigacao.dominio.estoque;

/**
 * Situação da cotação (acréscimo à Figura 10, RF08 e RF13). A requisição de compra do RF13 é a
 * cotação escolhida em REQUISITADA; as outras do mesmo item no projeto vão para DESCARTADA, e a
 * requisitada passa a RECEBIDA quando o material chega. O rótulo é só o texto das telas.
 */
public enum SituacaoCotacao {
    COTADA("Cotada"),
    REQUISITADA("Requisitada"),
    RECEBIDA("Recebida"),
    DESCARTADA("Descartada");

    private final String rotulo;

    SituacaoCotacao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
