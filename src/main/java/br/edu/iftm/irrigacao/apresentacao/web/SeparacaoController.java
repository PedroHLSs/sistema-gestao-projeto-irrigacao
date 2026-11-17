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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.iftm.irrigacao.dominio.estoque.Cotacao;
import br.edu.iftm.irrigacao.dominio.estoque.CotacaoService;
import br.edu.iftm.irrigacao.dominio.estoque.DisponibilidadeItem;
import br.edu.iftm.irrigacao.dominio.estoque.EstoqueService;
import br.edu.iftm.irrigacao.dominio.estoque.ResultadoConferencia;
import br.edu.iftm.irrigacao.dominio.estoque.ResultadoConferencia.Divergencia;
import br.edu.iftm.irrigacao.dominio.estoque.SeparacaoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

/**
 * Material do projeto aprovado no almoxarifado (RF13, RF14, G4, G5): disponibilidade, requisição de
 * compra dos faltantes pelo comprador, requisições abertas e recebimento, separação e conferência
 * pelo almoxarife. Fica sob {@code /estoque} (ALMOXARIFE, COMPRADOR e ADMINISTRADOR); quem requisita
 * e quem separa é conferido no {@code SecurityConfig}.
 */
@Controller
@RequestMapping("/estoque")
public class SeparacaoController {

    private final ProjetoService projetoService;
    private final EstoqueService estoqueService;
    private final CotacaoService cotacaoService;
    private final SeparacaoService separacaoService;

    public SeparacaoController(ProjetoService projetoService, EstoqueService estoqueService,
            CotacaoService cotacaoService, SeparacaoService separacaoService) {
        this.projetoService = projetoService;
        this.estoqueService = estoqueService;
        this.cotacaoService = cotacaoService;
        this.separacaoService = separacaoService;
    }

    // ---------- Separação do projeto ----------

    @GetMapping("/projetos/{id:\\d+}/separacao")
    public String tela(@PathVariable Long id, Model model) {
        Projeto projeto = projetoService.buscarPorId(id);
        List<DisponibilidadeItem> itens = estoqueService.disponibilidade(id);
        Map<Long, List<Cotacao>> cotacoesPorMaterial = cotacaoService.listar(id).stream()
                .collect(Collectors.groupingBy(cotacao -> cotacao.getMaterial().getId()));
        model.addAttribute("projeto", projeto);
        model.addAttribute("itens", itens);
        model.addAttribute("cotacoes", cotacoesPorMaterial);
        model.addAttribute("separaveis", itens.stream()
                .filter(separacaoService::podeSeparar)
                .map(item -> item.getItem().getId())
                .collect(Collectors.toSet()));
        model.addAttribute("emPreparacao", projeto.getSituacao() == SituacaoProjeto.PREPARANDO_MATERIAL
                && !projeto.isSeparacaoConferida());
        return "estoque/separacao";
    }

    /** G4: o comprador escolhe a cotação do faltante, que vira a requisição de compra (RF13). */
    @PostMapping("/projetos/{id:\\d+}/requisicoes")
    public String requisitar(@PathVariable Long id, @RequestParam Long cotacaoId, RedirectAttributes redirect) {
        Cotacao cotacao = cotacaoService.requisitar(id, cotacaoId);
        redirect.addFlashAttribute("sucesso", "Requisição de compra de " + cotacao.getMaterial().getCodigo()
                + " a " + cotacao.getFornecedor() + " registrada. As outras cotações do item foram descartadas.");
        return paginaDaSeparacao(id);
    }

    @PostMapping("/projetos/{id:\\d+}/separacao")
    public String registrar(@PathVariable Long id, @ModelAttribute("separacao") SeparacaoForm form,
            BindingResult resultado, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            redirect.addFlashAttribute("erro", "Informe as quantidades separadas em números, com ponto antes das "
                    + "casas decimais.");
            return paginaDaSeparacao(id);
        }
        separacaoService.registrar(id, form.getQuantidades());
        redirect.addFlashAttribute("sucesso", "Separação gravada. Confira quando todos os itens estiverem separados.");
        return paginaDaSeparacao(id);
    }

    /** G5: confere a separação; com divergência, a tela volta com a lista dos itens e a diferença. */
    @PostMapping("/projetos/{id:\\d+}/separacao/conferencia")
    public String conferir(@PathVariable Long id, RedirectAttributes redirect) {
        ResultadoConferencia resultado = separacaoService.conferir(id);
        if (!resultado.isConferida()) {
            redirect.addFlashAttribute("erro", "A separação não bate com a lista técnica. Nada saiu do estoque; "
                    + "corrija as quantidades e confira de novo.");
            redirect.addFlashAttribute("divergencias",
                    resultado.getDivergencias().stream().map(Divergencia::getDescricao).toList());
            return paginaDaSeparacao(id);
        }
        redirect.addFlashAttribute("sucesso", "Separação conferida: o material saiu do estoque e o projeto está em "
                + "\"Programando execução\".");
        return ProjetoController.paginaDoProjeto(id);
    }

    // ---------- Requisições abertas e recebimento ----------

    @GetMapping("/requisicoes")
    public String requisicoes(Model model) {
        model.addAttribute("requisicoes", cotacaoService.requisicoesAbertas());
        return "estoque/requisicoes";
    }

    /** Recebimento: RECEBIDA, entrada de compra e reserva para o projeto, na mesma transação. */
    @PostMapping("/requisicoes/{cotacaoId:\\d+}/recebimento")
    public String receber(@PathVariable Long cotacaoId, RedirectAttributes redirect) {
        Cotacao cotacao = cotacaoService.receber(cotacaoId);
        redirect.addFlashAttribute("sucesso", "Recebimento de " + cotacao.getMaterial().getCodigo()
                + " registrado: o material entrou no estoque e foi reservado para o projeto "
                + cotacao.getProjeto().getCodigo() + ".");
        return "redirect:/estoque/requisicoes";
    }

    private static String paginaDaSeparacao(Long id) {
        return "redirect:/estoque/projetos/" + id + "/separacao";
    }
}
