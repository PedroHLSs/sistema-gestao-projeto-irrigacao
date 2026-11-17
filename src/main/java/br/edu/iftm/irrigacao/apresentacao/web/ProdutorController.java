package br.edu.iftm.irrigacao.apresentacao.web;

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

import br.edu.iftm.irrigacao.dominio.comercial.Produtor;
import br.edu.iftm.irrigacao.dominio.comercial.ProdutorService;
import jakarta.validation.Valid;

/**
 * Cadastro de produtores (RF01): listar com busca por nome ou CPF/CNPJ, criar e editar. A página
 * de edição também lista as propriedades do produtor, que são mantidas pelo
 * {@link PropriedadeController}. O prefixo {@code /comercial} é liberado para VENDEDOR, DIRETORIA e
 * ADMINISTRADOR no {@code SecurityConfig} (RNF04).
 */
@Controller
@RequestMapping("/comercial/produtores")
public class ProdutorController {

    private static final String FORMULARIO = "comercial/formProdutor";

    private final ProdutorService produtorService;

    public ProdutorController(ProdutorService produtorService) {
        this.produtorService = produtorService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String busca, Model model) {
        model.addAttribute("produtores", produtorService.pesquisar(busca));
        model.addAttribute("busca", busca);
        return "comercial/listarProdutores";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("produtor", new ProdutorForm());
        return FORMULARIO;
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("produtor") ProdutorForm form, BindingResult resultado,
            RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return FORMULARIO;
        }
        Produtor produtor = produtorService.criar(form.getNome(), form.getCpfCnpj(), form.getTelefone(),
                form.getEmail());
        redirect.addFlashAttribute("sucesso",
                "Produtor " + produtor.getNome() + " cadastrado. Agora cadastre a propriedade dele.");
        return paginaDoProdutor(produtor.getId());
    }

    @GetMapping("/{id:\\d+}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("produtor", new ProdutorForm(produtorService.buscarPorId(id)));
        model.addAttribute("propriedades", produtorService.listarPropriedades(id));
        return FORMULARIO;
    }

    @PostMapping("/{id:\\d+}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("produtor") ProdutorForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("propriedades", produtorService.listarPropriedades(id));
            return FORMULARIO;
        }
        Produtor produtor = produtorService.atualizar(id, form.getNome(), form.getCpfCnpj(), form.getTelefone(),
                form.getEmail());
        redirect.addFlashAttribute("sucesso", "Produtor " + produtor.getNome() + " alterado.");
        return paginaDoProdutor(id);
    }

    static String paginaDoProdutor(Long produtorId) {
        return "redirect:/comercial/produtores/" + produtorId + "/editar";
    }
}
