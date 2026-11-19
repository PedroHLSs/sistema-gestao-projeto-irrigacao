package br.edu.iftm.irrigacao.dominio.projeto;

/**
 * Projeto em FINALIZANDO_EXECUCAO na fila do financeiro (RF20).
 *
 * @param projeto        o projeto, com propriedade e produtor carregados
 * @param motivoBloqueio por que ainda não pode ser liberado; {@code null} quando pode
 */
public record ProjetoParaLiberar(Projeto projeto, String motivoBloqueio) {

    public boolean isBloqueado() {
        return motivoBloqueio != null;
    }
}
