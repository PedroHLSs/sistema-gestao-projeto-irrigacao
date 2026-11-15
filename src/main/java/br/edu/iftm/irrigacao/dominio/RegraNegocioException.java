package br.edu.iftm.irrigacao.dominio;

/**
 * Erro de regra de negócio. A mensagem é escrita para o usuário e aparece no topo da página,
 * pelo tratamento feito em {@code TratadorDeErros}.
 */
public class RegraNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
