package br.edu.iftm.irrigacao.dominio.projeto;

import java.util.List;

import br.edu.iftm.irrigacao.dominio.estoque.ProgressoSeparacao;

/**
 * O trabalho do almoxarifado, em três filas (RF14, RF19): projetos esperando a separação e a
 * liberação da execução, projetos já em execução com material ainda por separar (a obra pode começar
 * com parte do material) e projetos com o aceite colhido esperando a devolução das sobras.
 *
 * @param aSeparar       projetos em PREPARANDO_MATERIAL
 * @param aContinuar     projetos com a execução liberada, antes do aceite, com item ainda não separado
 *                       por completo
 * @param sobrasAReceber projetos em FINALIZANDO_EXECUCAO com as sobras por tratar
 */
public record FilaEstoque(List<SeparacaoNaFila> aSeparar, List<SeparacaoNaFila> aContinuar,
        List<Projeto> sobrasAReceber) {

    /** O projeto e o andamento da separação dele. */
    public record SeparacaoNaFila(Projeto projeto, ProgressoSeparacao progresso) {
    }

    /** Projetos com separação por fazer ou por continuar, o número do painel do almoxarifado. */
    public int separacoesPendentes() {
        return aSeparar.size() + aContinuar.size();
    }
}
