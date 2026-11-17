package br.edu.iftm.irrigacao.apresentacao.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.LevantamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

/**
 * Consulta dos projetos, aberta a todos os perfis (RNF04): a lista com filtro por situação e busca,
 * a página central do projeto com a linha do tempo (RF21) e a agenda semanal das visitas (RF02).
 *
 * <p>A página do projeto só oferece as ações que a situação atual permite. Nesta etapa a única é o
 * agendamento da visita, feito pelo comercial em {@link ProjetoComercialController}.
 */
@Controller
@RequestMapping("/projetos")
public class ProjetoController {

    private final ProjetoService projetoService;
    private final LevantamentoService levantamentoService;

    public ProjetoController(ProjetoService projetoService, LevantamentoService levantamentoService) {
        this.projetoService = projetoService;
        this.levantamentoService = levantamentoService;
    }

    /**
     * @param situacao só os projetos desta situação; vazio traz todas
     * @param busca    trecho do código ou do nome do produtor
     */
    @GetMapping
    public String listar(@RequestParam(required = false) SituacaoProjeto situacao,
            @RequestParam(required = false) String busca, Model model) {
        model.addAttribute("projetos", projetoService.pesquisar(situacao, busca));
        model.addAttribute("situacoes", SituacaoProjeto.values());
        model.addAttribute("situacao", situacao);
        model.addAttribute("busca", busca);
        return "projetos/listarProjetos";
    }

    @GetMapping("/{id:\\d+}")
    public String detalhe(@PathVariable Long id, Model model) {
        Projeto projeto = projetoService.buscarPorId(id);
        Levantamento levantamento = levantamentoService.buscarPorProjeto(id).orElse(null);
        model.addAttribute("projeto", projeto);
        model.addAttribute("historico", projetoService.historico(id));
        model.addAttribute("levantamento", levantamento);
        model.addAttribute("podeAgendarVisita", levantamentoService.podeAgendarVisita(projeto, levantamento));
        return "projetos/detalheProjeto";
    }

    /**
     * @param semana qualquer dia da semana desejada, no formato 2026-11-16; vazio é a semana de hoje
     */
    @GetMapping("/agenda")
    public String agenda(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana,
            Model model) {
        model.addAttribute("agenda", levantamentoService.agendaDaSemana(semana));
        return "projetos/agenda";
    }

    static String paginaDoProjeto(Long projetoId) {
        return "redirect:/projetos/" + projetoId;
    }
}
