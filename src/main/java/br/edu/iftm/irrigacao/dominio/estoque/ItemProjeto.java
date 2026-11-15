package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;

import br.edu.iftm.irrigacao.dominio.EntidadeAuditada;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Item da lista técnica do projeto (RF05): um material e a quantidade prevista. Cada material
 * aparece uma vez por projeto.
 *
 * <p>{@code quantidadeSeparada} acompanha a separação no almoxarifado (RF14) e
 * {@code quantidadeApontada} soma os apontamentos de material feitos em campo (RF16).
 * {@code precoUnitario} é o preço de venda congelado quando o orçamento é gerado (RF09).
 */
@Entity
@Table(name = "item_projeto",
        uniqueConstraints = @UniqueConstraint(name = "uk_item_projeto_projeto_material",
                columnNames = { "projeto_id", "material_id" }))
public class ItemProjeto extends EntidadeAuditada {

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

    @NotNull
    @Column(name = "quantidade_prevista", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantidadePrevista;

    @NotNull
    @Column(name = "quantidade_separada", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantidadeSeparada = BigDecimal.ZERO;

    @NotNull
    @Column(name = "quantidade_apontada", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantidadeApontada = BigDecimal.ZERO;

    @Column(name = "preco_unitario", precision = 14, scale = 2)
    private BigDecimal precoUnitario;

    public ItemProjeto() {
    }

    public ItemProjeto(Long id, Projeto projeto, Material material, BigDecimal quantidadePrevista,
            BigDecimal quantidadeSeparada, BigDecimal quantidadeApontada, BigDecimal precoUnitario) {
        this.id = id;
        this.projeto = projeto;
        this.material = material;
        this.quantidadePrevista = quantidadePrevista;
        this.quantidadeSeparada = quantidadeSeparada;
        this.quantidadeApontada = quantidadeApontada;
        this.precoUnitario = precoUnitario;
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

    public BigDecimal getQuantidadePrevista() {
        return quantidadePrevista;
    }

    public void setQuantidadePrevista(BigDecimal quantidadePrevista) {
        this.quantidadePrevista = quantidadePrevista;
    }

    public BigDecimal getQuantidadeSeparada() {
        return quantidadeSeparada;
    }

    public void setQuantidadeSeparada(BigDecimal quantidadeSeparada) {
        this.quantidadeSeparada = quantidadeSeparada;
    }

    public BigDecimal getQuantidadeApontada() {
        return quantidadeApontada;
    }

    public void setQuantidadeApontada(BigDecimal quantidadeApontada) {
        this.quantidadeApontada = quantidadeApontada;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }
}
