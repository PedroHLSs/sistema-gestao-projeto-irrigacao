package br.edu.iftm.irrigacao.dominio.projeto.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioLogado;
import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;
import br.edu.iftm.irrigacao.dominio.projeto.HistoricoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.TransicaoInvalidaException;
import br.edu.iftm.irrigacao.dominio.projeto.TransicoesProjeto;
import br.edu.iftm.irrigacao.persistencia.HistoricoProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.PropriedadeRepository;
import br.edu.iftm.irrigacao.persistencia.UsuarioRepository;

/**
 * Abertura do projeto (RF01) e mudança de situação com histórico (RF21).
 */
@Service
public class ProjetoServiceImpl implements ProjetoService {

    /** Maior número que cabe nos 4 dígitos do código PRJ-AAAA-NNNN. */
    static final int MAIOR_NUMERO_DO_ANO = 9999;

    /** Tamanho da coluna {@code historico_projeto.observacao}. */
    static final int TAMANHO_OBSERVACAO = 500;

    static final String OBSERVACAO_ABERTURA = "Projeto aberto.";

    private final ProjetoRepository projetoRepository;
    private final PropriedadeRepository propriedadeRepository;
    private final HistoricoProjetoRepository historicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioLogado usuarioLogado;
    private final Clock clock;

    public ProjetoServiceImpl(ProjetoRepository projetoRepository, PropriedadeRepository propriedadeRepository,
            HistoricoProjetoRepository historicoRepository, UsuarioRepository usuarioRepository,
            UsuarioLogado usuarioLogado, Clock clock) {
        this.projetoRepository = projetoRepository;
        this.propriedadeRepository = propriedadeRepository;
        this.historicoRepository = historicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioLogado = usuarioLogado;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Projeto abrir(Long produtorId, Long propriedadeId) {
        Propriedade propriedade = propriedadeRepository.findById(propriedadeId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Propriedade não encontrada."));
        if (!propriedade.getProdutor().getId().equals(produtorId)) {
            throw new RegraNegocioException("A propriedade escolhida não pertence a esse produtor.");
        }
        Usuario responsavel = usuarioRepository.getReferenceById(usuarioLogado.idAtual()
                .orElseThrow(() -> new RegraNegocioException("Entre no sistema para abrir um projeto.")));
        LocalDate hoje = LocalDate.now(clock);
        String codigo = proximoCodigo(hoje.getYear());
        // Transição 1: o projeto nasce em EM_LEVANTAMENTO, sem situação anterior
        SituacaoProjeto inicial = TransicoesProjeto.SITUACAO_INICIAL;
        exigirPermitida(codigo, null, inicial);

        Projeto projeto = new Projeto();
        projeto.setCodigo(codigo);
        projeto.setPropriedade(propriedade);
        projeto.setResponsavel(responsavel);
        projeto.setDataAbertura(hoje);
        projeto.setSituacao(inicial);
        try {
            projeto = projetoRepository.saveAndFlush(projeto);
        } catch (DataIntegrityViolationException e) {
            // Outra abertura do mesmo instante gerou o mesmo código; a restrição única do banco segurou
            throw new RegraNegocioException("Outro projeto recebeu o código " + codigo
                    + " no mesmo instante. Tente abrir de novo.");
        }
        registrarHistorico(projeto, null, inicial, responsavel, OBSERVACAO_ABERTURA);
        return projeto;
    }

    @Override
    @Transactional
    public Projeto transitar(Long projetoId, SituacaoProjeto novaSituacao, String observacao) {
        Projeto projeto = projetoRepository.buscarParaAlterarSituacao(projetoId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
        SituacaoProjeto anterior = projeto.getSituacao();
        exigirPermitida(projeto.getCodigo(), anterior, novaSituacao);
        String texto = observacao(observacao);
        // Mudança feita sem ninguém logado (uma rotina automática, por exemplo) fica sem usuário
        Usuario usuario = usuarioLogado.idAtual().map(usuarioRepository::getReferenceById).orElse(null);

        projeto.setSituacao(novaSituacao);
        projetoRepository.save(projeto);
        registrarHistorico(projeto, anterior, novaSituacao, usuario, texto);
        return projeto;
    }

    @Override
    @Transactional(readOnly = true)
    public Projeto buscarPorId(Long id) {
        return projetoRepository.buscarComPropriedadeEProdutor(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Projeto> pesquisar(SituacaoProjeto situacao, String busca) {
        Set<SituacaoProjeto> situacoes = situacao == null ? EnumSet.allOf(SituacaoProjeto.class) : EnumSet.of(situacao);
        String termo = busca == null ? "" : busca.strip().toLowerCase(Locale.ROOT);
        return projetoRepository.pesquisar(situacoes, "%" + termo + "%");
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoricoProjeto> historico(Long projetoId) {
        return historicoRepository.listarPorProjeto(projetoId);
    }

    /**
     * Código {@code PRJ-AAAA-NNNN}: o número segue o maior já usado no ano e volta a 0001 no
     * primeiro projeto de cada ano.
     */
    private String proximoCodigo(int ano) {
        String prefixo = "PRJ-" + ano + "-";
        String ultimo = projetoRepository.ultimoCodigo(prefixo + "%");
        int numero = ultimo == null ? 1 : Integer.parseInt(ultimo.substring(prefixo.length())) + 1;
        if (numero > MAIOR_NUMERO_DO_ANO) {
            throw new RegraNegocioException("O ano " + ano + " já chegou ao projeto " + prefixo + MAIOR_NUMERO_DO_ANO
                    + ", o último que o código comporta.");
        }
        return String.format("%s%04d", prefixo, numero);
    }

    private static void exigirPermitida(String codigo, SituacaoProjeto atual, SituacaoProjeto nova) {
        if (!TransicoesProjeto.permite(atual, nova)) {
            throw new TransicaoInvalidaException(codigo, atual, nova);
        }
    }

    private static String observacao(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpo = texto.strip();
        if (limpo.length() > TAMANHO_OBSERVACAO) {
            throw new RegraNegocioException("A observação pode ter até " + TAMANHO_OBSERVACAO + " caracteres.");
        }
        return limpo;
    }

    private void registrarHistorico(Projeto projeto, SituacaoProjeto anterior, SituacaoProjeto nova, Usuario usuario,
            String observacao) {
        historicoRepository.save(new HistoricoProjeto(null, projeto, anterior, nova, usuario,
                LocalDateTime.now(clock), observacao));
    }
}
