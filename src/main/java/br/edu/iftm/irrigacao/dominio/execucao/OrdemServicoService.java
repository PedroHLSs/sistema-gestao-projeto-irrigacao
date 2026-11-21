package br.edu.iftm.irrigacao.dominio.execucao;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;

/**
 * Programação da equipe e ordem de serviço (RF15) e as passagens da execução que acontecem na
 * ordem: instalação concluída (transição 9) e correção registrada (transição 12), pela Figura 5.
 *
 * <p>O projeto pode ter várias ordens abertas ao mesmo tempo, uma por equipe ou etapa: a primeira
 * leva o projeto a EXECUTANDO_INSTALACAO e as outras entram na execução em andamento. A instalação
 * só termina, e a medição só começa, quando todas as ordens abertas concluírem a sua parte.
 */
public interface OrdemServicoService {

    /** Usuários ativos com perfil TECNICO_CAMPO, em ordem de nome. */
    List<Usuario> tecnicosDisponiveis();

    /**
     * Emite a ordem de serviço com o número seguinte do projeto. Em PROGRAMANDO_EXECUCAO, passa o
     * projeto para EXECUTANDO_INSTALACAO (transição 8); em EXECUTANDO_INSTALACAO, a ordem entra na
     * execução em andamento, sem mudar a situação.
     *
     * @param tecnicosIds um ou mais usuários ativos com perfil TECNICO_CAMPO
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException fora dessas duas situações, sem
     *                                                             técnico ou com data que já passou
     */
    OrdemServico emitir(Long projetoId, LocalDate dataProgramada, List<Long> tecnicosIds);

    /** Se o projeto aceita uma ordem nova agora: PROGRAMANDO_EXECUCAO ou EXECUTANDO_INSTALACAO. */
    boolean podeEmitir(Projeto projeto);

    /** Ordens do projeto com os técnicos, em ordem de número. */
    List<OrdemServico> doProjeto(Long projetoId);

    /** As ordens de cada projeto, com os técnicos, por id do projeto, numa consulta só. */
    Map<Long, List<OrdemServico>> dosProjetos(Collection<Long> projetoIds);

    /** Ordens abertas (sem data de conclusão) em que o usuário é técnico. */
    List<OrdemServico> abertasDoTecnico(Long usuarioId);

    /**
     * Ordens de todas as equipes para quem acompanha a execução (projetista e administrador), das mais
     * próximas para as mais distantes.
     *
     * @param tecnicoId         só as ordens em que este técnico está; {@code null} traz todas
     * @param incluirConcluidas {@code false} traz só as abertas
     */
    List<OrdemServico> acompanhamento(Long tecnicoId, boolean incluirConcluidas);

    /** Ordens abertas do projeto cuja equipe ainda não concluiu a instalação, em ordem de número. */
    List<OrdemServico> instalacaoPendente(Long projetoId);

    /**
     * A ordem com projeto e técnicos carregados. Um técnico de campo só vê as ordens em que está
     * escalado; para ele, a ordem de outra equipe responde como inexistente.
     */
    OrdemServico buscarParaUsuarioLogado(Long ordemId);

    /**
     * A equipe da ordem terminou a sua parte da instalação. Quando todas as ordens abertas do projeto
     * terminaram, o projeto passa de EXECUTANDO_INSTALACAO para VALIDANDO_MEDICAO (transição 9).
     */
    OrdemServico concluirInstalacao(Long ordemId);

    /**
     * Inconformidade corrigida depois da medição reprovada: CORRIGINDO_INSTALACAO ->
     * EXECUTANDO_INSTALACAO (transição 12), com o que foi corrigido no histórico. A ordem em que a
     * correção foi registrada volta a ter a instalação em aberto; as outras continuam concluídas.
     *
     * @param descricao o que foi corrigido, opcional
     */
    OrdemServico registrarCorrecao(Long ordemId, String descricao);
}
