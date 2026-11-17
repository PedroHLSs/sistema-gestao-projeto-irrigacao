package br.edu.iftm.irrigacao.dominio.comercial;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.edu.iftm.irrigacao.dominio.EntidadeAuditada;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

/**
 * Proposta comercial. Faz o papel de orçamento: cada versão do orçamento de um projeto é uma
 * proposta, numerada dentro do projeto (RF09).
 *
 * <p>{@code aprovadoPor} guarda quem da diretoria aprovou o desconto acima da alçada e
 * {@code observacaoDiretoria}, um acréscimo à Figura 10, o texto da resposta: na negativa, o motivo
 * mostrado ao vendedor (RF10). {@code dataEnvio} e {@code validadeDias} dão o vencimento (RF11),
 * {@code situacao} registra a resposta do cliente (RF12) e {@code horasPrevistas} entra no orçado x
 * realizado (RF22).
 */
@Entity
@Table(name = "proposta",
        uniqueConstraints = @UniqueConstraint(name = "uk_proposta_projeto_numero",
                columnNames = { "projeto_id", "numero" }))
public class Proposta extends EntidadeAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @NotNull
    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "valor_total", precision = 14, scale = 2)
    private BigDecimal valorTotal;

    @NotNull
    @Column(name = "percentual_desconto", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualDesconto = BigDecimal.ZERO;

    @Column(name = "data_envio")
    private LocalDate dataEnvio;

    @Column(name = "validade_dias")
    private Integer validadeDias;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 30)
    private SituacaoProposta situacao = SituacaoProposta.EM_ELABORACAO;

    @Column(name = "horas_previstas", precision = 8, scale = 2)
    private BigDecimal horasPrevistas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovado_por_id")
    private Usuario aprovadoPor;

    @Column(name = "observacao_diretoria", length = 500)
    private String observacaoDiretoria;

    public Proposta() {
    }

    public Proposta(Long id, Projeto projeto, Integer numero, BigDecimal valorTotal,
            BigDecimal percentualDesconto, LocalDate dataEnvio, Integer validadeDias,
            SituacaoProposta situacao, BigDecimal horasPrevistas, Usuario aprovadoPor, String observacaoDiretoria) {
        this.id = id;
        this.projeto = projeto;
        this.numero = numero;
        this.valorTotal = valorTotal;
        this.percentualDesconto = percentualDesconto;
        this.dataEnvio = dataEnvio;
        this.validadeDias = validadeDias;
        this.situacao = situacao;
        this.horasPrevistas = horasPrevistas;
        this.aprovadoPor = aprovadoPor;
        this.observacaoDiretoria = observacaoDiretoria;
    }

    /**
     * Último dia em que a proposta vale: {@code dataEnvio + validadeDias}, calculado (RF11). A
     * proposta enviada em 16/11 com 15 dias vale até 01/12, inclusive, e vence em 02/12. Vazio
     * enquanto a proposta não foi enviada.
     */
    public LocalDate getDataValidade() {
        if (dataEnvio == null || validadeDias == null) {
            return null;
        }
        return dataEnvio.plusDays(validadeDias);
    }

    public String getObservacaoDiretoria() {
        return observacaoDiretoria;
    }

    public void setObservacaoDiretoria(String observacaoDiretoria) {
        this.observacaoDiretoria = observacaoDiretoria;
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

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public BigDecimal getPercentualDesconto() {
        return percentualDesconto;
    }

    public void setPercentualDesconto(BigDecimal percentualDesconto) {
        this.percentualDesconto = percentualDesconto;
    }

    public LocalDate getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(LocalDate dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    public Integer getValidadeDias() {
        return validadeDias;
    }

    public void setValidadeDias(Integer validadeDias) {
        this.validadeDias = validadeDias;
    }

    public SituacaoProposta getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoProposta situacao) {
        this.situacao = situacao;
    }

    public BigDecimal getHorasPrevistas() {
        return horasPrevistas;
    }

    public void setHorasPrevistas(BigDecimal horasPrevistas) {
        this.horasPrevistas = horasPrevistas;
    }

    public Usuario getAprovadoPor() {
        return aprovadoPor;
    }

    public void setAprovadoPor(Usuario aprovadoPor) {
        this.aprovadoPor = aprovadoPor;
    }
}
