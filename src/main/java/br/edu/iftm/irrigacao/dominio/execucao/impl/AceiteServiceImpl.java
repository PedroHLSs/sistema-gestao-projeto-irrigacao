package br.edu.iftm.irrigacao.dominio.execucao.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioLogado;
import br.edu.iftm.irrigacao.dominio.execucao.Aceite;
import br.edu.iftm.irrigacao.dominio.execucao.AceiteService;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServico;
import br.edu.iftm.irrigacao.dominio.execucao.Pendencia;
import br.edu.iftm.irrigacao.dominio.execucao.TipoPendencia;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.TipoFoto;
import br.edu.iftm.irrigacao.persistencia.AceiteRepository;
import br.edu.iftm.irrigacao.persistencia.OrdemServicoRepository;
import br.edu.iftm.irrigacao.persistencia.PendenciaRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.UsuarioRepository;

/**
 * Aceite (RF18, G7) e pendência (transições 13 a 16).
 *
 * <p>A assinatura chega do {@code <canvas>} como PNG em base64. Antes de gravar, o service confere
 * que é PNG de verdade, com tamanho razoável e pelo menos um traço: canvas em branco não vale como
 * assinatura.
 */
@Service
public class AceiteServiceImpl implements AceiteService {

    private static final String PREFIXO_PNG = "data:image/png;base64,";
    private static final int LARGURA_MAXIMA = 4000;
    private static final int ALTURA_MAXIMA = 2000;
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final AceiteRepository aceiteRepository;
    private final PendenciaRepository pendenciaRepository;
    private final OrdemServicoRepository ordemRepository;
    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioLogado usuarioLogado;
    private final ProjetoService projetoService;
    private final Clock clock;

    public AceiteServiceImpl(AceiteRepository aceiteRepository, PendenciaRepository pendenciaRepository,
            OrdemServicoRepository ordemRepository, ProjetoRepository projetoRepository,
            UsuarioRepository usuarioRepository, UsuarioLogado usuarioLogado, ProjetoService projetoService,
            Clock clock) {
        this.aceiteRepository = aceiteRepository;
        this.pendenciaRepository = pendenciaRepository;
        this.ordemRepository = ordemRepository;
        this.projetoRepository = projetoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioLogado = usuarioLogado;
        this.projetoService = projetoService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Aceite registrarAceite(Long projetoId, String nomeResponsavel, String assinaturaPng) {
        Projeto projeto = buscarProjeto(projetoId);
        ExecucaoRegras.exigirSituacao(projeto, SituacaoProjeto.COLHENDO_ACEITE, "O aceite é colhido");
        String nome = nomeResponsavel == null ? "" : nomeResponsavel.strip();
        if (nome.isEmpty()) {
            throw new RegraNegocioException("Informe o nome de quem assina pelo cliente.");
        }
        if (nome.length() > 120) {
            throw new RegraNegocioException("O nome de quem assina pode ter até 120 caracteres.");
        }
        String assinatura = conferirAssinatura(assinaturaPng);
        if (aceiteRepository.findByProjetoId(projetoId).isPresent()) {
            throw new RegraNegocioException("O aceite do projeto " + projeto.getCodigo() + " já foi colhido.");
        }

        Aceite aceite = aceiteRepository.save(new Aceite(null, projeto, LocalDateTime.now(clock), nome, assinatura,
                usuarioAtual()));
        LocalDate hoje = LocalDate.now(clock);
        for (OrdemServico ordem : ordemRepository.findByProjetoIdAndDataConclusaoIsNull(projetoId)) {
            ordem.setDataConclusao(hoje);
        }
        projetoService.transitar(projetoId, SituacaoProjeto.FINALIZANDO_EXECUCAO, "Aceite assinado por " + nome + ".");
        return aceite;
    }

    @Override
    @Transactional
    public Pendencia registrarPendencia(Long projetoId, TipoPendencia tipo, String descricao,
            LocalDateTime dataReprogramada) {
        Projeto projeto = buscarProjeto(projetoId);
        ExecucaoRegras.exigirSituacao(projeto, SituacaoProjeto.COLHENDO_ACEITE, "A pendência é registrada");
        if (tipo == null) {
            throw new RegraNegocioException("Escolha se a pendência é técnica ou administrativa.");
        }
        String texto = descricao == null ? "" : descricao.strip();
        if (texto.isEmpty()) {
            throw new RegraNegocioException("Descreva por que o cliente não assinou.");
        }
        if (texto.length() > 500) {
            throw new RegraNegocioException("A descrição da pendência pode ter até 500 caracteres.");
        }
        LocalDateTime agora = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        if (dataReprogramada == null || dataReprogramada.truncatedTo(ChronoUnit.MINUTES).isBefore(agora)) {
            throw new RegraNegocioException("Informe para quando o tratamento foi reprogramado, de agora em diante.");
        }
        LocalDateTime reprogramada = dataReprogramada.truncatedTo(ChronoUnit.MINUTES);
        Pendencia pendencia = pendenciaRepository.save(new Pendencia(null, projeto, tipo, texto,
                LocalDateTime.now(clock), reprogramada, false));
        projetoService.transitar(projetoId, SituacaoProjeto.TRATANDO_PENDENCIA, "Cliente não assinou: pendência "
                + tipo.getRotulo().toLowerCase() + " reprogramada para " + DATA_HORA.format(reprogramada) + ". "
                + texto);
        return pendencia;
    }

    @Override
    @Transactional
    public Pendencia resolverPendencia(Long projetoId) {
        Projeto projeto = buscarProjeto(projetoId);
        ExecucaoRegras.exigirSituacao(projeto, SituacaoProjeto.TRATANDO_PENDENCIA, "A pendência é resolvida");
        List<Pendencia> abertas = pendenciaRepository.findByProjetoIdAndResolvidaFalse(projetoId);
        if (abertas.isEmpty()) {
            throw new RegraNegocioException("O projeto " + projeto.getCodigo() + " não tem pendência aberta.");
        }
        Pendencia maisNova = abertas.stream()
                .max((a, b) -> a.getDataRegistro().compareTo(b.getDataRegistro()))
                .orElseThrow();
        abertas.forEach(p -> p.setResolvida(true));
        if (maisNova.getTipo() == TipoPendencia.TECNICA) {
            projetoService.transitar(projetoId, SituacaoProjeto.VALIDANDO_MEDICAO,
                    "Pendência técnica resolvida; a instalação volta para a medição.");
        } else {
            projetoService.transitar(projetoId, SituacaoProjeto.COLHENDO_ACEITE,
                    "Pendência administrativa resolvida; o aceite pode ser colhido de novo.");
        }
        return maisNova;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Aceite> aceiteDoProjeto(Long projetoId) {
        return aceiteRepository.findByProjetoId(projetoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pendencia> pendenciasDoProjeto(Long projetoId) {
        return pendenciaRepository.findByProjetoIdOrderByDataRegistroDesc(projetoId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Pendencia> pendenciaAberta(Long projetoId) {
        return pendenciaRepository.findByProjetoIdAndResolvidaFalse(projetoId).stream()
                .max((a, b) -> a.getDataRegistro().compareTo(b.getDataRegistro()));
    }

    /**
     * @return o PNG em base64, sem o prefixo
     */
    private static String conferirAssinatura(String assinaturaPng) {
        String base64 = assinaturaPng == null ? "" : assinaturaPng.strip();
        if (base64.startsWith(PREFIXO_PNG)) {
            base64 = base64.substring(PREFIXO_PNG.length());
        }
        if (base64.isEmpty()) {
            throw new RegraNegocioException("Sem a assinatura do cliente o aceite não é gravado.");
        }
        if (base64.length() > TAMANHO_MAXIMO_ASSINATURA) {
            throw new RegraNegocioException("A assinatura enviada é grande demais. Limpe o quadro e assine de novo.");
        }
        byte[] png;
        try {
            png = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw assinaturaInvalida();
        }
        if (TipoFoto.detectar(png).filter(tipo -> tipo == TipoFoto.PNG).isEmpty()) {
            throw assinaturaInvalida();
        }
        if (!temTraco(png)) {
            throw new RegraNegocioException("Sem a assinatura do cliente o aceite não é gravado.");
        }
        return base64;
    }

    /** Pelo menos um pixel visível e que não seja branco. */
    private static boolean temTraco(byte[] png) {
        BufferedImage imagem = lerPng(png);
        boolean comTransparencia = imagem.getColorModel().hasAlpha();
        for (int y = 0; y < imagem.getHeight(); y++) {
            for (int x = 0; x < imagem.getWidth(); x++) {
                int argb = imagem.getRGB(x, y);
                int alfa = argb >>> 24;
                int vermelho = (argb >> 16) & 0xff;
                int verde = (argb >> 8) & 0xff;
                int azul = argb & 0xff;
                boolean branco = vermelho > 240 && verde > 240 && azul > 240;
                if ((!comTransparencia || alfa > 0) && !branco) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Lê as dimensões antes da imagem, para não abrir um PNG pequeno que descompacta em gigabytes. */
    private static BufferedImage lerPng(byte[] png) {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(png))) {
            Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) {
                throw assinaturaInvalida();
            }
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                if (leitor.getWidth(0) > LARGURA_MAXIMA || leitor.getHeight(0) > ALTURA_MAXIMA) {
                    throw new RegraNegocioException("A assinatura enviada é grande demais. Limpe o quadro e assine de novo.");
                }
                return leitor.read(0);
            } finally {
                leitor.dispose();
            }
        } catch (IOException e) {
            throw assinaturaInvalida();
        }
    }

    private static RegraNegocioException assinaturaInvalida() {
        return new RegraNegocioException("A assinatura enviada não é uma imagem válida. Limpe o quadro e assine de novo.");
    }

    /** Com a linha travada antes de ler a situação, para dois envios juntos não passarem os dois. */
    private Projeto buscarProjeto(Long projetoId) {
        return projetoRepository.buscarParaAlterarSituacao(projetoId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
    }

    private Usuario usuarioAtual() {
        return usuarioLogado.idAtual().map(usuarioRepository::getReferenceById).orElse(null);
    }
}
