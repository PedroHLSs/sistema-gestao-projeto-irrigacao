package br.edu.iftm.irrigacao.dominio.projeto;

import java.nio.file.Path;

/**
 * Foto do levantamento já gravada em disco, pronta para ser servida (RF03).
 *
 * @param arquivo caminho completo do arquivo, sempre dentro de {@code irrigacao.upload.dir}
 * @param tipo    formato da foto, que define o Content-Type da resposta
 */
public record FotoArmazenada(Path arquivo, TipoFoto tipo) {
}
