package br.edu.iftm.irrigacao.dominio.projeto;

import java.util.List;

/**
 * O trabalho do projetista, em três filas (RF05, RF15): levantamentos completos esperando o
 * dimensionamento e a lista técnica, projetos com a separação conferida esperando a programação da
 * execução, e projetos em execução, que podem receber outra ordem de serviço.
 *
 * @param aguardandoDimensionamento levantamentos completos de projetos em EM_LEVANTAMENTO
 * @param emProgramacao             projetos em PROGRAMANDO_EXECUCAO
 * @param emExecucao                projetos em EXECUTANDO_INSTALACAO
 */
public record FilaEngenharia(List<Levantamento> aguardandoDimensionamento, List<Projeto> emProgramacao,
        List<Projeto> emExecucao) {
}
