package br.edu.iftm.irrigacao.apresentacao.web;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import br.edu.iftm.irrigacao.dominio.estoque.SobraItem;
import br.edu.iftm.irrigacao.dominio.estoque.SobrasService;
import br.edu.iftm.irrigacao.dominio.execucao.AceiteService;
import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoHoras;
import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoService;
import br.edu.iftm.irrigacao.dominio.execucao.MedicaoService;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServico;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServicoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;

/**
 * Dados da aba Execução da página do projeto (RF15 a RF19): ordens, apontamentos, medições com o
 * resultado, aceite com a assinatura, pendências e sobras. Fica separado do
 * {@code ProjetoController} para a página não depender de todos os services da execução.
 */
@Component
public class PainelExecucao {

    private final OrdemServicoService ordemServicoService;
    private final ApontamentoService apontamentoService;
    private final MedicaoService medicaoService;
    private final AceiteService aceiteService;
    private final SobrasService sobrasService;

    public PainelExecucao(OrdemServicoService ordemServicoService, ApontamentoService apontamentoService,
            MedicaoService medicaoService, AceiteService aceiteService, SobrasService sobrasService) {
        this.ordemServicoService = ordemServicoService;
        this.apontamentoService = apontamentoService;
        this.medicaoService = medicaoService;
        this.aceiteService = aceiteService;
        this.sobrasService = sobrasService;
    }

    public void preencher(Model model, Projeto projeto) {
        Long id = projeto.getId();
        List<ApontamentoHoras> horas = apontamentoService.horasDoProjeto(id);
        List<SobraItem> sobras = sobrasService.sobras(id);
        List<OrdemServico> ordens = ordemServicoService.doProjeto(id);
        // A primeira ordem sai na programação; com a execução em andamento, cabe outra equipe
        boolean podeProgramar = ordemServicoService.podeEmitir(projeto);
        model.addAttribute("ordens", ordens);
        model.addAttribute("horasApontadas", horas);
        model.addAttribute("totalHoras", horas.stream().map(ApontamentoHoras::getHoras).reduce(BigDecimal.ZERO,
                BigDecimal::add));
        model.addAttribute("materiaisApontados", apontamentoService.materiaisDoProjeto(id));
        model.addAttribute("medicoes", medicaoService.doProjeto(id));
        model.addAttribute("aceite", aceiteService.aceiteDoProjeto(id).orElse(null));
        model.addAttribute("pendencias", aceiteService.pendenciasDoProjeto(id));
        model.addAttribute("sobras", sobras);
        model.addAttribute("podeProgramar", podeProgramar);
        model.addAttribute("podeDevolverSobras", sobrasService.podeDevolver(projeto));
        // A aba fica vazia até a separação ser conferida e o projeto chegar à programação
        model.addAttribute("execucaoComecou", podeProgramar || !ordens.isEmpty());
    }
}
