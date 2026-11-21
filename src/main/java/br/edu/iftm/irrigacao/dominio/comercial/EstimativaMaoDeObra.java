package br.edu.iftm.irrigacao.dominio.comercial;

import java.math.BigDecimal;

/**
 * Horas de mão de obra sugeridas para o orçamento (RF09): a área irrigada do levantamento vezes
 * {@code irrigacao.mao-obra.horas-por-hectare}, com 2 casas. O vendedor pode trocar as horas.
 *
 * @param areaIrrigadaHa  área irrigada informada no levantamento, em hectares
 * @param horasPorHectare horas por hectare usadas na conta
 * @param horas           horas sugeridas
 */
public record EstimativaMaoDeObra(BigDecimal areaIrrigadaHa, BigDecimal horasPorHectare, BigDecimal horas) {
}
