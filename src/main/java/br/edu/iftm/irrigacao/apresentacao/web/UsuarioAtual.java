package br.edu.iftm.irrigacao.apresentacao.web;

/**
 * O que o cabeçalho das páginas mostra do usuário logado: o nome e o perfil.
 */
public class UsuarioAtual {

    private final Long id;
    private final String nome;
    private final String perfil;

    public UsuarioAtual(Long id, String nome, String perfil) {
        this.id = id;
        this.nome = nome;
        this.perfil = perfil;
    }

    /** Vazio quando a autenticação não veio do login pelo formulário. */
    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    /** Rótulo do perfil, como "Técnico de campo". */
    public String getPerfil() {
        return perfil;
    }
}
