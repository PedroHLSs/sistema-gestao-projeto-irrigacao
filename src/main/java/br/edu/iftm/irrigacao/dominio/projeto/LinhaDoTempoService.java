package br.edu.iftm.irrigacao.dominio.projeto;

import java.util.List;

/**
 * Linha do tempo do projeto (RF21): junta o {@link HistoricoProjeto} com os registros de cada etapa.
 */
public interface LinhaDoTempoService {

    /** Acontecimentos do projeto em ordem cronológica, com usuário e data. */
    List<EventoProjeto> doProjeto(Long projetoId);
}
