package br.edu.iftm.irrigacao.dominio;

/**
 * O registro pedido não existe. A resposta é a página de erro 404.
 */
public class RegistroNaoEncontradoException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    public RegistroNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
