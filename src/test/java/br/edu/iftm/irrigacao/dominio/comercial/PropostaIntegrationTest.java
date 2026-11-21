package br.edu.iftm.irrigacao.dominio.comercial;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioAutenticado;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;
import br.edu.iftm.irrigacao.dominio.estoque.CotacaoService;
import br.edu.iftm.irrigacao.dominio.estoque.EstoqueService;
import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.Material;
import br.edu.iftm.irrigacao.dominio.estoque.MaterialService;
import br.edu.iftm.irrigacao.dominio.estoque.MovimentacaoEstoque;
import br.edu.iftm.irrigacao.dominio.estoque.SituacaoCotacao;
import br.edu.iftm.irrigacao.dominio.estoque.TipoMovimentacao;
import br.edu.iftm.irrigacao.dominio.projeto.HistoricoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.CotacaoRepository;
import br.edu.iftm.irrigacao.persistencia.HistoricoProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.ItemProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.LevantamentoRepository;
import br.edu.iftm.irrigacao.persistencia.MaterialRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.PropostaRepository;
import br.edu.iftm.irrigacao.persistencia.RelogioAjustavel;
import jakarta.persistence.EntityManager;

/**
 * Faltantes e cotação (RF08), orçamento (RF09), alçada (RF10, G2), envio e validade (RF11) e
 * resposta do cliente (RF12, G3), com o banco H2 e o relógio parado em 16/11/2026 às 09:00.
 *
 * <p>Os parâmetros são os do application.properties: hora a R$ 85,00 de preço e R$ 45,00 de custo,
 * alçada de 5%, validade de 15 dias e alerta 3 dias antes. A lista técnica dos projetos tem 20
 * barras de tubo (R$ 62,90, custo R$ 44,80, 50 em estoque) e 400 gotejadores (R$ 0,95, custo
 * R$ 0,52, 100 em estoque, então faltam 300).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(RelogioDeTesteConfig.class)
class PropostaIntegrationTest {

    private static final String SENHA = "senha-de-teste-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RelogioAjustavel relogio;

    @Autowired
    private PropostaService propostaService;

    @Autowired
    private VerificadorDePropostas verificador;

    @Autowired
    private CotacaoService cotacaoService;

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
    private ProjetoRepository projetoRepository;

    @Autowired
    private ItemProjetoRepository itemProjetoRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private PropostaRepository propostaRepository;

    @Autowired
    private CotacaoRepository cotacaoRepository;

    @Autowired
    private HistoricoProjetoRepository historicoRepository;

    @Autowired
    private LevantamentoRepository levantamentoRepository;

    @Autowired
    private EntityManager em;

    private Usuario ana;
    private Usuario diana;
    private Long tuboId;
    private Long gotejadorId;
    private Long projetoId;
    private Long outroProjetoId;

    @BeforeEach
    void prepararDados() {
        relogio.definir(RelogioDeTesteConfig.INICIO);
        ana = usuarioService.criar("Ana Vendedora", "ana", SENHA, Perfil.VENDEDOR);
        diana = usuarioService.criar("Diana Diretora", "diana", SENHA, Perfil.DIRETORIA);
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
        projetoId = projetoOrcando("PRJ-2026-0001", fazenda, tubo, gotejador);
        outroProjetoId = projetoOrcando("PRJ-2026-0002", fazenda, tubo, gotejador);
        // A entrada limpa o contexto de persistência: fica por último
        estoqueService.entradaCompra(tuboId, new BigDecimal("50"));
        estoqueService.entradaCompra(gotejadorId, new BigDecimal("100"));
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    private Long projetoOrcando(String codigo, Propriedade propriedade, Material tubo, Material gotejador) {
        Projeto projeto = new Projeto();
        projeto.setCodigo(codigo);
        projeto.setPropriedade(propriedade);
        projeto.setResponsavel(ana);
        projeto.setDataAbertura(LocalDate.of(2026, 11, 10));
        projeto.setSituacao(SituacaoProjeto.ORCANDO);
        projeto = projetoRepository.save(projeto);
        itemProjetoRepository.save(new ItemProjeto(null, projeto, tubo, new BigDecimal("20"), BigDecimal.ZERO,
                BigDecimal.ZERO, null));
        itemProjetoRepository.save(new ItemProjeto(null, projeto, gotejador, new BigDecimal("400"), BigDecimal.ZERO,
                BigDecimal.ZERO, null));
        return projeto.getId();
    }

    /** Coloca o usuário na sessão, como no login, para as chamadas diretas ao service. */
    private void logar(Usuario usuario) {
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private RequestPostProcessor logado(String login) {
        return user(userDetailsService.loadUserByUsername(login));
    }

    private void cotarGotejador() {
        cotacaoService.registrar(projetoId, gotejadorId, "Irrigar Distribuidora", new BigDecimal("0.48"), 10,
                new BigDecimal("300"));
        cotacaoService.registrar(projetoId, gotejadorId, "AgroGotas Ltda", new BigDecimal("0.45"), 15,
                new BigDecimal("300"));
    }

    private Proposta gerar(String desconto) {
        return propostaService.gerar(projetoId, new BigDecimal("10"), new BigDecimal(desconto));
    }

    private Proposta relida(Long id) {
        em.flush();
        em.clear();
        return propostaRepository.findById(id).orElseThrow();
    }

    private Projeto projeto(Long id) {
        em.flush();
        em.clear();
        return projetoRepository.findById(id).orElseThrow();
    }

    private HistoricoProjeto ultimaMudanca(Long id) {
        List<HistoricoProjeto> historico = historicoRepository.listarPorProjeto(id);
        return historico.get(historico.size() - 1);
    }

    // ---------- Valores (RF09) ----------

    @Test
    @DisplayName("valorTotal com mão de obra e desconto; custoOrcado com a cotação mais barata só no item faltante")
    void testValorTotalECustoOrcado() {
        cotarGotejador();
        // Cotação mais barata que o cadastro para o tubo, que não falta: não entra no custo
        cotacaoService.registrar(projetoId, tuboId, "Tubos Baratos", new BigDecimal("30.00"), 5, new BigDecimal("20"));

        Proposta proposta = gerar("5");

        // (20 x 62,90 + 400 x 0,95 + 10 x 85,00) x 0,95 = 2.488,00 x 0,95
        assertThat(proposta.getValorTotal()).isEqualByComparingTo("2363.60");
        assertThat(proposta.getNumero()).isEqualTo(1);
        assertThat(proposta.getSituacao()).isEqualTo(SituacaoProposta.EM_ELABORACAO);
        assertThat(proposta.getValidadeDias()).isEqualTo(15);
        assertThat(proposta.getHorasPrevistas()).isEqualByComparingTo("10");
        assertThat(proposta.getPercentualDesconto()).isEqualByComparingTo("5");
        assertThat(proposta.getDataEnvio()).isNull();

        // 20 x 44,80 (cadastro) + 400 x 0,45 (AgroGotas) + 10 x 45,00
        assertThat(projeto(projetoId).getCustoOrcado()).isEqualByComparingTo("1526.00");
        List<ItemProjeto> itens = itemProjetoRepository.listarPorProjetoComMaterial(projetoId);
        assertThat(itens).extracting(ItemProjeto::getPrecoUnitario)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("0.95"), new BigDecimal("62.90"));

        Orcamento previa = propostaService.simular(projetoId, new BigDecimal("10"), new BigDecimal("5"));
        assertThat(previa.getValorDesconto()).isEqualByComparingTo("124.40");
        assertThat(previa.getLinhas().get(0).getOrigemDoCusto()).isEqualTo("cotação de AgroGotas Ltda");
        assertThat(previa.getLinhas().get(1).getOrigemDoCusto()).isEqualTo("cadastro");
    }

    @Test
    @DisplayName("Faltante sem cotação custa pelo cadastro; o total arredonda só no fim")
    void testFaltanteSemCotacaoEArredondamento() {
        Orcamento orcamento = propostaService.simular(projetoId, new BigDecimal("10"), new BigDecimal("3.33"));

        // 2.488,00 x 96,67% = 2.405,1496
        assertThat(orcamento.getValorTotal()).isEqualByComparingTo("2405.15");
        // 20 x 44,80 + 400 x 0,52 + 10 x 45,00
        assertThat(orcamento.getCustoOrcado()).isEqualByComparingTo("1554.00");
        assertThat(propostaRepository.count()).isZero();
    }

    @Test
    @DisplayName("Os preços ficam congelados: mudar o cadastro depois não muda a proposta em elaboração")
    void testPrecoCongelado() {
        Proposta proposta = gerar("0");
        materialService.atualizar(tuboId, "TB-PVC-050", "Tubo PVC 50 mm", "barra", new BigDecimal("70.00"),
                new BigDecimal("44.80"));

        Proposta alterada = propostaService.alterar(proposta.getId(), new BigDecimal("12"), BigDecimal.ZERO);

        // 20 x 62,90 + 400 x 0,95 + 12 x 85,00, com o tubo ainda a 62,90
        assertThat(relida(alterada.getId()).getValorTotal()).isEqualByComparingTo("2658.00");
    }

    @Test
    @DisplayName("Uma proposta em andamento por vez, e só com o projeto em ORCANDO")
    void testGerarSoQuandoPode() {
        gerar("0");

        assertThatThrownBy(() -> gerar("0"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já tem uma proposta em elaboração ou na diretoria");

        Projeto outro = projeto(outroProjetoId);
        outro.setSituacao(SituacaoProjeto.EM_LEVANTAMENTO);
        projetoRepository.saveAndFlush(outro);
        assertThatThrownBy(() -> propostaService.gerar(outroProjetoId, BigDecimal.ONE, BigDecimal.ZERO))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("O orçamento é gerado com o projeto em \"Orçando\"");
    }

    // ---------- Alçada (RF10, G2) ----------

    @Test
    @DisplayName("Desconto exatamente no limite da alçada vai direto: ENVIADA hoje e projeto aguardando resposta")
    void testDescontoNoLimite() {
        Proposta proposta = gerar("5.00");
        assertThat(propostaService.podeEnviar(proposta)).isTrue();
        assertThatThrownBy(() -> propostaService.pedirAprovacao(proposta.getId()))
                .hasMessageContaining("está dentro da alçada de 5%");

        propostaService.enviar(proposta.getId());

        Proposta enviada = relida(proposta.getId());
        assertThat(enviada.getSituacao()).isEqualTo(SituacaoProposta.ENVIADA);
        assertThat(enviada.getDataEnvio()).isEqualTo(LocalDate.of(2026, 11, 16));
        assertThat(enviada.getDataValidade()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(projeto(projetoId).getSituacao()).isEqualTo(SituacaoProjeto.AGUARDANDO_RESPOSTA);
        HistoricoProjeto transicao = ultimaMudanca(projetoId);
        assertThat(transicao.getSituacaoAnterior()).isEqualTo(SituacaoProjeto.ORCANDO);
        assertThat(transicao.getObservacao()).isEqualTo("Proposta 1 enviada ao cliente, válida até 01/12/2026.");
    }

    @Test
    @DisplayName("Desconto logo acima da alçada não vai sem a diretoria, e o projeto continua em ORCANDO")
    void testDescontoLogoAcima() {
        Proposta proposta = gerar("5.01");
        assertThat(propostaService.podeEnviar(proposta)).isFalse();

        assertThatThrownBy(() -> propostaService.enviar(proposta.getId()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O desconto de 5,01% passa da alçada de 5% e não foi aprovado pela diretoria. "
                        + "Peça a aprovação antes de enviar.");
        propostaService.pedirAprovacao(proposta.getId());

        assertThat(relida(proposta.getId()).getSituacao()).isEqualTo(SituacaoProposta.AGUARDANDO_DIRETORIA);
        assertThat(projeto(projetoId).getSituacao()).isEqualTo(SituacaoProjeto.ORCANDO);
        assertThat(propostaService.pendentesDaDiretoria()).extracting(Proposta::getId).containsExactly(proposta.getId());
    }

    @Test
    @DisplayName("Diretoria nega com motivo e depois aprova; só com a aprovação a proposta vai ao cliente")
    void testNegarEAprovar() {
        Long id = gerar("8").getId();
        propostaService.pedirAprovacao(id);
        logar(diana);

        assertThatThrownBy(() -> propostaService.negarDesconto(id, "  "))
                .hasMessage("Informe o motivo da negativa, para o vendedor refazer o desconto.");
        propostaService.negarDesconto(id, "Margem abaixo de 30% neste cliente.");

        Proposta negada = relida(id);
        assertThat(negada.getSituacao()).isEqualTo(SituacaoProposta.EM_ELABORACAO);
        assertThat(negada.getAprovadoPor()).isNull();
        assertThat(negada.getObservacaoDiretoria()).isEqualTo("Margem abaixo de 30% neste cliente.");
        assertThat(propostaService.podeEnviar(negada)).isFalse();

        propostaService.pedirAprovacao(id);
        assertThat(relida(id).getObservacaoDiretoria()).isNull();
        propostaService.aprovarDesconto(id, null);

        Proposta aprovada = relida(id);
        assertThat(aprovada.getSituacao()).isEqualTo(SituacaoProposta.EM_ELABORACAO);
        assertThat(aprovada.getAprovadoPor().getId()).isEqualTo(diana.getId());
        assertThat(propostaService.podeEnviar(aprovada)).isTrue();
        propostaService.enviar(id);
        assertThat(relida(id).getSituacao()).isEqualTo(SituacaoProposta.ENVIADA);
    }

    @Test
    @DisplayName("Mudar o desconto limpa aprovadoPor; mudar só as horas mantém a aprovação")
    void testMudarDescontoLimpaAprovacao() {
        Long id = gerar("7").getId();
        propostaService.pedirAprovacao(id);
        logar(diana);
        propostaService.aprovarDesconto(id, "Cliente antigo.");

        propostaService.alterar(id, new BigDecimal("12"), new BigDecimal("7.00"));
        Proposta mesmasHoras = relida(id);
        assertThat(mesmasHoras.getAprovadoPor()).isNotNull();
        assertThat(mesmasHoras.getObservacaoDiretoria()).isEqualTo("Cliente antigo.");

        propostaService.alterar(id, new BigDecimal("12"), new BigDecimal("6.50"));
        Proposta outroDesconto = relida(id);
        assertThat(outroDesconto.getAprovadoPor()).isNull();
        assertThat(outroDesconto.getObservacaoDiretoria()).isNull();
        assertThat(propostaService.podeEnviar(outroDesconto)).isFalse();
    }

    // ---------- Validade (RF11) ----------

    @Test
    @DisplayName("No último dia válido a aprovação vale, com reserva de tudo o que está disponível")
    void testAprovacaoNoUltimoDia() {
        Long id = gerar("0").getId();
        propostaService.enviar(id);
        relogio.definir(LocalDateTime.of(2026, 12, 1, 23, 59));

        assertThat(verificador.verificar()).isZero();
        propostaService.registrarAprovacao(id);

        assertThat(relida(id).getSituacao()).isEqualTo(SituacaoProposta.APROVADA);
        assertThat(projeto(projetoId).getSituacao()).isEqualTo(SituacaoProjeto.PREPARANDO_MATERIAL);
        // Tubo: 20 de 50 disponíveis; gotejador: só os 100 que há
        assertThat(materialRepository.findById(tuboId).orElseThrow().getSaldoReservado()).isEqualByComparingTo("20");
        assertThat(materialRepository.findById(gotejadorId).orElseThrow().getSaldoReservado()).isEqualByComparingTo("100");
        List<MovimentacaoEstoque> reservas = estoqueService.extrato(gotejadorId, null, null, projetoId,
                PageRequest.of(0, 10)).getContent();
        assertThat(reservas).singleElement().satisfies(m -> {
            assertThat(m.getTipo()).isEqualTo(TipoMovimentacao.RESERVA);
            assertThat(m.getQuantidade()).isEqualByComparingTo("100");
        });
    }

    @Test
    @DisplayName("No dia seguinte ao último válido a aprovação é bloqueada e a verificação vence e refaz a proposta")
    void testDiaSeguinteAoVencimento() {
        Long id = gerar("4").getId();
        propostaService.enviar(id);
        materialService.atualizar(tuboId, "TB-PVC-050", "Tubo PVC 50 mm", "barra", new BigDecimal("70.00"),
                new BigDecimal("44.80"));
        relogio.definir(LocalDateTime.of(2026, 12, 2, 0, 0));

        assertThatThrownBy(() -> propostaService.registrarAprovacao(id))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A proposta 1 venceu: valia até 01/12/2026. A aprovação depois do vencimento não é "
                        + "registrada; a proposta é refeita com os preços atuais.");
        assertThat(verificador.verificar()).isEqualTo(1);

        assertThat(relida(id).getSituacao()).isEqualTo(SituacaoProposta.VENCIDA);
        assertThat(projeto(projetoId).getSituacao()).isEqualTo(SituacaoProjeto.ORCANDO);
        HistoricoProjeto transicao = ultimaMudanca(projetoId);
        assertThat(transicao.getSituacaoAnterior()).isEqualTo(SituacaoProjeto.AGUARDANDO_RESPOSTA);
        assertThat(transicao.getObservacao()).startsWith("Proposta 1 venceu: valia até 01/12/2026.");
        Proposta nova = propostaRepository.findFirstByProjetoIdOrderByNumeroDesc(projetoId).orElseThrow();
        assertThat(nova.getNumero()).isEqualTo(2);
        assertThat(nova.getSituacao()).isEqualTo(SituacaoProposta.EM_ELABORACAO);
        assertThat(nova.getPercentualDesconto()).isEqualByComparingTo("4");
        // (20 x 70,00 + 400 x 0,95 + 10 x 85,00) x 0,96: o tubo já com o preço novo
        assertThat(nova.getValorTotal()).isEqualByComparingTo("2524.80");
        assertThat(verificador.verificar()).isZero();
    }

    @Test
    @DisplayName("Alerta: propostas enviadas do vendedor que vencem em até 3 dias, contando o último dia")
    void testAlerta() throws Exception {
        Long id = gerar("0").getId();
        propostaService.enviar(id);

        relogio.definir(LocalDateTime.of(2026, 11, 27, 8, 0));
        assertThat(propostaService.alertarVencimento(ana.getId())).isEmpty();
        relogio.definir(LocalDateTime.of(2026, 11, 28, 8, 0));
        assertThat(propostaService.alertarVencimento(ana.getId())).extracting(Proposta::getId).containsExactly(id);
        assertThat(propostaService.alertarVencimento(diana.getId())).isEmpty();
        mockMvc.perform(get("/").with(logado("ana")))
                .andExpect(content().string(containsString("1 proposta(s) enviada(s) perto de vencer")));
        relogio.definir(LocalDateTime.of(2026, 12, 1, 18, 0));
        assertThat(propostaService.alertarVencimento(ana.getId())).hasSize(1);
        relogio.definir(LocalDateTime.of(2026, 12, 2, 8, 0));
        assertThat(propostaService.alertarVencimento(ana.getId())).isEmpty();
        mockMvc.perform(get("/").with(logado("ana")))
                .andExpect(content().string(not(containsString("proposta(s) enviada(s) perto de vencer"))));
    }

    // ---------- Recusa (RF12, G3) ----------

    @Test
    @DisplayName("Recusa leva o projeto a CANCELADO, com o motivo no histórico")
    void testRecusa() {
        Long id = gerar("0").getId();
        propostaService.enviar(id);

        assertThatThrownBy(() -> propostaService.registrarRecusa(id, ""))
                .hasMessage("Informe o motivo da recusa do cliente.");
        propostaService.registrarRecusa(id, "Preço acima do que o cliente pode pagar.");

        assertThat(relida(id).getSituacao()).isEqualTo(SituacaoProposta.RECUSADA);
        assertThat(projeto(projetoId).getSituacao()).isEqualTo(SituacaoProjeto.CANCELADO);
        HistoricoProjeto transicao = ultimaMudanca(projetoId);
        assertThat(transicao.getSituacaoNova()).isEqualTo(SituacaoProjeto.CANCELADO);
        assertThat(transicao.getObservacao())
                .isEqualTo("Proposta 1 recusada pelo cliente: Preço acima do que o cliente pode pagar.");
        assertThatThrownBy(() -> propostaService.registrarAprovacao(id)).isInstanceOf(RegraNegocioException.class);
    }

    // ---------- Faltantes e cotação (RF08) ----------

    @Test
    @DisplayName("Faltantes: o comprador vê e cota, a mais barata fica em destaque e o vendedor não registra")
    void testFaltantesECotacao() throws Exception {
        String tela = "/comercial/projetos/" + projetoId + "/faltantes";

        mockMvc.perform(get(tela).with(logado("carlos")))
                .andExpect(status().isOk())
                .andExpect(view().name("comercial/faltantes"))
                .andExpect(TrilhaDeNavegacao.doProjeto(projetoId, "Faltantes e cotações"))
                .andExpect(content().string(containsString("Faltam 300 un")))
                .andExpect(content().string(not(containsString("TB-PVC-050 · Tubo"))))
                .andExpect(content().string(containsString("Registrar cotação")));

        mockMvc.perform(post(tela + "/cotacoes").with(csrf()).with(logado("carlos"))
                        .param("materialId", gotejadorId.toString())
                        .param("fornecedor", "Irrigar Distribuidora")
                        .param("precoUnitario", "0.48")
                        .param("prazoDias", "10")
                        .param("quantidade", "300"))
                .andExpect(redirectedUrl(tela))
                .andExpect(flash().attribute("sucesso", "Cotação de Irrigar Distribuidora para GT-AUT-2L registrada."));
        mockMvc.perform(post(tela + "/cotacoes").with(csrf()).with(logado("carlos"))
                        .param("materialId", gotejadorId.toString())
                        .param("fornecedor", "AgroGotas Ltda")
                        .param("precoUnitario", "0.45")
                        .param("quantidade", "300"))
                .andExpect(redirectedUrl(tela));

        assertThat(cotacaoRepository.listarPorProjetoComMaterial(projetoId)).hasSize(2)
                .allSatisfy(c -> assertThat(c.getSituacao()).isEqualTo(SituacaoCotacao.COTADA));
        String pagina = mockMvc.perform(get(tela).with(logado("ana")))
                .andReturn().getResponse().getContentAsString();
        // O badge fica na linha da AgroGotas, a mais barata, e só nela
        String badge = "mais barata</span>";
        assertThat(pagina).contains(badge).doesNotContain("Registrar cotação");
        assertThat(pagina.indexOf(badge)).isEqualTo(pagina.lastIndexOf(badge));
        assertThat(pagina.indexOf(badge)).isGreaterThan(pagina.indexOf("AgroGotas Ltda"));
        assertThat(pagina.indexOf(badge)).isLessThan(pagina.indexOf("Irrigar Distribuidora"));

        mockMvc.perform(post(tela + "/cotacoes").with(csrf()).with(logado("ana"))
                        .param("materialId", gotejadorId.toString())
                        .param("fornecedor", "Outro")
                        .param("precoUnitario", "0.40")
                        .param("quantidade", "300"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/comercial/propostas").with(logado("carlos"))).andExpect(status().isForbidden());
    }

    // ---------- Critério de aceite ----------

    @Test
    @DisplayName("Aceite: uma proposta vai direto ao cliente, outra passa pela diretoria; impressão; aprovação com reservas no extrato")
    void testFluxoCompletoDaEtapa() throws Exception {
        String projetoUm = "/comercial/projetos/" + projetoId;
        String projetoDois = "/comercial/projetos/" + outroProjetoId;

        // 1. Projeto 1: desconto de 5%, dentro da alçada, direto para o cliente
        mockMvc.perform(get(projetoUm + "/orcamento").with(logado("ana"))
                        .param("horasPrevistas", "10").param("percentualDesconto", "5"))
                .andExpect(status().isOk())
                .andExpect(TrilhaDeNavegacao.doProjeto(projetoId, "Orçamento"))
                .andExpect(content().string(containsString("R$ 2.363,60")))
                .andExpect(content().string(containsString("Gerar proposta")));
        String paginaUm = mockMvc.perform(post(projetoUm + "/propostas").with(csrf()).with(logado("ana"))
                        .param("horasPrevistas", "10").param("percentualDesconto", "5"))
                .andExpect(redirectedUrlPattern("/comercial/propostas/*"))
                .andReturn().getResponse().getRedirectedUrl();
        mockMvc.perform(post(paginaUm + "/enviar").with(csrf()).with(logado("ana")))
                .andExpect(redirectedUrl(paginaUm));

        // 2. A impressão traz produtor, propriedade, itens, mão de obra, desconto, total e validade
        mockMvc.perform(get(paginaUm + "/imprimir").with(logado("ana")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("João Batista Ferreira")))
                .andExpect(content().string(containsString("529.982.247-25")))
                .andExpect(content().string(containsString("Fazenda Boa Vista")))
                .andExpect(content().string(containsString("GT-AUT-2L")))
                .andExpect(content().string(containsString("Mão de obra")))
                .andExpect(content().string(containsString("Desconto de 5,00%")))
                .andExpect(content().string(containsString("R$ 2.363,60")))
                .andExpect(content().string(containsString("válida até 01/12/2026")))
                .andExpect(content().string(containsString("data-imprimir")));

        // 3. Projeto 2: desconto de 8%, passa pela diretoria
        String paginaDois = mockMvc.perform(post(projetoDois + "/propostas").with(csrf()).with(logado("ana"))
                        .param("horasPrevistas", "10").param("percentualDesconto", "8"))
                .andReturn().getResponse().getRedirectedUrl();
        mockMvc.perform(post(paginaDois + "/enviar").with(csrf()).with(logado("ana"))
                        .header(HttpHeaders.REFERER, "http://localhost" + paginaDois))
                .andExpect(flash().attribute("erro", containsString("passa da alçada")));
        mockMvc.perform(post(paginaDois + "/pedir-aprovacao").with(csrf()).with(logado("ana")))
                .andExpect(redirectedUrl(paginaDois));
        String idDois = paginaDois.substring(paginaDois.lastIndexOf('/') + 1);
        mockMvc.perform(get("/diretoria/aprovacoes").with(logado("diana")))
                .andExpect(content().string(containsString("PRJ-2026-0002 · proposta nº 1")))
                .andExpect(content().string(containsString("8,00% de desconto")));
        mockMvc.perform(post("/diretoria/aprovacoes/" + idDois + "/aprovar").with(csrf()).with(logado("diana"))
                        .param("observacao", "Cliente antigo."))
                .andExpect(redirectedUrl("/diretoria/aprovacoes"));
        mockMvc.perform(get(paginaDois).with(logado("ana")))
                .andExpect(TrilhaDeNavegacao.doProjeto(outroProjetoId, "Proposta 1"))
                .andExpect(content().string(containsString("Desconto aprovado por")))
                .andExpect(content().string(containsString("Diana Diretora")));
        mockMvc.perform(post(paginaDois + "/enviar").with(csrf()).with(logado("ana")))
                .andExpect(redirectedUrl(paginaDois));
        assertThat(projeto(outroProjetoId).getSituacao()).isEqualTo(SituacaoProjeto.AGUARDANDO_RESPOSTA);

        // 4. Cliente do projeto 1 aprova: PREPARANDO_MATERIAL e reservas no extrato
        mockMvc.perform(post(paginaUm + "/aprovacao-cliente").with(csrf()).with(logado("ana")))
                .andExpect(redirectedUrl("/projetos/" + projetoId));
        assertThat(projeto(projetoId).getSituacao()).isEqualTo(SituacaoProjeto.PREPARANDO_MATERIAL);
        mockMvc.perform(get("/estoque/materiais/" + gotejadorId + "/extrato").with(logado("almir")))
                .andExpect(content().string(containsString("Reserva")))
                .andExpect(content().string(containsString("PRJ-2026-0001")))
                .andExpect(content().string(containsString("+100")));
        // Com o material reservado, a página do projeto abre na aba Material, com o andamento da separação
        mockMvc.perform(get("/projetos/" + projetoId).with(logado("ana")))
                .andExpect(model().attribute("abaAtual", AbaProjeto.MATERIAL))
                .andExpect(content().string(containsString("itens separados por completo.")))
                .andExpect(content().string(containsString("separar o material e liberar a execução")))
                .andExpect(content().string(containsString("Aguardando o almoxarifado.")))
                .andExpect(content().string(containsString("proposta-APROVADA")))
                .andExpect(content().string(containsString("Proposta 1 aprovada pelo cliente.")));
    }

    @Test
    @DisplayName("Só a versão atual é impressa, e a lista de propostas vence as atrasadas ao abrir")
    void testImpressaoEListaDePropostas() throws Exception {
        Long id = gerar("0").getId();
        propostaService.enviar(id);
        relogio.definir(LocalDateTime.of(2026, 12, 3, 9, 0));

        mockMvc.perform(get("/comercial/propostas").with(logado("ana")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1 proposta venceu e foi refeita com os preços atuais.")))
                .andExpect(content().string(containsString("PRJ-2026-0001 nº 2")));
        mockMvc.perform(get("/comercial/propostas/" + id + "/imprimir").with(logado("ana")))
                .andExpect(redirectedUrl("/comercial/propostas/" + id))
                .andExpect(flash().attribute("erro", "Só a versão atual da proposta pode ser impressa."));
    }

    // ---------- Estimativa de mão de obra (RF09) ----------

    private void levantamentoComArea(Long projeto, Double areaHa) {
        Levantamento levantamento = new Levantamento();
        levantamento.setProjeto(projetoRepository.getReferenceById(projeto));
        levantamento.setAreaIrrigadaHa(areaHa);
        levantamentoRepository.save(levantamento);
    }

    @Test
    @DisplayName("As horas sugeridas são a área irrigada vezes 60 h/ha; sem área, não há sugestão")
    void testEstimativaDeMaoDeObra() {
        levantamentoComArea(projetoId, 12.5);
        levantamentoComArea(outroProjetoId, null);

        EstimativaMaoDeObra estimativa = propostaService.estimarMaoDeObra(projetoId).orElseThrow();
        assertThat(estimativa.areaIrrigadaHa()).isEqualByComparingTo("12.5");
        assertThat(estimativa.horasPorHectare()).isEqualByComparingTo("60");
        assertThat(estimativa.horas()).isEqualByComparingTo("750.00");
        assertThat(propostaService.estimarMaoDeObra(outroProjetoId)).isEmpty();
    }

    @Test
    @DisplayName("O orçamento abre com as horas sugeridas, e as horas digitadas pelo vendedor prevalecem")
    void testOrcamentoComHorasSugeridas() throws Exception {
        levantamentoComArea(projetoId, 12.5);
        String tela = "/comercial/projetos/" + projetoId + "/orcamento";

        mockMvc.perform(get(tela).with(logado("ana")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("12,5 ha × 60 h/ha = 750 h")))
                .andExpect(content().string(containsString("value=\"750.00\"")));
        mockMvc.perform(get(tela).with(logado("ana")).param("horasPrevistas", "10").param("percentualDesconto", "5"))
                .andExpect(content().string(containsString("value=\"10\"")))
                .andExpect(content().string(containsString("R$ 2.363,60")));
        mockMvc.perform(get("/comercial/projetos/" + outroProjetoId + "/orcamento").with(logado("ana")))
                .andExpect(content().string(containsString("não tem a área irrigada")));
    }

    // ---------- Renegociação (RF11) ----------

    @Test
    @DisplayName("Renegociar: a enviada fica renegociada, o projeto volta a Orçando e a versão nova mantém os preços")
    void testRenegociar() {
        Long id = gerar("3").getId();
        propostaService.enviar(id);
        // O cadastro muda depois do envio; a renegociação não muda o preço que o cliente já viu
        materialService.atualizar(tuboId, "TB-PVC-050", "Tubo PVC 50 mm", "barra", new BigDecimal("70.00"),
                new BigDecimal("44.80"));

        Proposta nova = propostaService.renegociar(id);

        assertThat(relida(id).getSituacao()).isEqualTo(SituacaoProposta.RENEGOCIADA);
        assertThat(projeto(projetoId).getSituacao()).isEqualTo(SituacaoProjeto.ORCANDO);
        HistoricoProjeto transicao = ultimaMudanca(projetoId);
        assertThat(transicao.getSituacaoAnterior()).isEqualTo(SituacaoProjeto.AGUARDANDO_RESPOSTA);
        assertThat(transicao.getObservacao()).startsWith("Proposta 1 reaberta para renegociação a pedido do cliente.");
        Proposta segunda = relida(nova.getId());
        assertThat(segunda.getNumero()).isEqualTo(2);
        assertThat(segunda.getSituacao()).isEqualTo(SituacaoProposta.EM_ELABORACAO);
        assertThat(segunda.getHorasPrevistas()).isEqualByComparingTo("10");
        assertThat(segunda.getPercentualDesconto()).isEqualByComparingTo("3");
        assertThat(segunda.getDataEnvio()).isNull();
        // (20 x 62,90 + 400 x 0,95 + 10 x 85,00) x 0,97: o tubo ainda a 62,90
        assertThat(segunda.getValorTotal()).isEqualByComparingTo("2413.36");

        // O vendedor dá o desconto que o cliente pediu, dentro da alçada, e envia de novo
        propostaService.alterar(nova.getId(), new BigDecimal("10"), new BigDecimal("5"));
        assertThat(relida(nova.getId()).getValorTotal()).isEqualByComparingTo("2363.60");
        propostaService.enviar(nova.getId());
        assertThat(relida(nova.getId()).getSituacao()).isEqualTo(SituacaoProposta.ENVIADA);
        assertThat(projeto(projetoId).getSituacao()).isEqualTo(SituacaoProjeto.AGUARDANDO_RESPOSTA);
    }

    @Test
    @DisplayName("Renegociar mantém a aprovação da diretoria para o mesmo desconto; mudar o desconto a desfaz")
    void testRenegociarComAprovacaoDaDiretoria() {
        logar(ana);
        Long id = gerar("8").getId();
        propostaService.pedirAprovacao(id);
        logar(diana);
        propostaService.aprovarDesconto(id, "Cliente antigo.");
        logar(ana);
        propostaService.enviar(id);

        Proposta nova = propostaService.renegociar(id);
        Proposta segunda = relida(nova.getId());
        assertThat(segunda.getAprovadoPor().getLogin()).isEqualTo("diana");
        assertThat(segunda.getObservacaoDiretoria()).isEqualTo("Cliente antigo.");
        assertThat(propostaService.podeEnviar(segunda)).isTrue();

        propostaService.alterar(nova.getId(), new BigDecimal("10"), new BigDecimal("9"));
        Proposta alterada = relida(nova.getId());
        assertThat(alterada.getAprovadoPor()).isNull();
        assertThat(propostaService.podeEnviar(alterada)).isFalse();
    }

    @Test
    @DisplayName("Só a proposta enviada e ainda válida é renegociada")
    void testRenegociarSoQuandoPode() {
        Long id = gerar("0").getId();
        assertThatThrownBy(() -> propostaService.renegociar(id))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não pode ser renegociada");

        propostaService.enviar(id);
        relogio.definir(LocalDateTime.of(2026, 12, 2, 0, 0));
        assertThatThrownBy(() -> propostaService.renegociar(id))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("venceu: valia até 01/12/2026");
        assertThat(relida(id).getSituacao()).isEqualTo(SituacaoProposta.ENVIADA);
    }

    @Test
    @DisplayName("Pela tela: o botão aparece na enviada, leva à versão nova, e a lista filtra por situação")
    void testRenegociarPelaTelaEFiltroDaLista() throws Exception {
        Long id = gerar("0").getId();
        propostaService.enviar(id);
        String pagina = "/comercial/propostas/" + id;

        mockMvc.perform(get(pagina).with(logado("ana")))
                .andExpect(content().string(containsString("Cliente pediu mudança")))
                .andExpect(content().string(containsString(pagina + "/renegociacao")));
        String novaPagina = mockMvc.perform(post(pagina + "/renegociacao").with(csrf()).with(logado("ana")))
                .andExpect(redirectedUrlPattern("/comercial/propostas/*"))
                .andExpect(flash().attribute("sucesso", containsString("Proposta 2 aberta para a renegociação")))
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(novaPagina).isNotEqualTo(pagina);
        mockMvc.perform(get(novaPagina).with(logado("ana")))
                .andExpect(content().string(containsString("Horas e desconto")))
                .andExpect(content().string(not(containsString("Cliente pediu mudança"))));

        // Padrão: em andamento, só a versão nova
        mockMvc.perform(get("/comercial/propostas").with(logado("ana")))
                .andExpect(content().string(containsString("PRJ-2026-0001 nº 2")))
                .andExpect(content().string(not(containsString("PRJ-2026-0001 nº 1"))));
        mockMvc.perform(get("/comercial/propostas").param("filtro", "RENEGOCIADA").with(logado("ana")))
                .andExpect(content().string(containsString("PRJ-2026-0001 nº 1")))
                .andExpect(content().string(containsString("proposta-RENEGOCIADA")))
                .andExpect(content().string(not(containsString("PRJ-2026-0001 nº 2"))));
        mockMvc.perform(get("/comercial/propostas").param("filtro", "TODAS").with(logado("ana")))
                .andExpect(content().string(containsString("PRJ-2026-0001 nº 1")))
                .andExpect(content().string(containsString("PRJ-2026-0001 nº 2")));
        mockMvc.perform(get("/comercial/propostas").param("filtro", "APROVADA").with(logado("ana")))
                .andExpect(content().string(containsString("Nenhuma proposta nesta situação.")));
        // Filtro desconhecido volta para as em andamento
        mockMvc.perform(get("/comercial/propostas").param("filtro", "QUALQUER").with(logado("ana")))
                .andExpect(content().string(containsString("PRJ-2026-0001 nº 2")))
                .andExpect(content().string(not(containsString("PRJ-2026-0001 nº 1"))));
    }

    @Test
    @DisplayName("A proposta aprovada sai da lista padrão, mas aparece no filtro de aprovadas")
    void testAprovadaNoFiltro() throws Exception {
        Long id = gerar("0").getId();
        propostaService.enviar(id);
        propostaService.registrarAprovacao(id);

        mockMvc.perform(get("/comercial/propostas").with(logado("ana")))
                .andExpect(content().string(containsString("Nenhuma proposta em andamento.")));
        mockMvc.perform(get("/comercial/propostas").param("filtro", "APROVADA").with(logado("ana")))
                .andExpect(content().string(containsString("PRJ-2026-0001 nº 1")))
                .andExpect(content().string(containsString("1 proposta(s)")));
    }
}
