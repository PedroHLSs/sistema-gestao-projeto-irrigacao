package br.edu.iftm.irrigacao.apresentacao.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Cada arquivo enviado é uma foto JPEG ou PNG, reconhecida pelos primeiros bytes, com até 5 MB
 * (RF03). Campo de arquivo vazio passa. O {@code LevantamentoService} confere de novo antes de
 * gravar.
 */
@Documented
@Constraint(validatedBy = { FotosValidasValidator.class, FotosValidasValidator.FotoUnica.class })
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FotosValidas {

    String message() default "Envie só fotos JPEG ou PNG de até 5 MB cada.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
