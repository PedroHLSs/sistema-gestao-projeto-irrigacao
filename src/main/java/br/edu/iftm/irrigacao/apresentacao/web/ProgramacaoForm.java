package br.edu.iftm.irrigacao.apresentacao.web;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Programação da equipe para emitir a ordem de serviço (RF15). Perfil e situação dos técnicos são
 * conferidos pelo {@code OrdemServicoService}.
 */
public class ProgramacaoForm {

    @NotNull(message = "Informe a data programada.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataProgramada;

    @NotEmpty(message = "Escolha pelo menos um técnico de campo.")
    private List<Long> tecnicos = new ArrayList<>();

    public ProgramacaoForm() {
    }

    public ProgramacaoForm(LocalDate dataProgramada, List<Long> tecnicos) {
        this.dataProgramada = dataProgramada;
        this.tecnicos = tecnicos;
    }

    public LocalDate getDataProgramada() {
        return dataProgramada;
    }

    public void setDataProgramada(LocalDate dataProgramada) {
        this.dataProgramada = dataProgramada;
    }

    public List<Long> getTecnicos() {
        return tecnicos;
    }

    public void setTecnicos(List<Long> tecnicos) {
        this.tecnicos = tecnicos;
    }
}
