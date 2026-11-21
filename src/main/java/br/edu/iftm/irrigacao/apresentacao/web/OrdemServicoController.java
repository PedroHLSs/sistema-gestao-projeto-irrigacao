package br.edu.iftm.irrigacao.apresentacao.web;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioLogado;
import br.edu.iftm.irrigacao.dominio.estoque.ListaTecnicaService;
import br.edu.iftm.irrigacao.dominio.estoque.Quantidade;
import br.edu.iftm.irrigacao.dominio.execucao.AceiteService;
import br.edu.iftm.irrigacao.dominio.execucao.Apontamento;
import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoHoras;
import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoMaterial;
import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoService;
import br.edu.iftm.irrigacao.dominio.execucao.Medicao;
import br.edu.iftm.irrigacao.dominio.execucao.MedicaoService;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServico;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServicoService;
import br.edu.iftm.irrigacao.dominio.execucao.Pendencia;
import br.edu.iftm.irrigacao.dominio.execucao.TipoPendencia;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import jakarta.validation.Valid;

/**
 * Telas de campo da execução (RF15 a RF18, RNF06), sob {@code /campo} (TECNICO_CAMPO, PROJETISTA e
 * ADMINISTRADOR): as ordens abertas do técnico logado (ou de todas as equipes, para quem acompanha) e
 * a tela da ordem, que mostra só o passo da situação atual do projeto (Figura 5): apontar e concluir
 * a instalação, medir, registrar a correção, colher o aceite ou registrar e resolver a pendência.
 *
 * <p>Um técnico só abre as ordens em que está escalado; quem confere é o
 * {@code OrdemServicoService}.
 */
@Controller
@RequestMapping("/campo")
public class OrdemServicoController {

    private static final String TELA_ORDEM = "campo/ordemServico";

    private final OrdemServicoService ordemServicoService;
    private final ApontamentoService apontamentoService;
    private final MedicaoService medicaoService;
    private final AceiteService aceiteService;
    private final ListaTecnicaService listaTecnicaService;
    private final UsuarioLogado usuarioLogado;
    private final Clock clock;

    public OrdemServicoController(OrdemServicoService ordemServicoService, ApontamentoService apontamentoService,
            MedicaoService medicaoService, AceiteService aceiteService, ListaTecnicaService listaTecnicaService,
            UsuarioLogado usuarioLogado, Clock clock) {
        this.ordemServicoService = ordemServicoService;
        this.apontamentoService = apontamentoService;
        this.medicaoService = medicaoService;
        this.aceiteService = aceiteService;
        this.listaTecnicaService = listaTecnicaService;
        this.usuarioLogado = usuarioLogado;
        this.clock = clock;
    }

    @ModelAttribute("tiposPendencia")
    public TipoPendencia[] tiposPendencia() {
        return TipoPendencia.values();
    }

    /**
     * Para o técnico de campo, as ordens abertas em que ele está, em cartões grandes (RNF06). Para
     * quem acompanha a execução (projetista e administrador), as ordens de todas as equipes, com
     * filtro por técnico e a opção de ver as concluídas (RF15).
     *
     * @param tecnico    só as ordens deste técnico; vazio traz todas
     * @param concluidas inclui as ordens já concluídas
     */
    @GetMapping("/minhas-os")
    public String minhasOrdens(@RequestParam(required = false) Long tecnico,
            @RequestParam(defaultValue = "false") boolean concluidas, Authentication autenticacao, Model model) {
        boolean ehTecnico = autenticacao.getAuthorities().stream()
                .anyMatch(authority -> Perfil.TECNICO_CAMPO.name().equals(authority.getAuthority()));
        List<OrdemServico> ordens;
        if (ehTecnico) {
            ordens = usuarioLogado.idAtual()
                    .map(ordemServicoService::abertasDoTecnico)
                    .orElse(List.of());
        } else {
            ordens = ordemServicoService.acompanhamento(tecnico, concluidas);
            model.addAttribute("tecnicos", ordemServicoService.tecnicosDisponiveis());
            model.addAttribute("tecnicoEscolhido", tecnico);
            model.addAttribute("concluidas", concluidas);
        }
        model.addAttribute("acompanhamento", !ehTecnico);
        model.addAttribute("ordens", ordens);
        model.addAttribute("hoje", LocalDate.now(clock));
        return "campo/minhasOrdens";
    }

    @GetMapping("/ordens/{id:\\d+}")
    public String ordem(@PathVariable Long id, Model model) {
        return tela(id, model);
    }

    // ---------- Instalação: apontamentos e conclusão (RF16) ----------

    @PostMapping("/ordens/{id:\\d+}/horas")
    public String apontarHoras(@PathVariable Long id, @Valid @ModelAttribute("horasForm") HorasForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return tela(id, model);
        }
        ApontamentoHoras horas = apontamentoService.apontarHoras(id, form.getHoras());
        redirect.addFlashAttribute("sucesso", Quantidade.formatar(horas.getHoras()) + " h apontadas.");
        return paginaDaOrdem(id);
    }

    @PostMapping("/ordens/{id:\\d+}/materiais")
    public String apontarMaterial(@PathVariable Long id,
            @Valid @ModelAttribute("materialForm") ApontamentoMaterialForm form, BindingResult resultado, Model model,
            RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return tela(id, model);
        }
        ApontamentoMaterial apontamento = apontamentoService.apontarMaterial(id, form.getMaterialId(),
                form.getQuantidade());
        redirect.addFlashAttribute("sucesso", Quantidade.formatar(apontamento.getQuantidade()) + " "
                + apontamento.getMaterial().getUnidade() + " de " + apontamento.getMaterial().getCodigo() + " apontados.");
        apontamentoService.avisoDeExcesso(apontamento.getOrdemServico().getProjeto().getId(), form.getMaterialId())
                .ifPresent(aviso -> redirect.addFlashAttribute("aviso", aviso));
        return paginaDaOrdem(id);
    }

    @PostMapping("/ordens/{id:\\d+}/apontamentos/ultimo/exclusao")
    public String excluirUltimo(@PathVariable Long id, RedirectAttributes redirect) {
        Apontamento excluido = apontamentoService.excluirUltimo(id);
        String texto = excluido instanceof ApontamentoHoras horas
                ? Quantidade.formatar(horas.getHoras()) + " h"
                : Quantidade.formatar(((ApontamentoMaterial) excluido).getQuantidade()) + " de "
                        + ((ApontamentoMaterial) excluido).getMaterial().getCodigo();
        redirect.addFlashAttribute("sucesso", "Último lançamento excluído (" + texto + ").");
        return paginaDaOrdem(id);
    }

    @PostMapping("/ordens/{id:\\d+}/conclusao")
    public String concluirInstalacao(@PathVariable Long id, RedirectAttributes redirect) {
        OrdemServico ordem = ordemServicoService.concluirInstalacao(id);
        List<OrdemServico> faltando = ordemServicoService.instalacaoPendente(ordem.getProjeto().getId());
        if (faltando.isEmpty()) {
            redirect.addFlashAttribute("sucesso", "Instalação concluída. Agora meça a pressão e a vazão.");
        } else {
            redirect.addFlashAttribute("sucesso", "Instalação da OS " + ordem.getNumero() + " concluída. A medição "
                    + "começa quando " + descrever(faltando) + " também concluir.");
        }
        return paginaDaOrdem(id);
    }

    // ---------- Medição (RF17, G6) e correção ----------

    @PostMapping("/ordens/{id:\\d+}/medicoes")
    public String medir(@PathVariable Long id, @Valid @ModelAttribute("medicaoForm") MedicaoForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return tela(id, model);
        }
        Long projetoId = ordemServicoService.buscarParaUsuarioLogado(id).getProjeto().getId();
        Medicao medicao = medicaoService.registrar(projetoId, form.getPressaoMedidaMca(), form.getVazaoMedidaM3h(),
                form.fotoEnviada(), form.getLatitude(), form.getLongitude());
        if (medicao.isAprovada()) {
            redirect.addFlashAttribute("sucesso", "Medição dentro do parâmetro do projeto. Agora colha o aceite do cliente.");
        } else {
            redirect.addFlashAttribute("erro", "Medição fora do parâmetro do projeto. Corrija a instalação e registre "
                    + "a correção para medir de novo.");
        }
        return paginaDaOrdem(id);
    }

    @PostMapping("/ordens/{id:\\d+}/correcao")
    public String registrarCorrecao(@PathVariable Long id, @Valid @ModelAttribute("correcaoForm") CorrecaoForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return tela(id, model);
        }
        ordemServicoService.registrarCorrecao(id, form.getDescricao());
        redirect.addFlashAttribute("sucesso", "Correção registrada. A instalação voltou para execução: aponte o que "
                + "foi usado e conclua a instalação para medir de novo.");
        return paginaDaOrdem(id);
    }

    // ---------- Aceite (RF18, G7) e pendência ----------

    @PostMapping("/ordens/{id:\\d+}/aceite")
    public String colherAceite(@PathVariable Long id, @Valid @ModelAttribute("aceiteForm") AceiteForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return tela(id, model);
        }
        Long projetoId = ordemServicoService.buscarParaUsuarioLogado(id).getProjeto().getId();
        aceiteService.registrarAceite(projetoId, form.getNomeResponsavel(), form.getAssinatura());
        redirect.addFlashAttribute("sucesso", "Aceite gravado e ordem de serviço concluída. O almoxarifado já pode "
                + "conferir as sobras.");
        return paginaDaOrdem(id);
    }

    @PostMapping("/ordens/{id:\\d+}/pendencia")
    public String registrarPendencia(@PathVariable Long id, @Valid @ModelAttribute("pendenciaForm") PendenciaForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("abrirPendencia", true);
            return tela(id, model);
        }
        Long projetoId = ordemServicoService.buscarParaUsuarioLogado(id).getProjeto().getId();
        aceiteService.registrarPendencia(projetoId, form.getTipo(), form.getDescricao(), form.getDataReprogramada());
        redirect.addFlashAttribute("sucesso", "Pendência registrada. Quando ela for tratada, marque como resolvida.");
        return paginaDaOrdem(id);
    }

    @PostMapping("/ordens/{id:\\d+}/pendencia/resolucao")
    public String resolverPendencia(@PathVariable Long id, RedirectAttributes redirect) {
        Long projetoId = ordemServicoService.buscarParaUsuarioLogado(id).getProjeto().getId();
        Pendencia pendencia = aceiteService.resolverPendencia(projetoId);
        redirect.addFlashAttribute("sucesso", pendencia.getTipo() == TipoPendencia.TECNICA
                ? "Pendência técnica resolvida. Meça a pressão e a vazão de novo."
                : "Pendência administrativa resolvida. Colha o aceite do cliente.");
        return paginaDaOrdem(id);
    }

    // ---------- Apoio ----------

    private String tela(Long ordemId, Model model) {
        OrdemServico ordem = ordemServicoService.buscarParaUsuarioLogado(ordemId);
        Projeto projeto = ordem.getProjeto();
        SituacaoProjeto situacao = projeto.getSituacao();
        boolean aberta = ordem.getDataConclusao() == null;
        boolean instalando = aberta && situacao == SituacaoProjeto.EXECUTANDO_INSTALACAO;
        boolean minhaParteConcluida = ordem.getInstalacaoConcluidaEm() != null;
        model.addAttribute("ordem", ordem);
        model.addAttribute("projeto", projeto);
        model.addAttribute("executando", instalando && !minhaParteConcluida);
        model.addAttribute("aguardandoOutras", instalando && minhaParteConcluida);
        if (instalando && minhaParteConcluida) {
            model.addAttribute("faltando", ordemServicoService.instalacaoPendente(projeto.getId()));
        }
        model.addAttribute("validando", aberta && situacao == SituacaoProjeto.VALIDANDO_MEDICAO);
        model.addAttribute("corrigindo", aberta && situacao == SituacaoProjeto.CORRIGINDO_INSTALACAO);
        model.addAttribute("colhendo", aberta && situacao == SituacaoProjeto.COLHENDO_ACEITE);
        model.addAttribute("tratando", aberta && situacao == SituacaoProjeto.TRATANDO_PENDENCIA);

        model.addAttribute("itens", listaTecnicaService.listar(projeto.getId()));
        model.addAttribute("lancamentos", LancamentoDaOrdem.juntar(apontamentoService.horasDaOrdem(ordemId),
                apontamentoService.materiaisDaOrdem(ordemId)));
        if (projeto.getPressaoProjetoMca() != null && projeto.getVazaoProjetoM3h() != null
                && projeto.getToleranciaPercentual() != null) {
            model.addAttribute("faixa", medicaoService.faixa(projeto));
        }
        model.addAttribute("medicoes", medicaoService.doProjeto(projeto.getId()));
        model.addAttribute("pendencia", aceiteService.pendenciaAberta(projeto.getId()).orElse(null));
        model.addAttribute("agora", LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES));

        adicionarSeFaltar(model, "horasForm", new HorasForm());
        adicionarSeFaltar(model, "materialForm", new ApontamentoMaterialForm());
        adicionarSeFaltar(model, "medicaoForm", new MedicaoForm());
        adicionarSeFaltar(model, "correcaoForm", new CorrecaoForm());
        adicionarSeFaltar(model, "aceiteForm", new AceiteForm());
        adicionarSeFaltar(model, "pendenciaForm", new PendenciaForm());
        return TELA_ORDEM;
    }

    /** "a OS 2 (Tânia Técnica)" ou "as OS 2 e 3". */
    private static String descrever(List<OrdemServico> ordens) {
        if (ordens.size() == 1) {
            OrdemServico ordem = ordens.get(0);
            return "a OS " + ordem.getNumero() + " ("
                    + ordem.getTecnicos().stream().map(Usuario::getNome).collect(Collectors.joining(", ")) + ")";
        }
        List<String> numeros = ordens.stream().map(o -> String.valueOf(o.getNumero())).toList();
        return "as OS " + String.join(", ", numeros.subList(0, numeros.size() - 1)) + " e "
                + numeros.get(numeros.size() - 1);
    }

    private static void adicionarSeFaltar(Model model, String nome, Object form) {
        if (!model.containsAttribute(nome)) {
            model.addAttribute(nome, form);
        }
    }

    static String paginaDaOrdem(Long id) {
        return "redirect:/campo/ordens/" + id;
    }
}
