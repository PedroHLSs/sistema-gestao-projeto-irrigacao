package br.edu.iftm.irrigacao.dominio.projeto;

/**
 * Soma do orçado x realizado de todos os projetos encerrados, para o painel da diretoria (RF22).
 *
 * @param projetos quantos projetos estão encerrados
 * @param apuracao valores somados e margens sobre a soma
 */
public record ResumoOrcadoRealizado(long projetos, Apuracao apuracao) {
}
