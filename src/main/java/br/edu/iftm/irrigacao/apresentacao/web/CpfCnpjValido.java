package br.edu.iftm.irrigacao.apresentacao.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * O campo é um CPF ou CNPJ com os dígitos verificadores corretos, com ou sem pontuação (RF01).
 * Campo vazio passa: quem cobra o preenchimento é o {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = CpfCnpjValidoValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CpfCnpjValido {

    String message() default "CPF ou CNPJ inválido. Confira os números digitados.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
