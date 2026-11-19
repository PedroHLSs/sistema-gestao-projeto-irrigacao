package br.edu.iftm.irrigacao.dominio.projeto.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.comercial.Proposta;
import br.edu.iftm.irrigacao.dominio.comercial.SituacaoProposta;
import br.edu.iftm.irrigacao.dominio.estoque.Material;
import br.edu.iftm.irrigacao.dominio.projeto.Apuracao;
import br.edu.iftm.irrigacao.dominio.projeto.OrcadoRealizado;
import br.edu.iftm.irrigacao.dominio.projeto.OrcadoRealizadoLinha;
import br.edu.iftm.irrigacao.dominio.projeto.OrcadoRealizadoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ResumoOrcadoRealizado;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.ApontamentoHorasRepository;
import br.edu.iftm.irrigacao.persistencia.ApontamentoMaterialRepository;
import br.edu.iftm.irrigacao.persistencia.ItemProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.PropostaRepository;

/**
 * Orçado x realizado (RF22). A lista e o resumo somam no banco, sem uma consulta por projeto.
 */
@Service
public class OrcadoRealizadoServiceImpl implements OrcadoRealizadoService {

    /** Limites do filtro sem data: parâmetro nulo de data não tem tipo definido no PostgreSQL. */
    private static final LocalDate SEM_INICIO = LocalDate.of(1900, 1, 1);
    private static final LocalDate SEM_FIM = LocalDate.of(9999, 12, 31);

    private final ProjetoRepository projetoRepository;
    private final PropostaRepository propostaRepository;
    private final ItemProjetoRepository itemProjetoRepository;
    private final ApontamentoMaterialRepository apontamentoMaterialRepository;
    private final ApontamentoHorasRepository apontamentoHorasRepository;

    public OrcadoRealizadoServiceImpl(ProjetoRepository projetoRepository, PropostaRepository propostaRepository,
            ItemProjetoRepository itemProjetoRepository, ApontamentoMaterialRepository apontamentoMaterialRepository,
            ApontamentoHorasRepository apontamentoHorasRepository) {
        this.projetoRepository = projetoRepository;
        this.propostaRepository = propostaRepository;
        this.itemProjetoRepository = itemProjetoRepository;
        this.apontamentoMaterialRepository = apontamentoMaterialRepository;
        this.apontamentoHorasRepository = apontamentoHorasRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OrcadoRealizado doProjeto(Long projetoId) {
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
        Proposta aprovada = propostaRepository.findFirstByProjetoIdAndSituacao(projetoId, SituacaoProposta.APROVADA)
                .orElse(null);
        Map<Long, BigDecimal> apontado = apontamentoMaterialRepository.somarPorMaterial(projetoId).stream()
                .collect(Collectors.toMap(ApontamentoMaterialRepository.SomaPorMaterial::getMaterialId,
                        ApontamentoMaterialRepository.SomaPorMaterial::getQuantidade));
        List<OrcadoRealizado.Material> materiais = itemProjetoRepository.listarPorProjetoComMaterial(projetoId).stream()
                .map(item -> {
                    Material material = item.getMaterial();
                    return new OrcadoRealizado.Material(material.getCodigo(), material.getDescricao(),
                            material.getUnidade(), item.getQuantidadePrevista(),
                            apontado.getOrDefault(material.getId(), BigDecimal.ZERO), material.getCustoUnitario());
                })
                .toList();
        BigDecimal custoMateriais = materiais.stream().map(OrcadoRealizado.Material::getCustoApontado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ApontamentoHorasRepository.TotalDeHoras horas = apontamentoHorasRepository.totalDoProjeto(projetoId);

        BigDecimal valorTotal = aprovada == null ? null : aprovada.getValorTotal();
        return new OrcadoRealizado(Apuracao.de(valorTotal, projeto.getCustoOrcado(), custoMateriais, horas.getCusto()),
                custoMateriais, horas.getCusto(), materiais, aprovada == null ? null : aprovada.getHorasPrevistas(),
                horas.getHoras());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrcadoRealizadoLinha> encerrados(LocalDate de, LocalDate ate) {
        return projetoRepository.listarEncerradosComCustos(SituacaoProjeto.ENCERRADO, SituacaoProposta.APROVADA,
                de == null ? SEM_INICIO : de, ate == null ? SEM_FIM : ate).stream()
                .map(linha -> new OrcadoRealizadoLinha(linha.getId(), linha.getCodigo(), linha.getProdutor(),
                        linha.getDataLiberacao(), Apuracao.de(linha.getValorTotal(), linha.getCustoOrcado(),
                                linha.getCustoMateriais(), linha.getCustoHoras())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResumoOrcadoRealizado resumo() {
        ProjetoRepository.TotalOrcado orcado = projetoRepository.totalOrcadoNaSituacao(SituacaoProjeto.ENCERRADO);
        BigDecimal valorTotal = propostaRepository.somarValorDosProjetos(SituacaoProposta.APROVADA,
                SituacaoProjeto.ENCERRADO);
        BigDecimal custoMateriais = apontamentoMaterialRepository.somarCustoDosProjetos(SituacaoProjeto.ENCERRADO);
        BigDecimal custoHoras = apontamentoHorasRepository.somarCustoDosProjetos(SituacaoProjeto.ENCERRADO);
        return new ResumoOrcadoRealizado(orcado.getProjetos(),
                Apuracao.de(valorTotal, orcado.getCustoOrcado(), custoMateriais, custoHoras));
    }
}
