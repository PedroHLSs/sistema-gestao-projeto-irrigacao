package br.edu.iftm.irrigacao.apresentacao.web;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;

/**
 * Formulário de agendamento da visita técnica (RF02). O campo é um {@code datetime-local}, que o
 * navegador envia como {@code 2026-11-20T08:30}.
 */
public class AgendamentoForm {

    @NotNull(message = "Informe a data e a hora da visita.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dataAgendada;

    public AgendamentoForm() {
    }

    public AgendamentoForm(LocalDateTime dataAgendada) {
        this.dataAgendada = dataAgendada;
    }

    public LocalDateTime getDataAgendada() {
        return dataAgendada;
    }

    public void setDataAgendada(LocalDateTime dataAgendada) {
        this.dataAgendada = dataAgendada;
    }
}
