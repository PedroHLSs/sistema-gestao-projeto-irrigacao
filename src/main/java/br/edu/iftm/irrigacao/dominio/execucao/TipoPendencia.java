package br.edu.iftm.irrigacao.dominio.execucao;

/**
 * Tipo da pendência registrada quando o aceite é recusado (Figura 10, RF18). A pendência
 * TECNICA leva o projeto de volta à medição (transição 16); a ADMINISTRATIVA, a uma nova visita
 * de aceite (transição 15). O rótulo é só o texto das telas.
 */
public enum TipoPendencia {
    TECNICA("Técnica"),
    ADMINISTRATIVA("Administrativa");

    private final String rotulo;

    TipoPendencia(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
