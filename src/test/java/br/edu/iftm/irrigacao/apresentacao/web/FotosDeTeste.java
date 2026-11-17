package br.edu.iftm.irrigacao.apresentacao.web;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

import org.springframework.mock.web.MockMultipartFile;

/**
 * Fotos de verdade, bem pequenas, geradas na hora para os testes de upload.
 */
final class FotosDeTeste {

    private FotosDeTeste() {
    }

    static byte[] jpeg() {
        return imagem("jpg");
    }

    static byte[] png() {
        return imagem("png");
    }

    /** Campo {@code novasFotos} do formulário do levantamento. */
    static MockMultipartFile arquivo(String nome, String tipo, byte[] conteudo) {
        return new MockMultipartFile("novasFotos", nome, tipo, conteudo);
    }

    private static byte[] imagem(String formato) {
        BufferedImage imagem = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        imagem.setRGB(1, 1, 0x0B6E4F);
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            ImageIO.write(imagem, formato, saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
