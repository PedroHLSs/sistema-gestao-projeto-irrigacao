package br.edu.iftm.irrigacao.dominio.projeto;

import java.time.LocalDateTime;

import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
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
import jakarta.validation.constraints.NotNull;

/**
 * Registro de uma mudança de situação do projeto (RF21). É gravado por
 * {@code ProjetoService.transitar(...)} na mesma transação da mudança e não é editado depois,
 * por isso não estende {@code EntidadeAuditada}.
 *
 * <p>{@code situacaoAnterior} fica vazia no registro de abertura do projeto.
 */
@Entity
@Table(name = "historico_projeto")
public class HistoricoProjeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_anterior", length = 30)
    private SituacaoProjeto situacaoAnterior;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_nova", nullable = false, length = 30)
    private SituacaoProjeto situacaoNova;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @NotNull
    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "observacao", length = 500)
    private String observacao;

    public HistoricoProjeto() {
    }

    public HistoricoProjeto(Long id, Projeto projeto, SituacaoProjeto situacaoAnterior,
            SituacaoProjeto situacaoNova, Usuario usuario, LocalDateTime dataHora, String observacao) {
        this.id = id;
        this.projeto = projeto;
        this.situacaoAnterior = situacaoAnterior;
        this.situacaoNova = situacaoNova;
        this.usuario = usuario;
        this.dataHora = dataHora;
        this.observacao = observacao;
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

    public SituacaoProjeto getSituacaoAnterior() {
        return situacaoAnterior;
    }

    public void setSituacaoAnterior(SituacaoProjeto situacaoAnterior) {
        this.situacaoAnterior = situacaoAnterior;
    }

    public SituacaoProjeto getSituacaoNova() {
        return situacaoNova;
    }

    public void setSituacaoNova(SituacaoProjeto situacaoNova) {
        this.situacaoNova = situacaoNova;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}
