package br.edu.iftm.irrigacao.dominio.execucao;

import java.time.LocalDateTime;

import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Termo de aceite assinado pelo cliente, um por projeto (RF18).
 *
 * <p>{@code nomeResponsavel} é quem assinou pelo cliente e {@code usuario} é o técnico que colheu
 * o aceite. {@code assinatura} guarda o desenho da assinatura como PNG em base64.
 */
@Entity
@Table(name = "aceite",
        uniqueConstraints = @UniqueConstraint(name = "uk_aceite_projeto", columnNames = "projeto_id"))
public class Aceite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @NotNull
    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @NotBlank
    @Column(name = "nome_responsavel", nullable = false, length = 120)
    private String nomeResponsavel;

    @Column(name = "assinatura", columnDefinition = "text")
    private String assinatura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Aceite() {
    }

    public Aceite(Long id, Projeto projeto, LocalDateTime dataHora, String nomeResponsavel, String assinatura,
            Usuario usuario) {
        this.id = id;
        this.projeto = projeto;
        this.dataHora = dataHora;
        this.nomeResponsavel = nomeResponsavel;
        this.assinatura = assinatura;
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Projeto getProjeto() {
        return projeto;
    }

    public void setProjeto(Projeto projeto) {
        this.projeto = projeto;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getNomeResponsavel() {
        return nomeResponsavel;
    }

    public void setNomeResponsavel(String nomeResponsavel) {
        this.nomeResponsavel = nomeResponsavel;
    }

    public String getAssinatura() {
        return assinatura;
    }

    public void setAssinatura(String assinatura) {
        this.assinatura = assinatura;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
