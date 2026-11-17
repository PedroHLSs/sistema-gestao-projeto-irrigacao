package br.edu.iftm.irrigacao.dominio.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.config.RelogioDeTesteConfig;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioAutenticado;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;
import br.edu.iftm.irrigacao.dominio.comercial.Produtor;
import br.edu.iftm.irrigacao.dominio.comercial.ProdutorService;
import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.persistencia.ItemProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.MaterialRepository;
import br.edu.iftm.irrigacao.persistencia.MovimentacaoEstoqueRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.RelogioAjustavel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Cadastro de materiais (RF06) e operações de estoque (RF07, RNF05) com a aplicação inteira e o
 * banco H2. Cada teste roda numa transação desfeita no fim; o que depende de commit (concorrência e
 * desfazer o UPDATE quando a conta do projeto não fecha) está em {@link EstoqueConcorrenciaTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(RelogioDeTesteConfig.class)
class EstoqueIntegrationTest {

    private static final String SENHA = "senha-do-almoxarife";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RelogioAjustavel relogio;

    @Autowired
    private EstoqueService estoqueService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private ProdutorService produtorService;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoRepository;

    @Autowired
    private ItemProjetoRepository itemProjetoRepository;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private EntityManagerFactory emf;

    private Usuario almir;
    private Projeto projetoA;
    private Projeto projetoB;
    private Long tuboId;

    @BeforeEach
    void prepararDados() {
        relogio.definir(RelogioDeTesteConfig.INICIO);
        almir = usuarioService.criar("Almir Almoxarife", "almir", SENHA, Perfil.ALMOXARIFE);
        Usuario vendedor = usuarioService.criar("Ana Vendedora", "ana", SENHA, Perfil.VENDEDOR);
        Produtor produtor = produtorService.criar("João Batista Ferreira", "529.982.247-25", null, null);
        Propriedade fazenda = produtorService.criarPropriedade(produtor.getId(), "Fazenda Boa Vista", "Uberlândia",
                null, null, null);
        projetoA = projetoRepository.save(projeto("PRJ-2026-0001", fazenda, vendedor));
        projetoB = projetoRepository.save(projeto("PRJ-2026-0002", fazenda, vendedor));
        tuboId = materialService.criar("TB-PVC-050", "Tubo PVC 50 mm", "barra", new BigDecimal("62.90"),
                new BigDecimal("44.80")).getId();
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    private static Projeto projeto(String codigo, Propriedade propriedade, Usuario responsavel) {
        Projeto projeto = new Projeto();
        projeto.setCodigo(codigo);
        projeto.setPropriedade(propriedade);
        projeto.setResponsavel(responsavel);
        projeto.setDataAbertura(LocalDate.of(2026, 11, 16));
        return projeto;
    }

    /** Coloca o almoxarife na sessão, como no login pelo formulário, para as chamadas diretas ao service. */
    private void almirLogado() {
        UsuarioAutenticado principal = new UsuarioAutenticado(almir);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private RequestPostProcessor almirNaTela() {
        return user(userDetailsService.loadUserByUsername("almir"));
    }

    private Material relido(Long id) {
        em.flush();
        em.clear();
        return materialRepository.findById(id).orElseThrow();
    }

    private static BigDecimal q(String valor) {
        return new BigDecimal(valor);
    }

    private long movimentacoes() {
        return movimentacaoRepository.count();
    }

    // ---------- Cada operação ----------

    @Test
    @DisplayName("Entrada de compra soma no saldo e grava a movimentação com usuário, data e sem projeto")
    void testEntradaCompra() {
        almirLogado();

        MovimentacaoEstoque mov = estoqueService.entradaCompra(tuboId, q("50"));

        Material tubo = relido(tuboId);
        assertThat(tubo.getSaldo()).isEqualByComparingTo("50");
        assertThat(tubo.getSaldoReservado()).isEqualByComparingTo("0");
        MovimentacaoEstoque gravada = movimentacaoRepository.findById(mov.getId()).orElseThrow();
        assertThat(gravada.getTipo()).isEqualTo(TipoMovimentacao.ENTRADA_COMPRA);
        assertThat(gravada.getQuantidade()).isEqualByComparingTo("50");
        assertThat(gravada.getMaterial().getId()).isEqualTo(tuboId);
        assertThat(gravada.getProjeto()).isNull();
        assertThat(gravada.getUsuario().getId()).isEqualTo(almir.getId());
        assertThat(gravada.getDataHora()).isEqualTo(RelogioDeTesteConfig.INICIO);
    }

    @Test
    @DisplayName("Ajuste grava a diferença, para mais e para menos")
    void testAjuste() {
        estoqueService.entradaCompra(tuboId, q("50"));

        MovimentacaoEstoque paraMais = estoqueService.ajuste(tuboId, q("62.5"));
        MovimentacaoEstoque paraMenos = estoqueService.ajuste(tuboId, q("45"));

        assertThat(relido(tuboId).getSaldo()).isEqualByComparingTo("45");
        assertThat(paraMais.getTipo()).isEqualTo(TipoMovimentacao.AJUSTE);
        assertThat(paraMais.getQuantidade()).isEqualByComparingTo("12.5");
        assertThat(paraMenos.getQuantidade()).isEqualByComparingTo("-17.5");
        assertThat(paraMenos.getProjeto()).isNull();
    }

    @Test
    @DisplayName("Ajuste abaixo do reservado é recusado com o reservado na mensagem, sem alterar nada")
    void testAjusteAbaixoDoReservado() {
        estoqueService.entradaCompra(tuboId, q("50"));
        estoqueService.reservar(tuboId, projetoA.getId(), q("30"));
        long antes = movimentacoes();

        assertThatThrownBy(() -> estoqueService.ajuste(tuboId, q("29.999")))
                .isInstanceOfSatisfying(SaldoInsuficienteException.class,
                        e -> assertThat(e.getDisponivel()).isEqualByComparingTo("30"))
                .hasMessage("O saldo de TB-PVC-050 (Tubo PVC 50 mm) não pode ficar abaixo do reservado, que é 30 barra.");

        Material tubo = relido(tuboId);
        assertThat(tubo.getSaldo()).isEqualByComparingTo("50");
        assertThat(tubo.getSaldoReservado()).isEqualByComparingTo("30");
        assertThat(movimentacoes()).isEqualTo(antes);
        // ajustar exatamente para o reservado é permitido
        assertThat(estoqueService.ajuste(tuboId, q("30")).getQuantidade()).isEqualByComparingTo("-20");
    }

    @Test
    @DisplayName("Ajuste para o mesmo saldo, negativo ou com mais de 3 casas decimais é recusado")
    void testAjusteInvalido() {
        estoqueService.entradaCompra(tuboId, q("50"));

        assertThatThrownBy(() -> estoqueService.ajuste(tuboId, q("50.000")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O saldo de TB-PVC-050 já é 50 barra. Nada foi ajustado.");
        assertThatThrownBy(() -> estoqueService.ajuste(tuboId, q("-1")))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> estoqueService.ajuste(tuboId, q("1.0001")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("3 casas decimais");
        assertThat(relido(tuboId).getSaldo()).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("O UPDATE do ajuste não altera nada se o saldo mudou depois da leitura")
    void testAjusteComSaldoQueMudou() {
        estoqueService.entradaCompra(tuboId, q("50"));

        // como se o ajuste tivesse lido 40 e outra operação tivesse levado o saldo a 50 antes do UPDATE
        int linhas = materialRepository.ajustarSaldo(tuboId, q("40"), q("45"));

        assertThat(linhas).isZero();
        assertThat(relido(tuboId).getSaldo()).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("Reserva soma no reservado, para o projeto, e não mexe no saldo")
    void testReservar() {
        estoqueService.entradaCompra(tuboId, q("50"));

        MovimentacaoEstoque mov = estoqueService.reservar(tuboId, projetoA.getId(), q("20"));

        Material tubo = relido(tuboId);
        assertThat(tubo.getSaldo()).isEqualByComparingTo("50");
        assertThat(tubo.getSaldoReservado()).isEqualByComparingTo("20");
        MovimentacaoEstoque gravada = movimentacaoRepository.findById(mov.getId()).orElseThrow();
        assertThat(gravada.getTipo()).isEqualTo(TipoMovimentacao.RESERVA);
        assertThat(gravada.getProjeto().getId()).isEqualTo(projetoA.getId());
    }

    @Test
    @DisplayName("Reserva acima do disponível falha com o disponível na mensagem e não altera nada")
    void testReservaAcimaDoDisponivel() {
        estoqueService.entradaCompra(tuboId, q("50"));
        estoqueService.reservar(tuboId, projetoB.getId(), q("20"));
        long antes = movimentacoes();

        assertThatThrownBy(() -> estoqueService.reservar(tuboId, projetoA.getId(), q("30.001")))
                .isInstanceOfSatisfying(SaldoInsuficienteException.class,
                        e -> assertThat(e.getDisponivel()).isEqualByComparingTo("30"))
                .hasMessage("Saldo insuficiente de TB-PVC-050 (Tubo PVC 50 mm): pedido 30,001 barra, disponível 30 barra.");

        Material tubo = relido(tuboId);
        assertThat(tubo.getSaldo()).isEqualByComparingTo("50");
        assertThat(tubo.getSaldoReservado()).isEqualByComparingTo("20");
        assertThat(movimentacoes()).isEqualTo(antes);
        // o disponível inteiro ainda pode ser reservado
        estoqueService.reservar(tuboId, projetoA.getId(), q("30"));
        assertThat(relido(tuboId).getSaldoReservado()).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("Saída para o projeto tira do saldo e do reservado ao mesmo tempo")
    void testBaixarReservaParaProjeto() {
        estoqueService.entradaCompra(tuboId, q("50"));
        estoqueService.reservar(tuboId, projetoA.getId(), q("20"));

        MovimentacaoEstoque mov = estoqueService.baixarReservaParaProjeto(tuboId, projetoA.getId(), q("15"));

        Material tubo = relido(tuboId);
        assertThat(tubo.getSaldo()).isEqualByComparingTo("35");
        assertThat(tubo.getSaldoReservado()).isEqualByComparingTo("5");
        assertThat(movimentacaoRepository.findById(mov.getId()).orElseThrow().getTipo())
                .isEqualTo(TipoMovimentacao.SAIDA_PROJETO);
    }

    @Test
    @DisplayName("Saída acima do reservado do material é recusada sem alterar nada")
    void testBaixaAcimaDoReservado() {
        estoqueService.entradaCompra(tuboId, q("50"));
        estoqueService.reservar(tuboId, projetoA.getId(), q("5"));

        assertThatThrownBy(() -> estoqueService.baixarReservaParaProjeto(tuboId, projetoA.getId(), q("6")))
                .isInstanceOf(SaldoInsuficienteException.class)
                .hasMessage("Reserva insuficiente de TB-PVC-050 (Tubo PVC 50 mm): pedido 6 barra, reservado 5 barra.");

        Material tubo = relido(tuboId);
        assertThat(tubo.getSaldo()).isEqualByComparingTo("50");
        assertThat(tubo.getSaldoReservado()).isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("Devolução soma no saldo e fica ligada ao projeto")
    void testDevolver() {
        estoqueService.entradaCompra(tuboId, q("50"));
        estoqueService.reservar(tuboId, projetoA.getId(), q("20"));
        estoqueService.baixarReservaParaProjeto(tuboId, projetoA.getId(), q("20"));

        MovimentacaoEstoque mov = estoqueService.devolver(tuboId, projetoA.getId(), q("3.5"));

        Material tubo = relido(tuboId);
        assertThat(tubo.getSaldo()).isEqualByComparingTo("33.5");
        assertThat(tubo.getSaldoReservado()).isEqualByComparingTo("0");
        MovimentacaoEstoque gravada = movimentacaoRepository.findById(mov.getId()).orElseThrow();
        assertThat(gravada.getTipo()).isEqualTo(TipoMovimentacao.DEVOLUCAO);
        assertThat(gravada.getProjeto().getId()).isEqualTo(projetoA.getId());
    }

    @Test
    @DisplayName("Quantidade zero, negativa ou com mais de 3 casas decimais é recusada em toda operação")
    void testQuantidadeInvalida() {
        Long a = projetoA.getId();
        for (BigDecimal invalida : List.of(q("0"), q("-1"), q("0.0001"))) {
            assertThatThrownBy(() -> estoqueService.entradaCompra(tuboId, invalida)).isInstanceOf(RegraNegocioException.class);
            assertThatThrownBy(() -> estoqueService.reservar(tuboId, a, invalida)).isInstanceOf(RegraNegocioException.class);
            assertThatThrownBy(() -> estoqueService.baixarReservaParaProjeto(tuboId, a, invalida))
                    .isInstanceOf(RegraNegocioException.class);
            assertThatThrownBy(() -> estoqueService.devolver(tuboId, a, invalida)).isInstanceOf(RegraNegocioException.class);
        }
        assertThat(movimentacoes()).isZero();
    }

    @Test
    @DisplayName("A soma dos efeitos das movimentações bate com o saldo e o reservado do material")
    void testMovimentacoesExplicamOSaldo() {
        estoqueService.entradaCompra(tuboId, q("100"));
        estoqueService.reservar(tuboId, projetoA.getId(), q("40"));
        estoqueService.reservar(tuboId, projetoB.getId(), q("25.5"));
        estoqueService.baixarReservaParaProjeto(tuboId, projetoA.getId(), q("30"));
        estoqueService.devolver(tuboId, projetoA.getId(), q("4"));
        estoqueService.ajuste(tuboId, q("70"));
        estoqueService.entradaCompra(tuboId, q("12.25"));

        Material tubo = relido(tuboId);
        List<MovimentacaoEstoque> todas = movimentacaoRepository.findAll();
        BigDecimal saldo = todas.stream().map(m -> m.getTipo().efeitoNoSaldo(m.getQuantidade()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reservado = todas.stream().map(m -> m.getTipo().efeitoNoReservado(m.getQuantidade()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(todas).hasSize(7);
        assertThat(tubo.getSaldo()).isEqualByComparingTo("82.25").isEqualByComparingTo(saldo);
        assertThat(tubo.getSaldoReservado()).isEqualByComparingTo("35.5").isEqualByComparingTo(reservado);
    }

    // ---------- Cadastro sem mexer no saldo ----------

    @Test
    @DisplayName("Salvar o cadastro, mesmo com um material lido antes de uma entrada, não mexe no saldo")
    void testCadastroNaoMexeNoSaldo() {
        Material lidoAntes = materialRepository.findById(tuboId).orElseThrow();
        estoqueService.entradaCompra(tuboId, q("40"));

        // o objeto lido antes da entrada ainda tem saldo 0; salvar o cadastro não pode levar o saldo de volta
        lidoAntes.setDescricao("Tubo PVC irrigação 50 mm");
        lidoAntes.setSaldo(q("999"));
        materialRepository.save(lidoAntes);
        materialService.atualizar(tuboId, "tb-pvc-050", "Tubo PVC irrigação 50 mm, barra 6 m", "barra",
                q("65.00"), q("46.00"));

        Material tubo = relido(tuboId);
        assertThat(tubo.getDescricao()).isEqualTo("Tubo PVC irrigação 50 mm, barra 6 m");
        assertThat(tubo.getCodigo()).isEqualTo("TB-PVC-050");
        assertThat(tubo.getPrecoVenda()).isEqualByComparingTo("65.00");
        assertThat(tubo.getSaldo()).isEqualByComparingTo("40");
        assertThat(tubo.getSaldoReservado()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("O formulário de edição ignora um parâmetro saldo enviado no POST")
    void testFormularioIgnoraSaldo() throws Exception {
        estoqueService.entradaCompra(tuboId, q("40"));

        mockMvc.perform(post("/estoque/materiais/" + tuboId).with(csrf()).with(almirNaTela())
                        .param("codigo", "TB-PVC-050")
                        .param("descricao", "Tubo PVC 50 mm")
                        .param("unidade", "barra")
                        .param("precoVenda", "62.90")
                        .param("custoUnitario", "44.80")
                        .param("saldo", "999")
                        .param("saldoReservado", "0"))
                .andExpect(redirectedUrl("/estoque/materiais"));

        assertThat(relido(tuboId).getSaldo()).isEqualByComparingTo("40");
    }

    @Test
    @DisplayName("Material novo nasce com saldo zero e código em maiúsculas; código repetido é recusado")
    void testCriarMaterial() {
        Material filtro = materialService.criar("  ft-dis-200 ", "Filtro de disco 2\"", "un", q("289.00"), q("205.00"));

        Material lido = relido(filtro.getId());
        assertThat(lido.getCodigo()).isEqualTo("FT-DIS-200");
        assertThat(lido.getSaldo()).isEqualByComparingTo("0");
        assertThat(lido.getSaldoReservado()).isEqualByComparingTo("0");
        assertThatThrownBy(() -> materialService.criar("FT-DIS-200", "Outro", "un", q("1"), q("1")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Já existe um material com o código FT-DIS-200.");
    }

    // ---------- Disponibilidade ----------

    @Test
    @DisplayName("Disponibilidade da lista técnica: prevista, disponível, já reservada para o projeto e faltante")
    void testDisponibilidade() {
        Long a = projetoA.getId();
        // Tubo: sobra estoque
        estoqueService.entradaCompra(tuboId, q("50"));
        estoqueService.reservar(tuboId, a, q("10"));
        // Gotejador: o pouco que há está reservado para outro projeto
        Long gotejadorId = materialService.criar("GT-AUT-2L", "Gotejador 2 L/h", "un", q("0.95"), q("0.52")).getId();
        estoqueService.entradaCompra(gotejadorId, q("2"));
        estoqueService.reservar(gotejadorId, projetoB.getId(), q("2"));
        // Registro: parte reservada e já entregue ao projeto, que continua contando como atendida
        Long registroId = materialService.criar("RG-ESF-050", "Registro de esfera 50 mm", "un", q("42"), q("27.3")).getId();
        estoqueService.entradaCompra(registroId, q("8"));
        estoqueService.reservar(registroId, a, q("6"));
        estoqueService.baixarReservaParaProjeto(registroId, a, q("6"));

        em.flush();
        em.clear();
        itemProjetoRepository.save(new ItemProjeto(null, projetoRepository.getReferenceById(a),
                materialRepository.getReferenceById(tuboId), q("30"), q("0"), q("0"), null));
        itemProjetoRepository.save(new ItemProjeto(null, projetoRepository.getReferenceById(a),
                materialRepository.getReferenceById(gotejadorId), q("5"), q("0"), q("0"), null));
        itemProjetoRepository.save(new ItemProjeto(null, projetoRepository.getReferenceById(a),
                materialRepository.getReferenceById(registroId), q("10"), q("0"), q("0"), null));
        em.flush();
        em.clear();

        List<DisponibilidadeItem> itens = estoqueService.disponibilidade(a);

        // em ordem de descrição: Gotejador, Registro, Tubo
        assertThat(itens).extracting(i -> i.getMaterial().getCodigo())
                .containsExactly("GT-AUT-2L", "RG-ESF-050", "TB-PVC-050");
        DisponibilidadeItem gotejador = itens.get(0);
        assertThat(gotejador.getPrevista()).isEqualByComparingTo("5");
        assertThat(gotejador.getDisponivel()).isEqualByComparingTo("0");
        assertThat(gotejador.getReservadaParaProjeto()).isEqualByComparingTo("0");
        assertThat(gotejador.getFaltante()).isEqualByComparingTo("5");
        DisponibilidadeItem registro = itens.get(1);
        assertThat(registro.getDisponivel()).isEqualByComparingTo("2");
        assertThat(registro.getReservadaParaProjeto()).isEqualByComparingTo("6");
        assertThat(registro.getFaltante()).isEqualByComparingTo("2");
        DisponibilidadeItem tubo = itens.get(2);
        assertThat(tubo.getDisponivel()).isEqualByComparingTo("40");
        assertThat(tubo.getReservadaParaProjeto()).isEqualByComparingTo("10");
        assertThat(tubo.getFaltante()).isEqualByComparingTo("0");
        assertThat(estoqueService.disponibilidade(projetoB.getId())).isEmpty();
    }

    @Test
    @DisplayName("A disponibilidade sai numa consulta só, com o material já carregado")
    void testDisponibilidadeNumaConsultaSo() {
        Long a = projetoA.getId();
        for (int i = 1; i <= 3; i++) {
            Long id = materialService.criar("MT-" + i, "Material " + i, "un", q("1"), q("1")).getId();
            estoqueService.entradaCompra(id, q("10"));
            estoqueService.reservar(id, a, q("1"));
            itemProjetoRepository.save(new ItemProjeto(null, projetoRepository.getReferenceById(a),
                    materialRepository.getReferenceById(id), q("4"), q("0"), q("0"), null));
        }
        em.flush();
        em.clear();
        Statistics estatisticas = emf.unwrap(SessionFactory.class).getStatistics();
        estatisticas.setStatisticsEnabled(true);
        estatisticas.clear();
        try {
            List<Object[]> linhas = itemProjetoRepository.disponibilidade(a, TipoMovimentacao.RESERVA);

            assertThat(linhas).hasSize(3);
            assertThat(linhas).allSatisfy(linha -> {
                ItemProjeto item = (ItemProjeto) linha[0];
                assertThat(Hibernate.isInitialized(item.getMaterial())).isTrue();
                assertThat(item.getMaterial().getCodigo()).startsWith("MT-");
                assertThat(new BigDecimal(linha[1].toString())).isEqualByComparingTo("1");
            });
            assertThat(estatisticas.getPrepareStatementCount()).isEqualTo(1);
        } finally {
            estatisticas.setStatisticsEnabled(false);
        }
    }

    // ---------- Extrato ----------

    @Test
    @DisplayName("Extrato paginado, do mais novo para o mais antigo, com filtro por período e por projeto")
    void testExtrato() {
        // 22 entradas, uma por dia, de 01/11 a 22/11, e uma reserva para cada projeto em 10/11
        for (int dia = 1; dia <= 22; dia++) {
            relogio.definir(LocalDateTime.of(2026, 11, dia, 8, 0));
            estoqueService.entradaCompra(tuboId, q("1"));
        }
        relogio.definir(LocalDateTime.of(2026, 11, 10, 15, 0));
        estoqueService.reservar(tuboId, projetoA.getId(), q("2"));
        estoqueService.reservar(tuboId, projetoB.getId(), q("3"));

        Page<MovimentacaoEstoque> primeira = estoqueService.extrato(tuboId, null, null, null, PageRequest.of(0, 20));
        Page<MovimentacaoEstoque> segunda = estoqueService.extrato(tuboId, null, null, null, PageRequest.of(1, 20));

        assertThat(primeira.getTotalElements()).isEqualTo(24);
        assertThat(primeira.getTotalPages()).isEqualTo(2);
        assertThat(primeira.getContent()).hasSize(20);
        assertThat(primeira.getContent().get(0).getDataHora()).isEqualTo(LocalDateTime.of(2026, 11, 22, 8, 0));
        assertThat(segunda.getContent()).hasSize(4);
        assertThat(segunda.getContent().get(3).getDataHora()).isEqualTo(LocalDateTime.of(2026, 11, 1, 8, 0));

        Page<MovimentacaoEstoque> periodo = estoqueService.extrato(tuboId, LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12), null, PageRequest.of(0, 20));
        assertThat(periodo.getContent()).extracting(MovimentacaoEstoque::getDataHora).containsExactly(
                LocalDateTime.of(2026, 11, 12, 8, 0), LocalDateTime.of(2026, 11, 11, 8, 0),
                LocalDateTime.of(2026, 11, 10, 15, 0), LocalDateTime.of(2026, 11, 10, 15, 0),
                LocalDateTime.of(2026, 11, 10, 8, 0));

        Page<MovimentacaoEstoque> doProjetoB = estoqueService.extrato(tuboId, null, null, projetoB.getId(),
                PageRequest.of(0, 20));
        assertThat(doProjetoB.getContent()).hasSize(1);
        assertThat(doProjetoB.getContent().get(0).getQuantidade()).isEqualByComparingTo("3");
        assertThat(estoqueService.projetosDoMaterial(tuboId)).extracting(Projeto::getCodigo)
                .containsExactly("PRJ-2026-0001", "PRJ-2026-0002");
    }

    // ---------- Critério de aceite pela tela ----------

    @Test
    @DisplayName("Aceite: cadastra o material, dá entrada e vê o extrato")
    void testCadastrarDarEntradaEVerOExtrato() throws Exception {
        mockMvc.perform(post("/estoque/materiais").with(csrf()).with(almirNaTela())
                        .param("codigo", "as-set-34")
                        .param("descricao", "Aspersor setorial de impacto 3/4\"")
                        .param("unidade", "un")
                        .param("precoVenda", "48.90")
                        .param("custoUnitario", "31.50"))
                .andExpect(redirectedUrl("/estoque/materiais"))
                .andExpect(flash().attribute("sucesso",
                        "Material AS-SET-34 cadastrado com saldo zero. O saldo entra pela entrada de compra."));
        Long id = materialRepository.findByCodigo("AS-SET-34").orElseThrow().getId();

        mockMvc.perform(get("/estoque/materiais").with(almirNaTela()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AS-SET-34")))
                .andExpect(content().string(containsString("R$ 48,90")));

        relogio.definir(LocalDateTime.of(2026, 11, 16, 14, 30));
        mockMvc.perform(post("/estoque/materiais/" + id + "/entrada").with(csrf()).with(almirNaTela())
                        .param("quantidade", "150"))
                .andExpect(redirectedUrl("/estoque/materiais/" + id + "/extrato"))
                .andExpect(flash().attribute("sucesso",
                        "Entrada de 150 un de AS-SET-34 registrada. Saldo agora: 150 un."));

        mockMvc.perform(get("/estoque/materiais/" + id + "/extrato").with(almirNaTela()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("16/11/2026 14:30")))
                .andExpect(content().string(containsString("Entrada de compra")))
                .andExpect(content().string(containsString("+150")))
                .andExpect(content().string(containsString("Almir Almoxarife")))
                .andExpect(content().string(not(containsString("Nenhuma movimentação no período."))));

        Material aspersor = relido(id);
        assertThat(aspersor.getSaldo()).isEqualByComparingTo("150");
        MovimentacaoEstoque entrada = estoqueService.extrato(id, null, null, null, PageRequest.of(0, 1)).getContent().get(0);
        assertThat(entrada.getTipo()).isEqualTo(TipoMovimentacao.ENTRADA_COMPRA);
        assertThat(entrada.getUsuario().getId()).isEqualTo(almir.getId());
    }

    @Test
    @DisplayName("Busca em JSON por código ou descrição, para o projetista")
    void testBuscaJson() throws Exception {
        estoqueService.entradaCompra(tuboId, q("50"));
        estoqueService.reservar(tuboId, projetoA.getId(), q("8"));
        usuarioService.criar("Paulo Projetista", "paulo", SENHA, Perfil.PROJETISTA);

        mockMvc.perform(get("/estoque/materiais/busca").param("q", "pvc")
                        .with(user(userDetailsService.loadUserByUsername("paulo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("TB-PVC-050"))
                .andExpect(jsonPath("$[0].disponivel").value(42.0))
                .andExpect(jsonPath("$[0].saldoReservado").doesNotExist());
    }
}
