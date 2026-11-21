package br.edu.iftm.irrigacao.dominio.comercial;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Parâmetros de negócio da proposta, lidos do {@code application.properties}: preço e custo da hora
 * de mão de obra (RF09, RF22), horas de mão de obra por hectare para a estimativa do orçamento
 * (RF09), alçada de desconto do vendedor (RF10), validade e alerta de vencimento (RF11).
 */
@Component
public class ParametrosComerciais {

    private final BigDecimal precoHora;
    private final BigDecimal custoHora;
    private final BigDecimal horasPorHectare;
    private final BigDecimal alcadaVendedor;
    private final int validadeDias;
    private final int diasAlerta;

    public ParametrosComerciais(@Value("${irrigacao.mao-obra.preco-hora}") BigDecimal precoHora,
            @Value("${irrigacao.mao-obra.custo-hora}") BigDecimal custoHora,
            @Value("${irrigacao.mao-obra.horas-por-hectare}") BigDecimal horasPorHectare,
            @Value("${irrigacao.alcada-desconto.vendedor}") BigDecimal alcadaVendedor,
            @Value("${irrigacao.proposta.validade-dias}") int validadeDias,
            @Value("${irrigacao.proposta.dias-alerta}") int diasAlerta) {
        this.precoHora = precoHora;
        this.custoHora = custoHora;
        this.horasPorHectare = horasPorHectare;
        this.alcadaVendedor = alcadaVendedor;
        this.validadeDias = validadeDias;
        this.diasAlerta = diasAlerta;
    }

    /** Horas de mão de obra por hectare irrigado, que sugerem as horas do orçamento. */
    public BigDecimal getHorasPorHectare() {
        return horasPorHectare;
    }

    /** Preço de venda da hora de mão de obra, em R$, que entra no valor da proposta. */
    public BigDecimal getPrecoHora() {
        return precoHora;
    }

    /** Custo da hora de mão de obra, em R$, que entra no custo orçado. */
    public BigDecimal getCustoHora() {
        return custoHora;
    }

    /** Desconto máximo, em %, que o vendedor envia sem a diretoria. */
    public BigDecimal getAlcadaVendedor() {
        return alcadaVendedor;
    }

    /** Validade da proposta, em dias depois do envio. */
    public int getValidadeDias() {
        return validadeDias;
    }

    /** Quantos dias antes do vencimento a proposta entra no alerta. */
    public int getDiasAlerta() {
        return diasAlerta;
    }
}
