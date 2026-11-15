package br.edu.iftm.irrigacao.dominio.estoque;

/**
 * Situação da cotação de compra. A requisição de compra do RF13 é a cotação em
 * {@code REQUISITADA}.
 */
public enum SituacaoCotacao {
    COTADA,
    REQUISITADA,
    RECEBIDA,
    DESCARTADA
}
