package br.edu.iftm.irrigacao.dominio.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import br.edu.iftm.irrigacao.config.RelogioDeTesteConfig;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.comercial.Produtor;
import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.persistencia.MaterialRepository;
import br.edu.iftm.irrigacao.persistencia.MovimentacaoEstoqueRepository;
import br.edu.iftm.irrigacao.persistencia.ProdutorRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.PropriedadeRepository;
import br.edu.iftm.irrigacao.persistencia.UsuarioRepository;

/**
 * Atomicidade do estoque com commit de verdade (RNF05): várias threads ao mesmo tempo e o UPDATE
 * desfeito quando a operação é recusada depois dele.
 *
 * <p>Sem {@code @Transactional} na classe: cada chamada ao service abre e fecha a própria
 * transação, como numa requisição. Por isso os dados são criados antes de cada teste e apagados
 * depois.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelogioDeTesteConfig.class)
class EstoqueConcorrenciaTest {

    private static final int THREADS = 10;

    @Autowired
    private EstoqueService estoqueService;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoRepository;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private PropriedadeRepository propriedadeRepository;

    @Autowired
    private ProdutorRepository produtorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario vendedor;
    private Produtor produtor;
    private Propriedade fazenda;
    private Projeto projetoA;
    private Projeto projetoB;
    private Long materialId;

    @BeforeEach
    void criarDados() {
        vendedor = usuarioRepository.save(new Usuario(null, "Vera Vendedora", "vera.concorrencia", "hash",
                Perfil.VENDEDOR, true));
        produtor = produtorRepository.save(new Produtor(null, "Produtor da Concorrência", "11144477735", null, null));
        fazenda = propriedadeRepository.save(new Propriedade(null, produtor, "Fazenda Concorrência", "Uberaba", null,
                null, null));
        projetoA = projetoRepository.save(projeto("PRJ-CONC-0001"));
        projetoB = projetoRepository.save(projeto("PRJ-CONC-0002"));
        materialId = materialRepository.save(new Material(null, "MT-CONC", "Material da concorrência", "un",
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO)).getId();
    }

    /** Os outros testes desfazem a própria transação; só estes dados ficam gravados, e saem aqui. */
    @AfterEach
    void apagarDados() {
        movimentacaoRepository.deleteAllInBatch();
        projetoRepository.deleteAllById(List.of(projetoA.getId(), projetoB.getId()));
        propriedadeRepository.deleteById(fazenda.getId());
        produtorRepository.deleteById(produtor.getId());
        usuarioRepository.deleteById(vendedor.getId());
        materialRepository.deleteById(materialId);
    }

    private Projeto projeto(String codigo) {
        Projeto projeto = new Projeto();
        projeto.setCodigo(codigo);
        projeto.setPropriedade(fazenda);
        projeto.setResponsavel(vendedor);
        projeto.setDataAbertura(LocalDate.of(2026, 11, 16));
        return projeto;
    }

    private Material material() {
        return materialRepository.findById(materialId).orElseThrow();
    }

    private long movimentacoesDoTipo(TipoMovimentacao tipo) {
        return movimentacaoRepository.findAll().stream().filter(m -> m.getTipo() == tipo).count();
    }

    /**
     * Solta as {@value #THREADS} threads juntas, cada uma reservando {@code quantidade}, e devolve
     * quantas conseguiram. Qualquer erro que não seja saldo insuficiente faz o teste falhar.
     */
    private int reservarAoMesmoTempo(BigDecimal quantidade) throws Exception {
        ExecutorService threads = Executors.newFixedThreadPool(THREADS);
        CountDownLatch prontas = new CountDownLatch(THREADS);
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                resultados.add(threads.submit(() -> {
                    prontas.countDown();
                    largada.await();
                    try {
                        estoqueService.reservar(materialId, projetoA.getId(), quantidade);
                        return true;
                    } catch (SaldoInsuficienteException e) {
                        return false;
                    }
                }));
            }
            prontas.await();
            largada.countDown();
            int conseguiram = 0;
            for (Future<Boolean> resultado : resultados) {
                if (resultado.get(60, TimeUnit.SECONDS)) {
                    conseguiram++;
                }
            }
            return conseguiram;
        } finally {
            threads.shutdownNow();
        }
    }

    @Test
    @DisplayName("10 threads reservando 3 de um saldo de 10: só 3 conseguem e o reservado nunca passa do saldo")
    void testDezThreadsDisputandoOSaldo() throws Exception {
        estoqueService.entradaCompra(materialId, new BigDecimal("10"));

        int conseguiram = reservarAoMesmoTempo(new BigDecimal("3"));

        Material material = material();
        assertThat(material.getSaldoReservado()).isLessThanOrEqualTo(material.getSaldo());
        assertThat(conseguiram).isEqualTo(3);
        assertThat(material.getSaldo()).isEqualByComparingTo("10");
        assertThat(material.getSaldoReservado()).isEqualByComparingTo("9");
        assertThat(movimentacoesDoTipo(TipoMovimentacao.RESERVA)).isEqualTo(3);
    }

    @Test
    @DisplayName("10 threads reservando 1 de um saldo de 7: nenhuma reserva se perde e nenhuma sobra")
    void testDezThreadsSemPerderReserva() throws Exception {
        estoqueService.entradaCompra(materialId, new BigDecimal("7"));

        int conseguiram = reservarAoMesmoTempo(BigDecimal.ONE);

        Material material = material();
        assertThat(conseguiram).isEqualTo(7);
        assertThat(material.getSaldoReservado()).isEqualByComparingTo("7");
        assertThat(movimentacoesDoTipo(TipoMovimentacao.RESERVA)).isEqualTo(7);
    }

    @Test
    @DisplayName("Reserva acima do disponível falha e não grava nada")
    void testReservaAcimaDoDisponivelNaoGrava() {
        estoqueService.entradaCompra(materialId, new BigDecimal("10"));
        estoqueService.reservar(materialId, projetoB.getId(), new BigDecimal("6"));

        assertThatThrownBy(() -> estoqueService.reservar(materialId, projetoA.getId(), new BigDecimal("5")))
                .isInstanceOf(SaldoInsuficienteException.class)
                .hasMessageContaining("disponível 4 un");

        Material material = material();
        assertThat(material.getSaldo()).isEqualByComparingTo("10");
        assertThat(material.getSaldoReservado()).isEqualByComparingTo("6");
        assertThat(movimentacoesDoTipo(TipoMovimentacao.RESERVA)).isEqualTo(1);
    }

    @Test
    @DisplayName("Baixa maior que a reserva do próprio projeto é recusada e o UPDATE é desfeito")
    void testBaixaNaoConsomeReservaDeOutroProjeto() {
        estoqueService.entradaCompra(materialId, new BigDecimal("20"));
        estoqueService.reservar(materialId, projetoA.getId(), new BigDecimal("5"));
        estoqueService.reservar(materialId, projetoB.getId(), new BigDecimal("10"));

        // o material tem 15 reservados, mas só 5 são do projeto A
        assertThatThrownBy(() -> estoqueService.baixarReservaParaProjeto(materialId, projetoA.getId(),
                new BigDecimal("8")))
                .isInstanceOf(SaldoInsuficienteException.class)
                .hasMessage("O projeto PRJ-CONC-0001 tem 5 un de MT-CONC (Material da concorrência) reservado(s); "
                        + "não dá para baixar 8 un.");

        Material material = material();
        assertThat(material.getSaldo()).isEqualByComparingTo("20");
        assertThat(material.getSaldoReservado()).isEqualByComparingTo("15");
        assertThat(movimentacoesDoTipo(TipoMovimentacao.SAIDA_PROJETO)).isZero();
    }

    @Test
    @DisplayName("Devolução maior que o que o projeto recebeu é recusada e o UPDATE é desfeito")
    void testDevolucaoAcimaDoRecebido() {
        estoqueService.entradaCompra(materialId, new BigDecimal("20"));
        estoqueService.reservar(materialId, projetoA.getId(), new BigDecimal("5"));
        estoqueService.baixarReservaParaProjeto(materialId, projetoA.getId(), new BigDecimal("5"));
        estoqueService.devolver(materialId, projetoA.getId(), new BigDecimal("2"));

        assertThatThrownBy(() -> estoqueService.devolver(materialId, projetoA.getId(), new BigDecimal("3.5")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O projeto PRJ-CONC-0001 tem 3 un de MT-CONC (Material da concorrência) para devolver; "
                        + "não dá para devolver 3,5 un.");

        assertThat(material().getSaldo()).isEqualByComparingTo("17");
        assertThat(movimentacoesDoTipo(TipoMovimentacao.DEVOLUCAO)).isEqualTo(1);
    }
}
