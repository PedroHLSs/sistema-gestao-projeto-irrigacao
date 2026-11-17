package br.edu.iftm.irrigacao.apresentacao.web;

import java.time.Clock;
import java.time.LocalDate;
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

import br.edu.iftm.irrigacao.dominio.execucao.OrdemServico;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServicoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import jakarta.validation.Valid;

/**
 * Programação da equipe e emissão da ordem de serviço (RF15, atividade 11), sob {@code /engenharia}
 * (PROJETISTA e ADMINISTRADOR). Com o projeto fora de PROGRAMANDO_EXECUCAO a tela só lista as
 * ordens já emitidas.
 */
@Controller
@RequestMapping("/engenharia/projetos/{id:\\d+}/programacao")
public class ProgramacaoController {

    private static final String TELA = "engenharia/programacao";

    private final ProjetoService projetoService;
    private final OrdemServicoService ordemServicoService;
    private final Clock clock;

    public ProgramacaoController(ProjetoService projetoService, OrdemServicoService ordemServicoService, Clock clock) {
        this.projetoService = projetoService;
        this.ordemServicoService = ordemServicoService;
        this.clock = clock;
    }

    @GetMapping
    public String tela(@PathVariable Long id, Model model) {
        model.addAttribute("programacao", new ProgramacaoForm(LocalDate.now(clock), List.of()));
        return tela(model, projetoService.buscarPorId(id));
    }

    @PostMapping
    public String emitir(@PathVariable Long id, @Valid @ModelAttribute("programacao") ProgramacaoForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return tela(model, projetoService.buscarPorId(id));
        }
        OrdemServico ordem = ordemServicoService.emitir(id, form.getDataProgramada(), form.getTecnicos());
        redirect.addFlashAttribute("sucesso", "Ordem de serviço " + ordem.getNumero() + " emitida. O projeto está em \""
                + SituacaoProjeto.EXECUTANDO_INSTALACAO.getRotulo() + "\" e a ordem aparece em \"Minhas OS\" de cada técnico.");
        return ProjetoController.paginaDoProjeto(id);
    }

    private String tela(Model model, Projeto projeto) {
        model.addAttribute("projeto", projeto);
        model.addAttribute("tecnicos", ordemServicoService.tecnicosDisponiveis());
        model.addAttribute("ordens", ordemServicoService.doProjeto(projeto.getId()));
        model.addAttribute("podeEmitir", projeto.getSituacao() == SituacaoProjeto.PROGRAMANDO_EXECUCAO);
        model.addAttribute("hoje", LocalDate.now(clock));
        return TELA;
    }
}
