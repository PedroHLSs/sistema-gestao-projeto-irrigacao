package br.edu.iftm.irrigacao.apresentacao.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.iftm.irrigacao.dominio.comercial.Produtor;
import br.edu.iftm.irrigacao.dominio.comercial.ProdutorService;
import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;
import jakarta.validation.Valid;

/**
 * Cadastro das propriedades de um produtor (RF01). A URL sempre leva o produtor dono, e a
 * propriedade de outro produtor responde 404.
 */
@Controller
@RequestMapping("/comercial/produtores/{produtorId:\\d+}/propriedades")
public class PropriedadeController {

    private static final String FORMULARIO = "comercial/formPropriedade";

    private final ProdutorService produtorService;

    public PropriedadeController(ProdutorService produtorService) {
        this.produtorService = produtorService;
    }

    /** O produtor dono, para o cabeçalho do formulário. Produtor que não existe responde 404. */
    @ModelAttribute("produtor")
    public Produtor produtor(@PathVariable Long produtorId) {
        return produtorService.buscarPorId(produtorId);
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("propriedade", new PropriedadeForm());
        return FORMULARIO;
    }

    @PostMapping
    public String criar(@PathVariable Long produtorId, @Valid @ModelAttribute("propriedade") PropriedadeForm form,
            BindingResult resultado, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return FORMULARIO;
        }
        Propriedade propriedade = produtorService.criarPropriedade(produtorId, form.getNome(), form.getMunicipio(),
                form.getLatitude(), form.getLongitude(), form.getAreaTotalHa());
        redirect.addFlashAttribute("sucesso", "Propriedade " + propriedade.getNome() + " cadastrada.");
        return ProdutorController.paginaDoProdutor(produtorId);
    }

    @GetMapping("/{id:\\d+}/editar")
    public String editar(@PathVariable Long produtorId, @PathVariable Long id, Model model) {
        model.addAttribute("propriedade", new PropriedadeForm(produtorService.buscarPropriedade(produtorId, id)));
        return FORMULARIO;
    }

    @PostMapping("/{id:\\d+}")
    public String atualizar(@PathVariable Long produtorId, @PathVariable Long id,
            @Valid @ModelAttribute("propriedade") PropriedadeForm form, BindingResult resultado,
            RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return FORMULARIO;
        }
        Propriedade propriedade = produtorService.atualizarPropriedade(produtorId, id, form.getNome(),
                form.getMunicipio(), form.getLatitude(), form.getLongitude(), form.getAreaTotalHa());
        redirect.addFlashAttribute("sucesso", "Propriedade " + propriedade.getNome() + " alterada.");
        return ProdutorController.paginaDoProdutor(produtorId);
    }
}
