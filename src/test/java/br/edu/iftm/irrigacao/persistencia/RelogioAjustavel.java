package br.edu.iftm.irrigacao.persistencia;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Relógio dos testes: fica parado na data e hora que o teste definir.
 */
public class RelogioAjustavel extends Clock {

    private final ZoneId zona = ZoneId.systemDefault();
    private Instant instante;

    public RelogioAjustavel(LocalDateTime dataHora) {
        definir(dataHora);
    }

    public void definir(LocalDateTime dataHora) {
        this.instante = dataHora.atZone(zona).toInstant();
    }

    /** Adianta o relógio, para separar no tempo dois registros do mesmo teste. */
    public void avancar(Duration intervalo) {
        this.instante = instante.plus(intervalo);
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId outraZona) {
        return Clock.fixed(instante, outraZona);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
