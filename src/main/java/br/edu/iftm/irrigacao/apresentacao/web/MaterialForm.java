package br.edu.iftm.irrigacao.apresentacao.web;

import java.math.BigDecimal;

import br.edu.iftm.irrigacao.dominio.estoque.Material;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Formulário de inclusão e edição do material (RF06). Não tem campo de saldo: o saldo só muda
 * pelas movimentações de estoque (RF07, RNF05), e um parâmetro {@code saldo} enviado no POST não
 * tem onde ser gravado.
 */
@CodigoMaterialUnico
public class MaterialForm {

    private static final String MENSAGEM_VALOR = "Use até 12 dígitos e 2 casas decimais.";

    /** Vazio na inclusão. Na edição vem do {@code {id}} da URL. */
    private Long id;

    @NotBlank(message = "Informe o código.")
    @Size(max = 30, message = "O código pode ter até 30 caracteres.")
    private String codigo;

    @NotBlank(message = "Informe a descrição.")
    @Size(max = 150, message = "A descrição pode ter até 150 caracteres.")
    private String descricao;

    @NotBlank(message = "Informe a unidade.")
    @Size(max = 10, message = "A unidade pode ter até 10 caracteres, como un, m ou barra.")
    private String unidade;

    @NotNull(message = "Informe o preço de venda.")
    @PositiveOrZero(message = "O preço de venda não pode ser negativo.")
    @Digits(integer = 12, fraction = 2, message = MENSAGEM_VALOR)
    private BigDecimal precoVenda;

    @NotNull(message = "Informe o custo unitário.")
    @PositiveOrZero(message = "O custo unitário não pode ser negativo.")
    @Digits(integer = 12, fraction = 2, message = MENSAGEM_VALOR)
    private BigDecimal custoUnitario;

    public MaterialForm() {
    }

    public MaterialForm(Material material) {
        this.id = material.getId();
        this.codigo = material.getCodigo();
        this.descricao = material.getDescricao();
        this.unidade = material.getUnidade();
        this.precoVenda = material.getPrecoVenda();
        this.custoUnitario = material.getCustoUnitario();
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
}
