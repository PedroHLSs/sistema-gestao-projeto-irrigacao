package br.edu.iftm.irrigacao.dominio.estoque.impl;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.estoque.Cotacao;
import br.edu.iftm.irrigacao.dominio.estoque.CotacaoService;
import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.Quantidade;
import br.edu.iftm.irrigacao.dominio.estoque.SituacaoCotacao;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.CotacaoRepository;
import br.edu.iftm.irrigacao.persistencia.ItemProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;

/**
 * Cotações dos faltantes (RF08).
 */
@Service
public class CotacaoServiceImpl implements CotacaoService {

    private final CotacaoRepository cotacaoRepository;
    private final ProjetoRepository projetoRepository;
    private final ItemProjetoRepository itemProjetoRepository;

    public CotacaoServiceImpl(CotacaoRepository cotacaoRepository, ProjetoRepository projetoRepository,
            ItemProjetoRepository itemProjetoRepository) {
        this.cotacaoRepository = cotacaoRepository;
        this.projetoRepository = projetoRepository;
        this.itemProjetoRepository = itemProjetoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cotacao> listar(Long projetoId) {
        return cotacaoRepository.listarPorProjetoComMaterial(projetoId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Cotacao> maisBaratas(Long projetoId) {
        // A consulta vem em ordem de preço dentro de cada material: fica a primeira de cada um
        Map<Long, Cotacao> maisBaratas = new LinkedHashMap<>();
        for (Cotacao cotacao : cotacaoRepository.listarValidas(projetoId, SituacaoCotacao.DESCARTADA)) {
            maisBaratas.putIfAbsent(cotacao.getMaterial().getId(), cotacao);
        }
        return maisBaratas;
    }

    @Override
    @Transactional
    public Cotacao registrar(Long projetoId, Long materialId, String fornecedor, BigDecimal precoUnitario,
            Integer prazoDias, BigDecimal quantidade) {
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
        if (projeto.getSituacao() != SituacaoProjeto.ORCANDO) {
            throw new RegraNegocioException("As cotações são registradas com o projeto em \""
                    + SituacaoProjeto.ORCANDO.getRotulo() + "\". O projeto " + projeto.getCodigo() + " está em \""
                    + projeto.getSituacao().getRotulo() + "\".");
        }
        ItemProjeto item = itemProjetoRepository.findByProjetoIdAndMaterialId(projetoId, materialId)
                .orElseThrow(() -> new RegraNegocioException("O material não está na lista técnica do projeto "
                        + projeto.getCodigo() + "."));
        if (fornecedor == null || fornecedor.isBlank() || fornecedor.strip().length() > 120) {
            throw new RegraNegocioException("Informe o fornecedor, com até 120 caracteres.");
        }
        if (precoUnitario == null || precoUnitario.signum() <= 0 || precoUnitario.stripTrailingZeros().scale() > 2) {
            throw new RegraNegocioException("Informe o preço unitário, maior que zero e com até 2 casas decimais.");
        }
        if (prazoDias != null && prazoDias < 0) {
            throw new RegraNegocioException("O prazo não pode ser negativo.");
        }
        if (quantidade == null || quantidade.signum() <= 0 || !Quantidade.cabeNaColuna(quantidade)) {
            throw new RegraNegocioException("Informe a quantidade, maior que zero e com até 3 casas decimais.");
        }
        return cotacaoRepository.save(new Cotacao(null, projeto, item.getMaterial(), fornecedor.strip(), precoUnitario,
                prazoDias, quantidade, SituacaoCotacao.COTADA));
    }
}
