package br.edu.iftm.irrigacao.apresentacao.web;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.iftm.irrigacao.dominio.estoque.EstoqueService;
import br.edu.iftm.irrigacao.dominio.estoque.Material;
import br.edu.iftm.irrigacao.dominio.estoque.MaterialService;
import br.edu.iftm.irrigacao.dominio.estoque.MovimentacaoEstoque;
import br.edu.iftm.irrigacao.dominio.estoque.Quantidade;
import jakarta.validation.Valid;

/**
 * Cadastro de materiais e preços (RF06) e controle de estoque (RF07): lista com saldo, reservado e
 * disponível, inclusão e edição, entrada de compra, ajuste de saldo, extrato paginado e a busca em
 * JSON para a lista técnica.
 *
 * <p>O prefixo {@code /estoque} é liberado para ALMOXARIFE, COMPRADOR e ADMINISTRADOR; a busca em
 * JSON também para o PROJETISTA, que monta a lista técnica ({@code SecurityConfig}, RNF04).
 */
@Controller
@RequestMapping("/estoque/materiais")
public class MaterialController {

    /** Movimentações por página no extrato. */
    static final int TAMANHO_PAGINA = 20;

    private static final String FORMULARIO = "estoque/formMaterial";
    private static final String LISTA = "redirect:/estoque/materiais";

    private final MaterialService materialService;
    private final EstoqueService estoqueService;

    public MaterialController(MaterialService materialService, EstoqueService estoqueService) {
        this.materialService = materialService;
        this.estoqueService = estoqueService;
    }

    // ---------- Cadastro (RF06) ----------

    @GetMapping
    public String listar(@RequestParam(required = false) String busca, Model model) {
        model.addAttribute("materiais", materialService.pesquisar(busca));
        model.addAttribute("busca", busca);
        return "estoque/listarMateriais";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("material", new MaterialForm());
        return FORMULARIO;
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("material") MaterialForm form, BindingResult resultado,
            RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return FORMULARIO;
        }
        Material material = materialService.criar(form.getCodigo(), form.getDescricao(), form.getUnidade(),
                form.getPrecoVenda(), form.getCustoUnitario());
        redirect.addFlashAttribute("sucesso", "Material " + material.getCodigo()
                + " cadastrado com saldo zero. O saldo entra pela entrada de compra.");
        return LISTA;
    }

    @GetMapping("/{id:\\d+}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("material", new MaterialForm(materialService.buscarPorId(id)));
        return FORMULARIO;
    }

    @PostMapping("/{id:\\d+}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("material") MaterialForm form,
            BindingResult resultado, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return FORMULARIO;
        }
        Material material = materialService.atualizar(id, form.getCodigo(), form.getDescricao(), form.getUnidade(),
                form.getPrecoVenda(), form.getCustoUnitario());
        redirect.addFlashAttribute("sucesso", "Material " + material.getCodigo() + " alterado.");
        return LISTA;
    }

    // ---------- Movimentações (RF07) ----------

    @GetMapping("/{id:\\d+}/entrada")
    public String formularioDeEntrada(@PathVariable Long id, Model model) {
        model.addAttribute("entrada", new QuantidadeForm());
        model.addAttribute("material", materialService.buscarPorId(id));
        return "estoque/entradaMaterial";
    }

    @PostMapping("/{id:\\d+}/entrada")
    public String entrada(@PathVariable Long id, @Valid @ModelAttribute("entrada") QuantidadeForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("material", materialService.buscarPorId(id));
            return "estoque/entradaMaterial";
        }
        estoqueService.entradaCompra(id, form.getQuantidade());
        Material material = materialService.buscarPorId(id);
        redirect.addFlashAttribute("sucesso", "Entrada de " + Quantidade.formatar(form.getQuantidade()) + " "
                + material.getUnidade() + " de " + material.getCodigo() + " registrada. Saldo agora: "
                + Quantidade.formatar(material.getSaldo()) + " " + material.getUnidade() + ".");
        return paginaDoExtrato(id);
    }

    @GetMapping("/{id:\\d+}/ajuste")
    public String formularioDeAjuste(@PathVariable Long id, Model model) {
        Material material = materialService.buscarPorId(id);
        model.addAttribute("ajuste", new AjusteForm(material.getSaldo()));
        model.addAttribute("material", material);
        return "estoque/ajusteMaterial";
    }

    @PostMapping("/{id:\\d+}/ajuste")
    public String ajuste(@PathVariable Long id, @Valid @ModelAttribute("ajuste") AjusteForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("material", materialService.buscarPorId(id));
            return "estoque/ajusteMaterial";
        }
        MovimentacaoEstoque movimentacao = estoqueService.ajuste(id, form.getNovoSaldo());
        Material material = materialService.buscarPorId(id);
        redirect.addFlashAttribute("sucesso", "Saldo de " + material.getCodigo() + " ajustado para "
                + Quantidade.formatar(material.getSaldo()) + " " + material.getUnidade() + " ("
                + Quantidade.comSinal(movimentacao.getQuantidade()) + ").");
        return paginaDoExtrato(id);
    }

    /**
     * @param pagina número da página, a partir de 0
     */
    @GetMapping("/{id:\\d+}/extrato")
    public String extrato(@PathVariable Long id, @Valid @ModelAttribute("filtro") ExtratoFiltro filtro,
            BindingResult resultado, @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("material", materialService.buscarPorId(id));
        model.addAttribute("projetos", estoqueService.projetosDoMaterial(id));
        PageRequest pageable = PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA);
        Page<MovimentacaoEstoque> movimentacoes = resultado.hasErrors()
                ? Page.empty(pageable)
                : estoqueService.extrato(id, filtro.getDe(), filtro.getAte(), filtro.getProjetoId(), pageable);
        model.addAttribute("movimentacoes", movimentacoes);
        return "estoque/extratoMaterial";
    }

    // ---------- Busca para a lista técnica ----------

    /**
     * Até 20 materiais cujo código ou descrição contém {@code q}, em JSON, para a lista técnica.
     * Sem {@code q}, a lista volta vazia.
     */
    @GetMapping(value = "/busca", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<MaterialResumo> busca(@RequestParam(name = "q", required = false) String q) {
        return materialService.buscarParaListaTecnica(q).stream().map(MaterialResumo::de).toList();
    }

    private static String paginaDoExtrato(Long id) {
        return "redirect:/estoque/materiais/" + id + "/extrato";
    }
}
