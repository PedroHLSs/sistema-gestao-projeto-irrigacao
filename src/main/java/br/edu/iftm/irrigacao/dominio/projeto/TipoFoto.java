package br.edu.iftm.irrigacao.dominio.projeto;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Formatos de foto aceitos no levantamento (RF03): JPEG e PNG.
 *
 * <p>O formato é reconhecido pelos primeiros bytes do arquivo, e não pelo nome nem pelo tipo que o
 * navegador informa, que o usuário controla. O arquivo é gravado com a extensão do formato
 * reconhecido.
 */
public enum TipoFoto {
    JPEG("jpg", "image/jpeg", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }),
    PNG("png", "image/png", new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' });

    private final String extensao;
    private final String tipoConteudo;
    private final byte[] assinatura;

    TipoFoto(String extensao, String tipoConteudo, byte[] assinatura) {
        this.extensao = extensao;
        this.tipoConteudo = tipoConteudo;
        this.assinatura = assinatura;
    }

    public String getExtensao() {
        return extensao;
    }

    /** O Content-Type com que a foto é servida. */
    public String getTipoConteudo() {
        return tipoConteudo;
    }

    /** O formato pelos primeiros bytes do arquivo; vazio quando não é JPEG nem PNG. */
    public static Optional<TipoFoto> detectar(byte[] conteudo) {
        if (conteudo == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(tipo -> conteudo.length >= tipo.assinatura.length
                        && Arrays.equals(conteudo, 0, tipo.assinatura.length, tipo.assinatura, 0, tipo.assinatura.length))
                .findFirst();
    }

    /** O formato de uma foto já gravada, pela extensão do caminho. */
    public static Optional<TipoFoto> pelaExtensao(String caminho) {
        String nome = caminho.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(tipo -> nome.endsWith("." + tipo.extensao)).findFirst();
    }
}
