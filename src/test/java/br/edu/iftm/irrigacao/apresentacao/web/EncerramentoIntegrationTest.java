package br.edu.iftm.irrigacao.apresentacao.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
import br.edu.iftm.irrigacao.dominio.comercial.Proposta;
import br.edu.iftm.irrigacao.dominio.comercial.PropostaService;
import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;
import br.edu.iftm.irrigacao.dominio.estoque.EstoqueService;
import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.ListaTecnicaService;
import br.edu.iftm.irrigacao.dominio.estoque.MaterialService;
import br.edu.iftm.irrigacao.dominio.estoque.SeparacaoService;
import br.edu.iftm.irrigacao.dominio.estoque.SobrasService;
import br.edu.iftm.irrigacao.dominio.execucao.AceiteService;
import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoService;
import br.edu.iftm.irrigacao.dominio.execucao.MedicaoService;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServicoService;
import br.edu.iftm.irrigacao.dominio.execucao.TipoPendencia;
import br.edu.iftm.irrigacao.dominio.projeto.Apuracao;
import br.edu.iftm.irrigacao.dominio.projeto.DadosLevantamento;
import br.edu.iftm.irrigacao.dominio.projeto.DimensionamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.EventoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.FotoEnviada;
import br.edu.iftm.irrigacao.dominio.projeto.Garantia;
import br.edu.iftm.irrigacao.dominio.projeto.LevantamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.LiberacaoFaturamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.LinhaDoTempoService;
import br.edu.iftm.irrigacao.dominio.projeto.OrcadoRealizado;
import br.edu.iftm.irrigacao.dominio.projeto.OrcadoRealizadoCsv;
import br.edu.iftm.irrigacao.dominio.projeto.OrcadoRealizadoLinha;
import br.edu.iftm.irrigacao.dominio.projeto.OrcadoRealizadoService;
import br.edu.iftm.irrigacao.dominio.projeto.PainelInicial;
import br.edu.iftm.irrigacao.dominio.projeto.PainelService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.TipoFonteAgua;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.RelogioAjustavel;
import jakarta.persistence.EntityManager;

/**
 * Encerramento do projeto (RF20, RF21, RF22) e painel inicial, com o ciclo inteiro passando pelos
 * services de cada etapa, no banco H2.
 *
 * <p>Valores do projeto padrão: 20 barras de tubo (venda 62,90, custo 44,80), 400 gotejadores (venda
 * 0,95, custo 0,52) e 10 h previstas (venda 85,00, custo 45,00). Valor total de 2.488,00 e custo orçado
 * de 1.554,00. A execução aponta 8 h, 17 barras e 400 gotejadores: custo realizado de 1.329,60.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(RelogioDeTesteConfig.class)
class EncerramentoIntegrationTest {

    private static final String SENHA = "senha-de-teste-1";
    private static final LocalDate HOJE = RelogioDeTesteConfig.INICIO.toLocalDate();

    @Autowired private MockMvc mockMvc;
    @Autowired private RelogioAjustavel relogio;
    @Autowired private EntityManager em;
    @Autowired private UserDetailsService userDetailsService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private ProdutorService produtorService;
    @Autowired private MaterialService materialService;
    @Autowired private EstoqueService estoqueService;
    @Autowired private ProjetoService projetoService;
    @Autowired private ProjetoRepository projetoRepository;
    @Autowired private LevantamentoService levantamentoService;
    @Autowired private DimensionamentoService dimensionamentoService;
    @Autowired private ListaTecnicaService listaTecnicaService;
    @Autowired private PropostaService propostaService;
    @Autowired private SeparacaoService separacaoService;
    @Autowired private OrdemServicoService ordemServicoService;
    @Autowired private ApontamentoService apontamentoService;
    @Autowired private MedicaoService medicaoService;
    @Autowired private AceiteService aceiteService;
    @Autowired private SobrasService sobrasService;
    @Autowired private LiberacaoFaturamentoService liberacaoService;
    @Autowired private OrcadoRealizadoService orcadoRealizadoService;
    @Autowired private LinhaDoTempoService linhaDoTempoService;
    @Autowired private PainelService painelService;

    private Usuario ana;
    private Usuario pedro;
    private Usuario tito;
    private Usuario almir;
    private Usuario fabio;
    private Usuario diana;
    private Propriedade fazenda;
    private Long tuboId;
    private Long gotejadorId;

    @BeforeEach
    void preparar() {
        relogio.definir(RelogioDeTesteConfig.INICIO);
        ana = usuarioService.criar("Ana Vendedora", "ana", SENHA, Perfil.VENDEDOR);
        pedro = usuarioService.criar("Pedro Projetista", "pedro", SENHA, Perfil.PROJETISTA);
        tito = usuarioService.criar("Tito Técnico", "tito", SENHA, Perfil.TECNICO_CAMPO);
        almir = usuarioService.criar("Almir Almoxarife", "almir", SENHA, Perfil.ALMOXARIFE);
        fabio = usuarioService.criar("Fábio Financeiro", "fabio", SENHA, Perfil.FINANCEIRO);
        diana = usuarioService.criar("Diana Diretora", "diana", SENHA, Perfil.DIRETORIA);
        usuarioService.criar("Carlos Comprador", "carlos", SENHA, Perfil.COMPRADOR);
        Produtor joao = produtorService.criar("João Batista Ferreira", "529.982.247-25", null, null);
        fazenda = produtorService.criarPropriedade(joao.getId(), "Fazenda Boa Vista", "Uberlândia", null, null, null);
        tuboId = materialService.criar("TB-PVC-050", "Tubo PVC 50 mm", "barra", new BigDecimal("62.90"),
                new BigDecimal("44.80")).getId();
        gotejadorId = materialService.criar("GT-AUT-2L", "Gotejador 2 L/h", "un", new BigDecimal("0.95"),
                new BigDecimal("0.52")).getId();
        estoqueService.entradaCompra(tuboId, new BigDecimal("500"));
        estoqueService.entradaCompra(gotejadorId, new BigDecimal("4000"));
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    // ---------- Apoio ----------

    private void logar(Usuario usuario) {
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private RequestPostProcessor logado(String login) {
        return user(userDetailsService.loadUserByUsername(login));
    }

    /** Separa no tempo os registros de cada etapa, para a linha do tempo ter ordem definida. */
    private void passarTempo() {
        relogio.avancar(Duration.ofMinutes(10));
    }

    private Projeto projeto(Long id) {
        em.flush();
        em.clear();
        return projetoRepository.findById(id).orElseThrow();
    }

    private Long abrirComLevantamentoCompleto() {
        logar(ana);
        Long id = projetoService.abrir(fazenda.getProdutor().getId(), fazenda.getId()).getId();
        passarTempo();
        logar(pedro);
        levantamentoService.salvar(id, new DadosLevantamento(LocalDateTime.of(2026, 11, 16, 8, 0), "Café", 12.5,
                TipoFonteAgua.POCO_ARTESIANO, 8.0, "Plana", -18.91, -48.26),
                List.of(new FotoEnviada("foto.png", FotosDeTeste.png())));
        return id;
    }

    private Long ateAprovarProposta(BigDecimal desconto) {
        Long id = abrirComLevantamentoCompleto();
        passarTempo();
        dimensionamentoService.registrar(id, 30.0, 6.0, 2.0);
        listaTecnicaService.adicionar(id, tuboId, new BigDecimal("20"));
        listaTecnicaService.adicionar(id, gotejadorId, new BigDecimal("400"));
        dimensionamentoService.concluirListaTecnica(id);
        passarTempo();
        logar(ana);
        Proposta proposta = propostaService.gerar(id, new BigDecimal("10"), desconto);
        if (!propostaService.podeEnviar(proposta)) {
            propostaService.pedirAprovacao(proposta.getId());
            logar(diana);
            propostaService.aprovarDesconto(proposta.getId(), null);
            logar(ana);
        }
        passarTempo();
        propostaService.enviar(proposta.getId());
        passarTempo();
        propostaService.registrarAprovacao(proposta.getId());
        return id;
    }

    private Long ateProgramar(BigDecimal desconto) {
        Long id = ateAprovarProposta(desconto);
        passarTempo();
        logar(almir);
        Map<Long, BigDecimal> separadas = new LinkedHashMap<>();
        for (ItemProjeto item : listaTecnicaService.listar(id)) {
            separadas.put(item.getId(), item.getQuantidadePrevista());
        }
        separacaoService.separar(id, separadas);
        separacaoService.liberarParaExecucao(id);
        return id;
    }

    /** Vai até o aceite assinado: 8 h, 17 barras e 400 gotejadores apontados. Sobram 3 barras. */
    private Long ateFinalizar(BigDecimal desconto) {
        Long id = ateProgramar(desconto);
        passarTempo();
        logar(pedro);
        Long ordemId = ordemServicoService.emitir(id, HOJE, List.of(tito.getId())).getId();
        passarTempo();
        logar(tito);
        apontamentoService.apontarHoras(ordemId, new BigDecimal("8"));
        passarTempo();
        apontamentoService.apontarMaterial(ordemId, tuboId, new BigDecimal("17"));
        apontamentoService.apontarMaterial(ordemId, gotejadorId, new BigDecimal("400"));
        passarTempo();
        ordemServicoService.concluirInstalacao(ordemId);
        passarTempo();
        medicaoService.registrar(id, 30.0, 6.0, null, null, null);
        passarTempo();
        aceiteService.registrarAceite(id, "João Batista Ferreira", assinatura());
        em.flush();
        em.clear();
        return id;
    }

    private Long ateResolverSobras() {
        Long id = ateFinalizar(BigDecimal.ZERO);
        passarTempo();
        logar(almir);
        Long itemTubo = listaTecnicaService.listar(id).stream()
                .filter(item -> item.getMaterial().getId().equals(tuboId)).findFirst().orElseThrow().getId();
        sobrasService.devolver(id, Map.of(itemTubo, new BigDecimal("3")));
        em.flush();
        em.clear();
        return id;
    }

    private Long encerrar() {
        Long id = ateResolverSobras();
        passarTempo();
        logar(fabio);
        liberacaoService.liberar(id, HOJE, 12);
        em.flush();
        em.clear();
        return id;
    }

    private static String assinatura() {
        BufferedImage imagem = new BufferedImage(300, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pincel = imagem.createGraphics();
        pincel.setColor(Color.DARK_GRAY);
        pincel.drawLine(20, 80, 280, 40);
        pincel.dispose();
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            ImageIO.write(imagem, "png", saida);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(saida.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------- Liberação do faturamento (RF20) ----------

    @Test
    @DisplayName("Sem as sobras resolvidas a liberação é bloqueada e nada é gravado")
    void testLiberacaoBloqueadaSemSobras() throws Exception {
        Long id = ateFinalizar(BigDecimal.ZERO);
        logar(fabio);

        assertThatThrownBy(() -> liberacaoService.liberar(id, HOJE, 12))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("sobras");

        Projeto projeto = projeto(id);
        assertThat(projeto.getSituacao()).isEqualTo(SituacaoProjeto.FINALIZANDO_EXECUCAO);
        assertThat(projeto.getDataLiberacaoFaturamento()).isNull();
        assertThat(projeto.getDataInicioGarantia()).isNull();
        assertThat(projeto.getPrazoGarantiaMeses()).isNull();

        mockMvc.perform(get("/financeiro/liberacoes").with(logado("fabio")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ainda não foram resolvidas")))
                .andExpect(content().string(containsString("disabled")))
                .andExpect(content().string(not(containsString("href=\"/financeiro/liberacoes/" + id + "\""))));
        mockMvc.perform(post("/financeiro/liberacoes/{id}", id)
                .param("dataInicioGarantia", HOJE.toString()).param("prazoGarantiaMeses", "12")
                .with(csrf()).with(logado("fabio")))
                .andExpect(status().is3xxRedirection());
        assertThat(projeto(id).getSituacao()).isEqualTo(SituacaoProjeto.FINALIZANDO_EXECUCAO);
    }

    @Test
    @DisplayName("Com as sobras resolvidas, liberar grava as datas e encerra o projeto (transição 17)")
    void testLiberar() throws Exception {
        Long id = ateResolverSobras();
        relogio.definir(LocalDateTime.of(2026, 11, 20, 10, 0));

        mockMvc.perform(post("/financeiro/liberacoes/{id}", id)
                .param("dataInicioGarantia", "2026-11-18").param("prazoGarantiaMeses", "12")
                .with(csrf()).with(logado("fabio")))
                .andExpect(redirectedUrl("/projetos/" + id));

        Projeto projeto = projeto(id);
        assertThat(projeto.getSituacao()).isEqualTo(SituacaoProjeto.ENCERRADO);
        assertThat(projeto.getDataLiberacaoFaturamento()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(projeto.getDataInicioGarantia()).isEqualTo(LocalDate.of(2026, 11, 18));
        assertThat(projeto.getPrazoGarantiaMeses()).isEqualTo(12);
        EventoProjeto ultimo = linhaDoTempoService.doProjeto(id).getLast();
        assertThat(ultimo.situacaoAnterior()).isEqualTo(SituacaoProjeto.FINALIZANDO_EXECUCAO);
        assertThat(ultimo.situacaoNova()).isEqualTo(SituacaoProjeto.ENCERRADO);
        assertThat(ultimo.usuario()).isEqualTo("Fábio Financeiro");
        assertThat(ultimo.detalhe()).contains("12 meses", "18/11/2026", "18/11/2027");
    }

    @Test
    @DisplayName("A garantia não começa antes do aceite nem depois de hoje, e o prazo vai de 1 a 120 meses")
    void testRegrasDaGarantia() throws Exception {
        Long id = ateResolverSobras();
        logar(fabio);

        assertThatThrownBy(() -> liberacaoService.liberar(id, HOJE.minusDays(1), 12))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("antes do aceite");
        assertThatThrownBy(() -> liberacaoService.liberar(id, HOJE.plusDays(1), 12))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("depois de hoje");
        assertThatThrownBy(() -> liberacaoService.liberar(id, HOJE, 0))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("de 1 a 120");
        assertThatThrownBy(() -> liberacaoService.liberar(id, HOJE, 121))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("de 1 a 120");

        mockMvc.perform(post("/financeiro/liberacoes/{id}", id)
                .param("dataInicioGarantia", "").param("prazoGarantiaMeses", "abc")
                .with(csrf()).with(logado("fabio")))
                .andExpect(status().isOk())
                .andExpect(view().name("financeiro/liberarFaturamento"))
                .andExpect(model().attributeHasFieldErrors("liberacao", "dataInicioGarantia", "prazoGarantiaMeses"))
                .andExpect(content().string(containsString("Informe a data de início da garantia.")))
                .andExpect(content().string(containsString("Informe um número inteiro.")));
        assertThat(projeto(id).getSituacao()).isEqualTo(SituacaoProjeto.FINALIZANDO_EXECUCAO);
    }

    @Test
    @DisplayName("A diretoria acompanha a fila, mas só o financeiro libera")
    void testQuemLibera() throws Exception {
        Long id = ateResolverSobras();

        mockMvc.perform(get("/financeiro/liberacoes").with(logado("diana")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/financeiro/liberacoes/" + id + "\""))));
        for (String login : List.of("diana", "ana")) {
            mockMvc.perform(post("/financeiro/liberacoes/{id}", id)
                    .param("dataInicioGarantia", HOJE.toString()).param("prazoGarantiaMeses", "12")
                    .with(csrf()).with(logado(login)))
                    .andExpect(status().isForbidden());
        }
        // Na página do projeto, a aba Encerramento leva à liberação, só para o financeiro
        String liberacao = "href=\"/financeiro/liberacoes/" + id + "\"";
        mockMvc.perform(get("/projetos/{id}", id).with(logado("fabio")))
                .andExpect(model().attribute("abaAtual", AbaProjeto.ENCERRAMENTO))
                .andExpect(content().string(containsString("Liberação do faturamento")))
                .andExpect(content().string(containsString(liberacao)));
        mockMvc.perform(get("/projetos/{id}", id).with(logado("diana")))
                .andExpect(content().string(containsString("Liberação do faturamento")))
                .andExpect(content().string(not(containsString(liberacao))));
        mockMvc.perform(get("/financeiro/liberacoes/{id}", id).with(logado("fabio")))
                .andExpect(status().isOk())
                .andExpect(TrilhaDeNavegacao.doProjeto(id, "Liberar faturamento"))
                .andExpect(content().string(containsString("Liberar e encerrar o projeto")));
        assertThat(projeto(id).getSituacao()).isEqualTo(SituacaoProjeto.FINALIZANDO_EXECUCAO);
    }

    // ---------- Garantia (RF21) ----------

    @Test
    @DisplayName("A garantia fica vigente até o último dia e vencida no dia seguinte")
    void testGarantiaVigenteEVencida() throws Exception {
        Long id = encerrar();

        relogio.definir(LocalDateTime.of(2027, 11, 16, 23, 59));
        Garantia ultimoDia = liberacaoService.garantia(projeto(id)).orElseThrow();
        assertThat(ultimoDia.fim()).isEqualTo(LocalDate.of(2027, 11, 16));
        assertThat(ultimoDia.vigente()).isTrue();
        mockMvc.perform(get("/projetos/{id}", id).with(logado("ana")))
                .andExpect(model().attribute("abaAtual", AbaProjeto.ENCERRAMENTO))
                // Encerrado, não há próximo passo
                .andExpect(content().string(not(containsString("Próximo passo:"))))
                .andExpect(content().string(containsString("Até 16/11/2027")))
                .andExpect(content().string(containsString(">Vigente</span>")))
                .andExpect(content().string(not(containsString(">Vencida</span>"))));

        relogio.definir(LocalDateTime.of(2027, 11, 17, 0, 0));
        assertThat(liberacaoService.garantia(projeto(id)).orElseThrow().vigente()).isFalse();
        mockMvc.perform(get("/projetos/{id}", id).with(logado("ana")))
                .andExpect(content().string(containsString(">Vencida</span>")))
                .andExpect(content().string(not(containsString(">Vigente</span>"))));
    }

    @Test
    @DisplayName("Prazo que termina em mês mais curto para no último dia do mês")
    void testGarantiaEmMesCurto() {
        Projeto projeto = new Projeto();
        projeto.setDataInicioGarantia(LocalDate.of(2026, 8, 31));
        projeto.setPrazoGarantiaMeses(6);

        assertThat(Garantia.doProjeto(projeto, HOJE).orElseThrow().fim()).isEqualTo(LocalDate.of(2027, 2, 28));
    }

    // ---------- Orçado x realizado (RF22) ----------

    @Test
    @DisplayName("O quadro bate com o que foi orçado e apontado")
    void testOrcadoRealizado() {
        Long id = encerrar();

        OrcadoRealizado quadro = orcadoRealizadoService.doProjeto(id);

        Apuracao apuracao = quadro.apuracao();
        assertThat(apuracao.valorTotal()).isEqualByComparingTo("2488.00");
        assertThat(apuracao.custoOrcado()).isEqualByComparingTo("1554.00");
        // 17 x 44,80 + 400 x 0,52 + 8 x 45,00
        assertThat(quadro.custoMateriais()).isEqualByComparingTo("969.60");
        assertThat(quadro.custoHoras()).isEqualByComparingTo("360.00");
        assertThat(apuracao.custoRealizado()).isEqualByComparingTo("1329.60");
        assertThat(apuracao.getMargemPrevista()).isEqualByComparingTo("934.00");
        assertThat(apuracao.getMargemPrevistaPercentual()).isEqualByComparingTo("37.54");
        assertThat(apuracao.getMargemReal()).isEqualByComparingTo("1158.40");
        assertThat(apuracao.getMargemRealPercentual()).isEqualByComparingTo("46.56");
        assertThat(quadro.horasPrevistas()).isEqualByComparingTo("10");
        assertThat(quadro.horasApontadas()).isEqualByComparingTo("8");
        assertThat(quadro.getDiferencaHoras()).isEqualByComparingTo("-2");
        OrcadoRealizado.Material tubo = quadro.materiais().stream()
                .filter(m -> m.codigo().equals("TB-PVC-050")).findFirst().orElseThrow();
        assertThat(tubo.previsto()).isEqualByComparingTo("20");
        assertThat(tubo.apontado()).isEqualByComparingTo("17");
        assertThat(tubo.getDiferenca()).isEqualByComparingTo("-3");
        assertThat(tubo.getCustoApontado()).isEqualByComparingTo("761.60");
    }

    @Test
    @DisplayName("A lista da diretoria dá os mesmos valores do quadro do projeto")
    void testListaIgualAoQuadro() {
        Long id = encerrar();

        List<OrcadoRealizadoLinha> linhas = orcadoRealizadoService.encerrados(null, null);

        assertThat(linhas).hasSize(1);
        assertThat(linhas.get(0).projetoId()).isEqualTo(id);
        assertThat(linhas.get(0).produtor()).isEqualTo("João Batista Ferreira");
        assertThat(linhas.get(0).apuracao().valorTotal()).isEqualByComparingTo("2488.00");
        assertThat(linhas.get(0).apuracao().custoOrcado()).isEqualByComparingTo("1554.00");
        assertThat(linhas.get(0).apuracao().custoRealizado()).isEqualByComparingTo("1329.60");
    }

    @Test
    @DisplayName("O realizado usa o custo unitário atual, e a tela avisa")
    void testCustoAtualDoMaterial() throws Exception {
        Long id = encerrar();
        materialService.atualizar(tuboId, "TB-PVC-050", "Tubo PVC 50 mm", "barra", new BigDecimal("62.90"),
                new BigDecimal("50.00"));
        em.flush();
        em.clear();

        // 17 x 50,00 + 400 x 0,52 + 8 x 45,00
        assertThat(orcadoRealizadoService.doProjeto(id).apuracao().custoRealizado()).isEqualByComparingTo("1418.00");
        mockMvc.perform(get("/projetos/{id}", id).with(logado("ana")))
                .andExpect(content().string(containsString("não guarda o custo do material na data do consumo")));
        mockMvc.perform(get("/diretoria/orcado-realizado").with(logado("diana")))
                .andExpect(content().string(containsString("não guarda o custo do material na data do consumo")));
    }

    @Test
    @DisplayName("Com desconto de 100% o valor é zero e o percentual da margem fica vazio")
    void testValorTotalZero() {
        Apuracao apuracao = Apuracao.de(new BigDecimal("0.00"), new BigDecimal("100.00"), new BigDecimal("30"),
                new BigDecimal("20"));

        assertThat(apuracao.getMargemReal()).isEqualByComparingTo("-50.00");
        assertThat(apuracao.getMargemRealPercentual()).isNull();
        assertThat(apuracao.getMargemPrevistaPercentual()).isNull();
    }

    @Test
    @DisplayName("O filtro por período inclui as duas datas e recusa período invertido")
    void testFiltroPorPeriodo() throws Exception {
        encerrar();

        assertThat(orcadoRealizadoService.encerrados(HOJE, HOJE)).hasSize(1);
        assertThat(orcadoRealizadoService.encerrados(HOJE.plusDays(1), null)).isEmpty();
        assertThat(orcadoRealizadoService.encerrados(null, HOJE.minusDays(1))).isEmpty();

        mockMvc.perform(get("/diretoria/orcado-realizado").param("de", "2026-11-16").param("ate", "2026-11-16")
                .with(logado("diana")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("R$ 1.329,60")))
                .andExpect(content().string(containsString("46,56%")));
        mockMvc.perform(get("/diretoria/orcado-realizado").param("de", "2026-11-17").param("ate", "2026-11-16")
                .with(logado("diana")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A data final não pode ser anterior à inicial.")))
                .andExpect(content().string(not(containsString("Exportar CSV"))));
        mockMvc.perform(get("/diretoria/orcado-realizado/exportar").param("de", "2026-11-17").param("ate", "2026-11-16")
                .with(logado("diana")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/diretoria/orcado-realizado").with(logado("ana")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("O financeiro também consulta e exporta o orçado x realizado; o vendedor não")
    void testFinanceiroNoOrcadoRealizado() throws Exception {
        encerrar();

        mockMvc.perform(get("/").with(logado("fabio")))
                .andExpect(content().string(containsString("Orçado x realizado</a>")));
        mockMvc.perform(get("/diretoria/orcado-realizado").with(logado("fabio")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("R$ 1.329,60")))
                .andExpect(content().string(containsString("46,56%")));
        mockMvc.perform(get("/diretoria/orcado-realizado/exportar").with(logado("fabio")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")));
        mockMvc.perform(get("/diretoria/aprovacoes").with(logado("fabio")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/diretoria/orcado-realizado/exportar").with(logado("ana")))
                .andExpect(status().isForbidden());
    }

    // ---------- CSV (RF22) ----------

    @Test
    @DisplayName("O CSV sai em UTF-8 com BOM, com acentos, ponto e vírgula e vírgula decimal")
    void testCsv() throws Exception {
        encerrar();

        byte[] arquivo = mockMvc.perform(get("/diretoria/orcado-realizado/exportar").with(logado("diana")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("orcado-realizado.csv")))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(arquivo).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        String texto = new String(arquivo, 3, arquivo.length - 3, StandardCharsets.UTF_8);
        String[] linhas = texto.split("\r\n");
        assertThat(linhas).hasSize(2);
        assertThat(linhas[0]).startsWith("Projeto;Produtor;Liberação do faturamento;Valor total (R$);Custo orçado (R$)");
        assertThat(linhas[1]).contains(";João Batista Ferreira;16/11/2026;2488,00;1554,00;1329,60;934,00;37,54;1158,40;46,56");
    }

    @Test
    @DisplayName("No CSV, texto com separador vai entre aspas e texto com cara de fórmula não executa")
    void testCsvComTextoPerigoso() {
        Apuracao apuracao = Apuracao.de(new BigDecimal("100.00"), new BigDecimal("150.00"), new BigDecimal("200"),
                BigDecimal.ZERO);
        byte[] arquivo = OrcadoRealizadoCsv.gerar(List.of(
                new OrcadoRealizadoLinha(1L, "PRJ-2026-0001", "=HYPERLINK(\"http://x\")", HOJE, apuracao),
                new OrcadoRealizadoLinha(2L, "PRJ-2026-0002", "Silva; Souza", HOJE, apuracao)));

        String texto = new String(arquivo, StandardCharsets.UTF_8);
        assertThat(texto).contains(";\"'=HYPERLINK(\"\"http://x\"\")\";");
        assertThat(texto).contains(";\"Silva; Souza\";");
        // Número negativo continua número, sem o apóstrofo
        assertThat(texto).contains(";-100,00;-100,00");
    }

    // ---------- Linha do tempo (RF21) ----------

    @Test
    @DisplayName("A linha do tempo mostra o ciclo inteiro, em ordem, com usuário e data")
    void testLinhaDoTempoDoCicloInteiro() throws Exception {
        Long id = encerrar();

        List<EventoProjeto> eventos = linhaDoTempoService.doProjeto(id);

        assertThat(eventos).isSortedAccordingTo(java.util.Comparator.comparing(EventoProjeto::dataHora));
        assertThat(eventos).extracting(EventoProjeto::titulo).filteredOn(java.util.Objects::nonNull)
                .containsSubsequence("Levantamento enviado", "Proposta 1 gerada", "Material separado", "OS 1 emitida",
                        "Horas apontadas", "Material apontado", "Medição dentro do parâmetro", "Aceite colhido",
                        "Sobra devolvida ao estoque");
        assertThat(eventos).extracting(EventoProjeto::situacaoNova).filteredOn(java.util.Objects::nonNull)
                .containsExactly(SituacaoProjeto.EM_LEVANTAMENTO, SituacaoProjeto.ORCANDO,
                        SituacaoProjeto.AGUARDANDO_RESPOSTA, SituacaoProjeto.PREPARANDO_MATERIAL,
                        SituacaoProjeto.PROGRAMANDO_EXECUCAO, SituacaoProjeto.EXECUTANDO_INSTALACAO,
                        SituacaoProjeto.VALIDANDO_MEDICAO, SituacaoProjeto.COLHENDO_ACEITE,
                        SituacaoProjeto.FINALIZANDO_EXECUCAO, SituacaoProjeto.ENCERRADO);
        assertThat(eventos).allSatisfy(evento -> assertThat(evento.usuario()).isNotBlank());
        assertThat(usuarioDe(eventos, "Levantamento enviado")).isEqualTo("Pedro Projetista");
        assertThat(usuarioDe(eventos, "Proposta 1 gerada")).isEqualTo("Ana Vendedora");
        assertThat(usuarioDe(eventos, "Material separado")).isEqualTo("Almir Almoxarife");
        assertThat(usuarioDe(eventos, "OS 1 emitida")).isEqualTo("Pedro Projetista");
        assertThat(usuarioDe(eventos, "Aceite colhido")).isEqualTo("Tito Técnico");
        assertThat(usuarioDe(eventos, "Sobra devolvida ao estoque")).isEqualTo("Almir Almoxarife");

        mockMvc.perform(get("/projetos/{id}", id).with(logado("ana")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Projeto aberto em")))
                .andExpect(content().string(containsString("Sobra devolvida ao estoque")))
                .andExpect(content().string(containsString("3 barra de TB-PVC-050")));
    }

    @Test
    @DisplayName("A pendência entra na linha do tempo com quem registrou")
    void testPendenciaNaLinhaDoTempo() {
        Long id = ateProgramar(BigDecimal.ZERO);
        logar(pedro);
        Long ordemId = ordemServicoService.emitir(id, HOJE, List.of(tito.getId())).getId();
        logar(tito);
        ordemServicoService.concluirInstalacao(ordemId);
        medicaoService.registrar(id, 30.0, 6.0, null, null, null);
        passarTempo();
        aceiteService.registrarPendencia(id, TipoPendencia.ADMINISTRATIVA, "Responsável viajando.",
                LocalDateTime.of(2026, 11, 20, 9, 0));
        em.flush();
        em.clear();

        EventoProjeto pendencia = linhaDoTempoService.doProjeto(id).stream()
                .filter(evento -> "Pendência administrativa registrada".equals(evento.titulo()))
                .findFirst().orElseThrow();
        assertThat(pendencia.detalhe()).isEqualTo("Responsável viajando.");
        assertThat(pendencia.usuario()).isEqualTo("Tito Técnico");
    }

    private static String usuarioDe(List<EventoProjeto> eventos, String titulo) {
        return eventos.stream().filter(evento -> titulo.equals(evento.titulo())).findFirst().orElseThrow().usuario();
    }

    // ---------- Painel inicial ----------

    @Test
    @DisplayName("Vendedor: projetos dele por situação, propostas perto do vencimento e pendências")
    void testPainelDoVendedor() throws Exception {
        abrirComLevantamentoCompleto();
        ateAprovarProposta(BigDecimal.ZERO);
        logar(ana);
        Long aguardando = abrirComLevantamentoCompleto();
        dimensionamentoService.registrar(aguardando, 30.0, 6.0, 2.0);
        listaTecnicaService.adicionar(aguardando, tuboId, new BigDecimal("1"));
        dimensionamentoService.concluirListaTecnica(aguardando);
        logar(ana);
        propostaService.enviar(propostaService.gerar(aguardando, BigDecimal.ONE, BigDecimal.ZERO).getId());
        em.flush();
        em.clear();

        PainelInicial.Vendedor painel = painelService.montar(Perfil.VENDEDOR, ana.getId()).vendedor();
        assertThat(painel.projetosPorSituacao()).extracting(PainelInicial.ProjetosNaSituacao::situacao)
                .containsExactly(SituacaoProjeto.EM_LEVANTAMENTO, SituacaoProjeto.AGUARDANDO_RESPOSTA,
                        SituacaoProjeto.PREPARANDO_MATERIAL);
        assertThat(painel.propostasPertoDoVencimento()).isZero();
        assertThat(painel.pendencias()).isZero();
        assertThat(painelService.montar(Perfil.VENDEDOR, pedro.getId()).vendedor().projetosPorSituacao()).isEmpty();

        // Validade de 15 dias a partir do envio: entra no alerta 3 dias antes do último dia
        relogio.definir(RelogioDeTesteConfig.INICIO.plusDays(13));
        assertThat(painelService.montar(Perfil.VENDEDOR, ana.getId()).vendedor().propostasPertoDoVencimento()).isEqualTo(1);
        assertThat(propostaService.contarAlertasDeVencimento(ana.getId()))
                .isEqualTo(propostaService.alertarVencimento(ana.getId()).size());
        relogio.definir(RelogioDeTesteConfig.INICIO.plusDays(30));
        assertThat(painelService.montar(Perfil.VENDEDOR, ana.getId()).vendedor().propostasPertoDoVencimento()).isZero();
    }


    @Test
    @DisplayName("Projetista: só conta o levantamento completo, pelo mesmo critério do G1")
    void testPainelDoProjetista() {
        assertThat(painelService.montar(Perfil.PROJETISTA, pedro.getId()).projetista().levantamentosCompletos()).isZero();
        Long completo = abrirComLevantamentoCompleto();
        logar(ana);
        Long incompleto = projetoService.abrir(fazenda.getProdutor().getId(), fazenda.getId()).getId();
        logar(pedro);
        levantamentoService.salvar(incompleto, new DadosLevantamento(LocalDateTime.of(2026, 11, 16, 8, 0), "Café", 12.5,
                TipoFonteAgua.POCO_ARTESIANO, 8.0, "Plana", -18.91, -48.26), List.of());
        em.flush();
        em.clear();

        assertThat(levantamentoService.camposFaltantes(completo)).isEmpty();
        assertThat(levantamentoService.camposFaltantes(incompleto)).containsExactly("pelo menos uma foto");
        assertThat(painelService.montar(Perfil.PROJETISTA, pedro.getId()).projetista().levantamentosCompletos()).isEqualTo(1);
    }

    @Test
    @DisplayName("Almoxarife, técnico e financeiro veem os números da própria fila")
    void testPaineisDasFilas() throws Exception {
        Long id = ateFinalizar(BigDecimal.ZERO);

        assertThat(painelService.montar(Perfil.ALMOXARIFE, almir.getId()).estoque().sobrasAReceber()).isEqualTo(1);
        assertThat(painelService.montar(Perfil.COMPRADOR, null).estoque().sobrasAReceber()).isEqualTo(1);
        PainelInicial.Financeiro financeiro = painelService.montar(Perfil.FINANCEIRO, fabio.getId()).financeiro();
        assertThat(financeiro.projetosParaLiberar()).isZero();
        assertThat(financeiro.aguardandoSobras()).isEqualTo(1);
        assertThat(painelService.montar(Perfil.TECNICO_CAMPO, tito.getId()).tecnico().ordensDoDia()).isZero();

        logar(almir);
        sobrasService.devolver(id, Map.of(listaTecnicaService.listar(id).stream()
                .filter(item -> item.getMaterial().getId().equals(tuboId)).findFirst().orElseThrow().getId(),
                new BigDecimal("3")));
        em.flush();
        em.clear();

        assertThat(painelService.montar(Perfil.FINANCEIRO, fabio.getId()).financeiro().projetosParaLiberar()).isEqualTo(1);
        assertThat(painelService.montar(Perfil.ALMOXARIFE, almir.getId()).estoque().sobrasAReceber()).isZero();
        mockMvc.perform(get("/").with(logado("fabio")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Projetos para liberar")))
                .andExpect(content().string(not(containsString("Separações pendentes"))));
    }

    @Test
    @DisplayName("Técnico: conta só as ordens abertas de hoje em que ele está escalado")
    void testPainelDoTecnico() throws Exception {
        Long id = ateProgramar(BigDecimal.ZERO);
        logar(pedro);
        ordemServicoService.emitir(id, HOJE.plusDays(1), List.of(tito.getId()));
        em.flush();
        em.clear();

        assertThat(painelService.montar(Perfil.TECNICO_CAMPO, tito.getId()).tecnico().ordensDoDia()).isZero();
        relogio.definir(RelogioDeTesteConfig.INICIO.plusDays(1));
        assertThat(painelService.montar(Perfil.TECNICO_CAMPO, tito.getId()).tecnico().ordensDoDia()).isEqualTo(1);
        assertThat(painelService.montar(Perfil.TECNICO_CAMPO, pedro.getId()).tecnico().ordensDoDia()).isZero();
        mockMvc.perform(get("/").with(logado("tito")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ordens de serviço para hoje")));
    }

    @Test
    @DisplayName("Diretoria: propostas aguardando e resumo igual à soma dos projetos encerrados")
    void testPainelDaDiretoria() throws Exception {
        encerrar();

        PainelInicial.Diretoria painel = painelService.montar(Perfil.DIRETORIA, null).diretoria();

        assertThat(painel.propostasAguardando()).isZero();
        assertThat(painel.orcadoRealizado().projetos()).isEqualTo(1);
        assertThat(painel.orcadoRealizado().apuracao().valorTotal()).isEqualByComparingTo("2488.00");
        assertThat(painel.orcadoRealizado().apuracao().custoOrcado()).isEqualByComparingTo("1554.00");
        assertThat(painel.orcadoRealizado().apuracao().custoRealizado()).isEqualByComparingTo("1329.60");
        mockMvc.perform(get("/").with(logado("diana")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Propostas aguardando a diretoria")))
                .andExpect(content().string(containsString("R$ 1.158,40 (46,56%)")));
    }

    @Test
    @DisplayName("O administrador vê os painéis das áreas, sem os pessoais")
    void testPainelDoAdministrador() {
        PainelInicial painel = painelService.montar(Perfil.ADMINISTRADOR, null);

        assertThat(painel.projetista()).isNotNull();
        assertThat(painel.estoque()).isNotNull();
        assertThat(painel.financeiro()).isNotNull();
        assertThat(painel.diretoria()).isNotNull();
        assertThat(painel.vendedor()).isNull();
        assertThat(painel.tecnico()).isNull();
    }
}
