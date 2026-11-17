package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;

/**
 * Sobra de um item da lista técnica depois da instalação (RF19, G8): o separado menos o apontado,
 * nunca negativo, e o que já voltou ao estoque em movimentações de DEVOLUCAO.
 */
public class SobraItem {

    private final ItemProjeto item;
    private final BigDecimal sobra;
    private final BigDecimal devolvida;

    public SobraItem(ItemProjeto item, BigDecimal sobra, BigDecimal devolvida) {
        this.item = item;
        this.sobra = sobra;
        this.devolvida = devolvida;
    }

    public ItemProjeto getItem() {
        return item;
    }

    public Material getMaterial() {
        return item.getMaterial();
    }

    public BigDecimal getSeparada() {
        return item.getQuantidadeSeparada();
    }

    public BigDecimal getApontada() {
        return item.getQuantidadeApontada();
    }

    public BigDecimal getSobra() {
        return sobra;
    }

    public BigDecimal getDevolvida() {
        return devolvida;
    }

    /** Parte da sobra que não voltou ao estoque (perda, quebra ou material que ficou no cliente). */
    public BigDecimal getNaoDevolvida() {
        return sobra.subtract(devolvida).max(BigDecimal.ZERO);
    }

    public boolean isTemSobra() {
        return sobra.signum() > 0;
    }
}
