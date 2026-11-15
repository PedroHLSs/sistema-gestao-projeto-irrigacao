package br.edu.iftm.irrigacao.dominio.acesso;

/**
 * Perfil de acesso do usuário (Figura 10). É um atributo de {@code Usuario}, sem tabela própria.
 *
 * <p>O nome do valor ({@code VENDEDOR}, {@code PROJETISTA}...) é a authority usada pelo Spring
 * Security nas regras de acesso (RNF04). O rótulo é só o texto que aparece nas telas.
 */
public enum Perfil {
    VENDEDOR("Vendedor"),
    PROJETISTA("Projetista"),
    COMPRADOR("Comprador"),
    ALMOXARIFE("Almoxarife"),
    TECNICO_CAMPO("Técnico de campo"),
    FINANCEIRO("Financeiro"),
    DIRETORIA("Diretoria"),
    ADMINISTRADOR("Administrador");

    private final String rotulo;

    Perfil(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
