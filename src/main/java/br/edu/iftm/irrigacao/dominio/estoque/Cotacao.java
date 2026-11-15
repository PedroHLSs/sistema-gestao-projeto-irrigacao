package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;

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
 * Cotação de um material com um fornecedor, para um projeto (RF08). Quando o comprador decide
 * comprar, a mesma cotação passa a {@code REQUISITADA} e vale como requisição de compra (RF13).
 */
@Entity
@Table(name = "cotacao")
public class Cotacao extends EntidadeAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @NotBlank
    @Column(name = "fornecedor", nullable = false, length = 120)
    private String fornecedor;

    @NotNull
    @Column(name = "preco_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "prazo_dias")
    private Integer prazoDias;

    @NotNull
    @Column(name = "quantidade", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantidade;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 20)
    private SituacaoCotacao situacao = SituacaoCotacao.COTADA;

    public Cotacao() {
    }

    public Cotacao(Long id, Projeto projeto, Material material, String fornecedor, BigDecimal precoUnitario,
            Integer prazoDias, BigDecimal quantidade, SituacaoCotacao situacao) {
        this.id = id;
        this.projeto = projeto;
        this.material = material;
        this.fornecedor = fornecedor;
        this.precoUnitario = precoUnitario;
        this.prazoDias = prazoDias;
        this.quantidade = quantidade;
        this.situacao = situacao;
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

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(String fornecedor) {
        this.fornecedor = fornecedor;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public Integer getPrazoDias() {
        return prazoDias;
    }

    public void setPrazoDias(Integer prazoDias) {
        this.prazoDias = prazoDias;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public SituacaoCotacao getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoCotacao situacao) {
        this.situacao = situacao;
    }
}
