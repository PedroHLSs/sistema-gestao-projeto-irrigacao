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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
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

import br.edu.iftm.irrigacao.apresentacao.web.AbaProjeto;
import br.edu.iftm.irrigacao.apresentacao.web.TrilhaDeNavegacao;
import br.edu.iftm.irrigacao.config.RelogioDeTesteConfig;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.projeto.FilaEstoque;
import br.edu.iftm.irrigacao.dominio.projeto.PainelService;
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
    private SobrasService sobrasService;

    @Autowired
    private PainelService painelService;

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
    @DisplayName("Depois do aceite do cliente a requisição é recusada; antes dele, a compra continua durante a execução")
    void testRequisicaoForaDaSituacao() {
        Projeto projeto = projeto();
        projeto.setSituacao(SituacaoProjeto.FINALIZANDO_EXECUCAO);
        projetoRepository.saveAndFlush(projeto);

        assertThatThrownBy(() -> cotacaoService.requisitar(projetoId, agroGotasId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A requisição de compra é feita da aprovação da proposta até o aceite do cliente. "
                        + "O projeto PRJ-2026-0001 está em \"Finalizando execução\".");
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
        assertThatThrownBy(() -> separacaoService.separar(projetoId, Map.of(gotejadorItem, new BigDecimal("400"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("GT-AUT-2L ainda não está todo reservado para o projeto: 100 de 400 un. "
                        + "Receba a compra antes de separar.");

        separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("20")));
        em.flush();
        em.clear();
        assertThat(itemProjetoRepository.findById(tuboItem).orElseThrow().getQuantidadeSeparada())
                .isEqualByComparingTo("20");
        assertThat(itemProjetoRepository.findById(gotejadorItem).orElseThrow().getQuantidadeSeparada())
                .isEqualByComparingTo("0");
    }

    // ---------- Separação aos poucos (RF14) ----------

    private BigDecimal separada(Long itemId) {
        em.flush();
        em.clear();
        return itemProjetoRepository.findById(itemId).orElseThrow().getQuantidadeSeparada();
    }

    @Test
    @DisplayName("Separação aos poucos: cada gravação soma ao separado, nada sai do estoque antes, e a conferência passa no fim")
    void testSeparacaoAosPoucos() {
        receberAgroGotas();

        ProgressoSeparacao primeira = separacaoService.separar(projetoId, Map.of(gotejadorItem, new BigDecimal("150")));
        assertThat(primeira.getItensAlterados()).isEqualTo(1);
        assertThat(primeira.getCompletos()).isZero();
        assertThat(primeira.getEmAndamento()).isEqualTo(1);
        assertThat(primeira.getPercentual()).isZero();

        ProgressoSeparacao segunda = separacaoService.separar(projetoId,
                Map.of(gotejadorItem, new BigDecimal("250"), tuboItem, new BigDecimal("12.5")));
        assertThat(segunda.getItensAlterados()).isEqualTo(2);
        assertThat(segunda.getCompletos()).isEqualTo(1);
        assertThat(segunda.getEmAndamento()).isEqualTo(1);
        assertThat(segunda.getFaltam()).isEqualTo(1);
        assertThat(segunda.getPercentual()).isEqualTo(50);
        assertThat(segunda.isCompleta()).isFalse();
        assertThat(separada(gotejadorItem)).isEqualByComparingTo("400");
        assertThat(separada(tuboItem)).isEqualByComparingTo("12.5");
        assertThat(saidas(tuboId)).isZero();
        assertThat(saidas(gotejadorId)).isZero();

        ProgressoSeparacao terceira = separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("7.5")));
        assertThat(terceira.isCompleta()).isTrue();
        assertThat(terceira.getPercentual()).isEqualTo(100);
        assertThat(separada(tuboItem)).isEqualByComparingTo("20");

        assertThat(separacaoService.liberarParaExecucao(projetoId).isCompleta()).isTrue();
        assertThat(material(tuboId).getSaldo()).isEqualByComparingTo("30");
        assertThat(material(gotejadorId).getSaldo()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Separar além do que falta é recusado; com o item completo, a mensagem diz que já está todo separado")
    void testSeparacaoAlemDaPrevista() {
        separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("15")));

        assertThatThrownBy(() -> separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("10"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Separar mais 10 barra de TB-PVC-050 passa da quantidade prevista (20 barra). "
                        + "Falta separar 5 barra.");
        assertThat(separada(tuboItem)).isEqualByComparingTo("15");

        separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("5")));
        assertThatThrownBy(() -> separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("0.5"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("TB-PVC-050 já está todo separado: 20 barra.");
        assertThat(separada(tuboItem)).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("Quantidade negativa tira do separado para corrigir um engano, sem deixar abaixo de zero")
    void testCorrecaoComNegativo() {
        separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("12")));
        separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("-2")));
        assertThat(separada(tuboItem)).isEqualByComparingTo("10");

        assertThatThrownBy(() -> separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("-11"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Não dá para tirar 11 barra de TB-PVC-050: só 10 barra foram separados.");
        assertThat(separada(tuboItem)).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Com um item recusado na gravação, nenhum item muda; vazio e zero deixam o item como está")
    void testSeparacaoTudoOuNada() {
        Map<Long, BigDecimal> comGotejadorSemReserva = new HashMap<>();
        comGotejadorSemReserva.put(tuboItem, new BigDecimal("20"));
        comGotejadorSemReserva.put(gotejadorItem, new BigDecimal("100"));

        assertThatThrownBy(() -> separacaoService.separar(projetoId, comGotejadorSemReserva))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageStartingWith("GT-AUT-2L ainda não está todo reservado");
        assertThat(separada(tuboItem)).isEqualByComparingTo("0");

        Map<Long, BigDecimal> semNada = new HashMap<>();
        semNada.put(tuboItem, null);
        semNada.put(gotejadorItem, BigDecimal.ZERO);
        ProgressoSeparacao progresso = separacaoService.separar(projetoId, semNada);
        assertThat(progresso.getItensAlterados()).isZero();
        assertThat(progresso.getTotal()).isEqualTo(2);
        assertThat(separada(tuboItem)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Tela: andamento, situação de cada item, o que falta e o botão de preencher; gravar sem nada só avisa")
    void testTelaDaSeparacaoAosPoucos() throws Exception {
        String separacao = "/estoque/projetos/" + projetoId + "/separacao";
        receberAgroGotas();

        mockMvc.perform(post(separacao).with(csrf()).with(logado("almir"))
                        .param("quantidades[" + tuboItem + "]", "20")
                        .param("quantidades[" + gotejadorItem + "]", "150"))
                .andExpect(redirectedUrl(separacao))
                .andExpect(flash().attribute("sucesso", "Separação gravada em 2 itens. 1 de 2 itens estão completos; "
                        + "continue quando quiser, o que já foi separado fica guardado. Com pelo menos um item "
                        + "separado, a execução já pode ser liberada."));

        mockMvc.perform(get(separacao).with(logado("almir")))
                .andExpect(status().isOk())
                .andExpect(TrilhaDeNavegacao.doProjeto(projetoId, "Separação do material"))
                .andExpect(content().string(containsString("1 de 2 itens separados por completo")))
                .andExpect(content().string(containsString("1 item em andamento")))
                .andExpect(content().string(containsString("Separar agora")))
                .andExpect(content().string(containsString(">Completo<")))
                .andExpect(content().string(containsString(">Em andamento<")))
                .andExpect(content().string(containsString("data-valor=\"250\"")))
                .andExpect(content().string(containsString("min=\"-150\"")))
                .andExpect(content().string(containsString("Separar tudo o que falta")))
                .andExpect(content().string(containsString("Liberar para a execução")))
                .andExpect(content().string(containsString("A obra pode começar com o que já foi separado.")));

        mockMvc.perform(post(separacao).with(csrf()).with(logado("almir"))
                        .param("quantidades[" + tuboItem + "]", "")
                        .param("quantidades[" + gotejadorItem + "]", ""))
                .andExpect(redirectedUrl(separacao))
                .andExpect(flash().attribute("aviso", "Nada foi gravado: informe em \"Separar agora\" quanto você "
                        + "separou de cada item."));

        mockMvc.perform(post(separacao).with(csrf()).with(logado("almir"))
                        .header(HttpHeaders.REFERER, "http://localhost" + separacao)
                        .param("quantidades[" + gotejadorItem + "]", "300"))
                .andExpect(redirectedUrl(separacao))
                .andExpect(flash().attribute("erro", "Separar mais 300 un de GT-AUT-2L passa da quantidade prevista "
                        + "(400 un). Falta separar 250 un."));
        assertThat(separada(gotejadorItem)).isEqualByComparingTo("150");
    }

    // ---------- Liberação da execução (G5) ----------

    @Test
    @DisplayName("Sem nenhum item separado a execução não é liberada, e nada muda no estoque nem no projeto")
    void testLiberarSemSeparacao() {
        assertThatThrownBy(() -> separacaoService.liberarParaExecucao(projetoId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Separe pelo menos um item antes de liberar a execução: a equipe não vai a campo sem "
                        + "material.");

        Projeto projeto = projeto();
        assertThat(projeto.getSituacao()).isEqualTo(SituacaoProjeto.PREPARANDO_MATERIAL);
        assertThat(projeto.isSeparacaoConferida()).isFalse();
        assertThat(saidas(tuboId)).isZero();
        assertThat(saidas(gotejadorId)).isZero();
    }

    @Test
    @DisplayName("Liberação parcial: com um item separado a obra começa; só o separado sai e o resto fica reservado")
    void testLiberacaoParcial() {
        // O gotejador ainda espera a compra; do tubo, 18 das 20 barras estão separadas
        separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("18")));

        ProgressoSeparacao progresso = separacaoService.liberarParaExecucao(projetoId);

        assertThat(progresso.getCompletos()).isZero();
        assertThat(progresso.getEmAndamento()).isEqualTo(1);
        Material tubo = material(tuboId);
        assertThat(tubo.getSaldo()).isEqualByComparingTo("32");
        assertThat(tubo.getSaldoReservado()).isEqualByComparingTo("2");
        assertThat(movimentacoes(tuboId).get(0)).satisfies(m -> {
            assertThat(m.getTipo()).isEqualTo(TipoMovimentacao.SAIDA_PROJETO);
            assertThat(m.getQuantidade()).isEqualByComparingTo("18");
        });
        assertThat(saidas(gotejadorId)).isZero();
        assertThat(material(gotejadorId).getSaldoReservado()).isEqualByComparingTo("100");
        Projeto projeto = projeto();
        assertThat(projeto.isSeparacaoConferida()).isTrue();
        assertThat(projeto.getSituacao()).isEqualTo(SituacaoProjeto.PROGRAMANDO_EXECUCAO);
        List<HistoricoProjeto> historico = historicoRepository.listarPorProjeto(projetoId);
        assertThat(historico.get(historico.size() - 1).getObservacao()).isEqualTo("Material liberado para a execução: "
                + "o separado de 1 de 2 itens saiu do estoque para o projeto. O restante continua reservado e é "
                + "separado durante a instalação.");
    }

    @Test
    @DisplayName("Depois da liberação a separação e a compra continuam, e cada separação já sai do estoque")
    void testSeparacaoDuranteAExecucao() {
        separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("18")));
        separacaoService.liberarParaExecucao(projetoId);

        // O resto do tubo sai na própria separação
        ProgressoSeparacao tubo = separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("2")));
        assertThat(tubo.getCompletos()).isEqualTo(1);
        assertThat(material(tuboId).getSaldo()).isEqualByComparingTo("30");
        assertThat(material(tuboId).getSaldoReservado()).isEqualByComparingTo("0");
        assertThat(saidas(tuboId)).isEqualTo(2);

        // A compra do gotejador é requisitada e recebida com a instalação já programada, e então separada
        receberAgroGotas();
        ProgressoSeparacao tudo = separacaoService.separar(projetoId, Map.of(gotejadorItem, new BigDecimal("400")));
        assertThat(tudo.isCompleta()).isTrue();
        assertThat(material(gotejadorId).getSaldo()).isEqualByComparingTo("0");
        assertThat(material(gotejadorId).getSaldoReservado()).isEqualByComparingTo("0");

        // Tirar do separado não vale mais: o material já saiu do estoque
        assertThatThrownBy(() -> separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("-1"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Depois que a execução foi liberada a separação só soma: o separado de TB-PVC-050 já saiu "
                        + "do estoque para o projeto. O que sobrar volta na devolução das sobras.");
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.PROGRAMANDO_EXECUCAO);
    }

    @Test
    @DisplayName("Liberação com tudo separado: saída de cada item, sem reserva pendente, e transição 7")
    void testLiberacaoCompleta() {
        receberAgroGotas();
        separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("20"), gotejadorItem, new BigDecimal("400")));

        assertThat(separacaoService.liberarParaExecucao(projetoId).isCompleta()).isTrue();

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
        assertThat(transicao.getObservacao())
                .isEqualTo("Material liberado para a execução: o separado de 2 de 2 itens saiu do estoque para o projeto.");
    }

    @Test
    @DisplayName("Liberar duas vezes é recusado e não baixa o estoque de novo; com o aceite colhido, a separação fecha")
    void testLiberarDuasVezes() {
        receberAgroGotas();
        separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("20"), gotejadorItem, new BigDecimal("400")));
        separacaoService.liberarParaExecucao(projetoId);

        assertThatThrownBy(() -> separacaoService.liberarParaExecucao(projetoId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A execução do projeto PRJ-2026-0001 já foi liberada.");
        assertThat(material(tuboId).getSaldo()).isEqualByComparingTo("30");
        assertThat(saidas(tuboId)).isEqualTo(1);

        Projeto projeto = projeto();
        projeto.setSituacao(SituacaoProjeto.FINALIZANDO_EXECUCAO);
        projetoRepository.saveAndFlush(projeto);
        assertThatThrownBy(() -> separacaoService.separar(projetoId, Map.of(tuboItem, BigDecimal.ONE)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A separação é feita da aprovação da proposta até o aceite do cliente. O projeto "
                        + "PRJ-2026-0001 está em \"Finalizando execução\".");
    }

    @Test
    @DisplayName("Nas sobras, a reserva do que não foi separado nem usado volta ao disponível")
    void testReservaPendenteVoltaNasSobras() {
        // Obra liberada com 18 barras de tubo; o gotejador (100 reservados) nunca chegou a ser separado
        separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("18")));
        separacaoService.liberarParaExecucao(projetoId);
        Projeto projeto = projeto();
        projeto.setSituacao(SituacaoProjeto.FINALIZANDO_EXECUCAO);
        projetoRepository.saveAndFlush(projeto);

        // Nada foi apontado: as 18 barras voltam como sobra
        int liberadas = sobrasService.devolver(projetoId, Map.of(tuboItem, new BigDecimal("18")));

        assertThat(liberadas).isEqualTo(2);
        Material tubo = material(tuboId);
        assertThat(tubo.getSaldo()).isEqualByComparingTo("50");
        assertThat(tubo.getSaldoReservado()).isEqualByComparingTo("0");
        Material gotejador = material(gotejadorId);
        assertThat(gotejador.getSaldo()).isEqualByComparingTo("100");
        assertThat(gotejador.getSaldoReservado()).isEqualByComparingTo("0");
        assertThat(movimentacoes(gotejadorId).get(0)).satisfies(m -> {
            assertThat(m.getTipo()).isEqualTo(TipoMovimentacao.RESERVA);
            assertThat(m.getQuantidade()).isEqualByComparingTo("-100");
        });
        assertThat(estoqueService.disponibilidade(projetoId))
                .allSatisfy(item -> assertThat(item.getReservadaParaProjeto()).isEqualByComparingTo(
                        item.getMaterial().getCodigo().equals("TB-PVC-050") ? "18" : "0"));
    }

    // ---------- Fila do estoque ----------

    @Test
    @DisplayName("Fila do estoque: o projeto aparece para separar, depois para continuar, e sai com tudo separado")
    void testFilaDoEstoque() throws Exception {
        mockMvc.perform(get("/estoque/fila").with(logado("almir")))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque/fila"))
                .andExpect(content().string(containsString("0 de 2 itens separados por completo")))
                .andExpect(content().string(containsString("/estoque/projetos/" + projetoId + "/separacao")));
        assertThat(painelService.filaEstoque().aSeparar()).hasSize(1);
        assertThat(painelService.montar(Perfil.ALMOXARIFE, null).estoque().separacoesPendentes()).isEqualTo(1);

        // Obra liberada com parte do tubo: o projeto passa para a separação a continuar
        separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("18")));
        separacaoService.liberarParaExecucao(projetoId);
        FilaEstoque fila = painelService.filaEstoque();
        assertThat(fila.aSeparar()).isEmpty();
        assertThat(fila.aContinuar()).singleElement().satisfies(separacao -> {
            assertThat(separacao.projeto().getCodigo()).isEqualTo("PRJ-2026-0001");
            assertThat(separacao.progresso().getEmAndamento()).isEqualTo(1);
        });
        assertThat(painelService.montar(Perfil.ALMOXARIFE, null).estoque().separacoesPendentes()).isEqualTo(1);
        mockMvc.perform(get("/estoque/fila").with(logado("almir")))
                .andExpect(content().string(containsString("Separação a continuar")))
                .andExpect(content().string(containsString("situacao-PROGRAMANDO_EXECUCAO")));

        // Com tudo separado, o projeto sai da fila
        separacaoService.separar(projetoId, Map.of(tuboItem, new BigDecimal("2")));
        receberAgroGotas();
        separacaoService.separar(projetoId, Map.of(gotejadorItem, new BigDecimal("400")));
        assertThat(painelService.filaEstoque().aContinuar()).isEmpty();
        assertThat(painelService.montar(Perfil.ALMOXARIFE, null).estoque().separacoesPendentes()).isZero();
    }

    // ---------- Telas e permissões ----------

    @Test
    @DisplayName("O comprador requisita, o almoxarife separa e libera a execução; um não faz o que é do outro")
    void testPermissoes() throws Exception {
        String separacao = "/estoque/projetos/" + projetoId + "/separacao";

        mockMvc.perform(post("/estoque/projetos/" + projetoId + "/requisicoes").with(csrf()).with(logado("almir"))
                        .param("cotacaoId", agroGotasId.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(separacao).with(csrf()).with(logado("carlos"))
                        .param("quantidades[" + tuboItem + "]", "20"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(separacao + "/liberacao").with(csrf()).with(logado("carlos")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(separacao).with(logado("ana"))).andExpect(status().isForbidden());
        mockMvc.perform(get(separacao).with(logado("carlos"))).andExpect(status().isOk());
        mockMvc.perform(get("/estoque/requisicoes").with(logado("almir"))).andExpect(status().isOk());

        assertThat(situacao(agroGotasId)).isEqualTo(SituacaoCotacao.COTADA);
    }

    // ---------- Critério de aceite ----------

    @Test
    @DisplayName("Aceite: requisição, recebimento, separação parcial, liberação da execução e o resto separado depois, "
            + "com tudo no extrato")
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
                // Requisições de compra tem item próprio no menu: a página não tem trilha
                .andExpect(content().string(not(containsString("aria-label=\"Trilha de navegação\""))))
                .andExpect(content().string(containsString("300 un")))
                .andExpect(content().string(containsString("R$ 135,00")));
        mockMvc.perform(post("/estoque/requisicoes/" + agroGotasId + "/recebimento").with(csrf()).with(logado("almir")))
                .andExpect(redirectedUrl("/estoque/requisicoes"))
                .andExpect(flash().attribute("sucesso", containsString("reservado para o projeto PRJ-2026-0001")));

        // 3. Separação parcial: o tubo todo e 390 dos 400 gotejadores; já dá para liberar a execução
        mockMvc.perform(post(separacao).with(csrf()).with(logado("almir"))
                        .param("quantidades[" + tuboItem + "]", "20")
                        .param("quantidades[" + gotejadorItem + "]", "390"))
                .andExpect(redirectedUrl(separacao))
                .andExpect(flash().attribute("sucesso", "Separação gravada em 2 itens. 1 de 2 itens estão completos; "
                        + "continue quando quiser, o que já foi separado fica guardado. Com pelo menos um item "
                        + "separado, a execução já pode ser liberada."));
        mockMvc.perform(get(separacao).with(logado("almir")))
                .andExpect(content().string(containsString("Liberar para a execução")));
        mockMvc.perform(post(separacao + "/liberacao").with(csrf()).with(logado("almir")))
                .andExpect(redirectedUrl("/projetos/" + projetoId))
                .andExpect(flash().attribute("sucesso", "Execução liberada: o que foi separado saiu do estoque e o "
                        + "projeto está em \"Programando execução\". O restante continua reservado: separe aos poucos "
                        + "durante a instalação, e cada separação já sai do estoque."));
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.PROGRAMANDO_EXECUCAO);
        assertThat(material(gotejadorId).getSaldoReservado()).isEqualByComparingTo("10");

        // 4. Com a instalação já programada, os 10 gotejadores que faltavam são separados e saem do estoque
        mockMvc.perform(get(separacao).with(logado("almir")))
                .andExpect(content().string(containsString("A execução já foi liberada.")))
                .andExpect(content().string(not(containsString("Liberar para a execução"))));
        mockMvc.perform(post(separacao).with(csrf()).with(logado("almir"))
                        .param("quantidades[" + gotejadorItem + "]", "10"))
                .andExpect(redirectedUrl(separacao))
                .andExpect(flash().attribute("sucesso", "Separação gravada em 1 item. O que foi separado agora saiu do "
                        + "estoque para o projeto. Todos os itens estão separados."));
        assertThat(material(gotejadorId).getSaldoReservado()).isEqualByComparingTo("0");

        // 5. O extrato do gotejador mostra cada movimentação: entradas, reservas e as duas saídas
        String extrato = mockMvc.perform(get("/estoque/materiais/" + gotejadorId + "/extrato").with(logado("almir")))
                .andReturn().getResponse().getContentAsString();
        assertThat(extrato).contains("Entrada de compra", "Reserva", "Saída para projeto", "+300", "+100", "-390", "-10");
        assertThat(extrato.split("Entrada de compra", -1)).hasSize(3);
        assertThat(extrato.split(">Reserva<", -1)).hasSize(3);

        // 6. A aba Material mostra prevista, reservada e separada, e que todo o material saiu
        mockMvc.perform(get("/projetos/" + projetoId).with(logado("almir")))
                .andExpect(model().attribute("abaAtual", AbaProjeto.EXECUCAO))
                .andExpect(content().string(containsString("programar a execução e emitir a ordem de serviço")))
                .andExpect(content().string(containsString("Aguardando a engenharia.")))
                .andExpect(content().string(containsString("Reservada")))
                .andExpect(content().string(containsString(
                        "Execução liberada e todo o material separado: ele saiu do estoque para o projeto.")))
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
