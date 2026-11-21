package br.edu.iftm.irrigacao.apresentacao.web;

import java.math.BigDecimal;
import java.util.List;
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

import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.ListaTecnicaService;
import br.edu.iftm.irrigacao.dominio.estoque.Quantidade;
import br.edu.iftm.irrigacao.dominio.projeto.DimensionamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.LevantamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import jakarta.validation.Valid;

/**
 * Dimensionamento e lista técnica do projetista (RF05, atividade 5), na mesma tela. Fica sob
 * {@code /engenharia}, liberado para PROJETISTA e ADMINISTRADOR (RNF04).
 *
 * <p>A tela só oferece os formulários com o projeto em EM_LEVANTAMENTO e o levantamento completo
 * (G1); fora disso mostra o que falta ou os dados em modo leitura. O sistema não calcula nada.
 */
@Controller
@RequestMapping("/engenharia/projetos/{id:\\d+}")
public class EngenhariaController {

    private static final String TELA = "engenharia/dimensionamento";

    private final ProjetoService projetoService;
    private final LevantamentoService levantamentoService;
    private final DimensionamentoService dimensionamentoService;
    private final ListaTecnicaService listaTecnicaService;

    public EngenhariaController(ProjetoService projetoService, LevantamentoService levantamentoService,
            DimensionamentoService dimensionamentoService, ListaTecnicaService listaTecnicaService) {
        this.projetoService = projetoService;
        this.levantamentoService = levantamentoService;
        this.dimensionamentoService = dimensionamentoService;
        this.listaTecnicaService = listaTecnicaService;
    }

    @GetMapping("/dimensionamento")
    public String tela(@PathVariable Long id, Model model) {
        Projeto projeto = projetoService.buscarPorId(id);
        model.addAttribute("dimensionamento", new DimensionamentoForm(projeto));
        model.addAttribute("novoItem", new ItemListaForm());
        return tela(model, projeto);
    }

    @PostMapping("/dimensionamento")
    public String registrar(@PathVariable Long id, @Valid @ModelAttribute("dimensionamento") DimensionamentoForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("novoItem", new ItemListaForm());
            return tela(model, projetoService.buscarPorId(id));
        }
        dimensionamentoService.registrar(id, form.getPressaoProjetoMca(), form.getVazaoProjetoM3h(),
                form.getToleranciaPercentual());
        redirect.addFlashAttribute("sucesso", "Dimensionamento gravado.");
        return paginaDaEngenharia(id);
    }

    @PostMapping("/itens")
    public String adicionar(@PathVariable Long id, @Valid @ModelAttribute("novoItem") ItemListaForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            Projeto projeto = projetoService.buscarPorId(id);
            model.addAttribute("dimensionamento", new DimensionamentoForm(projeto));
            return tela(model, projeto);
        }
        ItemProjeto item = listaTecnicaService.adicionar(id, form.getMaterialId(), form.getQuantidadePrevista());
        redirect.addFlashAttribute("sucesso", item.getMaterial().getCodigo() + " adicionado à lista técnica.");
        avisarSePassaDoEstoque(item, redirect);
        return paginaDaEngenharia(id);
    }

    @PostMapping("/itens/{itemId:\\d+}")
    public String alterar(@PathVariable Long id, @PathVariable Long itemId,
            @Valid @ModelAttribute("alteracao") QuantidadeForm form, BindingResult resultado,
            RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            redirect.addFlashAttribute("erro", "Quantidade não alterada: " + resultado.getFieldError().getDefaultMessage());
            return paginaDaEngenharia(id);
        }
        ItemProjeto item = listaTecnicaService.alterarQuantidade(id, itemId, form.getQuantidade());
        redirect.addFlashAttribute("sucesso", "Quantidade de " + item.getMaterial().getCodigo() + " alterada.");
        avisarSePassaDoEstoque(item, redirect);
        return paginaDaEngenharia(id);
    }

    @PostMapping("/itens/{itemId:\\d+}/remover")
    public String remover(@PathVariable Long id, @PathVariable Long itemId, RedirectAttributes redirect) {
        listaTecnicaService.remover(id, itemId);
        redirect.addFlashAttribute("sucesso", "Item removido da lista técnica.");
        return paginaDaEngenharia(id);
    }

    @PostMapping("/concluir")
    public String concluir(@PathVariable Long id, RedirectAttributes redirect) {
        Projeto projeto = dimensionamentoService.concluirListaTecnica(id);
        redirect.addFlashAttribute("sucesso", "Lista técnica concluída. O projeto " + projeto.getCodigo()
                + " passou para \"" + projeto.getSituacao().getRotulo() + "\".");
        return ProjetoController.paginaDoProjeto(id);
    }

    private String tela(Model model, Projeto projeto) {
        List<String> faltantes = levantamentoService.camposFaltantes(projeto.getId());
        List<ItemProjeto> itens = listaTecnicaService.listar(projeto.getId());
        boolean emLevantamento = projeto.getSituacao() == SituacaoProjeto.EM_LEVANTAMENTO;
        model.addAttribute("projeto", projeto);
        model.addAttribute("levantamento", levantamentoService.buscarPorProjeto(projeto.getId()).orElse(null));
        model.addAttribute("faltantes", faltantes);
        model.addAttribute("emLevantamento", emLevantamento);
        model.addAttribute("liberada", emLevantamento && faltantes.isEmpty());
        model.addAttribute("itens", itens);
        model.addAttribute("faltasEmEstoque", itens.stream()
                .collect(Collectors.toMap(ItemProjeto::getId, listaTecnicaService::faltaEmEstoque)));
        model.addAttribute("total", listaTecnicaService.totalEstimado(itens));
        return TELA;
    }

    /**
     * Quantidade acima do disponível em estoque não é recusada, só avisada: o que faltar vira
     * faltante para cotação no orçamento (RF05, RF08).
     */
    private void avisarSePassaDoEstoque(ItemProjeto item, RedirectAttributes redirect) {
        BigDecimal falta = listaTecnicaService.faltaEmEstoque(item);
        if (falta.signum() == 0) {
            return;
        }
        String unidade = " " + item.getMaterial().getUnidade();
        redirect.addFlashAttribute("aviso", item.getMaterial().getCodigo() + ": a quantidade prevista ("
                + Quantidade.formatar(item.getQuantidadePrevista()) + unidade + ") passa do disponível em estoque ("
                + Quantidade.formatar(item.getMaterial().getDisponivel()) + unidade + "). Faltam "
                + Quantidade.formatar(falta) + unidade + ", que entram nos faltantes para cotação no orçamento.");
    }

    private static String paginaDaEngenharia(Long id) {
        return "redirect:/engenharia/projetos/" + id + "/dimensionamento";
    }
}
