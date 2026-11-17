package br.edu.iftm.irrigacao.dominio.projeto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Agendamento da visita técnica e agenda das visitas (RF02).
 *
 * <p>O {@link Levantamento} nasce no agendamento, só com {@code dataAgendada}. Os dados de campo
 * (RF03) são preenchidos depois, na visita.
 */
public interface LevantamentoService {

    Optional<Levantamento> buscarPorProjeto(Long projetoId);

    /**
     * A visita pode ser agendada, ou reagendada, enquanto o projeto está em EM_LEVANTAMENTO e a
     * visita ainda não aconteceu.
     *
     * @param levantamento o levantamento do projeto; {@code null} se ainda não foi agendado
     */
    boolean podeAgendarVisita(Projeto projeto, Levantamento levantamento);

    /**
     * Cria o levantamento só com a data agendada ou, se ele já existe e a visita não aconteceu,
     * troca a data.
     *
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException se a data já passou ou se o
     *                                                             projeto não permite agendamento
     */
    Levantamento agendarVisita(Long projetoId, LocalDateTime dataAgendada);

    /**
     * Visitas da semana (segunda a domingo) que contém o dia informado.
     *
     * @param dia qualquer dia da semana desejada; {@code null} é a semana de hoje
     */
    AgendaSemanal agendaDaSemana(LocalDate dia);
}
