package br.edu.iftm.irrigacao.dominio.projeto;

/**
 * Dimensionamento informado pelo projetista e conclusão da lista técnica (RF05, atividade 5).
 *
 * <p>O sistema não calcula nada: pressão, vazão e tolerância vêm do cálculo do projetista e só são
 * gravados no {@link Projeto}. Dimensionamento e lista técnica ficam liberados enquanto o projeto
 * está em EM_LEVANTAMENTO e o levantamento está completo (G1).
 */
public interface DimensionamentoService {

    /** Tolerância usada quando o projetista não informa outra, em %. */
    double TOLERANCIA_PADRAO = 2.0;

    /**
     * O projeto, se o dimensionamento e a lista técnica podem ser alterados.
     *
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException fora de EM_LEVANTAMENTO ou com o
     *                                                             levantamento incompleto, com o que
     *                                                             falta na mensagem
     */
    Projeto exigirEngenhariaLiberada(Long projetoId);

    /**
     * Grava pressão e vazão de projeto e a tolerância aceita na medição (RF05, RF17).
     *
     * @param toleranciaPercentual {@code null} vira {@link #TOLERANCIA_PADRAO}
     */
    Projeto registrar(Long projetoId, Double pressaoProjetoMca, Double vazaoProjetoM3h, Double toleranciaPercentual);

    /**
     * Conclui a lista técnica e passa o projeto de EM_LEVANTAMENTO para ORCANDO (transição 2). Exige
     * levantamento completo, pressão e vazão de projeto e pelo menos um item.
     *
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException com tudo o que falta na mensagem
     */
    Projeto concluirListaTecnica(Long projetoId);
}
