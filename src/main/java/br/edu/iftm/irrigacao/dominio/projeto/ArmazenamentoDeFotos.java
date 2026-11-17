package br.edu.iftm.irrigacao.dominio.projeto;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;

/**
 * Fotos do levantamento (RF03) e da medição (RF17) em disco, na pasta {@code irrigacao.upload.dir}.
 *
 * <p>Regras das duas: JPEG ou PNG reconhecido pelos primeiros bytes, até 5 MB, gravado com nome
 * UUID gerado pelo sistema, e o banco guarda só o caminho relativo à pasta. A foto vai para o disco
 * antes do commit; se a transação for desfeita, o arquivo é apagado.
 */
@Component
public class ArmazenamentoDeFotos {

    /** Tamanho máximo de cada foto, em bytes: 5 MB, o mesmo do levantamento. */
    public static final long TAMANHO_MAXIMO = LevantamentoService.TAMANHO_MAXIMO_FOTO;

    private final Path pastaDeUploads;

    public ArmazenamentoDeFotos(@Value("${irrigacao.upload.dir}") String pastaDeUploads) {
        this.pastaDeUploads = Path.of(pastaDeUploads).toAbsolutePath().normalize();
    }

    /**
     * @return o formato reconhecido
     * @throws RegraNegocioException se a foto está vazia, passa de 5 MB ou não é JPEG nem PNG
     */
    public TipoFoto conferir(FotoEnviada foto) {
        String nome = nomeParaMensagem(foto.nomeOriginal());
        if (foto.conteudo() == null || foto.conteudo().length == 0) {
            throw new RegraNegocioException("A foto " + nome + " está vazia.");
        }
        if (foto.conteudo().length > TAMANHO_MAXIMO) {
            throw new RegraNegocioException("A foto " + nome + " passa de 5 MB.");
        }
        return TipoFoto.detectar(foto.conteudo())
                .orElseThrow(() -> new RegraNegocioException("O arquivo " + nome + " não é uma foto JPEG nem PNG."));
    }

    /**
     * Grava a foto já conferida em {@code {pasta de uploads}/{subpasta}/{uuid}.{extensão}}.
     *
     * @param subpasta como {@code levantamento/12}
     * @return o caminho relativo à pasta de uploads, com barras normais, que vai para o banco
     */
    public String gravar(String subpasta, FotoEnviada foto, TipoFoto tipo) {
        String relativo = subpasta + "/" + UUID.randomUUID() + "." + tipo.getExtensao();
        Path destino = pastaDeUploads.resolve(relativo).normalize();
        if (!destino.startsWith(pastaDeUploads)) {
            throw new IllegalArgumentException("Subpasta fora da pasta de uploads: " + subpasta);
        }
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, foto.conteudo());
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gravar a foto.", e);
        }
        apagarSeATransacaoForDesfeita(destino);
        return relativo;
    }

    /**
     * A foto gravada, pronta para ser servida. O caminho vem do banco, mas a conferência garante que
     * nada fora da pasta de uploads é aberto.
     *
     * @throws RegistroNaoEncontradoException se o arquivo não existe ou não é foto
     */
    public FotoArmazenada abrir(String caminhoRelativo) {
        Path arquivo = pastaDeUploads.resolve(caminhoRelativo).normalize();
        Optional<TipoFoto> tipo = TipoFoto.pelaExtensao(arquivo.toString());
        if (!arquivo.startsWith(pastaDeUploads) || tipo.isEmpty() || !Files.isRegularFile(arquivo)) {
            throw new RegistroNaoEncontradoException("Foto não encontrada.");
        }
        return new FotoArmazenada(arquivo, tipo.get());
    }

    private static void apagarSeATransacaoForDesfeita(Path arquivo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        Files.deleteIfExists(arquivo);
                    } catch (IOException e) {
                        // Sobra um arquivo sem registro, que não aparece em lugar nenhum
                    }
                }
            }
        });
    }

    /** Só o nome do arquivo, sem pasta, com no máximo 60 caracteres. */
    private static String nomeParaMensagem(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return "sem nome";
        }
        String nome = nomeOriginal.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1);
        return "\"" + (nome.length() > 60 ? nome.substring(0, 60) + "..." : nome) + "\"";
    }
}
