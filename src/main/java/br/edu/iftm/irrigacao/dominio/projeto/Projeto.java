package br.edu.iftm.irrigacao.dominio.projeto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.edu.iftm.irrigacao.dominio.EntidadeAuditada;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Projeto de irrigação, do levantamento ao encerramento (RF01).
 *
 * <p>O projeto não guarda coleções. Itens, propostas, ordens de serviço e os demais registros
 * apontam para ele e são lidos pelos repositórios.
 *
 * <p>Os parâmetros hidráulicos ({@code pressaoProjetoMca}, {@code vazaoProjetoM3h}) são
 * informados pelo projetista (RF05); o sistema não os calcula. {@code toleranciaPercentual} é a
 * margem aceita na comparação com a medição (RF17). {@code separacaoConferida} (RF14) marca que
 * o almoxarifado liberou o material para a execução, com pelo menos um item separado; o restante
 * continua sendo separado durante a instalação. {@code sobrasDevolvidas} (RF19), {@code dataLiberacaoFaturamento} (RF20),
 * {@code dataInicioGarantia} e {@code prazoGarantiaMeses} (RF21) marcam as etapas finais.
 * {@code custoOrcado} é o custo previsto, usado no orçado x realizado (RF22).
 *
 * <p>{@code situacao} só deve mudar por {@code ProjetoService.transitar(...)}.
 */
@Entity
@Table(name = "projeto",
        uniqueConstraints = @UniqueConstraint(name = "uk_projeto_codigo", columnNames = "codigo"))
public class Projeto extends EntidadeAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "propriedade_id", nullable = false)
    private Propriedade propriedade;

    /** Vendedor que abriu o projeto. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    @NotNull
    @Column(name = "data_abertura", nullable = false)
    private LocalDate dataAbertura;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 30)
    private SituacaoProjeto situacao = SituacaoProjeto.EM_LEVANTAMENTO;

    @Column(name = "pressao_projeto_mca")
    private Double pressaoProjetoMca;

    @Column(name = "vazao_projeto_m3h")
    private Double vazaoProjetoM3h;

    @NotNull
    @Column(name = "tolerancia_percentual", nullable = false)
    private Double toleranciaPercentual = 2.0;

    @Column(name = "custo_orcado", precision = 14, scale = 2)
    private BigDecimal custoOrcado;

    @Column(name = "separacao_conferida", nullable = false)
    private boolean separacaoConferida = false;

    @Column(name = "sobras_devolvidas", nullable = false)
    private boolean sobrasDevolvidas = false;

    @Column(name = "data_liberacao_faturamento")
    private LocalDate dataLiberacaoFaturamento;

    @Column(name = "data_inicio_garantia")
    private LocalDate dataInicioGarantia;

    @Column(name = "prazo_garantia_meses")
    private Integer prazoGarantiaMeses;

    public Projeto() {
    }

    public Projeto(Long id, String codigo, Propriedade propriedade, Usuario responsavel,
            LocalDate dataAbertura, SituacaoProjeto situacao, Double pressaoProjetoMca,
            Double vazaoProjetoM3h, Double toleranciaPercentual, BigDecimal custoOrcado,
            boolean separacaoConferida, boolean sobrasDevolvidas, LocalDate dataLiberacaoFaturamento,
            LocalDate dataInicioGarantia, Integer prazoGarantiaMeses) {
        this.id = id;
        this.codigo = codigo;
        this.propriedade = propriedade;
        this.responsavel = responsavel;
        this.dataAbertura = dataAbertura;
        this.situacao = situacao;
        this.pressaoProjetoMca = pressaoProjetoMca;
        this.vazaoProjetoM3h = vazaoProjetoM3h;
        this.toleranciaPercentual = toleranciaPercentual;
        this.custoOrcado = custoOrcado;
        this.separacaoConferida = separacaoConferida;
        this.sobrasDevolvidas = sobrasDevolvidas;
        this.dataLiberacaoFaturamento = dataLiberacaoFaturamento;
        this.dataInicioGarantia = dataInicioGarantia;
        this.prazoGarantiaMeses = prazoGarantiaMeses;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Propriedade getPropriedade() {
        return propriedade;
    }

    public void setPropriedade(Propriedade propriedade) {
        this.propriedade = propriedade;
    }

    public Usuario getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(Usuario responsavel) {
        this.responsavel = responsavel;
    }

    public LocalDate getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDate dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public SituacaoProjeto getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoProjeto situacao) {
        this.situacao = situacao;
    }

    public Double getPressaoProjetoMca() {
        return pressaoProjetoMca;
    }

    public void setPressaoProjetoMca(Double pressaoProjetoMca) {
        this.pressaoProjetoMca = pressaoProjetoMca;
    }

    public Double getVazaoProjetoM3h() {
        return vazaoProjetoM3h;
    }

    public void setVazaoProjetoM3h(Double vazaoProjetoM3h) {
        this.vazaoProjetoM3h = vazaoProjetoM3h;
    }

    public Double getToleranciaPercentual() {
        return toleranciaPercentual;
    }

    public void setToleranciaPercentual(Double toleranciaPercentual) {
        this.toleranciaPercentual = toleranciaPercentual;
    }

    public BigDecimal getCustoOrcado() {
        return custoOrcado;
    }

    public void setCustoOrcado(BigDecimal custoOrcado) {
        this.custoOrcado = custoOrcado;
    }

    public boolean isSeparacaoConferida() {
        return separacaoConferida;
    }

    public void setSeparacaoConferida(boolean separacaoConferida) {
        this.separacaoConferida = separacaoConferida;
    }

    public boolean isSobrasDevolvidas() {
        return sobrasDevolvidas;
    }

    public void setSobrasDevolvidas(boolean sobrasDevolvidas) {
        this.sobrasDevolvidas = sobrasDevolvidas;
    }

    public LocalDate getDataLiberacaoFaturamento() {
        return dataLiberacaoFaturamento;
    }

    public void setDataLiberacaoFaturamento(LocalDate dataLiberacaoFaturamento) {
        this.dataLiberacaoFaturamento = dataLiberacaoFaturamento;
    }

    public LocalDate getDataInicioGarantia() {
        return dataInicioGarantia;
    }

    public void setDataInicioGarantia(LocalDate dataInicioGarantia) {
        this.dataInicioGarantia = dataInicioGarantia;
    }

    public Integer getPrazoGarantiaMeses() {
        return prazoGarantiaMeses;
    }

    public void setPrazoGarantiaMeses(Integer prazoGarantiaMeses) {
        this.prazoGarantiaMeses = prazoGarantiaMeses;
    }
}
