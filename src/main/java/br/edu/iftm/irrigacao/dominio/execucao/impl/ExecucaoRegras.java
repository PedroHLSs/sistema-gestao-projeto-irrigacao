package br.edu.iftm.irrigacao.dominio.execucao.impl;

import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

/**
 * Conferência comum aos services da execução: cada passo só vale numa situação do projeto
 * (Figura 5). A mensagem diz qual é a situação certa e em qual o projeto está.
 */
final class ExecucaoRegras {

    private ExecucaoRegras() {
    }

    /**
     * @param acao o começo da mensagem, como "A medição é registrada"
     */
    static void exigirSituacao(Projeto projeto, SituacaoProjeto situacao, String acao) {
        if (projeto.getSituacao() != situacao) {
            throw new RegraNegocioException(acao + " com o projeto em \"" + situacao.getRotulo() + "\". O projeto "
                    + projeto.getCodigo() + " está em \"" + projeto.getSituacao().getRotulo() + "\".");
        }
    }
}
