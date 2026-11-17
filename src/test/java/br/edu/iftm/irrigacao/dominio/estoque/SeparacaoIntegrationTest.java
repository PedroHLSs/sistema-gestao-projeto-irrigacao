package br.edu.iftm.irrigacao.dominio.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
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
import br.edu.iftm.irrigacao.dominio.comercial.PropostaService;
import br.edu.iftm.irrigacao.dominio.projeto.HistoricoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.CotacaoRepository;
import br.edu.iftm.irrigacao.persistencia.HistoricoProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.ItemProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.MaterialRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.RelogioAjustavel;
import jakarta.persistence.EntityManager;

/**
 * Requisição de compra e recebimento (RF13, G4), separação e conferência (RF14, G5), com o banco H2.
 *
 * <p>O projeto chega em PREPARANDO_MATERIAL pelo fluxo da etapa 7: lista técnica com 20 barras de
 * tubo (50 em estoque) e 400 gotejadores (100 em estoque), proposta aprovada e o disponível
 * reservado. Faltam 300 gotejadores, cotados com a Irrigar (R$ 0,48) e a AgroGotas (R$ 0,45).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(RelogioDeTesteConfig.class)
class SeparacaoIntegrationTest {

    private static final String SENHA = "senha-de-teste-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RelogioAjustavel relogio;

    @Autowired
    private SeparacaoService separacaoService;

    @Autowired
    private CotacaoService cotacaoService;

    @Autowired
    private EstoqueService estoqueService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private PropostaService propostaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private ProdutorService produtorService;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private ItemProjetoRepository itemProjetoRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private CotacaoRepository cotacaoRepository;

    @Autowired
    private HistoricoProjetoRepository historicoRepository;

    @Autowired
    private EntityManager em;

    private Long projetoId;
    private Long tuboId;
    private Long gotejadorId;
    private Long tuboItem;
    private Long gotejadorItem;
    private Long irrigarId;
    private Long agroGotasId;

    @BeforeEach
    void levarOProjetoAPrepararMaterial() {
        relogio.definir(RelogioDeTesteConfig.INICIO);
        Usuario ana = usuarioService.criar("Ana Vendedora", "ana", SENHA, Perfil.VENDEDOR);
        usuarioService.criar("Carlos Comprador", "carlos", SENHA, Perfil.COMPRADOR);
        usuarioService.criar("Almir Almoxarife", "almir", SENHA, Perfil.ALMOXARIFE);
        Produtor joao = produtorService.criar("João Batista Ferreira", "529.982.247-25", null, null);
        Propriedade fazenda = produtorService.criarPropriedade(joao.getId(), "Fazenda Boa Vista", "Uberlândia", null,
                null, null);
        Material tubo = materialService.criar("TB-PVC-050", "Tubo PVC 50 mm", "barra", new BigDecimal("62.90"),
                new BigDecimal("44.80"));
        Material gotejador = materialService.criar("GT-AUT-2L", "Gotejador 2 L/h", "un", new BigDecimal("0.95"),
                new BigDecimal("0.52"));
        tuboId = tubo.getId();
        gotejadorId = gotejador.getId();

        Projeto projeto = new Projeto();
        projeto.setCodigo("PRJ-2026-0001");
        projeto.setPropriedade(fazenda);
        projeto.setResponsavel(ana);
        projeto.setDataAbertura(LocalDate.of(2026, 11, 10));
        projeto.setSituacao(SituacaoProjeto.ORCANDO);
        projetoId = projetoRepository.save(projeto).getId();
        tuboItem = itemProjetoRepository.save(new ItemProjeto(null, projeto, tubo, new BigDecimal("20"),
                BigDecimal.ZERO, BigDecimal.ZERO, null)).getId();
        gotejadorItem = itemProjetoRepository.save(new ItemProjeto(null, projeto, gotejador, new BigDecimal("400"),
                BigDecimal.ZERO, BigDecimal.ZERO, null)).getId();

        estoqueService.entradaCompra(tuboId, new BigDecimal("50"));
        estoqueService.entradaCompra(gotejadorId, new BigDecimal("100"));
        irrigarId = cotacaoService.registrar(projetoId, gotejadorId, "Irrigar Distribuidora", new BigDecimal("0.48"),
                10, new BigDecimal("300")).getId();
        agroGotasId = cotacaoService.registrar(projetoId, gotejadorId, "AgroGotas Ltda", new BigDecimal("0.45"), 15,
                new BigDecimal("300")).getId();

        logar(ana);
        Long proposta = propostaService.gerar(projetoId, new BigDecimal("10"), BigDecimal.ZERO).getId();
        propostaService.enviar(proposta);
        propostaService.registrarAprovacao(proposta);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    private void logar(Usuario usuario) {
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private RequestPostProcessor logado(String login) {
        return user(userDetailsService.loadUserByUsername(login));
    }

    private Material material(Long id) {
        em.flush();
        em.clear();
        return materialRepository.findById(id).orElseThrow();
    }

    private Projeto projeto() {
        em.flush();
        em.clear();
        return projetoRepository.findById(projetoId).orElseThrow();
    }

    private SituacaoCotacao situacao(Long cotacaoId) {
        em.flush();
        em.clear();
        return cotacaoRepository.findById(cotacaoId).orElseThrow().getSituacao();
    }

    private List<MovimentacaoEstoque> movimentacoes(Long materialId) {
        em.flush();
        em.clear();
        return estoqueService.extrato(materialId, null, null, projetoId, PageRequest.of(0, 50)).getContent();
    }

    private long saidas(Long materialId) {
        return movimentacoes(materialId).stream().filter(m -> m.getTipo() == TipoMovimentacao.SAIDA_PROJETO).count();
    }

    private void receberAgroGotas() {
        cotacaoService.requisitar(projetoId, agroGotasId);
        cotacaoService.receber(agroGotasId);
    }

    // ---------- Requisição (RF13, G4) ----------

    @Test
    @DisplayName("Ponto de partida: aprovado, com o tubo todo reservado e o gotejador faltando 300")
    void testPontoDePartida() {
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.PREPARANDO_MATERIAL);
        assertThat(material(tuboId).getSaldoReservado()).isEqualByComparingTo("20");
        assertThat(material(gotejadorId).getSaldoReservado()).isEqualByComparingTo("100");
        DisponibilidadeItem gotejador = estoqueService.disponibilidade(projetoId).get(0);
        assertThat(gotejador.getFaltante()).isEqualByComparingTo("300");
        assertThat(separacaoService.podeSeparar(gotejador)).isFalse();
        assertThat(separacaoService.podeSeparar(estoqueService.disponibilidade(projetoId).get(1))).isTrue();
    }

    @Test
    @DisplayName("Requisição que cobre o faltante descarta as outras cotações do item")
    void testRequisicaoDescartaAsOutras() {
        assertThat(cotacaoService.quantidadesAComprar(projetoId).get(gotejadorId)).isEqualByComparingTo("300");

        cotacaoService.requisitar(projetoId, agroGotasId);

        assertThat(situacao(agroGotasId)).isEqualTo(SituacaoCotacao.REQUISITADA);
        assertThat(situacao(irrigarId)).isEqualTo(SituacaoCotacao.DESCARTADA);
        assertThat(cotacaoService.quantidadesAComprar(projetoId).get(gotejadorId)).isEqualByComparingTo("0");
        assertThat(cotacaoService.requisicoesAbertas()).extracting(Cotacao::getId).containsExactly(agroGotasId);
        assertThatThrownBy(() -> cotacaoService.requisitar(projetoId, irrigarId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A cotação de Irrigar Distribuidora está \"Descartada\" e não pode ser requisitada.");
        // Requisição não mexe no estoque
        assertThat(material(gotejadorId).getSaldo()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("O mesmo material tem várias requisições enquanto faltar comprar; coberto, a cotação que sobrou é descartada")
    void testVariasRequisicoesEnquantoFaltar() {
        // AgroGotas só tem 200 dos 300 que faltam; uma terceira cotação fica de reserva
        Cotacao agroGotas = cotacaoRepository.findById(agroGotasId).orElseThrow();
        agroGotas.setQuantidade(new BigDecimal("200"));
        Long valeId = cotacaoRepository.save(new Cotacao(null, agroGotas.getProjeto(), agroGotas.getMaterial(),
                "Gotas do Vale", new BigDecimal("0.50"), 20, new BigDecimal("300"), SituacaoCotacao.COTADA)).getId();
        cotacaoRepository.flush();

        cotacaoService.requisitar(projetoId, agroGotasId);

        assertThat(situacao(irrigarId)).isEqualTo(SituacaoCotacao.COTADA);
        assertThat(situacao(valeId)).isEqualTo(SituacaoCotacao.COTADA);
        assertThat(cotacaoService.quantidadesAComprar(projetoId).get(gotejadorId)).isEqualByComparingTo("100");

        cotacaoService.requisitar(projetoId, irrigarId);

        assertThat(situacao(irrigarId)).isEqualTo(SituacaoCotacao.REQUISITADA);
        assertThat(situacao(valeId)).isEqualTo(SituacaoCotacao.DESCARTADA);
        assertThat(cotacaoService.requisicoesAbertas()).extracting(Cotacao::getId)
                .containsExactlyInAnyOrder(agroGotasId, irrigarId);

        // As duas compras chegam: reserva só até as 400 previstas, e o que passou fica disponível
        cotacaoService.receber(agroGotasId);
        cotacaoService.receber(irrigarId);
        Material gotejador = material(gotejadorId);
        assertThat(gotejador.getSaldo()).isEqualByComparingTo("600");
        assertThat(gotejador.getSaldoReservado()).isEqualByComparingTo("400");
        assertThat(separacaoService.podeSeparar(estoqueService.disponibilidade(projetoId).get(0))).isTrue();
    }

    @Test
    @DisplayName("Item que a reserva já cobre não tem o que requisitar")
    void testNadaAComprar() {
        Material tubo = materialRepository.findById(tuboId).orElseThrow();
        Long cotacaoTubo = cotacaoRepository.save(new Cotacao(null, projetoRepository.getReferenceById(projetoId), tubo,
                "Tubos Baratos", new BigDecimal("30.00"), 5, new BigDecimal("20"), SituacaoCotacao.COTADA)).getId();

        assertThat(cotacaoService.quantidadesAComprar(projetoId).get(tuboId)).isEqualByComparingTo("0");
        assertThatThrownBy(() -> cotacaoService.requisitar(projetoId, cotacaoTubo))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("TB-PVC-050 não tem mais nada a comprar: a reserva e as requisições abertas já cobrem "
                        + "a quantidade prevista.");
        assertThat(situacao(cotacaoTubo)).isEqualTo(SituacaoCotacao.COTADA);
    }

    @Test
    @DisplayName("Fora de PREPARANDO_MATERIAL a requisição é recusada")
    void testRequisicaoForaDaSituacao() {
        Projeto projeto = projeto();
        projeto.setSituacao(SituacaoProjeto.PROGRAMANDO_EXECUCAO);
        projetoRepository.saveAndFlush(projeto);

        assertThatThrownBy(() -> cotacaoService.requisitar(projetoId, agroGotasId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A requisição de compra é feita com o projeto em \"Preparando material\". "
                        + "O projeto PRJ-2026-0001 está em \"Programando execução\".");
        assertThat(situacao(agroGotasId)).isEqualTo(SituacaoCotacao.COTADA);
    }

    // ---------- Recebimento ----------

    @Test
    @DisplayName("Recebimento: RECEBIDA, entrada no estoque e reserva para o projeto")
    void testRecebimentoGeraEntradaEReserva() {
        receberAgroGotas();

        assertThat(situacao(agroGotasId)).isEqualTo(SituacaoCotacao.RECEBIDA);
        assertThat(cotacaoService.requisicoesAbertas()).isEmpty();
        Material gotejador = material(gotejadorId);
        assertThat(gotejador.getSaldo()).isEqualByComparingTo("400");
        assertThat(gotejador.getSaldoReservado()).isEqualByComparingTo("400");
        List<MovimentacaoEstoque> doProjeto = movimentacoes(gotejadorId);
        assertThat(doProjeto).extracting(MovimentacaoEstoque::getTipo)
                .containsExactly(TipoMovimentacao.RESERVA, TipoMovimentacao.RESERVA);
        assertThat(doProjeto.get(0).getQuantidade()).isEqualByComparingTo("300");
        List<MovimentacaoEstoque> todas = estoqueService.extrato(gotejadorId, null, null, null, PageRequest.of(0, 50))
                .getContent();
        assertThat(todas).filteredOn(m -> m.getTipo() == TipoMovimentacao.ENTRADA_COMPRA)
                .extracting(MovimentacaoEstoque::getQuantidade)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("300"), new BigDecimal("100"));
        assertThat(separacaoService.podeSeparar(estoqueService.disponibilidade(projetoId).get(0))).isTrue();
        assertThatThrownBy(() -> cotacaoService.receber(agroGotasId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("só requisição aberta pode ser recebida");
    }

    @Test
    @DisplayName("O que chega a mais do que o item precisa entra no estoque, mas fica disponível")
    void testRecebimentoAMais() {
        Cotacao maior = cotacaoRepository.findById(agroGotasId).orElseThrow();
        maior.setQuantidade(new BigDecimal("350"));
        cotacaoRepository.saveAndFlush(maior);

        receberAgroGotas();

        Material gotejador = material(gotejadorId);
        assertThat(gotejador.getSaldo()).isEqualByComparingTo("450");
        assertThat(gotejador.getSaldoReservado()).isEqualByComparingTo("400");
    }

    // ---------- Separação (RF14) ----------

    @Test
    @DisplayName("Item ainda não reservado por inteiro não pode ser separado")
    void testSeparacaoSoDeItemReservado() {
        assertThatThrownBy(() -> separacaoService.registrar(projetoId, Map.of(gotejadorItem, new BigDecimal("400"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("GT-AUT-2L ainda não está todo reservado para o projeto: 100 de 400 un. "
                        + "Receba a compra antes de separar.");

        separacaoService.registrar(projetoId, Map.of(tuboItem, new BigDecimal("20")));
        em.flush();
        em.clear();
        assertThat(itemProjetoRepository.findById(tuboItem).orElseThrow().getQuantidadeSeparada())
                .isEqualByComparingTo("20");
        assertThat(itemProjetoRepository.findById(gotejadorItem).orElseThrow().getQuantidadeSeparada())
                .isEqualByComparingTo("0");
    }

    // ---------- Conferência (G5) ----------

    @Test
    @DisplayName("Separação com divergência: lista os itens com a diferença e não mexe no estoque nem no projeto")
    void testDivergenciaSemEfeito() {
        receberAgroGotas();
        separacaoService.registrar(projetoId, Map.of(tuboItem, new BigDecimal("18"), gotejadorItem, new BigDecimal("400")));

        ResultadoConferencia resultado = separacaoService.conferir(projetoId);

        assertThat(resultado.isConferida()).isFalse();
        assertThat(resultado.getDivergencias()).singleElement().satisfies(d -> {
            assertThat(d.getItem().getMaterial().getCodigo()).isEqualTo("TB-PVC-050");
            assertThat(d.getDiferenca()).isEqualByComparingTo("-2");
            assertThat(d.getDescricao()).isEqualTo("TB-PVC-050: prevista 20 barra, separada 18 barra (-2)");
        });
        assertThat(material(tuboId).getSaldo()).isEqualByComparingTo("50");
        assertThat(material(tuboId).getSaldoReservado()).isEqualByComparingTo("20");
        assertThat(material(gotejadorId).getSaldo()).isEqualByComparingTo("400");
        assertThat(saidas(tuboId)).isZero();
        assertThat(saidas(gotejadorId)).isZero();
        Projeto projeto = projeto();
        assertThat(projeto.getSituacao()).isEqualTo(SituacaoProjeto.PREPARANDO_MATERIAL);
        assertThat(projeto.isSeparacaoConferida()).isFalse();
    }

    @Test
    @DisplayName("Conferência correta: saída de cada item, separação conferida e transição 7")
    void testConferenciaBaixaOEstoque() {
        receberAgroGotas();
        separacaoService.registrar(projetoId, Map.of(tuboItem, new BigDecimal("20"), gotejadorItem, new BigDecimal("400")));

        ResultadoConferencia resultado = separacaoService.conferir(projetoId);

        assertThat(resultado.isConferida()).isTrue();
        Material tubo = material(tuboId);
        assertThat(tubo.getSaldo()).isEqualByComparingTo("30");
        assertThat(tubo.getSaldoReservado()).isEqualByComparingTo("0");
        Material gotejador = material(gotejadorId);
        assertThat(gotejador.getSaldo()).isEqualByComparingTo("0");
        assertThat(gotejador.getSaldoReservado()).isEqualByComparingTo("0");
        assertThat(movimentacoes(gotejadorId).get(0)).satisfies(m -> {
            assertThat(m.getTipo()).isEqualTo(TipoMovimentacao.SAIDA_PROJETO);
            assertThat(m.getQuantidade()).isEqualByComparingTo("400");
        });
        Projeto projeto = projeto();
        assertThat(projeto.isSeparacaoConferida()).isTrue();
        assertThat(projeto.getSituacao()).isEqualTo(SituacaoProjeto.PROGRAMANDO_EXECUCAO);
        List<HistoricoProjeto> historico = historicoRepository.listarPorProjeto(projetoId);
        HistoricoProjeto transicao = historico.get(historico.size() - 1);
        assertThat(transicao.getSituacaoAnterior()).isEqualTo(SituacaoProjeto.PREPARANDO_MATERIAL);
        assertThat(transicao.getObservacao()).isEqualTo("Separação conferida: 2 itens saíram do estoque para o projeto.");
    }

    @Test
    @DisplayName("Conferir duas vezes é recusado e não baixa o estoque de novo")
    void testConferirDuasVezes() {
        receberAgroGotas();
        separacaoService.registrar(projetoId, Map.of(tuboItem, new BigDecimal("20"), gotejadorItem, new BigDecimal("400")));
        separacaoService.conferir(projetoId);

        assertThatThrownBy(() -> separacaoService.conferir(projetoId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A separação do projeto PRJ-2026-0001 já foi conferida.");
        assertThatThrownBy(() -> separacaoService.registrar(projetoId, Map.of(tuboItem, BigDecimal.ONE)))
                .hasMessage("A separação do projeto PRJ-2026-0001 já foi conferida.");

        assertThat(material(tuboId).getSaldo()).isEqualByComparingTo("30");
        assertThat(saidas(tuboId)).isEqualTo(1);
    }

    // ---------- Telas e permissões ----------

    @Test
    @DisplayName("O comprador requisita, o almoxarife separa e confere; um não faz o que é do outro")
    void testPermissoes() throws Exception {
        String separacao = "/estoque/projetos/" + projetoId + "/separacao";

        mockMvc.perform(post("/estoque/projetos/" + projetoId + "/requisicoes").with(csrf()).with(logado("almir"))
                        .param("cotacaoId", agroGotasId.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(separacao).with(csrf()).with(logado("carlos"))
                        .param("quantidades[" + tuboItem + "]", "20"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(separacao + "/conferencia").with(csrf()).with(logado("carlos")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(separacao).with(logado("ana"))).andExpect(status().isForbidden());
        mockMvc.perform(get(separacao).with(logado("carlos"))).andExpect(status().isOk());
        mockMvc.perform(get("/estoque/requisicoes").with(logado("almir"))).andExpect(status().isOk());

        assertThat(situacao(agroGotasId)).isEqualTo(SituacaoCotacao.COTADA);
    }

    // ---------- Critério de aceite ----------

    @Test
    @DisplayName("Aceite: requisição, recebimento, separação com erro, correção e conferência, com tudo no extrato")
    void testFluxoCompletoDaEtapa() throws Exception {
        String separacao = "/estoque/projetos/" + projetoId + "/separacao";

        // 1. O comprador vê o faltante e requisita a cotação mais barata
        mockMvc.perform(get(separacao).with(logado("carlos")))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque/separacao"))
                .andExpect(content().string(containsString("Reservado para o projeto: 100 de 400 un.")))
                .andExpect(content().string(containsString("A comprar: 300 un")))
                .andExpect(content().string(containsString("Requisitar")))
                .andExpect(content().string(containsString("aguardando reserva")));
        mockMvc.perform(post("/estoque/projetos/" + projetoId + "/requisicoes").with(csrf()).with(logado("carlos"))
                        .param("cotacaoId", agroGotasId.toString()))
                .andExpect(redirectedUrl(separacao))
                .andExpect(flash().attribute("sucesso", "Requisição de compra de GT-AUT-2L a AgroGotas Ltda registrada. "
                        + "O item está coberto e as outras cotações dele foram descartadas."));

        // 2. O almoxarife recebe a compra
        mockMvc.perform(get("/estoque/requisicoes").with(logado("almir")))
                .andExpect(content().string(containsString("AgroGotas Ltda")))
                .andExpect(content().string(containsString("300 un")))
                .andExpect(content().string(containsString("R$ 135,00")));
        mockMvc.perform(post("/estoque/requisicoes/" + agroGotasId + "/recebimento").with(csrf()).with(logado("almir")))
                .andExpect(redirectedUrl("/estoque/requisicoes"))
                .andExpect(flash().attribute("sucesso", containsString("reservado para o projeto PRJ-2026-0001")));

        // 3. Separação com erro: 390 gotejadores em vez de 400
        mockMvc.perform(post(separacao).with(csrf()).with(logado("almir"))
                        .param("quantidades[" + tuboItem + "]", "20")
                        .param("quantidades[" + gotejadorItem + "]", "390"))
                .andExpect(redirectedUrl(separacao));
        mockMvc.perform(post(separacao + "/conferencia").with(csrf()).with(logado("almir")))
                .andExpect(redirectedUrl(separacao))
                .andExpect(flash().attribute("divergencias", hasItem("GT-AUT-2L: prevista 400 un, separada 390 un (-10)")));
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.PREPARANDO_MATERIAL);
        assertThat(saidas(gotejadorId)).isZero();

        // 4. Correção e conferência aprovada
        mockMvc.perform(post(separacao).with(csrf()).with(logado("almir"))
                        .param("quantidades[" + gotejadorItem + "]", "400"))
                .andExpect(redirectedUrl(separacao));
        mockMvc.perform(post(separacao + "/conferencia").with(csrf()).with(logado("almir")))
                .andExpect(redirectedUrl("/projetos/" + projetoId));
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.PROGRAMANDO_EXECUCAO);

        // 5. O extrato do gotejador mostra cada movimentação: entradas, reservas e a saída
        String extrato = mockMvc.perform(get("/estoque/materiais/" + gotejadorId + "/extrato").with(logado("almir")))
                .andReturn().getResponse().getContentAsString();
        assertThat(extrato).contains("Entrada de compra", "Reserva", "Saída para projeto", "+300", "+100", "-400");
        assertThat(extrato.split("Entrada de compra", -1)).hasSize(3);
        assertThat(extrato.split(">Reserva<", -1)).hasSize(3);

        // 6. A aba Lista técnica mostra prevista, reservada e separada
        mockMvc.perform(get("/projetos/" + projetoId).with(logado("almir")))
                .andExpect(content().string(containsString("Reservada")))
                .andExpect(content().string(containsString("Separada")))
                .andExpect(content().string(containsString("situacao-PROGRAMANDO_EXECUCAO")));
    }

    @Test
    @DisplayName("Quantidade separada que não é número volta com a mensagem, sem gravar")
    void testSeparacaoInvalida() throws Exception {
        String separacao = "/estoque/projetos/" + projetoId + "/separacao";

        mockMvc.perform(post(separacao).with(csrf()).with(logado("almir"))
                        .header(HttpHeaders.REFERER, "http://localhost" + separacao)
                        .param("quantidades[" + tuboItem + "]", "vinte"))
                .andExpect(redirectedUrl(separacao))
                .andExpect(flash().attributeExists("erro"));

        em.flush();
        em.clear();
        assertThat(itemProjetoRepository.findById(tuboItem).orElseThrow().getQuantidadeSeparada())
                .isEqualByComparingTo("0");
    }
}
