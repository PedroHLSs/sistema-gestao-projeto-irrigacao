package br.edu.iftm.irrigacao.dominio.comercial;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Orçamento e proposta (RF09), alçada de desconto (RF10, G2), envio e validade (RF11) e resposta do
 * cliente (RF12, G3), seguindo a Figura 9 e a tabela "Transições da proposta" do docs/modelo.md.
 *
 * <p>O projeto fica em ORCANDO enquanto a proposta é elaborada e passa pela diretoria. As mudanças
 * de situação do projeto vão por {@code ProjetoService.transitar}: envio (transição 3), vencimento
 * (4), aprovação (5) e recusa (6).
 *
 * <p>A proposta vale até {@link Proposta#getDataValidade()}, inclusive; no dia seguinte está
 * vencida.
 */
public interface PropostaService {

    /** Detalhamento com os preços e custos atuais, sem gravar nada: a prévia antes de gerar (RF09). */
    Orcamento simular(Long projetoId, BigDecimal horasPrevistas, BigDecimal percentualDesconto);

    /**
     * Gera a proposta de número seguinte em EM_ELABORACAO: congela {@code precoUnitario} de cada item
     * no preço de venda atual, grava {@code Projeto.custoOrcado} e o valor total (RF09). Só com o
     * projeto em ORCANDO e sem outra proposta em elaboração ou na diretoria.
     */
    Proposta gerar(Long projetoId, BigDecimal horasPrevistas, BigDecimal percentualDesconto);

    /**
     * Detalhamento de uma proposta com os preços congelados nos itens, para a tela e a impressão. O
     * custo por item é o atual; o custo orçado gravado está no projeto.
     */
    Orcamento detalhar(Long propostaId);

    /** Proposta com projeto, produtor, responsável e aprovador carregados. */
    Proposta buscarPorId(Long id);

    /** Todas as versões do orçamento do projeto, da mais nova para a mais antiga. */
    List<Proposta> listarDoProjeto(Long projetoId);

    /** Propostas em elaboração, na diretoria ou enviadas, para a lista do comercial. */
    List<Proposta> listarEmAndamento();

    /** Propostas em AGUARDANDO_DIRETORIA (RF10). */
    List<Proposta> pendentesDaDiretoria();

    /** A proposta em elaboração ou na diretoria do projeto, se houver. */
    Optional<Proposta> abertaDoProjeto(Long projetoId);

    /** Desconto até {@code irrigacao.alcada-desconto.vendedor}, inclusive (G2). */
    boolean dentroDaAlcada(BigDecimal percentualDesconto);

    /** Em elaboração, com desconto dentro da alçada ou aprovado pela diretoria (RF10, RF11). */
    boolean podeEnviar(Proposta proposta);

    /**
     * Troca horas e desconto da proposta em elaboração, mantendo os preços congelados, e recalcula o
     * total e o custo orçado. Mudar o desconto limpa {@code aprovadoPor} e a resposta da diretoria.
     */
    Proposta alterar(Long propostaId, BigDecimal horasPrevistas, BigDecimal percentualDesconto);

    /** Desconto acima da alçada: a proposta vai para AGUARDANDO_DIRETORIA (G2). */
    Proposta pedirAprovacao(Long propostaId);

    /** Diretoria aprova: EM_ELABORACAO com {@code aprovadoPor} = diretor logado. Observação opcional. */
    Proposta aprovarDesconto(Long propostaId, String observacao);

    /** Diretoria nega: EM_ELABORACAO sem aprovador e com o motivo, obrigatório, para o vendedor. */
    Proposta negarDesconto(Long propostaId, String motivo);

    /** ENVIADA com {@code dataEnvio} = hoje; projeto ORCANDO -> AGUARDANDO_RESPOSTA (transição 3). */
    Proposta enviar(Long propostaId);

    /**
     * Cliente aprovou dentro da validade: APROVADA, projeto em PREPARANDO_MATERIAL (transição 5) e
     * reserva no estoque de tudo o que estiver disponível para a lista técnica (G3).
     *
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException se a proposta já venceu
     */
    Proposta registrarAprovacao(Long propostaId);

    /** Cliente recusou: RECUSADA e projeto CANCELADO (transição 6), com o motivo no histórico (G3). */
    Proposta registrarRecusa(Long propostaId, String motivo);

    /** Ids das propostas ENVIADA que já passaram da validade. */
    List<Long> enviadasVencidas();

    /**
     * Vence a proposta enviada que passou da validade: VENCIDA, projeto AGUARDANDO_RESPOSTA ->
     * ORCANDO (transição 4) e nova proposta com o número seguinte, com os preços atuais
     * ({@code recalcularPrecos()} da Figura 9).
     *
     * @return a proposta nova; vazio se a proposta não estava enviada e vencida
     */
    Optional<Proposta> vencer(Long propostaId);

    /**
     * {@code alertarVencimento()} da Figura 9: propostas ENVIADA dos projetos do usuário que vencem
     * em até {@code irrigacao.proposta.dias-alerta} dias, contando hoje.
     */
    List<Proposta> alertarVencimento(Long usuarioId);

    /** Quantas propostas {@link #alertarVencimento} traria, contadas no banco. */
    long contarAlertasDeVencimento(Long usuarioId);
}
