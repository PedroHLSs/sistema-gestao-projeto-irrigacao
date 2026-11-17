package br.edu.iftm.irrigacao.apresentacao.web;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.iftm.irrigacao.dominio.estoque.Cotacao;
import br.edu.iftm.irrigacao.dominio.estoque.CotacaoService;
import br.edu.iftm.irrigacao.dominio.estoque.DisponibilidadeItem;
import br.edu.iftm.irrigacao.dominio.estoque.EstoqueService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import jakarta.validation.Valid;

/**
 * Faltantes da lista técnica e cotações com fornecedores (RF08, atividades 6 e 7). A tela é vista
 * pelo comercial e pelo comprador; quem registra a cotação é o COMPRADOR ({@code SecurityConfig}),
 * com o projeto em ORCANDO. A cotação mais barata de cada item fica em destaque.
 */
@Controller
@RequestMapping("/comercial/projetos/{id:\\d+}/faltantes")
public class FaltantesController {

    private static final String TELA = "comercial/faltantes";

    private final ProjetoService projetoService;
    private final EstoqueService estoqueService;
    private final CotacaoService cotacaoService;

    public FaltantesController(ProjetoService projetoService, EstoqueService estoqueService,
            CotacaoService cotacaoService) {
        this.projetoService = projetoService;
        this.estoqueService = estoqueService;
        this.cotacaoService = cotacaoService;
    }

    @GetMapping
    public String tela(@PathVariable Long id, Model model) {
        model.addAttribute("cotacao", new CotacaoForm());
        return montarTela(id, model);
    }

    @PostMapping("/cotacoes")
    public String registrar(@PathVariable Long id, @Valid @ModelAttribute("cotacao") CotacaoForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return montarTela(id, model);
        }
        Cotacao cotacao = cotacaoService.registrar(id, form.getMaterialId(), form.getFornecedor(),
                form.getPrecoUnitario(), form.getPrazoDias(), form.getQuantidade());
        redirect.addFlashAttribute("sucesso", "Cotação de " + cotacao.getFornecedor() + " para "
                + cotacao.getMaterial().getCodigo() + " registrada.");
        return "redirect:/comercial/projetos/" + id + "/faltantes";
    }

    private String montarTela(Long id, Model model) {
        Projeto projeto = projetoService.buscarPorId(id);
        List<DisponibilidadeItem> faltantes = estoqueService.disponibilidade(id).stream()
                .filter(item -> item.getFaltante().signum() > 0)
                .toList();
        Map<Long, List<Cotacao>> cotacoesPorMaterial = cotacaoService.listar(id).stream()
                .collect(Collectors.groupingBy(cotacao -> cotacao.getMaterial().getId()));
        model.addAttribute("projeto", projeto);
        model.addAttribute("faltantes", faltantes);
        model.addAttribute("cotacoes", cotacoesPorMaterial);
        model.addAttribute("maisBaratas", cotacaoService.maisBaratas(id));
        model.addAttribute("podeCotar", projeto.getSituacao() == SituacaoProjeto.ORCANDO);
        return TELA;
    }
}
