package br.edu.iftm.irrigacao.dominio.estoque.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.estoque.Cotacao;
import br.edu.iftm.irrigacao.dominio.estoque.CotacaoService;
import br.edu.iftm.irrigacao.dominio.estoque.DisponibilidadeItem;
import br.edu.iftm.irrigacao.dominio.estoque.EstoqueService;
import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.Quantidade;
import br.edu.iftm.irrigacao.dominio.estoque.SituacaoCotacao;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.CotacaoRepository;
import br.edu.iftm.irrigacao.persistencia.ItemProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;

/**
 * Cotações dos faltantes (RF08), requisição de compra e recebimento (RF13, G4).
 */
@Service
public class CotacaoServiceImpl implements CotacaoService {

    private final CotacaoRepository cotacaoRepository;
    private final ProjetoRepository projetoRepository;
    private final ItemProjetoRepository itemProjetoRepository;
    private final EstoqueService estoqueService;

    public CotacaoServiceImpl(CotacaoRepository cotacaoRepository, ProjetoRepository projetoRepository,
            ItemProjetoRepository itemProjetoRepository, EstoqueService estoqueService) {
        this.cotacaoRepository = cotacaoRepository;
        this.projetoRepository = projetoRepository;
        this.itemProjetoRepository = itemProjetoRepository;
        this.estoqueService = estoqueService;
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
    public boolean podeCotar(Projeto projeto) {
        return projeto.getSituacao() == SituacaoProjeto.ORCANDO
                || projeto.getSituacao() == SituacaoProjeto.PREPARANDO_MATERIAL;
    }

    @Override
    @Transactional
    public Cotacao registrar(Long projetoId, Long materialId, String fornecedor, BigDecimal precoUnitario,
            Integer prazoDias, BigDecimal quantidade) {
        Projeto projeto = buscarProjeto(projetoId);
        if (!podeCotar(projeto)) {
            throw new RegraNegocioException("As cotações são registradas com o projeto em \""
                    + SituacaoProjeto.ORCANDO.getRotulo() + "\" ou \"" + SituacaoProjeto.PREPARANDO_MATERIAL.getRotulo()
                    + "\". O projeto " + projeto.getCodigo() + " está em \"" + projeto.getSituacao().getRotulo() + "\".");
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

    @Override
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> quantidadesAComprar(Long projetoId) {
        Map<Long, BigDecimal> emRequisicao = new HashMap<>();
        for (Cotacao cotacao : cotacaoRepository.listarPorProjetoComMaterial(projetoId)) {
            if (cotacao.getSituacao() == SituacaoCotacao.REQUISITADA) {
                emRequisicao.merge(cotacao.getMaterial().getId(), cotacao.getQuantidade(), BigDecimal::add);
            }
        }
        Map<Long, BigDecimal> aComprar = new LinkedHashMap<>();
        for (DisponibilidadeItem item : estoqueService.disponibilidade(projetoId)) {
            Long materialId = item.getMaterial().getId();
            aComprar.put(materialId, item.getFaltante()
                    .subtract(emRequisicao.getOrDefault(materialId, BigDecimal.ZERO))
                    .max(BigDecimal.ZERO));
        }
        return aComprar;
    }

    @Override
    @Transactional
    public Cotacao requisitar(Long projetoId, Long cotacaoId) {
        Projeto projeto = buscarProjeto(projetoId);
        exigirSituacao(projeto, SituacaoProjeto.PREPARANDO_MATERIAL, "A requisição de compra é feita");
        Cotacao escolhida = buscarCotacao(cotacaoId);
        if (!escolhida.getProjeto().getId().equals(projetoId)) {
            throw new RegistroNaoEncontradoException("Cotação não encontrada.");
        }
        if (escolhida.getSituacao() != SituacaoCotacao.COTADA) {
            throw new RegraNegocioException("A cotação de " + escolhida.getFornecedor() + " está \""
                    + escolhida.getSituacao().getRotulo() + "\" e não pode ser requisitada.");
        }
        Long materialId = escolhida.getMaterial().getId();
        BigDecimal aComprar = quantidadesAComprar(projetoId).getOrDefault(materialId, BigDecimal.ZERO);
        if (aComprar.signum() <= 0) {
            throw new RegraNegocioException(escolhida.getMaterial().getCodigo() + " não tem mais nada a comprar: "
                    + "a reserva e as requisições abertas já cobrem a quantidade prevista.");
        }
        escolhida.setSituacao(SituacaoCotacao.REQUISITADA);
        if (escolhida.getQuantidade().compareTo(aComprar) >= 0) {
            // Item coberto: as cotações que ninguém escolheu não servem mais
            for (Cotacao outra : cotacaoRepository.findByProjetoIdAndMaterialId(projetoId, materialId)) {
                if (outra.getSituacao() == SituacaoCotacao.COTADA) {
                    outra.setSituacao(SituacaoCotacao.DESCARTADA);
                }
            }
        }
        return escolhida;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cotacao> requisicoesAbertas() {
        return cotacaoRepository.listarPorSituacao(SituacaoCotacao.REQUISITADA);
    }

    @Override
    @Transactional
    public Cotacao receber(Long cotacaoId) {
        Cotacao cotacao = buscarCotacao(cotacaoId);
        if (cotacao.getSituacao() != SituacaoCotacao.REQUISITADA) {
            throw new RegraNegocioException("A cotação de " + cotacao.getFornecedor() + " está \""
                    + cotacao.getSituacao().getRotulo() + "\": só requisição aberta pode ser recebida.");
        }
        Long projetoId = cotacao.getProjeto().getId();
        Long materialId = cotacao.getMaterial().getId();
        exigirSituacao(buscarProjeto(projetoId), SituacaoProjeto.PREPARANDO_MATERIAL, "O recebimento é registrado");
        BigDecimal recebida = cotacao.getQuantidade();
        // A situação muda antes: a entrada de estoque grava as alterações pendentes e limpa o contexto
        cotacao.setSituacao(SituacaoCotacao.RECEBIDA);
        estoqueService.entradaCompra(materialId, recebida);
        BigDecimal falta = estoqueService.disponibilidade(projetoId).stream()
                .filter(item -> item.getMaterial().getId().equals(materialId))
                .map(CotacaoServiceImpl::faltaReservar)
                .findFirst()
                .orElse(BigDecimal.ZERO);
        BigDecimal aReservar = recebida.min(falta);
        if (aReservar.signum() > 0) {
            estoqueService.reservar(materialId, projetoId, aReservar);
        }
        return buscarCotacao(cotacaoId);
    }

    private Projeto buscarProjeto(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
    }

    private Cotacao buscarCotacao(Long id) {
        return cotacaoRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Cotação não encontrada."));
    }

    private static void exigirSituacao(Projeto projeto, SituacaoProjeto situacao, String acao) {
        if (projeto.getSituacao() != situacao) {
            throw new RegraNegocioException(acao + " com o projeto em \"" + situacao.getRotulo() + "\". O projeto "
                    + projeto.getCodigo() + " está em \"" + projeto.getSituacao().getRotulo() + "\".");
        }
    }

    /** O que o item ainda precisa reservar para chegar à quantidade prevista. */
    private static BigDecimal faltaReservar(DisponibilidadeItem item) {
        return item.getPrevista().subtract(item.getReservadaParaProjeto());
    }
}
