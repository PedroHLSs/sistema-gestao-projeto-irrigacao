package br.edu.iftm.irrigacao.apresentacao.web;

import java.util.Arrays;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioLogado;
import br.edu.iftm.irrigacao.dominio.projeto.PainelService;

/**
 * Página inicial do sistema, com o painel do perfil logado.
 */
@Controller
public class HomeController {

    private final PainelService painelService;
    private final UsuarioLogado usuarioLogado;

    public HomeController(PainelService painelService, UsuarioLogado usuarioLogado) {
        this.painelService = painelService;
        this.usuarioLogado = usuarioLogado;
    }

    @GetMapping("/")
    public String index(Authentication autenticacao, Model model) {
        model.addAttribute("painel", painelService.montar(perfil(autenticacao), usuarioLogado.idAtual().orElse(null)));
        return "index";
    }

    /** A authority é o nome do {@link Perfil}. */
    private static Perfil perfil(Authentication autenticacao) {
        return autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .flatMap(authority -> Arrays.stream(Perfil.values()).filter(p -> p.name().equals(authority)))
                .findFirst()
                .orElse(null);
    }
}
