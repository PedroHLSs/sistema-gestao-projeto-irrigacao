package br.edu.iftm.irrigacao.apresentacao.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import br.edu.iftm.irrigacao.config.RelogioDeTesteConfig;
import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;
import br.edu.iftm.irrigacao.dominio.comercial.Produtor;
import br.edu.iftm.irrigacao.dominio.comercial.ProdutorService;
import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;
import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.MaterialService;
import br.edu.iftm.irrigacao.dominio.projeto.DadosLevantamento;
import br.edu.iftm.irrigacao.dominio.projeto.FotoEnviada;
import br.edu.iftm.irrigacao.dominio.projeto.HistoricoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.LevantamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.TipoFonteAgua;
import br.edu.iftm.irrigacao.persistencia.HistoricoProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.ItemProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.RelogioAjustavel;
import jakarta.persistence.EntityManager;

/**
 * Dimensionamento e lista técnica (RF05) e a transição 2, EM_LEVANTAMENTO para ORCANDO, de ponta a
 * ponta, com o banco H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(RelogioDeTesteConfig.class)
class EngenhariaIntegrationTest {

    private static final String SENHA = "senha-de-teste-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RelogioAjustavel relogio;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private ProdutorService produtorService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private LevantamentoService levantamentoService;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private ItemProjetoRepository itemProjetoRepository;

    @Autowired
    private HistoricoProjetoRepository historicoRepository;

    @Autowired
    private EntityManager em;

    @Value("${irrigacao.upload.dir}")
    private String pastaDeUploads;

    private Usuario paulo;
    private Long projetoId;
    private Long tuboId;
    private Long gotejadorId;
    private String tela;

    @BeforeEach
    void prepararDados() {
        relogio.definir(RelogioDeTesteConfig.INICIO);
        Usuario ana = usuarioService.criar("Ana Vendedora", "ana", SENHA, Perfil.VENDEDOR);
        usuarioService.criar("Tiago Técnico", "tiago", SENHA, Perfil.TECNICO_CAMPO);
        paulo = usuarioService.criar("Paulo Projetista", "paulo", SENHA, Perfil.PROJETISTA);
        Produtor joao = produtorService.criar("João Batista Ferreira", "529.982.247-25", null, null);
        Propriedade fazenda = produtorService.criarPropriedade(joao.getId(), "Fazenda Boa Vista", "Uberlândia", null,
                null, null);
        projetoId = projetoRepository.save(projeto("PRJ-2026-0001", fazenda, ana)).getId();
        tuboId = materialService.criar("TB-PVC-050", "Tubo PVC 50 mm", "barra", new BigDecimal("62.90"),
                new BigDecimal("44.80")).getId();
        gotejadorId = materialService.criar("GT-AUT-2L", "Gotejador 2 L/h", "un", new BigDecimal("0.95"),
                new BigDecimal("0.52")).getId();
        tela = "/engenharia/projetos/" + projetoId + "/dimensionamento";
    }

    @AfterEach
    void apagarFotos() {
        FileSystemUtils.deleteRecursively(Path.of(pastaDeUploads, "levantamento", projetoId.toString()).toFile());
    }

    private static Projeto projeto(String codigo, Propriedade propriedade, Usuario responsavel) {
        Projeto projeto = new Projeto();
        projeto.setCodigo(codigo);
        projeto.setPropriedade(propriedade);
        projeto.setResponsavel(responsavel);
        projeto.setDataAbertura(LocalDate.of(2026, 11, 16));
        return projeto;
    }

    private RequestPostProcessor logado(String login) {
        return user(userDetailsService.loadUserByUsername(login));
    }

    private void completarLevantamento() {
        levantamentoService.salvar(projetoId, new DadosLevantamento(LocalDateTime.of(2026, 11, 16, 8, 40), "Café", 12.5,
                TipoFonteAgua.POCO_ARTESIANO, 8.0, "Plana", -18.9113, -48.2622),
                List.of(new FotoEnviada("fonte.jpg", FotosDeTeste.jpeg())));
    }

    private MockHttpServletRequestBuilder comoPaulo(MockHttpServletRequestBuilder requisicao) {
        return requisicao.with(csrf()).with(logado("paulo")).header(HttpHeaders.REFERER, "http://localhost" + tela);
    }

    private MockHttpServletRequestBuilder dimensionar(String pressao, String vazao, String tolerancia) {
        return comoPaulo(post(tela))
                .param("pressaoProjetoMca", pressao)
                .param("vazaoProjetoM3h", vazao)
                .param("toleranciaPercentual", tolerancia);
    }

    private MockHttpServletRequestBuilder adicionar(Long materialId, String quantidade) {
        return comoPaulo(post("/engenharia/projetos/" + projetoId + "/itens"))
                .param("materialId", materialId.toString())
                .param("materialRotulo", "escolhido na busca")
                .param("quantidadePrevista", quantidade);
    }

    private MockHttpServletRequestBuilder concluir() {
        return comoPaulo(post("/engenharia/projetos/" + projetoId + "/concluir"));
    }

    private Projeto projetoRelido() {
        em.flush();
        em.clear();
        return projetoRepository.findById(projetoId).orElseThrow();
    }

    private List<ItemProjeto> itens() {
        em.flush();
        em.clear();
        return itemProjetoRepository.listarPorProjetoComMaterial(projetoId);
    }

    // ---------- Liberação ----------

    @Test
    @DisplayName("Com o levantamento incompleto a tela mostra o que falta e recusa dimensionamento, itens e conclusão")
    void testBloqueadaComLevantamentoIncompleto() throws Exception {
        mockMvc.perform(get(tela).with(logado("paulo")))
                .andExpect(status().isOk())
                .andExpect(view().name("engenharia/dimensionamento"))
                .andExpect(content().string(containsString("O dimensionamento é liberado quando o levantamento estiver completo.")))
                .andExpect(content().string(containsString("<li>pelo menos uma foto</li>")))
                .andExpect(content().string(not(containsString("name=\"pressaoProjetoMca\""))))
                .andExpect(content().string(not(containsString("/engenharia/projetos/" + projetoId + "/concluir"))));

        mockMvc.perform(dimensionar("30", "6.5", "2"))
                .andExpect(redirectedUrl(tela))
                .andExpect(flash().attribute("erro", "O levantamento do projeto PRJ-2026-0001 ainda está incompleto. "
                        + "Falta: data da visita, cultura, área irrigada, fonte de água, vazão da fonte, topografia, "
                        + "latitude, longitude, pelo menos uma foto."));
        mockMvc.perform(adicionar(tuboId, "10")).andExpect(flash().attributeExists("erro"));
        mockMvc.perform(concluir())
                .andExpect(flash().attribute("erro", containsString("completar o levantamento")));

        Projeto projeto = projetoRelido();
        assertThat(projeto.getPressaoProjetoMca()).isNull();
        assertThat(projeto.getSituacao()).isEqualTo(SituacaoProjeto.EM_LEVANTAMENTO);
        assertThat(itens()).isEmpty();
    }

    // ---------- Dimensionamento ----------

    @Test
    @DisplayName("Liberada: diz que o sistema não calcula, vem com tolerância 2 e grava o que o projetista informa")
    void testDimensionamento() throws Exception {
        completarLevantamento();

        mockMvc.perform(get(tela).with(logado("paulo")))
                .andExpect(content().string(containsString("O sistema não calcula nada")))
                .andExpect(content().string(containsString("name=\"toleranciaPercentual\" value=\"2.0\"")))
                .andExpect(content().string(containsString("data-busca-material=\"/estoque/materiais/busca\"")));

        mockMvc.perform(dimensionar("32.5", "6.4", "3"))
                .andExpect(redirectedUrl(tela))
                .andExpect(flash().attribute("sucesso", "Dimensionamento gravado."));

        Projeto projeto = projetoRelido();
        assertThat(projeto.getPressaoProjetoMca()).isEqualTo(32.5);
        assertThat(projeto.getVazaoProjetoM3h()).isEqualTo(6.4);
        assertThat(projeto.getToleranciaPercentual()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("Pressão zero, vazão vazia e tolerância acima de 100 aparecem ao lado dos campos")
    void testDimensionamentoInvalido() throws Exception {
        completarLevantamento();

        mockMvc.perform(dimensionar("0", "", "120"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("dimensionamento", "pressaoProjetoMca", "vazaoProjetoM3h",
                        "toleranciaPercentual"))
                .andExpect(content().string(containsString("A pressão deve ser maior que zero.")))
                .andExpect(content().string(containsString("Informe a vazão de projeto.")));

        assertThat(projetoRelido().getPressaoProjetoMca()).isNull();
    }

    // ---------- Lista técnica ----------

    @Test
    @DisplayName("Adiciona, altera e remove itens; o preço do item fica vazio e o total usa o preço de venda atual")
    void testListaTecnica() throws Exception {
        completarLevantamento();

        mockMvc.perform(adicionar(tuboId, "10"))
                .andExpect(redirectedUrl(tela))
                .andExpect(flash().attribute("sucesso", "TB-PVC-050 adicionado à lista técnica."));
        mockMvc.perform(adicionar(gotejadorId, "250.5")).andExpect(redirectedUrl(tela));
        mockMvc.perform(adicionar(tuboId, "3"))
                .andExpect(flash().attribute("erro", "TB-PVC-050 já está na lista técnica. Altere a quantidade na linha dele."));

        List<ItemProjeto> itens = itens();
        assertThat(itens).extracting(i -> i.getMaterial().getCodigo()).containsExactly("GT-AUT-2L", "TB-PVC-050");
        assertThat(itens).allSatisfy(i -> assertThat(i.getPrecoUnitario()).isNull());
        Long tuboItem = itens.get(1).getId();
        Long gotejadorItem = itens.get(0).getId();

        mockMvc.perform(comoPaulo(post("/engenharia/projetos/" + projetoId + "/itens/" + tuboItem)).param("quantidade", "12.5"))
                .andExpect(flash().attribute("sucesso", "Quantidade de TB-PVC-050 alterada."));

        // 12,5 x 62,90 + 250,5 x 0,95 = 786,25 + 237,975 = 1.024,225
        mockMvc.perform(get(tela).with(logado("paulo")))
                .andExpect(content().string(containsString("R$ 786,25")))
                .andExpect(content().string(containsString("R$ 1.024,23")));

        mockMvc.perform(comoPaulo(post("/engenharia/projetos/" + projetoId + "/itens/" + gotejadorItem + "/remover")))
                .andExpect(flash().attribute("sucesso", "Item removido da lista técnica."));
        assertThat(itens()).singleElement().satisfies(i -> {
            assertThat(i.getMaterial().getCodigo()).isEqualTo("TB-PVC-050");
            assertThat(i.getQuantidadePrevista()).isEqualByComparingTo("12.5");
        });
    }

    @Test
    @DisplayName("Quantidade inválida e item de outro projeto são recusados")
    void testItensInvalidos() throws Exception {
        completarLevantamento();

        mockMvc.perform(adicionar(tuboId, "0"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("novoItem", "quantidadePrevista"));
        mockMvc.perform(comoPaulo(post("/engenharia/projetos/" + projetoId + "/itens"))
                        .param("quantidadePrevista", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("novoItem", "materialId"))
                .andExpect(content().string(containsString("Escolha o material na busca.")));
        mockMvc.perform(comoPaulo(post("/engenharia/projetos/" + projetoId + "/itens/99999")).param("quantidade", "2"))
                .andExpect(status().isNotFound());

        assertThat(itens()).isEmpty();
    }

    // ---------- Conclusão ----------

    @Test
    @DisplayName("Concluir sem itens e sem parâmetros é recusado com tudo o que falta")
    void testConcluirIncompleto() throws Exception {
        completarLevantamento();

        mockMvc.perform(concluir())
                .andExpect(redirectedUrl(tela))
                .andExpect(flash().attribute("erro", "Para concluir a lista técnica falta informar a pressão de projeto; "
                        + "informar a vazão de projeto; lançar pelo menos um item na lista técnica."));

        mockMvc.perform(dimensionar("30", "6", "2"));
        mockMvc.perform(concluir())
                .andExpect(flash().attribute("erro",
                        "Para concluir a lista técnica falta lançar pelo menos um item na lista técnica."));

        assertThat(projetoRelido().getSituacao()).isEqualTo(SituacaoProjeto.EM_LEVANTAMENTO);
    }

    @Test
    @DisplayName("Concluir com itens mas sem parâmetros de projeto é recusado")
    void testConcluirSemParametros() throws Exception {
        completarLevantamento();
        mockMvc.perform(adicionar(tuboId, "10"));

        mockMvc.perform(concluir())
                .andExpect(flash().attribute("erro", "Para concluir a lista técnica falta informar a pressão de projeto; "
                        + "informar a vazão de projeto."));

        assertThat(projetoRelido().getSituacao()).isEqualTo(SituacaoProjeto.EM_LEVANTAMENTO);
    }

    @Test
    @DisplayName("Concluída, o projeto vai para ORCANDO pela transição 2 e nada mais muda na engenharia nem no campo")
    void testConcluirEBloquearDepois() throws Exception {
        completarLevantamento();
        mockMvc.perform(dimensionar("30", "6", "2"));
        mockMvc.perform(adicionar(tuboId, "10"));
        mockMvc.perform(adicionar(gotejadorId, "200"));
        relogio.definir(LocalDateTime.of(2026, 11, 17, 16, 20));

        mockMvc.perform(concluir())
                .andExpect(redirectedUrl("/projetos/" + projetoId))
                .andExpect(flash().attribute("sucesso",
                        "Lista técnica concluída. O projeto PRJ-2026-0001 passou para \"Orçando\"."));

        assertThat(projetoRelido().getSituacao()).isEqualTo(SituacaoProjeto.ORCANDO);
        List<HistoricoProjeto> historico = historicoRepository.listarPorProjeto(projetoId);
        HistoricoProjeto transicao = historico.get(historico.size() - 1);
        assertThat(transicao.getSituacaoAnterior()).isEqualTo(SituacaoProjeto.EM_LEVANTAMENTO);
        assertThat(transicao.getSituacaoNova()).isEqualTo(SituacaoProjeto.ORCANDO);
        assertThat(transicao.getObservacao()).isEqualTo("Lista técnica concluída com 2 itens.");
        assertThat(transicao.getUsuario().getId()).isEqualTo(paulo.getId());
        assertThat(transicao.getDataHora()).isEqualTo(LocalDateTime.of(2026, 11, 17, 16, 20));

        String foraDaSituacao = "O dimensionamento e a lista técnica só mudam com o projeto em \"Em levantamento\". "
                + "O projeto PRJ-2026-0001 está em \"Orçando\".";
        mockMvc.perform(dimensionar("40", "8", "2")).andExpect(flash().attribute("erro", foraDaSituacao));
        mockMvc.perform(adicionar(gotejadorId, "1")).andExpect(flash().attribute("erro", foraDaSituacao));
        mockMvc.perform(concluir()).andExpect(flash().attribute("erro", foraDaSituacao));
        mockMvc.perform(multipart("/campo/projetos/" + projetoId + "/levantamento").with(csrf()).with(logado("tiago"))
                        .header(HttpHeaders.REFERER, "http://localhost/projetos/" + projetoId)
                        .param("cultura", "Soja"))
                .andExpect(flash().attribute("erro", containsString("só pode ser alterado com o projeto em")));
        mockMvc.perform(get(tela).with(logado("paulo")))
                .andExpect(content().string(containsString("não mudam mais")))
                .andExpect(content().string(not(containsString("name=\"pressaoProjetoMca\""))))
                .andExpect(content().string(containsString("30,00 mca")));

        Projeto projeto = projetoRelido();
        assertThat(projeto.getPressaoProjetoMca()).isEqualTo(30.0);
        assertThat(itens()).hasSize(2);
    }

    @Test
    @DisplayName("Só o projetista dimensiona; técnico de campo e vendedor recebem acesso negado")
    void testPermissoes() throws Exception {
        completarLevantamento();

        for (String login : List.of("tiago", "ana")) {
            mockMvc.perform(get(tela).with(logado(login))).andExpect(status().isForbidden());
            mockMvc.perform(post("/engenharia/projetos/" + projetoId + "/concluir").with(csrf()).with(logado(login)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/projetos/" + projetoId).with(logado("tiago")))
                .andExpect(content().string(not(containsString(tela))));
        mockMvc.perform(get("/projetos/" + projetoId).with(logado("paulo")))
                .andExpect(content().string(containsString(tela)));
    }

    // ---------- Critério de aceite ----------

    @Test
    @DisplayName("Aceite: levantamento incompleto mostra o que falta; completo, dimensionado e com lista, chega em ORCANDO")
    void testFluxoCompletoDaEtapa() throws Exception {
        String campo = "/campo/projetos/" + projetoId + "/levantamento";
        String pagina = "/projetos/" + projetoId;

        // 1. Levantamento incompleto, pelo técnico
        mockMvc.perform(multipart(campo).with(csrf()).with(logado("tiago"))
                        .param("acao", "enviar")
                        .param("dataVisita", "2026-11-16T08:40")
                        .param("cultura", "Café"))
                .andExpect(flash().attribute("erro", containsString("Levantamento salvo, mas incompleto.")));
        mockMvc.perform(get(pagina).with(logado("ana")))
                .andExpect(content().string(containsString("Levantamento incompleto. Falta:")))
                .andExpect(content().string(containsString("<li>área irrigada</li>")));

        // 2. Completa o levantamento, com foto
        mockMvc.perform(multipart(campo).file(FotosDeTeste.arquivo("fonte.jpg", "image/jpeg", FotosDeTeste.jpeg()))
                        .with(csrf()).with(logado("tiago"))
                        .param("acao", "enviar")
                        .param("dataVisita", "2026-11-16T08:40")
                        .param("cultura", "Café")
                        .param("areaIrrigadaHa", "12.5")
                        .param("fonteAgua", "POCO_ARTESIANO")
                        .param("vazaoFonte", "8")
                        .param("topografia", "Plana")
                        .param("latitude", "-18.9113")
                        .param("longitude", "-48.2622"))
                .andExpect(redirectedUrl(pagina));

        // 3. Dimensiona e monta a lista, pelo projetista
        mockMvc.perform(dimensionar("30", "6", "2")).andExpect(flash().attributeExists("sucesso"));
        mockMvc.perform(adicionar(tuboId, "20")).andExpect(flash().attributeExists("sucesso"));
        mockMvc.perform(adicionar(gotejadorId, "400")).andExpect(flash().attributeExists("sucesso"));
        mockMvc.perform(concluir()).andExpect(redirectedUrl(pagina));

        // 4. O projeto está em ORCANDO, com a lista em modo leitura, mesmo para o projetista
        assertThat(projetoRelido().getSituacao()).isEqualTo(SituacaoProjeto.ORCANDO);
        mockMvc.perform(get(pagina).with(logado("paulo")))
                .andExpect(content().string(containsString("situacao-ORCANDO")))
                .andExpect(content().string(containsString("TB-PVC-050")))
                .andExpect(content().string(containsString("400 un")))
                // 20 x 62,90 + 400 x 0,95 = 1.258,00 + 380,00
                .andExpect(content().string(containsString("R$ 1.638,00")))
                .andExpect(content().string(containsString("30,00 mca")))
                .andExpect(content().string(containsString("Lista técnica concluída com 2 itens.")))
                .andExpect(content().string(not(containsString("Preencher levantamento"))))
                .andExpect(content().string(not(containsString("Dimensionar e montar a lista"))));
    }
}
