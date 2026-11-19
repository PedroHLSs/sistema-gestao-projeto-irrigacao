package br.edu.iftm.irrigacao.dominio.projeto;

import java.time.LocalDate;
import java.util.List;

/**
 * Comparação do custo orçado com o custo realizado (RF22).
 *
 * <p>Previsto: {@code Projeto.custoOrcado} e {@code valorTotal} da proposta APROVADA. Realizado: soma
 * de quantidade apontada x {@code Material.custoUnitario} atual, mais soma de horas x
 * {@code valorHora} do apontamento.
 */
public interface OrcadoRealizadoService {

    /**
     * Quadro completo do projeto, com o detalhe por material e de horas.
     *
     * @throws br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException se o projeto não existir
     */
    OrcadoRealizado doProjeto(Long projetoId);

    /**
     * Projetos ENCERRADO com o faturamento liberado no período, dos mais novos para os mais antigos.
     *
     * @param de  primeiro dia, inclusive; {@code null} não limita
     * @param ate último dia, inclusive; {@code null} não limita
     */
    List<OrcadoRealizadoLinha> encerrados(LocalDate de, LocalDate ate);

    /** Soma de todos os projetos ENCERRADO, apurada só com consultas de agregação. */
    ResumoOrcadoRealizado resumo();
}
