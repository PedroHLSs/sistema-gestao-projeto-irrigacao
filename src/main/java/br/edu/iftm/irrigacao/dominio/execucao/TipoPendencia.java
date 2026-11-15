package br.edu.iftm.irrigacao.dominio.execucao;

/**
 * Tipo da pendência registrada quando o aceite é recusado (Figura 10, RF18). A pendência
 * TECNICA leva o projeto de volta à medição; a ADMINISTRATIVA, a uma nova visita de aceite.
 */
public enum TipoPendencia {
    TECNICA,
    ADMINISTRATIVA
}
