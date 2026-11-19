package br.edu.iftm.irrigacao.apresentacao.web;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.iftm.irrigacao.dominio.projeto.LiberacaoFaturamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import jakarta.validation.Valid;

/**
 * Liberação do faturamento e registro da garantia (RF20, RF21, atividades 23 e 24). A fila fica sob
 * {@code /financeiro} (FINANCEIRO, DIRETORIA e ADMINISTRADOR); a liberação em si é do FINANCEIRO e do
 * ADMINISTRADOR, pelo {@code SecurityConfig}.
 */
@Controller
@RequestMapping("/financeiro/liberacoes")
public class LiberacaoController {

    private static final String FORMULARIO = "financeiro/liberarFaturamento";

    private final LiberacaoFaturamentoService liberacaoService;
    private final ProjetoService projetoService;
    private final Clock clock;

    public LiberacaoController(LiberacaoFaturamentoService liberacaoService, ProjetoService projetoService,
            Clock clock) {
        this.liberacaoService = liberacaoService;
        this.projetoService = projetoService;
        this.clock = clock;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("projetos", liberacaoService.projetosParaLiberar());
        return "financeiro/liberacoes";
    }

    @GetMapping("/{id:\\d+}")
    public String formulario(@PathVariable Long id, Model model) {
        LocalDate sugestao = liberacaoService.dataDoAceite(id).orElse(LocalDate.now(clock));
        model.addAttribute("liberacao", new LiberacaoForm(sugestao, null));
        return formulario(model, projetoService.buscarPorId(id));
    }

    @PostMapping("/{id:\\d+}")
    public String liberar(@PathVariable Long id, @Valid @ModelAttribute("liberacao") LiberacaoForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return formulario(model, projetoService.buscarPorId(id));
        }
        Projeto projeto = liberacaoService.liberar(id, form.getDataInicioGarantia(), form.getPrazoGarantiaMeses());
        redirect.addFlashAttribute("sucesso", "Faturamento do projeto " + projeto.getCodigo()
                + " liberado e garantia registrada. O projeto está encerrado.");
        return ProjetoController.paginaDoProjeto(id);
    }

    private String formulario(Model model, Projeto projeto) {
        model.addAttribute("projeto", projeto);
        model.addAttribute("motivoBloqueio", liberacaoService.motivoDoBloqueio(projeto).orElse(null));
        model.addAttribute("hoje", LocalDate.now(clock));
        return FORMULARIO;
    }
}
