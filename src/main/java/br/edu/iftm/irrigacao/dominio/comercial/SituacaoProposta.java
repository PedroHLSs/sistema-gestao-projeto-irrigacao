package br.edu.iftm.irrigacao.dominio.comercial;

/**
 * Situação da proposta (Figura 10). As mudanças seguem a Figura 9 e os gateways G2 e G3, como na
 * tabela "Transições da proposta" do docs/modelo.md (RF10, RF11, RF12). O rótulo é só o texto das
 * telas.
 */
public enum SituacaoProposta {
    EM_ELABORACAO("Em elaboração"),
    AGUARDANDO_DIRETORIA("Aguardando diretoria"),
    ENVIADA("Enviada"),
    VENCIDA("Vencida"),
    APROVADA("Aprovada"),
    RECUSADA("Recusada");

    private final String rotulo;

    SituacaoProposta(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
