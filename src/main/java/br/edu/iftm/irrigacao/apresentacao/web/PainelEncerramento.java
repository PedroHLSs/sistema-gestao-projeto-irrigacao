package br.edu.iftm.irrigacao.apresentacao.web;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import br.edu.iftm.irrigacao.dominio.projeto.LiberacaoFaturamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.OrcadoRealizadoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

/**
 * Dados da aba Encerramento da página do projeto: garantia (RF21) e orçado x realizado (RF22).
 */
@Component
public class PainelEncerramento {

    private final LiberacaoFaturamentoService liberacaoService;
    private final OrcadoRealizadoService orcadoRealizadoService;

    public PainelEncerramento(LiberacaoFaturamentoService liberacaoService,
            OrcadoRealizadoService orcadoRealizadoService) {
        this.liberacaoService = liberacaoService;
        this.orcadoRealizadoService = orcadoRealizadoService;
    }

    public void preencher(Model model, Projeto projeto) {
        boolean encerrado = projeto.getSituacao() == SituacaoProjeto.ENCERRADO;
        model.addAttribute("encerrado", encerrado);
        model.addAttribute("garantia", liberacaoService.garantia(projeto).orElse(null));
        model.addAttribute("quadro", encerrado ? orcadoRealizadoService.doProjeto(projeto.getId()) : null);
    }
}
