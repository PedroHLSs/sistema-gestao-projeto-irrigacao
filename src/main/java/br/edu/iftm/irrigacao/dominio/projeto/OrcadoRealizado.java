package br.edu.iftm.irrigacao.dominio.projeto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Quadro de orçado x realizado de um projeto (RF22).
 *
 * <p>Limitação conhecida: o modelo não guarda o custo do material na data do consumo, então o
 * realizado usa o {@code custoUnitario} atual do cadastro. As horas usam o {@code valorHora} gravado
 * em cada apontamento.
 *
 * @param apuracao       valores e margens
 * @param custoMateriais soma de quantidade apontada x custo unitário atual
 * @param custoHoras     soma de horas apontadas x valor da hora
 * @param materiais      previsto x apontado de cada material
 * @param horasPrevistas horas da proposta aprovada; {@code null} sem proposta aprovada
 * @param horasApontadas soma das horas apontadas
 */
public record OrcadoRealizado(Apuracao apuracao, BigDecimal custoMateriais, BigDecimal custoHoras,
        List<Material> materiais, BigDecimal horasPrevistas, BigDecimal horasApontadas) {

    /** Previsto x apontado de um material da lista técnica. */
    public record Material(String codigo, String descricao, String unidade, BigDecimal previsto, BigDecimal apontado,
            BigDecimal custoUnitario) {

        public BigDecimal getDiferenca() {
            return apontado.subtract(previsto);
        }

        /** Custo sem arredondar, para o total só ser arredondado no fim. */
        public BigDecimal getCustoApontado() {
            return custoUnitario == null ? BigDecimal.ZERO : apontado.multiply(custoUnitario);
        }
    }

    public BigDecimal getDiferencaHoras() {
        return horasPrevistas == null ? null : horasApontadas.subtract(horasPrevistas);
    }

    public BigDecimal getCustoMateriaisEmCentavos() {
        return custoMateriais.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getCustoHorasEmCentavos() {
        return custoHoras.setScale(2, RoundingMode.HALF_UP);
    }
}
