package br.edu.iftm.irrigacao.apresentacao.web;

import br.edu.iftm.irrigacao.dominio.projeto.DimensionamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Formulário do dimensionamento (RF05): os valores que o projetista calculou fora do sistema.
 */
public class DimensionamentoForm {

    @NotNull(message = "Informe a pressão de projeto.")
    @Positive(message = "A pressão deve ser maior que zero.")
    private Double pressaoProjetoMca;

    @NotNull(message = "Informe a vazão de projeto.")
    @Positive(message = "A vazão deve ser maior que zero.")
    private Double vazaoProjetoM3h;

    @NotNull(message = "Informe a tolerância.")
    @DecimalMin(value = "0", message = "A tolerância vai de 0 a 100%.")
    @DecimalMax(value = "100", message = "A tolerância vai de 0 a 100%.")
    private Double toleranciaPercentual = DimensionamentoService.TOLERANCIA_PADRAO;

    public DimensionamentoForm() {
    }

    public DimensionamentoForm(Projeto projeto) {
        this.pressaoProjetoMca = projeto.getPressaoProjetoMca();
        this.vazaoProjetoM3h = projeto.getVazaoProjetoM3h();
        this.toleranciaPercentual = projeto.getToleranciaPercentual() == null
                ? DimensionamentoService.TOLERANCIA_PADRAO
                : projeto.getToleranciaPercentual();
    }

    public Double getPressaoProjetoMca() {
        return pressaoProjetoMca;
    }

    public void setPressaoProjetoMca(Double pressaoProjetoMca) {
        this.pressaoProjetoMca = pressaoProjetoMca;
    }

    public Double getVazaoProjetoM3h() {
        return vazaoProjetoM3h;
    }

    public void setVazaoProjetoM3h(Double vazaoProjetoM3h) {
        this.vazaoProjetoM3h = vazaoProjetoM3h;
    }

    public Double getToleranciaPercentual() {
        return toleranciaPercentual;
    }

    public void setToleranciaPercentual(Double toleranciaPercentual) {
        this.toleranciaPercentual = toleranciaPercentual;
    }
}
