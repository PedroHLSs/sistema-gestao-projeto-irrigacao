package br.edu.iftm.irrigacao.apresentacao.web;

import br.edu.iftm.irrigacao.dominio.comercial.CpfCnpj;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Confere o dígito verificador do CPF ou CNPJ com {@link CpfCnpj} (RF01).
 */
public class CpfCnpjValidoValidator implements ConstraintValidator<CpfCnpjValido, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        return valor == null || valor.isBlank() || CpfCnpj.valido(valor);
    }
}
