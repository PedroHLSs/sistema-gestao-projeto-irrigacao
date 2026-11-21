package br.edu.iftm.irrigacao.dominio.projeto.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.EntidadeAuditada;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.comercial.Proposta;
import br.edu.iftm.irrigacao.dominio.estoque.MovimentacaoEstoque;
import br.edu.iftm.irrigacao.dominio.estoque.Quantidade;
import br.edu.iftm.irrigacao.dominio.execucao.Aceite;
import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoHoras;
import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoMaterial;
import br.edu.iftm.irrigacao.dominio.execucao.Medicao;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServico;
import br.edu.iftm.irrigacao.dominio.execucao.Pendencia;
import br.edu.iftm.irrigacao.dominio.projeto.EventoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.HistoricoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.LinhaDoTempoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.AceiteRepository;
import br.edu.iftm.irrigacao.persistencia.ApontamentoHorasRepository;
import br.edu.iftm.irrigacao.persistencia.ApontamentoMaterialRepository;
import br.edu.iftm.irrigacao.persistencia.HistoricoProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.LevantamentoRepository;
import br.edu.iftm.irrigacao.persistencia.MedicaoRepository;
import br.edu.iftm.irrigacao.persistencia.MovimentacaoEstoqueRepository;
import br.edu.iftm.irrigacao.persistencia.OrdemServicoRepository;
import br.edu.iftm.irrigacao.persistencia.PendenciaRepository;
import br.edu.iftm.irrigacao.persistencia.PropostaRepository;
import br.edu.iftm.irrigacao.persistencia.UsuarioRepository;

/**
 * Monta a linha do tempo (RF21). Levantamento, proposta, ordem de serviço e pendência não têm campo
 * próprio de usuário: quem registrou vem da auditoria (RNF03), que guarda o id. Os nomes são lidos de
 * uma vez só.
 *
 * <p>Num empate de horário, o registro da etapa vem antes da mudança de situação que ele provocou.
 */
@Service
public class LinhaDoTempoServiceImpl implements LinhaDoTempoService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final HistoricoProjetoRepository historicoRepository;
    private final LevantamentoRepository levantamentoRepository;
    private final PropostaRepository propostaRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final OrdemServicoRepository ordemRepository;
    private final ApontamentoHorasRepository horasRepository;
    private final ApontamentoMaterialRepository materialRepository;
    private final MedicaoRepository medicaoRepository;
    private final AceiteRepository aceiteRepository;
    private final PendenciaRepository pendenciaRepository;
    private final UsuarioRepository usuarioRepository;

    public LinhaDoTempoServiceImpl(HistoricoProjetoRepository historicoRepository,
            LevantamentoRepository levantamentoRepository, PropostaRepository propostaRepository,
            MovimentacaoEstoqueRepository movimentacaoRepository, OrdemServicoRepository ordemRepository,
            ApontamentoHorasRepository horasRepository, ApontamentoMaterialRepository materialRepository,
            MedicaoRepository medicaoRepository, AceiteRepository aceiteRepository,
            PendenciaRepository pendenciaRepository, UsuarioRepository usuarioRepository) {
        this.historicoRepository = historicoRepository;
        this.levantamentoRepository = levantamentoRepository;
        this.propostaRepository = propostaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.ordemRepository = ordemRepository;
        this.horasRepository = horasRepository;
        this.materialRepository = materialRepository;
        this.medicaoRepository = medicaoRepository;
        this.aceiteRepository = aceiteRepository;
        this.pendenciaRepository = pendenciaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoProjeto> doProjeto(Long projetoId) {
        Levantamento levantamento = levantamentoRepository.findByProjetoId(projetoId)
                .filter(l -> l.getDataVisita() != null)
                .orElse(null);
        List<Proposta> propostas = propostaRepository.listarDoProjeto(projetoId);
        List<OrdemServico> ordens = ordemRepository.listarDoProjeto(projetoId);
        List<Pendencia> pendencias = pendenciaRepository.findByProjetoIdOrderByDataRegistroDesc(projetoId);
        Map<Long, String> nomes = nomesDosAutores(levantamento, propostas, ordens, pendencias);

        List<EventoProjeto> eventos = new ArrayList<>();
        if (levantamento != null) {
            eventos.add(EventoProjeto.deEtapa(levantamento.getAlteradoEm(), "Levantamento enviado",
                    levantamento.getCultura(), nomes.get(levantamento.getAlteradoPor())));
        }
        propostas.forEach(p -> eventos.add(EventoProjeto.deEtapa(p.getCriadoEm(), "Proposta " + p.getNumero() + " gerada",
                detalheDaProposta(p), nomes.get(p.getCriadoPor()))));
        List<HistoricoProjeto> historico = historicoRepository.listarPorProjeto(projetoId);
        PeriodoDaInstalacao instalacao = PeriodoDaInstalacao.de(ordens, historico);
        movimentacaoRepository.listarPorProjetoComMaterial(projetoId)
                .forEach(m -> adicionarMovimentacao(eventos, m, instalacao));
        ordens.forEach(o -> eventos.add(EventoProjeto.deEtapa(o.getCriadoEm(), "OS " + o.getNumero() + " emitida",
                detalheDaOrdem(o), nomes.get(o.getCriadoPor()))));
        horasRepository.listarPorProjeto(projetoId).forEach(a -> eventos.add(eventoDeHoras(a)));
        materialRepository.listarPorProjetoComMaterial(projetoId).forEach(a -> eventos.add(eventoDeMaterial(a)));
        medicaoRepository.findByProjetoIdOrderByDataHoraDescIdDesc(projetoId).forEach(m -> eventos.add(eventoDeMedicao(m)));
        aceiteRepository.findByProjetoId(projetoId).ifPresent(a -> eventos.add(eventoDeAceite(a)));
        pendencias.forEach(p -> eventos.add(EventoProjeto.deEtapa(p.getDataRegistro(),
                "Pendência " + p.getTipo().getRotulo().toLowerCase(Locale.ROOT) + " registrada", p.getDescricao(),
                nomes.get(p.getCriadoPor()))));
        historico.forEach(h -> eventos.add(eventoDeHistorico(h)));

        // A ordenação é estável: no mesmo horário, vale a ordem em que os eventos entraram acima
        return eventos.stream()
                .filter(evento -> evento.dataHora() != null)
                .sorted(Comparator.comparing(EventoProjeto::dataHora))
                .toList();
    }

    /** Nome de cada usuário citado na auditoria dos registros, numa consulta só. */
    private Map<Long, String> nomesDosAutores(Levantamento levantamento, List<Proposta> propostas,
            List<OrdemServico> ordens, List<Pendencia> pendencias) {
        List<Long> ids = Stream.of(propostas.stream(), ordens.stream(), pendencias.stream())
                .flatMap(registros -> registros.map(EntidadeAuditada::getCriadoPor))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
        if (levantamento != null && levantamento.getAlteradoPor() != null) {
            ids.add(levantamento.getAlteradoPor());
        }
        Map<Long, String> nomes = new HashMap<>();
        usuarioRepository.findAllById(ids).forEach(usuario -> nomes.put(usuario.getId(), usuario.getNome()));
        return nomes;
    }

    /**
     * Da emissão da primeira ordem de serviço até a chegada a FINALIZANDO_EXECUCAO. Saída de estoque
     * nesse período é material apontado além do separado (RF16), e devolução é essa baixa desfeita
     * pela exclusão de um lançamento; fora dele são a separação e a sobra.
     *
     * @param inicio criação da primeira ordem; {@code null} sem ordem
     * @param fim    primeira chegada a FINALIZANDO_EXECUCAO; {@code null} enquanto não chegou
     */
    private record PeriodoDaInstalacao(LocalDateTime inicio, LocalDateTime fim) {

        static PeriodoDaInstalacao de(List<OrdemServico> ordens, List<HistoricoProjeto> historico) {
            LocalDateTime inicio = ordens.stream()
                    .map(OrdemServico::getCriadoEm)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            LocalDateTime fim = historico.stream()
                    .filter(h -> h.getSituacaoNova() == SituacaoProjeto.FINALIZANDO_EXECUCAO)
                    .map(HistoricoProjeto::getDataHora)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            return new PeriodoDaInstalacao(inicio, fim);
        }

        boolean contem(LocalDateTime momento) {
            return inicio != null && momento != null && momento.isAfter(inicio) && (fim == null || momento.isBefore(fim));
        }
    }

    /** Só a separação e a devolução entram na linha do tempo; reservas e compras não são etapas do projeto. */
    private static void adicionarMovimentacao(List<EventoProjeto> eventos, MovimentacaoEstoque movimentacao,
            PeriodoDaInstalacao instalacao) {
        boolean naInstalacao = instalacao.contem(movimentacao.getDataHora());
        String titulo = switch (movimentacao.getTipo()) {
            case SAIDA_PROJETO -> naInstalacao ? "Material usado além do separado baixado do estoque" : "Material separado";
            case DEVOLUCAO -> naInstalacao ? "Baixa além do separado desfeita" : "Sobra devolvida ao estoque";
            default -> null;
        };
        if (titulo != null) {
            eventos.add(EventoProjeto.deEtapa(movimentacao.getDataHora(), titulo,
                    quantidade(movimentacao.getQuantidade(), movimentacao.getMaterial().getUnidade(),
                            movimentacao.getMaterial().getCodigo()),
                    nome(movimentacao.getUsuario())));
        }
    }

    private static String detalheDaProposta(Proposta proposta) {
        if (proposta.getPercentualDesconto().signum() == 0) {
            return "Sem desconto";
        }
        String desconto = "Desconto de " + Quantidade.formatar(proposta.getPercentualDesconto()) + "%";
        return proposta.getAprovadoPor() == null ? desconto
                : desconto + ", aprovado por " + proposta.getAprovadoPor().getNome();
    }

    private static String detalheDaOrdem(OrdemServico ordem) {
        return "Programada para " + DATA.format(ordem.getDataProgramada()) + " com "
                + ordem.getTecnicos().stream().map(Usuario::getNome).collect(Collectors.joining(", "));
    }

    private static EventoProjeto eventoDeHoras(ApontamentoHoras apontamento) {
        return EventoProjeto.deEtapa(apontamento.getDataHora(), "Horas apontadas",
                "OS " + apontamento.getOrdemServico().getNumero() + ": " + Quantidade.formatar(apontamento.getHoras()) + " h",
                nome(apontamento.getUsuario()));
    }

    private static EventoProjeto eventoDeMaterial(ApontamentoMaterial apontamento) {
        return EventoProjeto.deEtapa(apontamento.getDataHora(), "Material apontado",
                "OS " + apontamento.getOrdemServico().getNumero() + ": " + quantidade(apontamento.getQuantidade(),
                        apontamento.getMaterial().getUnidade(), apontamento.getMaterial().getCodigo()),
                nome(apontamento.getUsuario()));
    }

    private static EventoProjeto eventoDeMedicao(Medicao medicao) {
        return EventoProjeto.deEtapa(medicao.getDataHora(),
                medicao.isAprovada() ? "Medição dentro do parâmetro" : "Medição fora do parâmetro",
                Quantidade.formatar(BigDecimal.valueOf(medicao.getPressaoMedidaMca())) + " mca e "
                        + Quantidade.formatar(BigDecimal.valueOf(medicao.getVazaoMedidaM3h())) + " m³/h",
                nome(medicao.getUsuario()));
    }

    private static EventoProjeto eventoDeAceite(Aceite aceite) {
        return EventoProjeto.deEtapa(aceite.getDataHora(), "Aceite colhido", "Assinado por " + aceite.getNomeResponsavel(),
                nome(aceite.getUsuario()));
    }

    private static EventoProjeto eventoDeHistorico(HistoricoProjeto historico) {
        return new EventoProjeto(historico.getDataHora(), null, historico.getObservacao(), nome(historico.getUsuario()),
                historico.getSituacaoAnterior(), historico.getSituacaoNova());
    }

    private static String quantidade(BigDecimal quantidade, String unidade, String codigo) {
        return Quantidade.formatar(quantidade) + " " + unidade + " de " + codigo;
    }

    private static String nome(Usuario usuario) {
        return usuario == null ? null : usuario.getNome();
    }
}
