package br.edu.iftm.irrigacao.apresentacao.web;

import java.math.BigDecimal;
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
import br.edu.iftm.irrigacao.dominio.estoque.ProgressoSeparacao;
import br.edu.iftm.irrigacao.dominio.estoque.Quantidade;
import br.edu.iftm.irrigacao.dominio.estoque.SeparacaoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

/**
 * Material do projeto aprovado no almoxarifado (RF13, RF14, G4, G5): disponibilidade, requisição de
 * compra dos faltantes pelo comprador, requisições abertas e recebimento, separação e liberação da
 * execução pelo almoxarife. A execução começa com parte do material, e a separação e a compra do
 * restante continuam durante a instalação, até o aceite. Fica sob {@code /estoque} (ALMOXARIFE, COMPRADOR e ADMINISTRADOR); quem requisita
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
        model.addAttribute("aComprar", cotacaoService.quantidadesAComprar(id));
        model.addAttribute("separaveis", itens.stream()
                .filter(separacaoService::podeSeparar)
                .map(item -> item.getItem().getId())
                .collect(Collectors.toSet()));
        model.addAttribute("separacaoAberta", separacaoService.separacaoAberta(projeto));
        // Antes da liberação a separação fica no almoxarifado; depois dela, cada gravação já sai do estoque
        model.addAttribute("podeLiberar", projeto.getSituacao() == SituacaoProjeto.PREPARANDO_MATERIAL
                && !projeto.isSeparacaoConferida());
        model.addAttribute("liberada", projeto.isSeparacaoConferida());
        model.addAttribute("progresso", new ProgressoSeparacao(itens.stream().map(DisponibilidadeItem::getItem).toList()));
        return "estoque/separacao";
    }

    /**
     * G4: o comprador escolhe a cotação do faltante, que vira a requisição de compra (RF13). Se ela
     * não cobre tudo, as outras cotações do item continuam disponíveis para outra requisição.
     */
    @PostMapping("/projetos/{id:\\d+}/requisicoes")
    public String requisitar(@PathVariable Long id, @RequestParam Long cotacaoId, RedirectAttributes redirect) {
        Cotacao cotacao = cotacaoService.requisitar(id, cotacaoId);
        BigDecimal falta = cotacaoService.quantidadesAComprar(id)
                .getOrDefault(cotacao.getMaterial().getId(), BigDecimal.ZERO);
        String resto = falta.signum() > 0
                ? " Ainda faltam comprar " + Quantidade.formatar(falta) + " " + cotacao.getMaterial().getUnidade()
                        + "; as outras cotações do item continuam disponíveis."
                : " O item está coberto e as outras cotações dele foram descartadas.";
        redirect.addFlashAttribute("sucesso", "Requisição de compra de " + cotacao.getMaterial().getCodigo()
                + " a " + cotacao.getFornecedor() + " registrada." + resto);
        return paginaDaSeparacao(id);
    }

    /** RF14: grava o que foi separado agora, somado ao que já estava; a separação pode ir aos poucos. */
    @PostMapping("/projetos/{id:\\d+}/separacao")
    public String registrar(@PathVariable Long id, @ModelAttribute("separacao") SeparacaoForm form,
            BindingResult resultado, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            redirect.addFlashAttribute("erro", "Informe as quantidades separadas em números, com ponto antes das "
                    + "casas decimais.");
            return paginaDaSeparacao(id);
        }
        ProgressoSeparacao progresso = separacaoService.separar(id, form.getQuantidades());
        int alterados = progresso.getItensAlterados();
        if (alterados == 0) {
            redirect.addFlashAttribute("aviso", "Nada foi gravado: informe em \"Separar agora\" quanto você separou "
                    + "de cada item.");
        } else {
            boolean liberada = projetoService.buscarPorId(id).isSeparacaoConferida();
            String gravada = "Separação gravada em " + alterados + (alterados == 1 ? " item. " : " itens. ")
                    + (liberada ? "O que foi separado agora saiu do estoque para o projeto. " : "");
            String andamento = progresso.isCompleta() ? "Todos os itens estão separados."
                    : progresso.getCompletos() + " de " + progresso.getTotal() + " itens estão completos; continue "
                            + "quando quiser, o que já foi separado fica guardado.";
            String liberar = liberada ? "" : " Com pelo menos um item separado, a execução já pode ser liberada.";
            redirect.addFlashAttribute("sucesso", gravada + andamento + liberar);
        }
        return paginaDaSeparacao(id);
    }

    /**
     * G5: libera a execução com o que já foi separado (pelo menos um item). O separado sai do estoque
     * e o projeto vai para a programação; o restante é separado durante a instalação.
     */
    @PostMapping("/projetos/{id:\\d+}/separacao/liberacao")
    public String liberar(@PathVariable Long id, RedirectAttributes redirect) {
        ProgressoSeparacao progresso = separacaoService.liberarParaExecucao(id);
        String restante = progresso.isCompleta() ? ""
                : " O restante continua reservado: separe aos poucos durante a instalação, e cada separação já sai "
                        + "do estoque.";
        redirect.addFlashAttribute("sucesso", "Execução liberada: o que foi separado saiu do estoque e o projeto está "
                + "em \"Programando execução\"." + restante);
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
