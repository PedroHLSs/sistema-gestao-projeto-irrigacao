package br.edu.iftm.irrigacao.apresentacao.web;

import java.time.format.DateTimeFormatter;
import java.util.List;

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

import br.edu.iftm.irrigacao.dominio.comercial.ProdutorService;
import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;
import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.LevantamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.TransicoesProjeto;
import jakarta.validation.Valid;

/**
 * Ações do comercial sobre o projeto: abertura (RF01, atividade 1) e agendamento da visita técnica
 * (RF02, atividade 2). Ficam sob {@code /comercial}, liberado para VENDEDOR, DIRETORIA e
 * ADMINISTRADOR (RNF04). A consulta do projeto, aberta a todos os perfis, está em
 * {@link ProjetoController}.
 */
@Controller
@RequestMapping("/comercial/projetos")
public class ProjetoComercialController {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final ProjetoService projetoService;
    private final ProdutorService produtorService;
    private final LevantamentoService levantamentoService;

    public ProjetoComercialController(ProjetoService projetoService, ProdutorService produtorService,
            LevantamentoService levantamentoService) {
        this.projetoService = projetoService;
        this.produtorService = produtorService;
        this.levantamentoService = levantamentoService;
    }

    /**
     * @param propriedadeId propriedade já escolhida, quando o usuário vem da página do produtor
     */
    @GetMapping("/novo")
    public String novo(@RequestParam(required = false) Long propriedadeId, Model model) {
        List<Propriedade> propriedades = produtorService.listarPropriedadesComProdutor();
        AberturaProjetoForm form = new AberturaProjetoForm();
        propriedades.stream()
                .filter(p -> p.getId().equals(propriedadeId))
                .findFirst()
                .ifPresent(p -> {
                    form.setProdutorId(p.getProdutor().getId());
                    form.setPropriedadeId(p.getId());
                });
        model.addAttribute("abertura", form);
        return formularioDeAbertura(model, propriedades);
    }

    @PostMapping
    public String abrir(@Valid @ModelAttribute("abertura") AberturaProjetoForm form, BindingResult resultado,
            Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return formularioDeAbertura(model, produtorService.listarPropriedadesComProdutor());
        }
        Projeto projeto = projetoService.abrir(form.getProdutorId(), form.getPropriedadeId());
        redirect.addFlashAttribute("sucesso",
                "Projeto " + projeto.getCodigo() + " aberto. O próximo passo é agendar a visita técnica.");
        return ProjetoController.paginaDoProjeto(projeto.getId());
    }

    @GetMapping("/{id:\\d+}/visita")
    public String agendamento(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        Projeto projeto = projetoService.buscarPorId(id);
        Levantamento levantamento = levantamentoService.buscarPorProjeto(id).orElse(null);
        if (!levantamentoService.podeAgendarVisita(projeto, levantamento)) {
            redirect.addFlashAttribute("erro", "A visita do projeto " + projeto.getCodigo()
                    + " não pode mais ser agendada: o agendamento só vale em \"Em levantamento\", antes da visita.");
            return ProjetoController.paginaDoProjeto(id);
        }
        model.addAttribute("agendamento",
                new AgendamentoForm(levantamento == null ? null : levantamento.getDataAgendada()));
        return formularioDeAgendamento(model, projeto, levantamento);
    }

    @PostMapping("/{id:\\d+}/visita")
    public String agendar(@PathVariable Long id, @Valid @ModelAttribute("agendamento") AgendamentoForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return formularioDeAgendamento(model, projetoService.buscarPorId(id),
                    levantamentoService.buscarPorProjeto(id).orElse(null));
        }
        Levantamento levantamento = levantamentoService.agendarVisita(id, form.getDataAgendada());
        redirect.addFlashAttribute("sucesso", "Visita do projeto " + levantamento.getProjeto().getCodigo()
                + " agendada para " + DATA_HORA.format(levantamento.getDataAgendada()) + ".");
        return ProjetoController.paginaDoProjeto(id);
    }

    private String formularioDeAbertura(Model model, List<Propriedade> propriedades) {
        model.addAttribute("produtores", produtorService.pesquisar(null));
        model.addAttribute("propriedades", propriedades);
        model.addAttribute("situacaoInicial", TransicoesProjeto.SITUACAO_INICIAL);
        return "comercial/abrirProjeto";
    }

    private String formularioDeAgendamento(Model model, Projeto projeto, Levantamento levantamento) {
        model.addAttribute("projeto", projeto);
        model.addAttribute("reagendamento", levantamento != null);
        return "comercial/agendarVisita";
    }
}
