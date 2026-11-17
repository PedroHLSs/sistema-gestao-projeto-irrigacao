package br.edu.iftm.irrigacao.apresentacao.web;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import br.edu.iftm.irrigacao.dominio.execucao.TipoPendencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Pendência registrada quando o cliente não assina o aceite (RF18, Figura 4). */
public class PendenciaForm {

    @NotNull(message = "Escolha se a pendência é técnica ou administrativa.")
    private TipoPendencia tipo;

    @NotBlank(message = "Descreva por que o cliente não assinou.")
    @Size(max = 500, message = "Use até 500 caracteres.")
    private String descricao;

    @NotNull(message = "Informe para quando o tratamento foi reprogramado.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dataReprogramada;

    public PendenciaForm() {
    }

    public PendenciaForm(TipoPendencia tipo, String descricao, LocalDateTime dataReprogramada) {
        this.tipo = tipo;
        this.descricao = descricao;
        this.dataReprogramada = dataReprogramada;
    }

    public TipoPendencia getTipo() {
        return tipo;
    }

    public void setTipo(TipoPendencia tipo) {
        this.tipo = tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDateTime getDataReprogramada() {
        return dataReprogramada;
    }

    public void setDataReprogramada(LocalDateTime dataReprogramada) {
        this.dataReprogramada = dataReprogramada;
    }
}
