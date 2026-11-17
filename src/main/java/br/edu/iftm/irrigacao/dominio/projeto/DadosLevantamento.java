package br.edu.iftm.irrigacao.dominio.projeto;

import java.time.LocalDateTime;

/**
 * Campos do levantamento preenchidos em campo (RF03). Todos podem vir vazios, porque o rascunho é
 * salvo como estiver; quem cobra o preenchimento é o G1 (RF04).
 *
 * @param vazaoFonte em m³/h
 */
public record DadosLevantamento(LocalDateTime dataVisita, String cultura, Double areaIrrigadaHa,
        TipoFonteAgua fonteAgua, Double vazaoFonte, String topografia, Double latitude, Double longitude) {
}
