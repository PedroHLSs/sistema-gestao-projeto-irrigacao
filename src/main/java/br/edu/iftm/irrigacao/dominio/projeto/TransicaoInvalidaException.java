package br.edu.iftm.irrigacao.dominio.projeto;

import java.util.stream.Collectors;

import br.edu.iftm.irrigacao.dominio.RegraNegocioException;

/**
 * Mudança de situação que o mapa de {@link TransicoesProjeto} não permite (RF21). Lançada por
 * {@code ProjetoService.transitar(...)} antes de qualquer alteração, então o projeto e o histórico
 * ficam como estavam.
 */
public class TransicaoInvalidaException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    private final SituacaoProjeto situacaoAtual;
    private final SituacaoProjeto situacaoPedida;

    /**
     * @param codigoProjeto  código do projeto, para a mensagem
     * @param situacaoAtual  situação de agora; {@code null} na abertura
     * @param situacaoPedida situação pedida
     */
    public TransicaoInvalidaException(String codigoProjeto, SituacaoProjeto situacaoAtual,
            SituacaoProjeto situacaoPedida) {
        super(mensagem(codigoProjeto, situacaoAtual, situacaoPedida));
        this.situacaoAtual = situacaoAtual;
        this.situacaoPedida = situacaoPedida;
    }

    public SituacaoProjeto getSituacaoAtual() {
        return situacaoAtual;
    }

    public SituacaoProjeto getSituacaoPedida() {
        return situacaoPedida;
    }

    private static String mensagem(String codigo, SituacaoProjeto atual, SituacaoProjeto pedida) {
        if (pedida == null) {
            return "Informe a nova situação do projeto " + codigo + ".";
        }
        if (atual == null) {
            return "O projeto " + codigo + " só pode ser aberto em \"" + TransicoesProjeto.SITUACAO_INICIAL.getRotulo()
                    + "\", e não em \"" + pedida.getRotulo() + "\".";
        }
        if (TransicoesProjeto.ehFinal(atual)) {
            return "O projeto " + codigo + " está \"" + atual.getRotulo()
                    + "\", que é uma situação final: ele não muda mais de situação.";
        }
        if (atual == pedida) {
            return "O projeto " + codigo + " já está em \"" + atual.getRotulo() + "\".";
        }
        String seguintes = TransicoesProjeto.seguintes(atual).stream()
                .map(s -> "\"" + s.getRotulo() + "\"")
                .collect(Collectors.joining(" ou "));
        return "O projeto " + codigo + " está em \"" + atual.getRotulo() + "\" e não pode passar para \""
                + pedida.getRotulo() + "\". Daqui ele só pode seguir para " + seguintes + ".";
    }
}
