package br.edu.iftm.irrigacao.apresentacao.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Tratamento dos erros de negócio lançados pelos services, para todos os controllers.
 */
@ControllerAdvice
public class TratadorDeErros {

    /** Registro que não existe: página de erro 404. */
    @ExceptionHandler(RegistroNaoEncontradoException.class)
    public void registroNaoEncontrado(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.NOT_FOUND.value());
    }

    /**
     * Regra de negócio violada: volta para a página de onde o usuário veio, com a mensagem da
     * exceção no topo.
     */
    @ExceptionHandler(RegraNegocioException.class)
    public String regraNegocio(RegraNegocioException excecao, HttpServletRequest request,
            RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", excecao.getMessage());
        return "redirect:" + paginaAnterior(request);
    }

    /**
     * Caminho da página que enviou a requisição, tirado do cabeçalho Referer. Só o caminho é
     * usado, então o redirecionamento nunca sai da aplicação. Sem Referer aproveitável, o destino
     * é a página inicial.
     */
    private String paginaAnterior(HttpServletRequest request) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null) {
            return "/";
        }
        try {
            URI origem = new URI(referer);
            String caminho = origem.getRawPath();
            String contexto = request.getContextPath();
            boolean daAplicacao = request.getServerName().equalsIgnoreCase(origem.getHost())
                    && caminho != null && caminho.startsWith(contexto + "/");
            if (!daAplicacao) {
                return "/";
            }
            caminho = caminho.substring(contexto.length());
            boolean seguro = !caminho.startsWith("//") && !caminho.contains("\\");
            boolean mesmaPagina = "GET".equals(request.getMethod()) && origem.getRawPath().equals(request.getRequestURI());
            if (!seguro || mesmaPagina) {
                return "/";
            }
            return origem.getRawQuery() == null ? caminho : caminho + "?" + origem.getRawQuery();
        } catch (URISyntaxException e) {
            return "/";
        }
    }
}
