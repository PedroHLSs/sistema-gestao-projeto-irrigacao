package br.edu.iftm.irrigacao.dominio.estoque.impl;

import java.math.BigDecimal;
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
import br.edu.iftm.irrigacao.dominio.estoque.Quantidade;
import br.edu.iftm.irrigacao.dominio.estoque.ResultadoConferencia;
import br.edu.iftm.irrigacao.dominio.estoque.ResultadoConferencia.Divergencia;
import br.edu.iftm.irrigacao.dominio.estoque.SeparacaoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.ItemProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;

/**
 * Separação e conferência do material do projeto (RF14, G5).
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
    @Transactional
    public void registrar(Long projetoId, Map<Long, BigDecimal> quantidadesPorItem) {
        exigirSeparacaoAberta(buscarProjeto(projetoId));
        Map<Long, DisponibilidadeItem> porItem = estoqueService.disponibilidade(projetoId).stream()
                .collect(Collectors.toMap(item -> item.getItem().getId(), Function.identity()));
        Map<Long, BigDecimal> separadas = new HashMap<>();
        for (Map.Entry<Long, BigDecimal> informado : quantidadesPorItem.entrySet()) {
            BigDecimal quantidade = informado.getValue();
            if (quantidade == null) {
                continue;
            }
            DisponibilidadeItem item = porItem.get(informado.getKey());
            if (item == null) {
                throw new RegistroNaoEncontradoException("Item da lista técnica não encontrado.");
            }
            String codigo = item.getMaterial().getCodigo();
            if (!podeSeparar(item)) {
                throw new RegraNegocioException(codigo + " ainda não está todo reservado para o projeto: "
                        + Quantidade.formatar(item.getReservadaParaProjeto()) + " de "
                        + Quantidade.formatar(item.getPrevista()) + " " + item.getMaterial().getUnidade()
                        + ". Receba a compra antes de separar.");
            }
            if (quantidade.signum() < 0 || !Quantidade.cabeNaColuna(quantidade)) {
                throw new RegraNegocioException("A quantidade separada de " + codigo
                        + " deve ser zero ou mais, com até 3 casas decimais.");
            }
            separadas.put(informado.getKey(), quantidade);
            // G4: o que entrou no estoque depois da aprovação ainda não está reservado para o projeto
            BigDecimal aReservar = item.getPrevista().subtract(item.getReservadaParaProjeto());
            if (aReservar.signum() > 0) {
                estoqueService.reservar(item.getMaterial().getId(), projetoId, aReservar);
            }
        }
        // A reserva limpa o contexto de persistência: os itens são lidos de novo antes de gravar
        for (ItemProjeto item : itemProjetoRepository.listarPorProjetoComMaterial(projetoId)) {
            if (separadas.containsKey(item.getId())) {
                item.setQuantidadeSeparada(separadas.get(item.getId()));
            }
        }
    }

    @Override
    @Transactional
    public ResultadoConferencia conferir(Long projetoId) {
        exigirSeparacaoAberta(buscarProjeto(projetoId));
        List<ItemProjeto> itens = itemProjetoRepository.listarPorProjetoComMaterial(projetoId);
        List<Divergencia> divergencias = itens.stream()
                .filter(item -> item.getQuantidadeSeparada().compareTo(item.getQuantidadePrevista()) != 0)
                .map(Divergencia::new)
                .toList();
        if (!divergencias.isEmpty()) {
            // Divergência: nada muda no estoque nem no projeto
            return new ResultadoConferencia(divergencias);
        }
        for (ItemProjeto item : itens) {
            estoqueService.baixarReservaParaProjeto(item.getMaterial().getId(), projetoId, item.getQuantidadePrevista());
        }
        // As baixas limparam o contexto de persistência: o projeto é lido de novo
        buscarProjeto(projetoId).setSeparacaoConferida(true);
        projetoService.transitar(projetoId, SituacaoProjeto.PROGRAMANDO_EXECUCAO, "Separação conferida: "
                + itens.size() + (itens.size() == 1 ? " item saiu" : " itens saíram") + " do estoque para o projeto.");
        return new ResultadoConferencia(List.of());
    }

    private Projeto buscarProjeto(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
    }

    private static void exigirSeparacaoAberta(Projeto projeto) {
        if (projeto.isSeparacaoConferida()) {
            throw new RegraNegocioException("A separação do projeto " + projeto.getCodigo() + " já foi conferida.");
        }
        if (projeto.getSituacao() != SituacaoProjeto.PREPARANDO_MATERIAL) {
            throw new RegraNegocioException("A separação é feita com o projeto em \""
                    + SituacaoProjeto.PREPARANDO_MATERIAL.getRotulo() + "\". O projeto " + projeto.getCodigo()
                    + " está em \"" + projeto.getSituacao().getRotulo() + "\".");
        }
    }
}
