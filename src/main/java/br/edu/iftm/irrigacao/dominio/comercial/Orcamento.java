package br.edu.iftm.irrigacao.dominio.comercial;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;

/**
 * Detalhamento do orçamento de um projeto (RF09): uma linha por item da lista técnica, a mão de
 * obra, o desconto e o total, como mostrado antes de gerar a proposta e na impressão.
 *
 * <pre>
 * valorTotal  = (soma de quantidade x preço unitário + horas x preço da hora) x (1 - desconto/100)
 * custoOrcado = soma de quantidade x custo unitário + horas x custo da hora
 * </pre>
 *
 * <p>Os dois valores são arredondados em centavos só no fim. Não é entidade: o orçamento gravado é
 * a {@link Proposta}, com o valor total, e o preço congelado em cada {@code ItemProjeto}.
 */
public class Orcamento {

    private static final BigDecimal CEM = new BigDecimal("100");

    private final List<Linha> linhas;
    private final BigDecimal horasPrevistas;
    private final BigDecimal precoHora;
    private final BigDecimal custoHora;
    private final BigDecimal percentualDesconto;

    public Orcamento(List<Linha> linhas, BigDecimal horasPrevistas, BigDecimal precoHora, BigDecimal custoHora,
            BigDecimal percentualDesconto) {
        this.linhas = List.copyOf(linhas);
        this.horasPrevistas = horasPrevistas;
        this.precoHora = precoHora;
        this.custoHora = custoHora;
        this.percentualDesconto = percentualDesconto;
    }

    public List<Linha> getLinhas() {
        return linhas;
    }

    public BigDecimal getHorasPrevistas() {
        return horasPrevistas;
    }

    public BigDecimal getPrecoHora() {
        return precoHora;
    }

    public BigDecimal getCustoHora() {
        return custoHora;
    }

    public BigDecimal getPercentualDesconto() {
        return percentualDesconto;
    }

    /** Soma de quantidade x preço unitário dos itens. */
    public BigDecimal getValorMateriais() {
        return linhas.stream().map(Linha::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Horas previstas x preço da hora. */
    public BigDecimal getValorMaoDeObra() {
        return horasPrevistas.multiply(precoHora);
    }

    /** Materiais mais mão de obra, antes do desconto. */
    public BigDecimal getSubtotal() {
        return getValorMateriais().add(getValorMaoDeObra());
    }

    /** O valor da proposta, em centavos. */
    public BigDecimal getValorTotal() {
        return getSubtotal().multiply(CEM.subtract(percentualDesconto)).divide(CEM)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Quanto o desconto tira do subtotal, já em centavos. */
    public BigDecimal getValorDesconto() {
        return getSubtotal().setScale(2, RoundingMode.HALF_UP).subtract(getValorTotal());
    }

    /** O custo previsto do projeto, em centavos, que vai para {@code Projeto.custoOrcado} (RF22). */
    public BigDecimal getCustoOrcado() {
        return linhas.stream().map(Linha::getSubtotalCusto).reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(horasPrevistas.multiply(custoHora))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Um item da lista técnica no orçamento. */
    public static class Linha {

        private final ItemProjeto item;
        private final BigDecimal precoUnitario;
        private final BigDecimal custoUnitario;
        private final String origemDoCusto;
        private final BigDecimal faltante;

        /**
         * @param origemDoCusto de onde veio o custo, para a tela: "cadastro" ou o fornecedor da
         *                      cotação
         * @param faltante      quanto falta no estoque para o item, zero quando não falta
         */
        public Linha(ItemProjeto item, BigDecimal precoUnitario, BigDecimal custoUnitario, String origemDoCusto,
                BigDecimal faltante) {
            this.item = item;
            this.precoUnitario = precoUnitario;
            this.custoUnitario = custoUnitario;
            this.origemDoCusto = origemDoCusto;
            this.faltante = faltante;
        }

        public ItemProjeto getItem() {
            return item;
        }

        public BigDecimal getQuantidade() {
            return item.getQuantidadePrevista();
        }

        public BigDecimal getPrecoUnitario() {
            return precoUnitario;
        }

        public BigDecimal getSubtotal() {
            return getQuantidade().multiply(precoUnitario);
        }

        public BigDecimal getCustoUnitario() {
            return custoUnitario;
        }

        public BigDecimal getSubtotalCusto() {
            return getQuantidade().multiply(custoUnitario);
        }

        public String getOrigemDoCusto() {
            return origemDoCusto;
        }

        public BigDecimal getFaltante() {
            return faltante;
        }
    }
}
