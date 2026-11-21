package br.edu.iftm.irrigacao.apresentacao.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.iftm.irrigacao.dominio.estoque.SobraItem;
import br.edu.iftm.irrigacao.dominio.estoque.SobrasService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;

/**
 * Devolução das sobras ao estoque (RF19, G8, Figura 8), sob {@code /estoque}. A consulta é do
 * almoxarifado e das compras; a confirmação, só do ALMOXARIFE e do ADMINISTRADOR, pelo
 * {@code SecurityConfig}.
 */
@Controller
@RequestMapping("/estoque/projetos/{id:\\d+}/sobras")
public class SobrasController {

    private final ProjetoService projetoService;
    private final SobrasService sobrasService;

    public SobrasController(ProjetoService projetoService, SobrasService sobrasService) {
        this.projetoService = projetoService;
        this.sobrasService = sobrasService;
    }

    @GetMapping
    public String tela(@PathVariable Long id, Model model) {
        Projeto projeto = projetoService.buscarPorId(id);
        List<SobraItem> sobras = sobrasService.sobras(id);
        model.addAttribute("projeto", projeto);
        model.addAttribute("sobras", sobras);
        model.addAttribute("temSobra", sobras.stream().anyMatch(SobraItem::isTemSobra));
        model.addAttribute("podeDevolver", sobrasService.podeDevolver(projeto));
        return "estoque/sobras";
    }

    @PostMapping
    public String devolver(@PathVariable Long id, @ModelAttribute("sobrasForm") SobrasForm form,
            BindingResult resultado, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            redirect.addFlashAttribute("erro", "Informe as quantidades devolvidas em números, com ponto antes das "
                    + "casas decimais.");
            return "redirect:/estoque/projetos/" + id + "/sobras";
        }
        boolean temSobra = sobrasService.sobras(id).stream().anyMatch(SobraItem::isTemSobra);
        int reservasLiberadas = sobrasService.devolver(id, form.getDevolvidas());
        String reservas = reservasLiberadas == 0 ? ""
                : reservasLiberadas == 1
                        ? " A reserva de 1 material que não chegou a ser separado voltou ao estoque disponível."
                        : " A reserva de " + reservasLiberadas + " materiais que não chegaram a ser separados voltou "
                                + "ao estoque disponível.";
        redirect.addFlashAttribute("sucesso", (temSobra
                ? "Devolução das sobras registrada: o que voltou entrou no estoque."
                : "Confirmado: o projeto não teve sobras.") + reservas);
        return ProjetoController.paginaDoProjeto(id);
    }
}
