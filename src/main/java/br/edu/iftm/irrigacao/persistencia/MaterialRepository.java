package br.edu.iftm.irrigacao.persistencia;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.iftm.irrigacao.dominio.estoque.Material;

/**
 * Cadastro de materiais (RF06) e as alterações de saldo (RF07).
 *
 * <p>Saldo só muda pelos UPDATEs condicionais abaixo, cada um numa instrução só, com a condição no
 * WHERE (RNF05). O banco trava a linha do material durante o UPDATE e confere a condição sobre o
 * valor mais novo, então duas operações ao mesmo tempo nunca partem do mesmo saldo. Quando a
 * condição não vale, o método devolve 0 e nada é alterado. Os UPDATEs limpam o contexto de
 * persistência ({@code clearAutomatically}), para a próxima leitura trazer o saldo do banco.
 */
public interface MaterialRepository extends JpaRepository<Material, Long> {

    Optional<Material> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    /** Código usado por outro material que não o do id informado (RF06). */
    boolean existsByCodigoAndIdNot(String codigo, Long id);

    List<Material> findAllByOrderByDescricao();

    /**
     * Materiais cujo código ou descrição contém o termo, em ordem de código (RF06).
     *
     * @param termo padrão do LIKE em minúsculas, como {@code %pvc%}; {@code %} traz todos
     */
    @Query("""
            select m from Material m
            where lower(m.codigo) like :termo or lower(m.descricao) like :termo
            order by m.codigo
            """)
    List<Material> pesquisar(@Param("termo") String termo);

    /** A mesma busca, limitada ao tamanho da página, para a busca da lista técnica. */
    @Query("""
            select m from Material m
            where lower(m.codigo) like :termo or lower(m.descricao) like :termo
            order by m.codigo
            """)
    List<Material> pesquisar(@Param("termo") String termo, Pageable limite);

    /** Entrada de compra e devolução: saldo + quantidade. Devolve 0 só se o material não existir. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Material m set m.saldo = m.saldo + :quantidade where m.id = :id")
    int adicionarSaldo(@Param("id") Long id, @Param("quantidade") BigDecimal quantidade);

    /**
     * Ajuste: troca o saldo só se ele ainda for {@code saldoAnterior} e se o novo saldo não ficar
     * abaixo do reservado. Assim a diferença gravada na movimentação é exata, mesmo que outra
     * operação tenha mexido no saldo depois da leitura: nesse caso nada é alterado.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Material m set m.saldo = :novoSaldo
            where m.id = :id and m.saldo = :saldoAnterior and m.saldoReservado <= :novoSaldo
            """)
    int ajustarSaldo(@Param("id") Long id, @Param("saldoAnterior") BigDecimal saldoAnterior,
            @Param("novoSaldo") BigDecimal novoSaldo);

    /** Reserva: reservado + quantidade, só se o disponível (saldo - reservado) comportar. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Material m set m.saldoReservado = m.saldoReservado + :quantidade
            where m.id = :id and m.saldo - m.saldoReservado >= :quantidade
            """)
    int reservar(@Param("id") Long id, @Param("quantidade") BigDecimal quantidade);

    /**
     * Saída para o projeto: tira a quantidade do saldo e do reservado ao mesmo tempo, só se o
     * reservado comportar. Como o reservado nunca passa do saldo, o saldo não fica negativo.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Material m
            set m.saldo = m.saldo - :quantidade, m.saldoReservado = m.saldoReservado - :quantidade
            where m.id = :id and m.saldoReservado >= :quantidade
            """)
    int baixarReserva(@Param("id") Long id, @Param("quantidade") BigDecimal quantidade);
}
