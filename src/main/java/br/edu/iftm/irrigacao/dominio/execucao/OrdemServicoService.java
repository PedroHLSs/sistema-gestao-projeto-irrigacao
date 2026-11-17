package br.edu.iftm.irrigacao.dominio.execucao;

import java.time.LocalDate;
import java.util.List;

import br.edu.iftm.irrigacao.dominio.acesso.Usuario;

/**
 * Programação da equipe e ordem de serviço (RF15) e as passagens da execução que acontecem na
 * ordem: instalação concluída (transição 9) e correção registrada (transição 12), pela Figura 5.
 */
public interface OrdemServicoService {

    /** Usuários ativos com perfil TECNICO_CAMPO, em ordem de nome. */
    List<Usuario> tecnicosDisponiveis();

    /**
     * Emite a ordem de serviço com o número seguinte do projeto e passa o projeto de
     * PROGRAMANDO_EXECUCAO para EXECUTANDO_INSTALACAO (transição 8).
     *
     * @param tecnicosIds um ou mais usuários ativos com perfil TECNICO_CAMPO
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException fora de PROGRAMANDO_EXECUCAO, sem
     *                                                             técnico ou com data que já passou
     */
    OrdemServico emitir(Long projetoId, LocalDate dataProgramada, List<Long> tecnicosIds);

    /** Ordens do projeto com os técnicos, em ordem de número. */
    List<OrdemServico> doProjeto(Long projetoId);

    /** Ordens abertas (sem data de conclusão) em que o usuário é técnico. */
    List<OrdemServico> abertasDoTecnico(Long usuarioId);

    /**
     * A ordem com projeto e técnicos carregados. Um técnico de campo só vê as ordens em que está
     * escalado; para ele, a ordem de outra equipe responde como inexistente.
     */
    OrdemServico buscarParaUsuarioLogado(Long ordemId);

    /** Instalação concluída: EXECUTANDO_INSTALACAO -> VALIDANDO_MEDICAO (transição 9). */
    OrdemServico concluirInstalacao(Long ordemId);

    /**
     * Inconformidade corrigida depois da medição reprovada: CORRIGINDO_INSTALACAO ->
     * EXECUTANDO_INSTALACAO (transição 12), com o que foi corrigido no histórico.
     *
     * @param descricao o que foi corrigido, opcional
     */
    OrdemServico registrarCorrecao(Long ordemId, String descricao);
}
