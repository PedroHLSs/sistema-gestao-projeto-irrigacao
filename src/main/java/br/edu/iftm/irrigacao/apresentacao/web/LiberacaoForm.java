package br.edu.iftm.irrigacao.apresentacao.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import br.edu.iftm.irrigacao.dominio.projeto.LiberacaoFaturamentoService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Garantia informada pelo financeiro ao liberar o faturamento (RF20, RF21). As datas aceitas são
 * conferidas pelo {@code LiberacaoFaturamentoService}.
 */
public class LiberacaoForm {

    @NotNull(message = "Informe a data de início da garantia.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicioGarantia;

    @NotNull(message = "Informe o prazo da garantia, em meses.")
    @Min(value = 1, message = "O prazo da garantia é de pelo menos 1 mês.")
    @Max(value = LiberacaoFaturamentoService.PRAZO_MAXIMO_MESES,
            message = "O prazo da garantia é de até {value} meses.")
    private Integer prazoGarantiaMeses;

    public LiberacaoForm() {
    }

    public LiberacaoForm(LocalDate dataInicioGarantia, Integer prazoGarantiaMeses) {
        this.dataInicioGarantia = dataInicioGarantia;
        this.prazoGarantiaMeses = prazoGarantiaMeses;
    }

    public LocalDate getDataInicioGarantia() {
        return dataInicioGarantia;
    }

    public void setDataInicioGarantia(LocalDate dataInicioGarantia) {
        this.dataInicioGarantia = dataInicioGarantia;
    }

    public Integer getPrazoGarantiaMeses() {
        return prazoGarantiaMeses;
    }

    public void setPrazoGarantiaMeses(Integer prazoGarantiaMeses) {
        this.prazoGarantiaMeses = prazoGarantiaMeses;
    }
}
