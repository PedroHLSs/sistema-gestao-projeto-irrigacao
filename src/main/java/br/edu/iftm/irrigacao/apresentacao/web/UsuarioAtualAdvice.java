package br.edu.iftm.irrigacao.apresentacao.web;

import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import br.edu.iftm.irrigacao.dominio.acesso.UsuarioAutenticado;

/**
 * Põe o usuário logado no modelo de todas as páginas, com o nome {@code usuarioAtual}, para o
 * fragment do cabeçalho mostrar o nome e o perfil (RNF04).
 */
@ControllerAdvice
public class UsuarioAtualAdvice {

    /**
     * @return o usuário da sessão, ou {@code null} quando não há ninguém logado
     */
    @ModelAttribute("usuarioAtual")
    public UsuarioAtual usuarioAtual(Authentication autenticacao) {
        if (autenticacao == null) {
            return null;
        }
        if (autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return new UsuarioAtual(usuario.getId(), usuario.getNome(), usuario.getPerfil().getRotulo());
        }
        // Autenticação que não veio do formulário de login, como o @WithMockUser dos testes
        String authorities = autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", "));
        return new UsuarioAtual(null, autenticacao.getName(), authorities);
    }
}
