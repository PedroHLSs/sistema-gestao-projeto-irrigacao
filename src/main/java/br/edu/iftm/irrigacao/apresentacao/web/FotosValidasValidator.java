package br.edu.iftm.irrigacao.apresentacao.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import br.edu.iftm.irrigacao.dominio.projeto.LevantamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.TipoFoto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Confere tamanho e formato das fotos enviadas no levantamento (RF03). As mensagens são fixas e
 * não levam o nome do arquivo, que vem do usuário.
 */
public class FotosValidasValidator implements ConstraintValidator<FotosValidas, List<MultipartFile>> {

    static final String MENSAGEM_TAMANHO = "Cada foto pode ter até 5 MB.";
    static final String MENSAGEM_FORMATO = "Envie só fotos JPEG ou PNG.";

    @Override
    public boolean isValid(List<MultipartFile> arquivos, ConstraintValidatorContext contexto) {
        if (arquivos == null) {
            return true;
        }
        Set<String> problemas = new LinkedHashSet<>();
        for (MultipartFile arquivo : arquivos) {
            if (arquivo == null || arquivo.isEmpty()) {
                continue;
            }
            if (arquivo.getSize() > LevantamentoService.TAMANHO_MAXIMO_FOTO) {
                problemas.add(MENSAGEM_TAMANHO);
            } else if (!ehFoto(arquivo)) {
                problemas.add(MENSAGEM_FORMATO);
            }
        }
        if (problemas.isEmpty()) {
            return true;
        }
        contexto.disableDefaultConstraintViolation();
        problemas.forEach(problema -> contexto.buildConstraintViolationWithTemplate(problema).addConstraintViolation());
        return false;
    }

    private static boolean ehFoto(MultipartFile arquivo) {
        try (InputStream entrada = arquivo.getInputStream()) {
            return TipoFoto.detectar(entrada.readNBytes(8)).isPresent();
        } catch (IOException e) {
            return false;
        }
    }
}
