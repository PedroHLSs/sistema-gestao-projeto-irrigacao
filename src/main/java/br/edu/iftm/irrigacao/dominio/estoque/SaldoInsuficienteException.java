package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;

import br.edu.iftm.irrigacao.dominio.RegraNegocioException;

/**
 * O UPDATE condicional do estoque não alterou nenhuma linha porque o saldo não comporta a
 * operação (RNF05): reserva acima do disponível, baixa acima do reservado ou ajuste abaixo do
 * reservado. A mensagem traz a quantidade que havia; nada foi alterado.
 */
public class SaldoInsuficienteException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    private final BigDecimal disponivel;

    /**
     * @param mensagem   texto para o usuário, com a quantidade disponível
     * @param disponivel quanto havia para a operação no momento da recusa
     */
    public SaldoInsuficienteException(String mensagem, BigDecimal disponivel) {
        super(mensagem);
        this.disponivel = disponivel;
    }

    public BigDecimal getDisponivel() {
        return disponivel;
    }
}
