package br.edu.iftm.irrigacao.dominio.projeto;

import java.time.LocalDate;

/**
 * Um projeto encerrado na lista de orçado x realizado da diretoria e no CSV (RF22).
 *
 * @param dataLiberacao dia em que o faturamento foi liberado, usado no filtro por período
 */
public record OrcadoRealizadoLinha(Long projetoId, String codigo, String produtor, LocalDate dataLiberacao,
        Apuracao apuracao) {
}
