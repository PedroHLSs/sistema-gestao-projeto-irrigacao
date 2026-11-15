package br.edu.iftm.irrigacao.dominio.execucao;

import java.time.LocalDateTime;

import br.edu.iftm.irrigacao.dominio.EntidadeAuditada;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Pendência registrada quando o cliente recusa o aceite, com a data da visita reprogramada
 * (RF18). Quem registrou fica em {@code criadoPor}, da auditoria.
 */
@Entity
@Table(name = "pendencia")
public class Pendencia extends EntidadeAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoPendencia tipo;

    @NotBlank
    @Column(name = "descricao", nullable = false, length = 500)
    private String descricao;

    @NotNull
    @Column(name = "data_registro", nullable = false)
    private LocalDateTime dataRegistro;

    @Column(name = "data_reprogramada")
    private LocalDateTime dataReprogramada;

    @Column(name = "resolvida", nullable = false)
    private boolean resolvida = false;

    public Pendencia() {
    }

    public Pendencia(Long id, Projeto projeto, TipoPendencia tipo, String descricao, LocalDateTime dataRegistro,
            LocalDateTime dataReprogramada, boolean resolvida) {
        this.id = id;
        this.projeto = projeto;
        this.tipo = tipo;
        this.descricao = descricao;
        this.dataRegistro = dataRegistro;
        this.dataReprogramada = dataReprogramada;
        this.resolvida = resolvida;
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

    public TipoPendencia getTipo() {
        return tipo;
    }

    public void setTipo(TipoPendencia tipo) {
        this.tipo = tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDateTime dataRegistro) {
        this.dataRegistro = dataRegistro;
    }

    public LocalDateTime getDataReprogramada() {
        return dataReprogramada;
    }

    public void setDataReprogramada(LocalDateTime dataReprogramada) {
        this.dataReprogramada = dataReprogramada;
    }

    public boolean isResolvida() {
        return resolvida;
    }

    public void setResolvida(boolean resolvida) {
        this.resolvida = resolvida;
    }
}
