package br.edu.iftm.irrigacao.apresentacao.web;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

import br.edu.iftm.irrigacao.dominio.projeto.FotoEnviada;
import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.LevantamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.TipoFonteAgua;
import jakarta.validation.Valid;

/**
 * Levantamento em campo pelo celular (RF03, RNF02, RNF06) e o G1 no envio (RF04). Fica sob
 * {@code /campo}, liberado para TECNICO_CAMPO, PROJETISTA e ADMINISTRADOR (RNF04), e só com o
 * projeto em EM_LEVANTAMENTO.
 *
 * <p>"Salvar rascunho" grava como estiver. "Enviar levantamento" grava e confere o G1: completo, o
 * dimensionamento fica liberado; incompleto, a tela volta com o que falta, e a página do projeto
 * mostra a mesma lista.
 */
@Controller
@RequestMapping("/campo/projetos/{id:\\d+}/levantamento")
public class LevantamentoController {

    static final String ACAO_ENVIAR = "enviar";

    private static final String FORMULARIO = "campo/levantamento";

    private final ProjetoService projetoService;
    private final LevantamentoService levantamentoService;
    private final Clock clock;

    public LevantamentoController(ProjetoService projetoService, LevantamentoService levantamentoService, Clock clock) {
        this.projetoService = projetoService;
        this.levantamentoService = levantamentoService;
        this.clock = clock;
    }

    @ModelAttribute("fontes")
    public TipoFonteAgua[] fontes() {
        return TipoFonteAgua.values();
    }

    @GetMapping
    public String formulario(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        Projeto projeto = projetoService.buscarPorId(id);
        if (!levantamentoService.podePreencher(projeto)) {
            redirect.addFlashAttribute("erro", "O levantamento do projeto " + projeto.getCodigo()
                    + " só pode ser preenchido com o projeto em \"Em levantamento\".");
            return ProjetoController.paginaDoProjeto(id);
        }
        Levantamento levantamento = levantamentoService.buscarPorProjeto(id).orElse(null);
        LevantamentoForm form = levantamento == null ? new LevantamentoForm() : new LevantamentoForm(levantamento);
        if (form.getDataVisita() == null) {
            // Em campo, a visita costuma ser agora: o técnico só corrige se não for
            form.setDataVisita(LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES));
        }
        model.addAttribute("dados", form);
        return formulario(model, projeto, levantamento);
    }

    /**
     * @param acao {@code rascunho} ou {@value #ACAO_ENVIAR}, conforme o botão usado
     */
    @PostMapping
    public String salvar(@PathVariable Long id, @Valid @ModelAttribute("dados") LevantamentoForm form,
            BindingResult resultado, @RequestParam(defaultValue = "rascunho") String acao, Model model,
            RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return formulario(model, projetoService.buscarPorId(id), levantamentoService.buscarPorProjeto(id).orElse(null));
        }
        List<FotoEnviada> fotos = form.fotosEnviadas();
        levantamentoService.salvar(id, form.dados(), fotos);
        String fotosNovas = fotos.isEmpty() ? "" : " " + fotos.size() + (fotos.size() == 1 ? " foto adicionada." : " fotos adicionadas.");

        if (!ACAO_ENVIAR.equals(acao)) {
            redirect.addFlashAttribute("sucesso", "Rascunho salvo." + fotosNovas);
            return paginaDoLevantamento(id);
        }
        List<String> faltam = levantamentoService.camposFaltantes(id);
        if (faltam.isEmpty()) {
            redirect.addFlashAttribute("sucesso",
                    "Levantamento completo e enviado." + fotosNovas + " O dimensionamento está liberado para o projetista.");
            return ProjetoController.paginaDoProjeto(id);
        }
        redirect.addFlashAttribute("erro", "Levantamento salvo, mas incompleto. Falta: " + String.join(", ", faltam) + ".");
        return paginaDoLevantamento(id);
    }

    private String formulario(Model model, Projeto projeto, Levantamento levantamento) {
        model.addAttribute("projeto", projeto);
        model.addAttribute("levantamento", levantamento);
        model.addAttribute("faltantes", levantamentoService.validarCamposObrigatorios(levantamento));
        return FORMULARIO;
    }

    private static String paginaDoLevantamento(Long id) {
        return "redirect:/campo/projetos/" + id + "/levantamento";
    }
}
