package br.edu.iftm.irrigacao.dominio.projeto.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.projeto.DimensionamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.LevantamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.ItemProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;

/**
 * Dimensionamento e conclusão da lista técnica (RF05). Só registra o que o projetista informa.
 */
@Service
public class DimensionamentoServiceImpl implements DimensionamentoService {

    private final ProjetoRepository projetoRepository;
    private final ItemProjetoRepository itemProjetoRepository;
    private final LevantamentoService levantamentoService;
    private final ProjetoService projetoService;

    public DimensionamentoServiceImpl(ProjetoRepository projetoRepository, ItemProjetoRepository itemProjetoRepository,
            LevantamentoService levantamentoService, ProjetoService projetoService) {
        this.projetoRepository = projetoRepository;
        this.itemProjetoRepository = itemProjetoRepository;
        this.levantamentoService = levantamentoService;
        this.projetoService = projetoService;
    }

    @Override
    @Transactional(readOnly = true)
    public Projeto exigirEngenhariaLiberada(Long projetoId) {
        Projeto projeto = buscarProjeto(projetoId);
        exigirLevantamentoEmAndamento(projeto);
        List<String> faltam = levantamentoService.camposFaltantes(projetoId);
        if (!faltam.isEmpty()) {
            throw new RegraNegocioException("O levantamento do projeto " + projeto.getCodigo()
                    + " ainda está incompleto. Falta: " + String.join(", ", faltam) + ".");
        }
        return projeto;
    }

    @Override
    @Transactional
    public Projeto registrar(Long projetoId, Double pressaoProjetoMca, Double vazaoProjetoM3h,
            Double toleranciaPercentual) {
        Projeto projeto = exigirEngenhariaLiberada(projetoId);
        if (pressaoProjetoMca == null || pressaoProjetoMca <= 0) {
            throw new RegraNegocioException("Informe a pressão de projeto, maior que zero.");
        }
        if (vazaoProjetoM3h == null || vazaoProjetoM3h <= 0) {
            throw new RegraNegocioException("Informe a vazão de projeto, maior que zero.");
        }
        double tolerancia = toleranciaPercentual == null ? TOLERANCIA_PADRAO : toleranciaPercentual;
        if (tolerancia < 0 || tolerancia > 100) {
            throw new RegraNegocioException("A tolerância vai de 0 a 100%.");
        }
        projeto.setPressaoProjetoMca(pressaoProjetoMca);
        projeto.setVazaoProjetoM3h(vazaoProjetoM3h);
        projeto.setToleranciaPercentual(tolerancia);
        return projetoRepository.save(projeto);
    }

    @Override
    @Transactional
    public Projeto concluirListaTecnica(Long projetoId) {
        Projeto projeto = buscarProjeto(projetoId);
        exigirLevantamentoEmAndamento(projeto);
        List<String> faltam = new ArrayList<>();
        List<String> levantamento = levantamentoService.camposFaltantes(projetoId);
        if (!levantamento.isEmpty()) {
            faltam.add("completar o levantamento (" + String.join(", ", levantamento) + ")");
        }
        if (projeto.getPressaoProjetoMca() == null) {
            faltam.add("informar a pressão de projeto");
        }
        if (projeto.getVazaoProjetoM3h() == null) {
            faltam.add("informar a vazão de projeto");
        }
        long itens = itemProjetoRepository.countByProjetoId(projetoId);
        if (itens == 0) {
            faltam.add("lançar pelo menos um item na lista técnica");
        }
        if (!faltam.isEmpty()) {
            throw new RegraNegocioException("Para concluir a lista técnica falta " + String.join("; ", faltam) + ".");
        }
        // Transição 2: EM_LEVANTAMENTO -> ORCANDO, validada pelo mapa e gravada no histórico
        return projetoService.transitar(projetoId, SituacaoProjeto.ORCANDO,
                "Lista técnica concluída com " + itens + (itens == 1 ? " item." : " itens."));
    }

    private Projeto buscarProjeto(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
    }

    private static void exigirLevantamentoEmAndamento(Projeto projeto) {
        if (projeto.getSituacao() != SituacaoProjeto.EM_LEVANTAMENTO) {
            throw new RegraNegocioException("O dimensionamento e a lista técnica só mudam com o projeto em \""
                    + SituacaoProjeto.EM_LEVANTAMENTO.getRotulo() + "\". O projeto " + projeto.getCodigo()
                    + " está em \"" + projeto.getSituacao().getRotulo() + "\".");
        }
    }
}
