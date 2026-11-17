package br.edu.iftm.irrigacao.dominio.execucao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Aceite do cliente (RF18, G7) e pendência quando o cliente não assina, pelas Figuras 4 e 5.
 */
public interface AceiteService {

    /** Tamanho máximo da assinatura em base64, em caracteres: cerca de 375 KB de PNG. */
    int TAMANHO_MAXIMO_ASSINATURA = 500_000;

    /**
     * Grava o aceite com o projeto em COLHENDO_ACEITE, conclui as ordens de serviço abertas com a
     * data de hoje e passa o projeto para FINALIZANDO_EXECUCAO (transição 13).
     *
     * @param assinaturaPng PNG em base64, com ou sem o prefixo {@code data:image/png;base64,}; não
     *                      pode estar em branco (só pixels transparentes)
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException sem nome, sem assinatura ou fora de
     *                                                             COLHENDO_ACEITE
     */
    Aceite registrarAceite(Long projetoId, String nomeResponsavel, String assinaturaPng);

    /**
     * O cliente não assinou: grava a pendência e passa o projeto de COLHENDO_ACEITE para
     * TRATANDO_PENDENCIA (transição 14).
     *
     * @param dataReprogramada quando a pendência deve ser tratada, de agora em diante
     */
    Pendencia registrarPendencia(Long projetoId, TipoPendencia tipo, String descricao, LocalDateTime dataReprogramada);

    /**
     * Resolve a pendência aberta. ADMINISTRATIVA volta para COLHENDO_ACEITE (transição 15); TECNICA
     * volta para VALIDANDO_MEDICAO (transição 16), para medir de novo.
     */
    Pendencia resolverPendencia(Long projetoId);

    Optional<Aceite> aceiteDoProjeto(Long projetoId);

    /** Pendências do projeto, da mais nova para a mais antiga. */
    List<Pendencia> pendenciasDoProjeto(Long projetoId);

    Optional<Pendencia> pendenciaAberta(Long projetoId);
}
