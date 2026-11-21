package br.edu.iftm.irrigacao.dominio.estoque.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.ListaTecnicaService;
import br.edu.iftm.irrigacao.dominio.estoque.Material;
import br.edu.iftm.irrigacao.dominio.estoque.Quantidade;
import br.edu.iftm.irrigacao.dominio.projeto.DimensionamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.persistencia.ItemProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.MaterialRepository;

/**
 * Lista técnica do projeto (RF05).
 */
@Service
public class ListaTecnicaServiceImpl implements ListaTecnicaService {

    private final ItemProjetoRepository itemProjetoRepository;
    private final MaterialRepository materialRepository;
    private final DimensionamentoService dimensionamentoService;

    public ListaTecnicaServiceImpl(ItemProjetoRepository itemProjetoRepository, MaterialRepository materialRepository,
            DimensionamentoService dimensionamentoService) {
        this.itemProjetoRepository = itemProjetoRepository;
        this.materialRepository = materialRepository;
        this.dimensionamentoService = dimensionamentoService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemProjeto> listar(Long projetoId) {
        return itemProjetoRepository.listarPorProjetoComMaterial(projetoId);
    }

    @Override
    public BigDecimal totalEstimado(List<ItemProjeto> itens) {
        return itens.stream()
                .map(item -> {
                    BigDecimal preco = item.getMaterial().getPrecoVenda();
                    return preco == null ? BigDecimal.ZERO : item.getQuantidadePrevista().multiply(preco);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal faltaEmEstoque(ItemProjeto item) {
        return item.getQuantidadePrevista().subtract(item.getMaterial().getDisponivel()).max(BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public ItemProjeto adicionar(Long projetoId, Long materialId, BigDecimal quantidadePrevista) {
        Projeto projeto = dimensionamentoService.exigirEngenhariaLiberada(projetoId);
        exigirQuantidade(quantidadePrevista);
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Material não encontrado."));
        if (itemProjetoRepository.findByProjetoIdAndMaterialId(projetoId, materialId).isPresent()) {
            throw new RegraNegocioException(material.getCodigo()
                    + " já está na lista técnica. Altere a quantidade na linha dele.");
        }
        // precoUnitario fica vazio: ele é congelado no orçamento (RF09)
        return itemProjetoRepository.save(new ItemProjeto(null, projeto, material, quantidadePrevista,
                BigDecimal.ZERO, BigDecimal.ZERO, null));
    }

    @Override
    @Transactional
    public ItemProjeto alterarQuantidade(Long projetoId, Long itemId, BigDecimal quantidadePrevista) {
        dimensionamentoService.exigirEngenhariaLiberada(projetoId);
        exigirQuantidade(quantidadePrevista);
        ItemProjeto item = buscarItem(projetoId, itemId);
        item.setQuantidadePrevista(quantidadePrevista);
        return itemProjetoRepository.save(item);
    }

    @Override
    @Transactional
    public void remover(Long projetoId, Long itemId) {
        dimensionamentoService.exigirEngenhariaLiberada(projetoId);
        itemProjetoRepository.delete(buscarItem(projetoId, itemId));
    }

    /** O item só é encontrado pelo projeto dono; item de outro projeto responde como inexistente. */
    private ItemProjeto buscarItem(Long projetoId, Long itemId) {
        return itemProjetoRepository.findById(itemId)
                .filter(item -> item.getProjeto().getId().equals(projetoId))
                .orElseThrow(() -> new RegistroNaoEncontradoException("Item da lista técnica não encontrado."));
    }

    private static void exigirQuantidade(BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new RegraNegocioException("Informe uma quantidade maior que zero.");
        }
        if (!Quantidade.cabeNaColuna(quantidade)) {
            throw new RegraNegocioException("Use até " + Quantidade.CASAS_DECIMAIS + " casas decimais e até "
                    + Quantidade.DIGITOS_INTEIROS + " dígitos antes da vírgula.");
        }
    }
}
