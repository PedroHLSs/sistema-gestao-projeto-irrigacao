package br.edu.iftm.irrigacao.apresentacao.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import br.edu.iftm.irrigacao.dominio.comercial.Proposta;
import br.edu.iftm.irrigacao.dominio.comercial.PropostaService;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServico;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServicoService;
import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.LevantamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;

/**
 * O próximo passo de cada projeto de uma lista ({@link ProximoPasso}), o mesmo do topo da página do
 * projeto. Os levantamentos, as propostas e as ordens de serviço da lista inteira vêm em uma consulta
 * cada, para a lista não fazer uma consulta por projeto.
 */
@Component
public class PassosDosProjetos {

    private final LevantamentoService levantamentoService;
    private final PropostaService propostaService;
    private final OrdemServicoService ordemServicoService;

    public PassosDosProjetos(LevantamentoService levantamentoService, PropostaService propostaService,
            OrdemServicoService ordemServicoService) {
        this.levantamentoService = levantamentoService;
        this.propostaService = propostaService;
        this.ordemServicoService = ordemServicoService;
    }

    /** @return id do projeto para o passo; projeto encerrado ou cancelado fica de fora */
    public Map<Long, ProximoPasso> calcular(List<Projeto> projetos, ProximoPasso.Quem quem) {
        if (projetos.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = projetos.stream().map(Projeto::getId).toList();
        Map<Long, Levantamento> levantamentos = levantamentoService.buscarPorProjetos(ids);
        Map<Long, Proposta> propostas = propostaService.ultimasDosProjetos(ids);
        Map<Long, List<OrdemServico>> ordens = ordemServicoService.dosProjetos(ids);
        Map<Long, ProximoPasso> passos = new HashMap<>();
        for (Projeto projeto : projetos) {
            Long id = projeto.getId();
            Levantamento levantamento = levantamentos.get(id);
            boolean completo = levantamentoService.validarCamposObrigatorios(levantamento).isEmpty();
            ProximoPasso.de(projeto, levantamento, completo, propostas.get(id), ordens.getOrDefault(id, List.of()), quem)
                    .ifPresent(passo -> passos.put(id, passo));
        }
        return passos;
    }
}
