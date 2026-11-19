package br.edu.iftm.irrigacao.dominio.projeto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Liberação do faturamento (RF20, atividade 23) e registro da garantia (RF21, atividade 24): é o
 * {@code liberarFaturamento()} da Figura 10 e a transição 17.
 */
public interface LiberacaoFaturamentoService {

    /** Maior prazo de garantia aceito, em meses. */
    int PRAZO_MAXIMO_MESES = 120;

    /** Projetos em FINALIZANDO_EXECUCAO, cada um com o motivo do bloqueio, se houver. */
    List<ProjetoParaLiberar> projetosParaLiberar();

    /**
     * @return por que o faturamento do projeto não pode ser liberado agora; vazio quando pode (G8:
     *         as sobras precisam estar resolvidas)
     */
    Optional<String> motivoDoBloqueio(Projeto projeto);

    /** Dia do aceite, sugerido como início da garantia; vazio se o projeto não tem aceite. */
    Optional<LocalDate> dataDoAceite(Long projetoId);

    /**
     * Grava {@code dataLiberacaoFaturamento} = hoje, o início e o prazo da garantia e leva o projeto
     * de FINALIZANDO_EXECUCAO a ENCERRADO (transição 17), tudo na mesma transação.
     *
     * @param dataInicioGarantia do dia do aceite até hoje
     * @param prazoGarantiaMeses de 1 a {@link #PRAZO_MAXIMO_MESES}
     * @return o projeto encerrado
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException se houver bloqueio ou a garantia
     *                                                             for inválida; nada é gravado
     */
    Projeto liberar(Long projetoId, LocalDate dataInicioGarantia, Integer prazoGarantiaMeses);

    /** Garantia do projeto na data de hoje, pelo {@code Clock}; vazio se não foi registrada (RF21). */
    Optional<Garantia> garantia(Projeto projeto);
}
