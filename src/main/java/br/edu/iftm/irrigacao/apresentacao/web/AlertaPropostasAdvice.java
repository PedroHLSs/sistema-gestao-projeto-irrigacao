package br.edu.iftm.irrigacao.apresentacao.web;

import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import br.edu.iftm.irrigacao.dominio.acesso.UsuarioAutenticado;
import br.edu.iftm.irrigacao.dominio.comercial.PropostaService;

/**
 * Põe em todas as páginas, com o nome {@code propostasVencendo}, quantas propostas enviadas dos
 * projetos do usuário vencem em até {@code irrigacao.proposta.dias-alerta} dias: o
 * {@code alertarVencimento()} da Figura 9 (RF11), mostrado como badge no menu. Só para quem acessa
 * o comercial.
 */
@ControllerAdvice
public class AlertaPropostasAdvice {

    private static final Set<String> PERFIS_DO_COMERCIAL = Set.of("VENDEDOR", "DIRETORIA", "ADMINISTRADOR");

    private final PropostaService propostaService;

    public AlertaPropostasAdvice(PropostaService propostaService) {
        this.propostaService = propostaService;
    }

    @ModelAttribute("propostasVencendo")
    public int propostasVencendo(Authentication autenticacao) {
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            return 0;
        }
        boolean doComercial = autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(PERFIS_DO_COMERCIAL::contains);
        return doComercial ? propostaService.alertarVencimento(usuario.getId()).size() : 0;
    }
}
