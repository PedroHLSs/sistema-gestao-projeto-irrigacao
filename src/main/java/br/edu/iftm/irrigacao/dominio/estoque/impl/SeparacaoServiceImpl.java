package br.edu.iftm.irrigacao.dominio.estoque.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.estoque.DisponibilidadeItem;
import br.edu.iftm.irrigacao.dominio.estoque.EstoqueService;
import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.ProgressoSeparacao;
import br.edu.iftm.irrigacao.dominio.estoque.Quantidade;
import br.edu.iftm.irrigacao.dominio.estoque.SeparacaoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.ItemProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;

/**
 * Separação do material e liberação da execução (RF14, G5). A separação é feita aos poucos: cada
 * gravação soma ao que já foi separado. A execução é liberada com pelo menos um item separado, e o
 * restante continua sendo separado durante a instalação, saindo do estoque a cada gravação.
 */
@Service
public class SeparacaoServiceImpl implements SeparacaoService {

    private final ProjetoRepository projetoRepository;
    private final ItemProjetoRepository itemProjetoRepository;
    private final EstoqueService estoqueService;
    private final ProjetoService projetoService;

    public SeparacaoServiceImpl(ProjetoRepository projetoRepository, ItemProjetoRepository itemProjetoRepository,
            EstoqueService estoqueService, ProjetoService projetoService) {
        this.projetoRepository = projetoRepository;
        this.itemProjetoRepository = itemProjetoRepository;
        this.estoqueService = estoqueService;
        this.projetoService = projetoService;
    }

    @Override
    public boolean podeSeparar(DisponibilidadeItem item) {
        return item.getFaltante().signum() == 0;
    }

    @Override
    public boolean separacaoAberta(Projeto projeto) {
        return projeto.getSituacao().isMaterialEmAndamento();
    }

    @Override
    @Transactional
    public ProgressoSeparacao separar(Long projetoId, Map<Long, BigDecimal> separadasAgora) {
        // A trava vem antes de ler o separado, para a soma partir do valor gravado por quem veio antes
        itemProjetoRepository.travarPorProjeto(projetoId);
        Projeto projeto = buscarProjeto(projetoId);
        exigirSeparacaoAberta(projeto);
        boolean liberada = projeto.isSeparacaoConferida();
        Map<Long, DisponibilidadeItem> porItem = estoqueService.disponibilidade(projetoId).stream()
                .collect(Collectors.toMap(item -> item.getItem().getId(), Function.identity()));
        // Primeiro confere todos os itens; só depois reserva, baixa e grava
        Map<Long, BigDecimal> novosTotais = new HashMap<>();
        List<DisponibilidadeItem> alterados = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> informado : separadasAgora.entrySet()) {
            BigDecimal agora = informado.getValue();
            if (agora == null || agora.signum() == 0) {
                continue;
            }
            DisponibilidadeItem item = porItem.get(informado.getKey());
            if (item == null) {
                throw new RegistroNaoEncontradoException("Item da lista técnica não encontrado.");
            }
            novosTotais.put(informado.getKey(), novoTotal(item, agora, liberada));
            alterados.add(item);
        }
        for (DisponibilidadeItem item : alterados) {
            Long materialId = item.getMaterial().getId();
            // G4: o que entrou no estoque depois da aprovação ainda não está reservado para o projeto
            BigDecimal falta = item.getPrevista().subtract(item.getReservadaParaProjeto());
            if (falta.signum() > 0) {
                estoqueService.reservar(materialId, projetoId, falta);
            }
            if (liberada) {
                BigDecimal saida = saidaDaSeparacao(item, novosTotais.get(item.getItem().getId()));
                if (saida.signum() > 0) {
                    estoqueService.baixarParaProjeto(materialId, projetoId, saida);
                }
            }
        }
        // A reserva e a baixa limpam o contexto de persistência: os itens são lidos de novo antes de gravar
        List<ItemProjeto> itens = itemProjetoRepository.listarPorProjetoComMaterial(projetoId);
        for (ItemProjeto item : itens) {
            if (novosTotais.containsKey(item.getId())) {
                item.setQuantidadeSeparada(novosTotais.get(item.getId()));
            }
        }
        return new ProgressoSeparacao(itens, novosTotais.size());
    }

    /**
     * Depois da liberação, o que sai do estoque nesta separação. O que o técnico apontou além do
     * separado já saiu no apontamento ({@code EstoqueService.acertarBaixaAlemDoSeparado}): do item sai
     * sempre o maior entre o separado e o apontado, e aqui sai só a diferença.
     */
    private static BigDecimal saidaDaSeparacao(DisponibilidadeItem item, BigDecimal novoTotal) {
        BigDecimal apontada = item.getItem().getQuantidadeApontada();
        return novoTotal.max(apontada).subtract(item.getSeparada().max(apontada));
    }

    /** O separado do item somado ao de agora, recusado se o item não pode ser separado ou se o total não vale. */
    private BigDecimal novoTotal(DisponibilidadeItem item, BigDecimal agora, boolean liberada) {
        String codigo = item.getMaterial().getCodigo();
        String unidade = " " + item.getMaterial().getUnidade();
        if (!podeSeparar(item)) {
            throw new RegraNegocioException(codigo + " ainda não está todo reservado para o projeto: "
                    + Quantidade.formatar(item.getReservadaParaProjeto()) + " de "
                    + Quantidade.formatar(item.getPrevista()) + unidade + ". Receba a compra antes de separar.");
        }
        if (!Quantidade.cabeNaColuna(agora)) {
            throw new RegraNegocioException("A quantidade separada de " + codigo + " deve ter até "
                    + Quantidade.CASAS_DECIMAIS + " casas decimais.");
        }
        if (liberada && agora.signum() < 0) {
            throw new RegraNegocioException("Depois que a execução foi liberada a separação só soma: o separado de "
                    + codigo + " já saiu do estoque para o projeto. O que sobrar volta na devolução das sobras.");
        }
        BigDecimal total = item.getSeparada().add(agora);
        if (total.signum() < 0) {
            throw new RegraNegocioException("Não dá para tirar " + Quantidade.formatar(agora.negate()) + unidade
                    + " de " + codigo + ": só " + Quantidade.formatar(item.getSeparada()) + unidade
                    + " foram separados.");
        }
        // Tirar é sempre aceito enquanto o total não fica negativo, mesmo acima da prevista
        if (agora.signum() > 0 && total.compareTo(item.getPrevista()) > 0) {
            if (item.getFaltaSeparar().signum() == 0) {
                throw new RegraNegocioException(codigo + " já está todo separado: "
                        + Quantidade.formatar(item.getPrevista()) + unidade + ".");
            }
            throw new RegraNegocioException("Separar mais " + Quantidade.formatar(agora) + unidade + " de " + codigo
                    + " passa da quantidade prevista (" + Quantidade.formatar(item.getPrevista()) + unidade
                    + "). Falta separar " + Quantidade.formatar(item.getFaltaSeparar()) + unidade + ".");
        }
        return total;
    }

    @Override
    @Transactional
    public ProgressoSeparacao liberarParaExecucao(Long projetoId) {
        // Uma separação gravada no meio da liberação não muda o que é baixado
        itemProjetoRepository.travarPorProjeto(projetoId);
        Projeto projeto = buscarProjeto(projetoId);
        if (projeto.isSeparacaoConferida()) {
            throw new RegraNegocioException("A execução do projeto " + projeto.getCodigo() + " já foi liberada.");
        }
        if (projeto.getSituacao() != SituacaoProjeto.PREPARANDO_MATERIAL) {
            throw new RegraNegocioException("A execução é liberada com o projeto em \""
                    + SituacaoProjeto.PREPARANDO_MATERIAL.getRotulo() + "\". O projeto " + projeto.getCodigo()
                    + " está em \"" + projeto.getSituacao().getRotulo() + "\".");
        }
        List<ItemProjeto> itens = itemProjetoRepository.listarPorProjetoComMaterial(projetoId);
        List<ItemProjeto> separados = itens.stream()
                .filter(item -> item.getQuantidadeSeparada().signum() > 0)
                .toList();
        if (separados.isEmpty()) {
            throw new RegraNegocioException("Separe pelo menos um item antes de liberar a execução: a equipe não "
                    + "vai a campo sem material.");
        }
        ProgressoSeparacao progresso = new ProgressoSeparacao(itens);
        for (ItemProjeto item : separados) {
            estoqueService.baixarReservaParaProjeto(item.getMaterial().getId(), projetoId, item.getQuantidadeSeparada());
        }
        // As baixas limparam o contexto de persistência: o projeto é lido de novo
        buscarProjeto(projetoId).setSeparacaoConferida(true);
        String restante = progresso.isCompleta() ? ""
                : " O restante continua reservado e é separado durante a instalação.";
        projetoService.transitar(projetoId, SituacaoProjeto.PROGRAMANDO_EXECUCAO, "Material liberado para a "
                + "execução: o separado de " + separados.size() + " de " + itens.size()
                + (itens.size() == 1 ? " item" : " itens") + " saiu do estoque para o projeto." + restante);
        return progresso;
    }

    private Projeto buscarProjeto(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
    }

    private void exigirSeparacaoAberta(Projeto projeto) {
        if (!separacaoAberta(projeto)) {
            throw new RegraNegocioException("A separação é feita da aprovação da proposta até o aceite do cliente. "
                    + "O projeto " + projeto.getCodigo() + " está em \"" + projeto.getSituacao().getRotulo() + "\".");
        }
    }
}
