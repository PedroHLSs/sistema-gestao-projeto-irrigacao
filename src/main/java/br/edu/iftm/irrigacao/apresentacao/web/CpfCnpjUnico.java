package br.edu.iftm.irrigacao.apresentacao.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * O CPF/CNPJ do formulário não pode pertencer a outro produtor (RF01). Vai na classe, e não no
 * campo, porque na edição a conferência precisa do id para não contar o próprio produtor. O erro
 * aparece no campo {@code cpfCnpj}.
 */
@Documented
@Constraint(validatedBy = CpfCnpjUnicoValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CpfCnpjUnico {

    String message() default "Já existe um produtor com este CPF/CNPJ.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
