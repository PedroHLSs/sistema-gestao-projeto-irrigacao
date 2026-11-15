package br.edu.iftm.irrigacao.dominio.acesso;

/**
 * Perfil de acesso do usuário (Figura 10). É um atributo de {@code Usuario}, sem tabela própria.
 */
public enum Perfil {
    VENDEDOR,
    PROJETISTA,
    COMPRADOR,
    ALMOXARIFE,
    TECNICO_CAMPO,
    FINANCEIRO,
    DIRETORIA,
    ADMINISTRADOR
}
