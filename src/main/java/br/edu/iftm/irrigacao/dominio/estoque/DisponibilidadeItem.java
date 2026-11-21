package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;

/**
 * Situação de estoque de um item da lista técnica do projeto (RF07, base para os faltantes do RF08).
 *
 * <ul>
 * <li>{@code prevista}: a quantidade do item na lista técnica;</li>
 * <li>{@code disponivel}: saldo menos reservado do material, para qualquer projeto;</li>
 * <li>{@code reservadaParaProjeto}: tudo o que já foi reservado para este projeto, inclusive o que
 * depois saiu do estoque para ele, porque a saída consome a reserva;</li>
 * <li>{@code faltante}: o que a reserva e o disponível não cobrem, nunca negativo.</li>
 * </ul>
 */
public class DisponibilidadeItem {

    private final ItemProjeto item;
    private final BigDecimal disponivel;
    private final BigDecimal reservadaParaProjeto;
    private final BigDecimal faltante;

    public DisponibilidadeItem(ItemProjeto item, BigDecimal disponivel, BigDecimal reservadaParaProjeto) {
        this.item = item;
        this.disponivel = disponivel;
        this.reservadaParaProjeto = reservadaParaProjeto;
        this.faltante = item.getQuantidadePrevista().subtract(reservadaParaProjeto).subtract(disponivel)
                .max(BigDecimal.ZERO);
    }

    public ItemProjeto getItem() {
        return item;
    }

    public Material getMaterial() {
        return item.getMaterial();
    }

    public BigDecimal getPrevista() {
        return item.getQuantidadePrevista();
    }

    public BigDecimal getDisponivel() {
        return disponivel;
    }

    public BigDecimal getReservadaParaProjeto() {
        return reservadaParaProjeto;
    }

    public BigDecimal getFaltante() {
        return faltante;
    }

    /** O que já foi separado do item, somando todas as vezes que a separação foi gravada (RF14). */
    public BigDecimal getSeparada() {
        return item.getQuantidadeSeparada();
    }

    /** O que falta separar para chegar na prevista; zero quando já chegou. */
    public BigDecimal getFaltaSeparar() {
        return getPrevista().subtract(getSeparada()).max(BigDecimal.ZERO);
    }
}
