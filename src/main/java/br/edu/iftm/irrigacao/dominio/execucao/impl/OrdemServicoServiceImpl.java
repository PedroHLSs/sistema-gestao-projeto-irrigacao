package br.edu.iftm.irrigacao.dominio.execucao.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioLogado;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServico;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServicoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.OrdemServicoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.UsuarioRepository;

/**
 * Ordem de serviço (RF15) e as transições 8, 9 e 12 da Figura 5.
 */
@Service
public class OrdemServicoServiceImpl implements OrdemServicoService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OrdemServicoRepository ordemRepository;
    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioLogado usuarioLogado;
    private final ProjetoService projetoService;
    private final Clock clock;

    public OrdemServicoServiceImpl(OrdemServicoRepository ordemRepository, ProjetoRepository projetoRepository,
            UsuarioRepository usuarioRepository, UsuarioLogado usuarioLogado, ProjetoService projetoService,
            Clock clock) {
        this.ordemRepository = ordemRepository;
        this.projetoRepository = projetoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioLogado = usuarioLogado;
        this.projetoService = projetoService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> tecnicosDisponiveis() {
        return usuarioRepository.findByPerfilAndAtivoTrueOrderByNome(Perfil.TECNICO_CAMPO);
    }

    @Override
    @Transactional
    public OrdemServico emitir(Long projetoId, LocalDate dataProgramada, List<Long> tecnicosIds) {
        // Linha travada antes de ler a situação: dois envios juntos não emitem duas ordens
        Projeto projeto = projetoRepository.buscarParaAlterarSituacao(projetoId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
        ExecucaoRegras.exigirSituacao(projeto, SituacaoProjeto.PROGRAMANDO_EXECUCAO, "A ordem de serviço é emitida");
        if (dataProgramada == null || dataProgramada.isBefore(LocalDate.now(clock))) {
            throw new RegraNegocioException("Informe a data programada, de hoje em diante.");
        }
        if (tecnicosIds == null || tecnicosIds.isEmpty()) {
            throw new RegraNegocioException("Escolha pelo menos um técnico de campo.");
        }
        List<Usuario> tecnicos = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(tecnicosIds)) {
            Usuario tecnico = usuarioRepository.findById(id)
                    .filter(u -> u.isAtivo() && u.getPerfil() == Perfil.TECNICO_CAMPO)
                    .orElseThrow(() -> new RegraNegocioException("Só usuários ativos com perfil Técnico de campo "
                            + "entram na ordem de serviço."));
            tecnicos.add(tecnico);
        }
        List<OrdemServico> anteriores = ordemRepository.findByProjetoIdOrderByNumero(projetoId);
        int numero = anteriores.isEmpty() ? 1 : anteriores.get(anteriores.size() - 1).getNumero() + 1;
        OrdemServico ordem = ordemRepository.save(new OrdemServico(null, projeto, numero, dataProgramada, null, tecnicos));
        projetoService.transitar(projetoId, SituacaoProjeto.EXECUTANDO_INSTALACAO, "Ordem de serviço " + numero
                + " emitida para " + DATA.format(dataProgramada) + " com "
                + tecnicos.stream().map(Usuario::getNome).collect(Collectors.joining(", ")) + ".");
        return ordem;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemServico> doProjeto(Long projetoId) {
        return ordemRepository.listarDoProjeto(projetoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemServico> abertasDoTecnico(Long usuarioId) {
        return ordemRepository.abertasDoTecnico(usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public OrdemServico buscarParaUsuarioLogado(Long ordemId) {
        OrdemServico ordem = ordemRepository.buscarCompleta(ordemId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Ordem de serviço não encontrada."));
        Long usuarioId = usuarioLogado.idAtual().orElse(null);
        if (usuarioId != null) {
            boolean tecnico = usuarioRepository.findById(usuarioId)
                    .map(u -> u.getPerfil() == Perfil.TECNICO_CAMPO)
                    .orElse(false);
            boolean escalado = ordem.getTecnicos().stream().anyMatch(t -> t.getId().equals(usuarioId));
            if (tecnico && !escalado) {
                throw new RegistroNaoEncontradoException("Ordem de serviço não encontrada.");
            }
        }
        return ordem;
    }

    @Override
    @Transactional
    public OrdemServico concluirInstalacao(Long ordemId) {
        OrdemServico ordem = buscarAberta(ordemId);
        ExecucaoRegras.exigirSituacao(ordem.getProjeto(), SituacaoProjeto.EXECUTANDO_INSTALACAO,
                "A instalação é concluída");
        projetoService.transitar(ordem.getProjeto().getId(), SituacaoProjeto.VALIDANDO_MEDICAO,
                "Instalação concluída (ordem de serviço " + ordem.getNumero() + ").");
        return ordem;
    }

    @Override
    @Transactional
    public OrdemServico registrarCorrecao(Long ordemId, String descricao) {
        OrdemServico ordem = buscarAberta(ordemId);
        ExecucaoRegras.exigirSituacao(ordem.getProjeto(), SituacaoProjeto.CORRIGINDO_INSTALACAO,
                "A correção é registrada");
        String texto = descricao == null || descricao.isBlank() ? null : descricao.strip();
        if (texto != null && texto.length() > 450) {
            throw new RegraNegocioException("O texto da correção pode ter até 450 caracteres.");
        }
        projetoService.transitar(ordem.getProjeto().getId(), SituacaoProjeto.EXECUTANDO_INSTALACAO,
                texto == null ? "Correção registrada." : "Correção registrada: " + texto);
        return ordem;
    }

    /**
     * A ordem aberta, com a linha do projeto travada antes de carregar a ordem: assim a situação
     * lida já é a de quem terminou primeiro, num clique duplo.
     */
    private OrdemServico buscarAberta(Long ordemId) {
        ordemRepository.projetoDaOrdem(ordemId).ifPresent(projetoRepository::buscarParaAlterarSituacao);
        OrdemServico ordem = buscarParaUsuarioLogado(ordemId);
        if (ordem.getDataConclusao() != null) {
            throw new RegraNegocioException("A ordem de serviço " + ordem.getNumero() + " já foi concluída.");
        }
        return ordem;
    }
}
