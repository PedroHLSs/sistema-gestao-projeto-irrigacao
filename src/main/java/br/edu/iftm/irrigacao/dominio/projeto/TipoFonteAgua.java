package br.edu.iftm.irrigacao.dominio.projeto;

/**
 * Fonte de água registrada no levantamento de campo (Figura 10, RF03). O rótulo é só o texto das
 * telas.
 */
public enum TipoFonteAgua {
    POCO_ARTESIANO("Poço artesiano"),
    POCO_SEMIARTESIANO("Poço semiartesiano"),
    RIO_CORREGO("Rio ou córrego"),
    REPRESA_ACUDE("Represa ou açude"),
    REDE_PUBLICA("Rede pública"),
    NASCENTE("Nascente");

    private final String rotulo;

    TipoFonteAgua(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
