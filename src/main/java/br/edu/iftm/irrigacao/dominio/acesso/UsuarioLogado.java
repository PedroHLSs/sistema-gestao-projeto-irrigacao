package br.edu.iftm.irrigacao.dominio.acesso;

import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import br.edu.iftm.irrigacao.persistencia.UsuarioRepository;

/**
 * Informa quem é o usuário da requisição atual, lendo o {@code SecurityContext} do Spring
 * Security. É a fonte do {@code criadoPor} e do {@code alteradoPor} da auditoria (RNF03).
 */
@Component
public class UsuarioLogado {

    private final UsuarioRepository usuarioRepository;

    public UsuarioLogado(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * No login pelo formulário o id já vem no {@link UsuarioAutenticado}. Quando a autenticação
     * foi montada de outro jeito e só traz o login (como o {@code @WithMockUser} dos testes), o id
     * é procurado no banco.
     *
     * @return o id do {@code Usuario} autenticado, ou vazio quando não há ninguém logado
     */
    public Optional<Long> idAtual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()
                || autenticacao instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        if (autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return Optional.ofNullable(usuario.getId());
        }
        return usuarioRepository.findIdByLogin(autenticacao.getName());
    }
}
