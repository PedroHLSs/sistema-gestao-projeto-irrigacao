package br.edu.iftm.irrigacao.apresentacao.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import br.edu.iftm.irrigacao.dominio.projeto.PainelService;

/**
 * Página de entrada da engenharia, aberta pelo menu (RF05, RF15): os projetos que esperam o
 * dimensionamento e a lista técnica, a programação da execução ou outra ordem de serviço. Fica sob
 * {@code /engenharia}, liberado para PROJETISTA e ADMINISTRADOR (RNF04).
 */
@Controller
public class FilaEngenhariaController {

    private final PainelService painelService;

    public FilaEngenhariaController(PainelService painelService) {
        this.painelService = painelService;
    }

    @GetMapping("/engenharia")
    public String fila(Model model) {
        model.addAttribute("fila", painelService.filaEngenharia());
        return "engenharia/fila";
    }
}
