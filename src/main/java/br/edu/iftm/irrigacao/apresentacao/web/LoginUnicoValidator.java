package br.edu.iftm.irrigacao.apresentacao.web;

import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Confere com o {@code UsuarioService} se o login já está em uso (RF23).
 */
public class LoginUnicoValidator implements ConstraintValidator<LoginUnico, UsuarioForm> {

    private final UsuarioService usuarioService;

    public LoginUnicoValidator(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public boolean isValid(UsuarioForm form, ConstraintValidatorContext contexto) {
        String login = form.getLogin();
        if (login == null || login.isBlank() || !usuarioService.loginEmUso(login, form.getId())) {
            return true;
        }
        contexto.disableDefaultConstraintViolation();
        contexto.buildConstraintViolationWithTemplate(contexto.getDefaultConstraintMessageTemplate())
                .addPropertyNode("login")
                .addConstraintViolation();
        return false;
    }
}
