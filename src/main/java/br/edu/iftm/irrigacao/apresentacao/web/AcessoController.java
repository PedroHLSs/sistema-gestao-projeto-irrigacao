package br.edu.iftm.irrigacao.apresentacao.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Páginas de login e de acesso negado (RNF04). Quem confere a senha e encerra a sessão é o
 * Spring Security, configurado em {@code SecurityConfig}.
 */
@Controller
public class AcessoController {

    /** Quem já está logado não precisa ver o formulário de novo. */
    @GetMapping("/login")
    public String login(Authentication autenticacao) {
        if (autenticacao != null) {
            return "redirect:/";
        }
        return "acesso/login";
    }

    /**
     * O Spring Security encaminha para cá, com status 403, a requisição que o perfil do usuário
     * não pode fazer. Aceita qualquer método porque o encaminhamento mantém o da requisição
     * original, que pode ser um POST.
     */
    @RequestMapping("/acesso-negado")
    public String acessoNegado() {
        return "acesso/acessoNegado";
    }
}
