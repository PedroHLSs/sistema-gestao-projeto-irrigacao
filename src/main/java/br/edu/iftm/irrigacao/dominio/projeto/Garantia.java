package br.edu.iftm.irrigacao.dominio.projeto;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Garantia do projeto encerrado (RF21), calculada a partir de {@code dataInicioGarantia} e
 * {@code prazoGarantiaMeses}. O enum de situação não separa "em garantia" de "garantia vencida"
 * (seção 4 do docs/modelo.md): isso depende da data de hoje.
 *
 * <p>A garantia vale até {@code fim}, inclusive, e está vencida no dia seguinte, a mesma contagem da
 * validade da proposta.
 *
 * @param inicio     primeiro dia da garantia
 * @param prazoMeses prazo em meses
 * @param fim        último dia da garantia
 * @param vigente    {@code true} até o último dia, inclusive
 */
public record Garantia(LocalDate inicio, int prazoMeses, LocalDate fim, boolean vigente) {

    /** @return a garantia do projeto; vazio enquanto o financeiro não a registrou */
    public static Optional<Garantia> doProjeto(Projeto projeto, LocalDate hoje) {
        if (projeto.getDataInicioGarantia() == null || projeto.getPrazoGarantiaMeses() == null) {
            return Optional.empty();
        }
        LocalDate fim = projeto.getDataInicioGarantia().plusMonths(projeto.getPrazoGarantiaMeses());
        return Optional.of(new Garantia(projeto.getDataInicioGarantia(), projeto.getPrazoGarantiaMeses(), fim,
                !hoje.isAfter(fim)));
    }
}
