package br.edu.iftm.irrigacao.apresentacao.web;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.edu.iftm.irrigacao.dominio.comercial.Proposta;
import br.edu.iftm.irrigacao.dominio.comercial.PropostaService;
import br.edu.iftm.irrigacao.dominio.estoque.DisponibilidadeItem;
import br.edu.iftm.irrigacao.dominio.estoque.EstoqueService;
import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.ListaTecnicaService;
import br.edu.iftm.irrigacao.dominio.estoque.ProgressoSeparacao;
import br.edu.iftm.irrigacao.dominio.execucao.MedicaoService;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServicoService;
import br.edu.iftm.irrigacao.dominio.projeto.AgendaSemanal;
import br.edu.iftm.irrigacao.dominio.projeto.FotoArmazenada;
import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.LevantamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.LinhaDoTempoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

/**
 * Consulta dos projetos, aberta a todos os perfis (RNF04): a lista com filtro por situação e busca,
 * a página central do projeto com a linha do tempo (RF21), a agenda semanal das visitas (RF02) e
 * as fotos do levantamento (RF03).
 *
 * <p>A página do projeto mostra o levantamento e a lista técnica em modo leitura e só oferece as
 * ações que a situação atual permite: agendar a visita (comercial), preencher o levantamento
 * (campo) e dimensionar (engenharia), cada uma na tela do seu perfil.
 */
@Controller
@RequestMapping("/projetos")
public class ProjetoController {

    private final ProjetoService projetoService;
    private final LevantamentoService levantamentoService;
    private final ListaTecnicaService listaTecnicaService;
    private final PropostaService propostaService;
    private final EstoqueService estoqueService;
    private final MedicaoService medicaoService;
    private final OrdemServicoService ordemServicoService;
    private final PainelExecucao painelExecucao;
    private final PainelEncerramento painelEncerramento;
    private final PassosDosProjetos passosDosProjetos;
    private final LinhaDoTempoService linhaDoTempoService;
    private final Clock clock;

    public ProjetoController(ProjetoService projetoService, LevantamentoService levantamentoService,
            ListaTecnicaService listaTecnicaService, PropostaService propostaService, EstoqueService estoqueService,
            MedicaoService medicaoService, OrdemServicoService ordemServicoService, PainelExecucao painelExecucao,
            PainelEncerramento painelEncerramento, PassosDosProjetos passosDosProjetos,
            LinhaDoTempoService linhaDoTempoService, Clock clock) {
        this.projetoService = projetoService;
        this.levantamentoService = levantamentoService;
        this.listaTecnicaService = listaTecnicaService;
        this.propostaService = propostaService;
        this.estoqueService = estoqueService;
        this.medicaoService = medicaoService;
        this.ordemServicoService = ordemServicoService;
        this.painelExecucao = painelExecucao;
        this.painelEncerramento = painelEncerramento;
        this.passosDosProjetos = passosDosProjetos;
        this.linhaDoTempoService = linhaDoTempoService;
        this.clock = clock;
    }

    /**
     * Cada projeto vem com o próximo passo e com quem está ({@link ProximoPasso}), para a lista dizer
     * onde cada um parou.
     *
     * @param situacao só os projetos desta situação; vazio traz todas
     * @param busca    trecho do código ou do nome do produtor
     * @param meus     só os projetos em que o próximo passo é do usuário
     */
    @GetMapping
    public String listar(@RequestParam(required = false) SituacaoProjeto situacao,
            @RequestParam(required = false) String busca, @RequestParam(defaultValue = "false") boolean meus,
            Model model, Authentication autenticacao) {
        List<Projeto> projetos = projetoService.pesquisar(situacao, busca);
        Map<Long, ProximoPasso> passos = passosDosProjetos.calcular(projetos, ProximoPasso.Quem.de(autenticacao));
        if (meus) {
            projetos = projetos.stream()
                    .filter(projeto -> passos.containsKey(projeto.getId()) && passos.get(projeto.getId()).acao() != null)
                    .toList();
        }
        model.addAttribute("projetos", projetos);
        model.addAttribute("passos", passos);
        model.addAttribute("fases", FaseProjeto.values());
        model.addAttribute("situacao", situacao);
        model.addAttribute("busca", busca);
        model.addAttribute("meus", meus);
        return "projetos/listarProjetos";
    }

    @GetMapping("/{id:\\d+}")
    public String detalhe(@PathVariable Long id, Model model, Authentication autenticacao) {
        Projeto projeto = projetoService.buscarPorId(id);
        Levantamento levantamento = levantamentoService.buscarPorProjeto(id).orElse(null);
        List<String> faltantes = levantamentoService.camposFaltantes(id);
        boolean levantamentoCompleto = faltantes.isEmpty();
        List<ItemProjeto> itens = listaTecnicaService.listar(id);
        List<Proposta> propostas = propostaService.listarDoProjeto(id);
        boolean podePreencher = levantamentoService.podePreencher(projeto);
        model.addAttribute("projeto", projeto);
        // Topo da página: o que falta fazer e com quem, e a aba de cada fase com o seu estado
        model.addAttribute("proximoPasso", ProximoPasso.de(projeto, levantamento, levantamentoCompleto,
                propostas.isEmpty() ? null : propostas.get(0), ordemServicoService.doProjeto(id),
                ProximoPasso.Quem.de(autenticacao)).orElse(null));
        model.addAttribute("abaAtual", AbaProjeto.daFase(projeto, levantamentoCompleto));
        model.addAttribute("abas", AbaProjeto.comEstados(projeto, levantamentoCompleto));
        model.addAttribute("linhaDoTempo", linhaDoTempoService.doProjeto(id));
        model.addAttribute("levantamento", levantamento);
        model.addAttribute("faltantes", faltantes);
        model.addAttribute("itens", itens);
        model.addAttribute("total", listaTecnicaService.totalEstimado(itens));
        // Aba Material (RF07, RF08, RF14): reserva na aprovação da proposta, faltantes e separação
        List<DisponibilidadeItem> disponibilidade = estoqueService.disponibilidade(id);
        model.addAttribute("materialComecou",
                AbaProjeto.MATERIAL.estadoNa(projeto, levantamentoCompleto) != AbaProjeto.Estado.PENDENTE);
        model.addAttribute("disponibilidade", disponibilidade);
        model.addAttribute("progressoSeparacao",
                new ProgressoSeparacao(disponibilidade.stream().map(DisponibilidadeItem::getItem).toList()));
        model.addAttribute("itensFaltantes", disponibilidade.stream()
                .filter(item -> item.getFaltante().signum() > 0)
                .count());
        // A separação e a compra do restante continuam durante a instalação, até o aceite (RF13, RF14)
        model.addAttribute("separacaoAberta", projeto.getSituacao().isMaterialEmAndamento());
        // Cancelado na proposta, o projeto não chega ao material, à execução nem ao encerramento
        model.addAttribute("cancelado", projeto.getSituacao() == SituacaoProjeto.CANCELADO);
        model.addAttribute("podeAgendarVisita", levantamentoService.podeAgendarVisita(projeto, levantamento));
        model.addAttribute("podePreencherLevantamento", podePreencher);
        model.addAttribute("podeDimensionar", podePreencher && levantamentoCompleto);
        // Aba Proposta (RF09 a RF12): todas as versões e o que dá para fazer agora
        boolean orcando = projeto.getSituacao() == SituacaoProjeto.ORCANDO;
        model.addAttribute("propostas", propostas);
        model.addAttribute("orcando", orcando);
        model.addAttribute("podeGerarOrcamento", orcando && propostaService.abertaDoProjeto(id).isEmpty());
        // Aba Execução (RF15 a RF19)
        painelExecucao.preencher(model, projeto);
        // Aba Encerramento (RF21, RF22)
        painelEncerramento.preencher(model, projeto);
        return "projetos/detalheProjeto";
    }

    /** Foto da medição (RF17), procurada pelo caminho gravado na medição daquele projeto. */
    @GetMapping("/{id:\\d+}/medicoes/{medicaoId:\\d+}/foto")
    public ResponseEntity<Resource> fotoDaMedicao(@PathVariable Long id, @PathVariable Long medicaoId) {
        return servir(medicaoService.foto(id, medicaoId));
    }

    /**
     * Foto do levantamento pela posição na lista (RF03). Qualquer usuário logado vê o projeto, e por
     * isso a foto; o arquivo é procurado pelo caminho gravado no levantamento daquele projeto, nunca
     * por um nome vindo da URL.
     */
    @GetMapping("/{id:\\d+}/levantamento/fotos/{indice:\\d+}")
    public ResponseEntity<Resource> foto(@PathVariable Long id, @PathVariable int indice) {
        return servir(levantamentoService.foto(id, indice));
    }

    private static ResponseEntity<Resource> servir(FotoArmazenada foto) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipo().getTipoConteudo()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(new FileSystemResource(foto.arquivo()));
    }

    /**
     * @param semana qualquer dia da semana desejada, no formato 2026-11-16; vazio é a semana de hoje
     * @param mes    mês do calendário ao lado da agenda, no formato 2026-12; vazio é o mês da semana.
     *               Muda só esse calendário: a semana aberta continua a mesma
     */
    @GetMapping("/agenda")
    public String agenda(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana,
            @RequestParam(required = false) YearMonth mes, Model model) {
        AgendaSemanal agenda = levantamentoService.agendaDaSemana(semana);
        model.addAttribute("agenda", agenda);
        model.addAttribute("miniCalendario", MiniCalendario.de(
                mes != null ? mes : MiniCalendario.mesDaSemana(agenda.getInicio()), agenda.getInicio(),
                LocalDate.now(clock)));
        return "projetos/agenda";
    }

    static String paginaDoProjeto(Long projetoId) {
        return "redirect:/projetos/" + projetoId;
    }
}
