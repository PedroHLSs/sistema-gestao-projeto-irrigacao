package br.edu.iftm.irrigacao.apresentacao.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * O código do formulário não pode pertencer a outro material (RF06). Vai na classe, e não no
 * campo, porque na edição a conferência precisa do id. O erro aparece no campo {@code codigo}.
 */
@Documented
@Constraint(validatedBy = CodigoMaterialUnicoValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CodigoMaterialUnico {

    String message() default "Já existe um material com este código.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
