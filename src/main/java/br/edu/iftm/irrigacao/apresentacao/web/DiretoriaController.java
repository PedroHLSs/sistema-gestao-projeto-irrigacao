package br.edu.iftm.irrigacao.apresentacao.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.iftm.irrigacao.dominio.comercial.ParametrosComerciais;
import br.edu.iftm.irrigacao.dominio.comercial.Proposta;
import br.edu.iftm.irrigacao.dominio.comercial.PropostaService;

/**
 * Aprovação de desconto acima da alçada pela diretoria (RF10, G2). Aprovar devolve a proposta ao
 * vendedor com o diretor como aprovador; negar devolve sem aprovador e com o motivo, que o vendedor
 * vê na proposta. Fica sob {@code /diretoria} (DIRETORIA e ADMINISTRADOR).
 */
@Controller
@RequestMapping("/diretoria/aprovacoes")
public class DiretoriaController {

    private static final String LISTA = "redirect:/diretoria/aprovacoes";

    private final PropostaService propostaService;
    private final ParametrosComerciais parametros;

    public DiretoriaController(PropostaService propostaService, ParametrosComerciais parametros) {
        this.propostaService = propostaService;
        this.parametros = parametros;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("pendentes", propostaService.pendentesDaDiretoria());
        model.addAttribute("parametros", parametros);
        return "diretoria/aprovacoes";
    }

    /** @param observacao texto opcional para o vendedor */
    @PostMapping("/{id:\\d+}/aprovar")
    public String aprovar(@PathVariable Long id, @RequestParam(required = false) String observacao,
            RedirectAttributes redirect) {
        Proposta proposta = propostaService.aprovarDesconto(id, observacao);
        redirect.addFlashAttribute("sucesso", "Desconto da proposta " + proposta.getNumero()
                + " aprovado. Ela volta ao vendedor para o envio.");
        return LISTA;
    }

    /** @param motivo obrigatório: é o que o vendedor lê para refazer o desconto */
    @PostMapping("/{id:\\d+}/negar")
    public String negar(@PathVariable Long id, @RequestParam(required = false) String motivo,
            RedirectAttributes redirect) {
        Proposta proposta = propostaService.negarDesconto(id, motivo);
        redirect.addFlashAttribute("sucesso", "Desconto da proposta " + proposta.getNumero()
                + " negado. O vendedor vê o motivo na proposta.");
        return LISTA;
    }
}
