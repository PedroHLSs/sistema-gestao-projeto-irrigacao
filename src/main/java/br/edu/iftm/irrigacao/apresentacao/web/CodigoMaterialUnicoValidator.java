package br.edu.iftm.irrigacao.apresentacao.web;

import br.edu.iftm.irrigacao.dominio.estoque.MaterialService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Confere com o {@code MaterialService} se o código já está em uso (RF06). O service compara o
 * código em maiúsculas e sem espaços nas pontas, como ele é gravado.
 */
public class CodigoMaterialUnicoValidator implements ConstraintValidator<CodigoMaterialUnico, MaterialForm> {

    private final MaterialService materialService;

    public CodigoMaterialUnicoValidator(MaterialService materialService) {
        this.materialService = materialService;
    }

    @Override
    public boolean isValid(MaterialForm form, ConstraintValidatorContext contexto) {
        String codigo = form.getCodigo();
        if (codigo == null || codigo.isBlank() || !materialService.codigoEmUso(codigo, form.getId())) {
            return true;
        }
        contexto.disableDefaultConstraintViolation();
        contexto.buildConstraintViolationWithTemplate(contexto.getDefaultConstraintMessageTemplate())
                .addPropertyNode("codigo")
                .addConstraintViolation();
        return false;
    }
}
