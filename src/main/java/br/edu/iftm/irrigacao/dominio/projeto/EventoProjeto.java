package br.edu.iftm.irrigacao.dominio.projeto;

import java.time.LocalDateTime;

/**
 * Um acontecimento da linha do tempo do projeto (RF21): uma mudança de situação do
 * {@link HistoricoProjeto} ou um registro de etapa, como uma medição ou um apontamento.
 *
 * @param dataHora         quando aconteceu
 * @param titulo           o que aconteceu; {@code null} na mudança de situação, mostrada pelas situações
 * @param detalhe          complemento, opcional
 * @param usuario          nome de quem registrou; {@code null} quando foi uma rotina automática
 * @param situacaoAnterior situação de origem; {@code null} na abertura e nos registros de etapa
 * @param situacaoNova     situação de destino; {@code null} nos registros de etapa
 */
public record EventoProjeto(LocalDateTime dataHora, String titulo, String detalhe, String usuario,
        SituacaoProjeto situacaoAnterior, SituacaoProjeto situacaoNova) {

    /** Registro de uma etapa, sem mudança de situação. */
    public static EventoProjeto deEtapa(LocalDateTime dataHora, String titulo, String detalhe, String usuario) {
        return new EventoProjeto(dataHora, titulo, detalhe, usuario, null, null);
    }
}
