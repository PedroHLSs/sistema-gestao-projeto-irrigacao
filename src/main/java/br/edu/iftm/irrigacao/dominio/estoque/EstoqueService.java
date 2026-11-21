package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.edu.iftm.irrigacao.dominio.projeto.Projeto;

/**
 * Controle de estoque (RF07) com atomicidade e saldo nunca negativo (RNF05).
 *
 * <p>Toda operação roda numa transação: o saldo muda por um UPDATE condicional do
 * {@code MaterialRepository} e a {@link MovimentacaoEstoque} é gravada junto, com tipo, quantidade,
 * data e hora (do {@code Clock}), usuário logado e projeto, quando houver. Se a condição do UPDATE
 * não vale, nada é gravado e o service lança {@link SaldoInsuficienteException}.
 *
 * <p>Quantidades: maiores que zero, com até 3 casas decimais.
 */
public interface EstoqueService {

    /** ENTRADA_COMPRA: saldo + quantidade. */
    MovimentacaoEstoque entradaCompra(Long materialId, BigDecimal quantidade);

    /**
     * AJUSTE: o saldo passa a ser {@code novoSaldo} e a movimentação grava a diferença, que pode ser
     * negativa. O novo saldo nunca fica abaixo do reservado.
     *
     * @throws SaldoInsuficienteException se o novo saldo ficar abaixo do reservado
     */
    MovimentacaoEstoque ajuste(Long materialId, BigDecimal novoSaldo);

    /**
     * RESERVA: reservado + quantidade, para o projeto.
     *
     * @throws SaldoInsuficienteException se a quantidade passar do disponível (saldo - reservado)
     */
    MovimentacaoEstoque reservar(Long materialId, Long projetoId, BigDecimal quantidade);

    /**
     * SAIDA_PROJETO: tira a quantidade do saldo e do reservado. Só baixa o que foi reservado para o
     * próprio projeto, para não consumir a reserva de outro.
     *
     * @throws SaldoInsuficienteException se a quantidade passar do reservado do material ou do que
     *                                    está reservado para o projeto
     */
    MovimentacaoEstoque baixarReservaParaProjeto(Long materialId, Long projetoId, BigDecimal quantidade);

    /**
     * DEVOLUCAO: sobra do projeto que volta ao estoque, saldo + quantidade. O projeto não devolve
     * mais do que recebeu.
     */
    MovimentacaoEstoque devolver(Long materialId, Long projetoId, BigDecimal quantidade);

    /**
     * SAIDA_PROJETO do material que vai para o projeto depois da liberação da execução (RF14, RF16):
     * usa primeiro o que já está reservado para o projeto e ainda não saiu; o que passar disso é
     * reservado do disponível (RESERVA) antes de sair.
     *
     * @throws SaldoInsuficienteException se o que passa da reserva do projeto passar do disponível
     */
    void baixarParaProjeto(Long materialId, Long projetoId, BigDecimal quantidade);

    /**
     * Devolve ao disponível o que ficou reservado para o projeto e não saiu do estoque (RF07): em
     * projeto que começou com a separação parcial, o que não chegou a ser separado nem usado. Grava
     * uma RESERVA com a quantidade negativa, sem mudar o saldo.
     *
     * @return a quantidade liberada, zero se não havia reserva pendente
     */
    BigDecimal liberarReservaPendente(Long materialId, Long projetoId);

    /**
     * Deixa baixado do estoque exatamente o que o projeto apontou além do separado (RF16, RF07): o
     * separado já saiu na liberação da execução ou na própria separação, e o que o técnico usou a mais
     * sai agora ({@link #baixarParaProjeto}). Se um lançamento é excluído e o excesso diminui, a
     * diferença volta como DEVOLUCAO. Chamado a cada apontamento de material e a cada exclusão,
     * durante a instalação, com a linha do item já travada.
     *
     * @param apontada  quantidade apontada do item, já com o lançamento novo ou sem o excluído
     * @param separada  quantidade separada do item
     * @throws SaldoInsuficienteException se o excesso passar do disponível do material
     */
    void acertarBaixaAlemDoSeparado(Long materialId, Long projetoId, BigDecimal apontada, BigDecimal separada);

    /**
     * Para cada item da lista técnica do projeto: prevista, disponível, já reservada para o projeto e
     * faltante. Uma consulta só.
     */
    List<DisponibilidadeItem> disponibilidade(Long projetoId);

    /**
     * Extrato do material, da movimentação mais nova para a mais antiga.
     *
     * @param de        primeiro dia do período; {@code null} sem limite
     * @param ate       último dia do período, inclusive; {@code null} sem limite
     * @param projetoId só as movimentações deste projeto; {@code null} todas
     */
    Page<MovimentacaoEstoque> extrato(Long materialId, LocalDate de, LocalDate ate, Long projetoId, Pageable pageable);

    /** Projetos que já movimentaram o material, em ordem de código. */
    List<Projeto> projetosDoMaterial(Long materialId);
}
