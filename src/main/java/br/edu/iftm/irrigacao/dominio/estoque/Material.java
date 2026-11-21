package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;

import org.hibernate.annotations.Check;

import br.edu.iftm.irrigacao.dominio.EntidadeAuditada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Material do cadastro de materiais e preços (RF06), com o saldo em estoque (RF07).
 * {@code precoVenda} entra no orçamento (RF09) e {@code custoUnitario} no custo real do
 * projeto (RF22).
 *
 * <p>Saldo (RNF05): o {@code @Check} impede no banco saldo negativo e reserva maior que o saldo.
 * As colunas {@code saldo} e {@code saldo_reservado} têm {@code updatable = false}. O valor
 * informado vale só na inclusão do material; depois disso, {@code setSaldo} e
 * {@code setSaldoReservado} não chegam ao banco, e quem altera saldo é o UPDATE condicional do
 * service de estoque. Assim, salvar o cadastro do material nunca sobrescreve um saldo que mudou
 * no meio do caminho.
 */
@Entity
@Table(name = "material",
        uniqueConstraints = @UniqueConstraint(name = "uk_material_codigo", columnNames = "codigo"))
@Check(name = "ck_material_saldo",
        constraints = "saldo >= 0 and saldo_reservado >= 0 and saldo_reservado <= saldo")
public class Material extends EntidadeAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Column(name = "codigo", nullable = false, length = 30)
    private String codigo;

    @NotBlank
    @Column(name = "descricao", nullable = false, length = 150)
    private String descricao;

    @NotBlank
    @Column(name = "unidade", nullable = false, length = 10)
    private String unidade;

    @Column(name = "preco_venda", precision = 14, scale = 2)
    private BigDecimal precoVenda;

    @Column(name = "custo_unitario", precision = 14, scale = 2)
    private BigDecimal custoUnitario;

    @NotNull
    @Column(name = "saldo", nullable = false, updatable = false, precision = 14, scale = 3)
    private BigDecimal saldo = BigDecimal.ZERO;

    @NotNull
    @Column(name = "saldo_reservado", nullable = false, updatable = false, precision = 14, scale = 3)
    private BigDecimal saldoReservado = BigDecimal.ZERO;

    public Material() {
    }

    public Material(Long id, String codigo, String descricao, String unidade, BigDecimal precoVenda,
            BigDecimal custoUnitario, BigDecimal saldo, BigDecimal saldoReservado) {
        this.id = id;
        this.codigo = codigo;
        this.descricao = descricao;
        this.unidade = unidade;
        this.precoVenda = precoVenda;
        this.custoUnitario = custoUnitario;
        this.saldo = saldo;
        this.saldoReservado = saldoReservado;
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

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public void setPrecoVenda(BigDecimal precoVenda) {
        this.precoVenda = precoVenda;
    }

    public BigDecimal getCustoUnitario() {
        return custoUnitario;
    }

    public void setCustoUnitario(BigDecimal custoUnitario) {
        this.custoUnitario = custoUnitario;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public BigDecimal getSaldoReservado() {
        return saldoReservado;
    }

    public void setSaldoReservado(BigDecimal saldoReservado) {
        this.saldoReservado = saldoReservado;
    }

    /**
     * Saldo menos reservado: o que ainda pode ser reservado para um projeto (RF07). Calculado, sem
     * coluna; o mapeamento é pelos campos, então o Hibernate não o vê.
     */
    public BigDecimal getDisponivel() {
        return saldo.subtract(saldoReservado);
    }
}
