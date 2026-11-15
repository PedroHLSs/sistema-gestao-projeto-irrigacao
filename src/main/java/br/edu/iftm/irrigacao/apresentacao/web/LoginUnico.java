package br.edu.iftm.irrigacao.apresentacao.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * O login do formulário não pode pertencer a outro usuário (RF23). Vai na classe, e não no campo,
 * porque na edição a conferência precisa do id para não contar o login do próprio usuário. O erro
 * aparece no campo {@code login}.
 */
@Documented
@Constraint(validatedBy = LoginUnicoValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUnico {

    String message() default "Já existe um usuário com este login.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
