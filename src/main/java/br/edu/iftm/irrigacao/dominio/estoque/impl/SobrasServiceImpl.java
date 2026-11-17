package br.edu.iftm.irrigacao.dominio.estoque.impl;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.estoque.EstoqueService;
import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.MovimentacaoEstoque;
import br.edu.iftm.irrigacao.dominio.estoque.Quantidade;
import br.edu.iftm.irrigacao.dominio.estoque.SobraItem;
import br.edu.iftm.irrigacao.dominio.estoque.SobrasService;
import br.edu.iftm.irrigacao.dominio.estoque.TipoMovimentacao;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.ItemProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.MovimentacaoEstoqueRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;

/**
 * Sobras (RF19, G8).
 *
 * <p>A linha do projeto é travada no começo, para duas confirmações ao mesmo tempo não devolverem a
 * sobra duas vezes. O {@code sobrasDevolvidas} é marcado antes das devoluções porque cada operação de
 * estoque limpa o contexto de persistência.
 */
@Service
public class SobrasServiceImpl implements SobrasService {

    private final ProjetoRepository projetoRepository;
    private final ItemProjetoRepository itemProjetoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final EstoqueService estoqueService;

    public SobrasServiceImpl(ProjetoRepository projetoRepository, ItemProjetoRepository itemProjetoRepository,
            MovimentacaoEstoqueRepository movimentacaoRepository, EstoqueService estoqueService) {
        this.projetoRepository = projetoRepository;
        this.itemProjetoRepository = itemProjetoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.estoqueService = estoqueService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SobraItem> sobras(Long projetoId) {
        Map<Long, BigDecimal> devolvidoPorMaterial = movimentacaoRepository.listarPorProjetoComMaterial(projetoId)
                .stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.DEVOLUCAO)
                .collect(Collectors.toMap(m -> m.getMaterial().getId(), MovimentacaoEstoque::getQuantidade,
                        BigDecimal::add));
        return itemProjetoRepository.listarPorProjetoComMaterial(projetoId).stream()
                .map(item -> new SobraItem(item, sobra(item),
                        devolvidoPorMaterial.getOrDefault(item.getMaterial().getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Override
    public boolean podeDevolver(Projeto projeto) {
        return projeto.getSituacao() == SituacaoProjeto.FINALIZANDO_EXECUCAO && !projeto.isSobrasDevolvidas();
    }

    @Override
    @Transactional
    public void devolver(Long projetoId, Map<Long, BigDecimal> devolvidas) {
        Projeto projeto = projetoRepository.buscarParaAlterarSituacao(projetoId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
        if (projeto.isSobrasDevolvidas()) {
            throw new RegraNegocioException("As sobras do projeto " + projeto.getCodigo() + " já foram tratadas.");
        }
        if (projeto.getSituacao() != SituacaoProjeto.FINALIZANDO_EXECUCAO) {
            throw new RegraNegocioException("As sobras são devolvidas com o projeto em \""
                    + SituacaoProjeto.FINALIZANDO_EXECUCAO.getRotulo() + "\". O projeto " + projeto.getCodigo()
                    + " está em \"" + projeto.getSituacao().getRotulo() + "\".");
        }

        Map<Long, BigDecimal> quantidades = devolvidas == null ? Map.of() : devolvidas;
        // material -> quantidade que volta ao estoque
        Map<Long, BigDecimal> aDevolver = new LinkedHashMap<>();
        for (ItemProjeto item : itemProjetoRepository.listarPorProjetoComMaterial(projetoId)) {
            BigDecimal sobra = sobra(item);
            if (sobra.signum() == 0) {
                continue;
            }
            String material = item.getMaterial().getCodigo();
            BigDecimal devolvida = quantidades.get(item.getId());
            if (devolvida == null) {
                throw new RegraNegocioException("Informe quanto voltou de " + material + ". Se nada voltou, informe 0.");
            }
            if (devolvida.signum() < 0 || !Quantidade.cabeNaColuna(devolvida)) {
                throw new RegraNegocioException("A quantidade devolvida de " + material
                        + " vai de 0 até a sobra, com até 3 casas decimais.");
            }
            if (devolvida.compareTo(sobra) > 0) {
                throw new RegraNegocioException("A sobra de " + material + " é " + Quantidade.formatar(sobra) + " "
                        + item.getMaterial().getUnidade() + "; não dá para devolver "
                        + Quantidade.formatar(devolvida) + ".");
            }
            if (devolvida.signum() > 0) {
                aDevolver.put(item.getMaterial().getId(), devolvida);
            }
        }

        projeto.setSobrasDevolvidas(true);
        projetoRepository.saveAndFlush(projeto);
        aDevolver.forEach((materialId, quantidade) -> estoqueService.devolver(materialId, projetoId, quantidade));
    }

    /** Separada menos apontada, nunca negativa: apontar mais que o separado não gera sobra. */
    private static BigDecimal sobra(ItemProjeto item) {
        return item.getQuantidadeSeparada().subtract(item.getQuantidadeApontada()).max(BigDecimal.ZERO);
    }
}
