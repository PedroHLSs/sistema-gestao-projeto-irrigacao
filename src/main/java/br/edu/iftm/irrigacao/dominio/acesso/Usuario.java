package br.edu.iftm.irrigacao.dominio.acesso;

import br.edu.iftm.irrigacao.dominio.EntidadeAuditada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Usuário do sistema, com um perfil de acesso (RF23, RNF04). A operação {@code autenticar(senha)} da Figura 10 fica com o Spring
 * Security, que compara a senha informada com {@code senhaHash}.
 */
@Entity
@Table(name = "usuario",
        uniqueConstraints = @UniqueConstraint(name = "uk_usuario_login", columnNames = "login"))
public class Usuario extends EntidadeAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @NotBlank
    @Column(name = "login", nullable = false, length = 60)
    private String login;

    @NotBlank
    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 20)
    private Perfil perfil;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    public Usuario() {
    }

    public Usuario(Long id, String nome, String login, String senhaHash, Perfil perfil, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.login = login;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.ativo = ativo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
