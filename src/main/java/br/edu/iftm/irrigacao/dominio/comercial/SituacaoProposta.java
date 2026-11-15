package br.edu.iftm.irrigacao.dominio.comercial;

/**
 * Situação da proposta comercial (Figura 10). As transições estão em docs/modelo.md e seguem a
 * Figura 9 e os gateways G2 e G3 (RF10, RF11).
 */
public enum SituacaoProposta {
    EM_ELABORACAO,
    AGUARDANDO_DIRETORIA,
    ENVIADA,
    VENCIDA,
    APROVADA,
    RECUSADA
}
