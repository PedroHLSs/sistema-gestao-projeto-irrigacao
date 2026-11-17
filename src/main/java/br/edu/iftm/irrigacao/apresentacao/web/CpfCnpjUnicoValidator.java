package br.edu.iftm.irrigacao.apresentacao.web;

import br.edu.iftm.irrigacao.dominio.comercial.CpfCnpj;
import br.edu.iftm.irrigacao.dominio.comercial.ProdutorService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Confere com o {@code ProdutorService} se o CPF/CNPJ já está em uso (RF01). Documento inválido
 * não é conferido aqui: o erro dele já vem do {@link CpfCnpjValido}.
 */
public class CpfCnpjUnicoValidator implements ConstraintValidator<CpfCnpjUnico, ProdutorForm> {

    private final ProdutorService produtorService;

    public CpfCnpjUnicoValidator(ProdutorService produtorService) {
        this.produtorService = produtorService;
    }

    @Override
    public boolean isValid(ProdutorForm form, ConstraintValidatorContext contexto) {
        String cpfCnpj = form.getCpfCnpj();
        if (!CpfCnpj.valido(cpfCnpj) || !produtorService.cpfCnpjEmUso(cpfCnpj, form.getId())) {
            return true;
        }
        contexto.disableDefaultConstraintViolation();
        contexto.buildConstraintViolationWithTemplate(contexto.getDefaultConstraintMessageTemplate())
                .addPropertyNode("cpfCnpj")
                .addConstraintViolation();
        return false;
    }
}
