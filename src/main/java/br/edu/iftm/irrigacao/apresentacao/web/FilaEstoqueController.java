package br.edu.iftm.irrigacao.apresentacao.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import br.edu.iftm.irrigacao.dominio.projeto.PainelService;

/**
 * Página de entrada do almoxarifado, aberta pelo menu (RF14, RF19): os projetos com separação a
 * fazer, com separação a continuar durante a execução e com sobras a receber, cada um com o botão
 * da tela de trabalho. Fica sob {@code /estoque}, liberado para ALMOXARIFE, COMPRADOR e
 * ADMINISTRADOR (RNF04).
 */
@Controller
public class FilaEstoqueController {

    private final PainelService painelService;

    public FilaEstoqueController(PainelService painelService) {
        this.painelService = painelService;
    }

    @GetMapping("/estoque/fila")
    public String fila(Model model) {
        model.addAttribute("fila", painelService.filaEstoque());
        return "estoque/fila";
    }
}
