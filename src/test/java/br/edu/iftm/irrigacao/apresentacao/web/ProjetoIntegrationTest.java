package br.edu.iftm.irrigacao.apresentacao.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.config.RelogioDeTesteConfig;
import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;
import br.edu.iftm.irrigacao.dominio.comercial.Produtor;
import br.edu.iftm.irrigacao.dominio.comercial.ProdutorService;
import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;
import br.edu.iftm.irrigacao.dominio.projeto.HistoricoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.TransicaoInvalidaException;
import br.edu.iftm.irrigacao.persistencia.HistoricoProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.LevantamentoRepository;
import br.edu.iftm.irrigacao.persistencia.ProdutorRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.PropriedadeRepository;
import br.edu.iftm.irrigacao.persistencia.RelogioAjustavel;
import jakarta.persistence.EntityManager;

/**
 * Abertura do projeto (RF01), agendamento da visita (RF02), situação e histórico (RF21) de ponta a
 * ponta, com o banco H2 e o relógio parado na segunda-feira 16/11/2026 às 09:00.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(RelogioDeTesteConfig.class)
class ProjetoIntegrationTest {

    private static final String SENHA = "senha-da-ana-1";

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
    private ProjetoService projetoService;

    @Autowired
    private ProdutorRepository produtorRepository;

    @Autowired
    private PropriedadeRepository propriedadeRepository;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private HistoricoProjetoRepository historicoRepository;

    @Autowired
    private LevantamentoRepository levantamentoRepository;

    @Autowired
    private EntityManager em;

    private Usuario ana;
    private Produtor joao;
    private Propriedade fazenda;

    @BeforeEach
    void prepararDados() {
        relogio.definir(RelogioDeTesteConfig.INICIO);
        ana = usuarioService.criar("Ana Vendedora", "ana", SENHA, Perfil.VENDEDOR);
        usuarioService.criar("Tiago Técnico", "tiago", SENHA, Perfil.TECNICO_CAMPO);
        joao = produtorService.criar("João Batista Ferreira", "529.982.247-25", null, null);
        fazenda = produtorService.criarPropriedade(joao.getId(), "Fazenda Boa Vista", "Uberlândia", null, null, 42.5);
    }

    private RequestPostProcessor logado(String login) {
        return user(userDetailsService.loadUserByUsername(login));
    }

    private RequestPostProcessor anaLogada() {
        return logado("ana");
    }

    /** Abre o projeto pela tela, como a vendedora, e devolve o id. */
    private Long abrirProjeto(Produtor produtor, Propriedade propriedade) throws Exception {
        String destino = mockMvc.perform(post("/comercial/projetos").with(csrf()).with(anaLogada())
                        .param("produtorId", produtor.getId().toString())
                        .param("propriedadeId", propriedade.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(destino).startsWith("/projetos/");
        return Long.valueOf(destino.substring("/projetos/".length()));
    }

    private Projeto relido(Long id) {
        em.flush();
        em.clear();
        return projetoRepository.findById(id).orElseThrow();
    }

    // ---------- Critério de aceite da etapa ----------

    @Test
    @DisplayName("Aceite: cria produtor, propriedade e projeto, agenda a visita e vê o projeto em levantamento com a abertura no histórico")
    void testFluxoCompletoDaEtapa() throws Exception {
        // Produtor e propriedade pela tela
        mockMvc.perform(post("/comercial/produtores").with(csrf()).with(anaLogada())
                        .param("nome", "Maria Aparecida Souza")
                        .param("cpfCnpj", "111.444.777-35"))
                .andExpect(status().is3xxRedirection());
        Produtor maria = produtorService.pesquisar("111.444.777-35").get(0);
        mockMvc.perform(post("/comercial/produtores/" + maria.getId() + "/propriedades").with(csrf()).with(anaLogada())
                        .param("nome", "Sítio Esperança")
                        .param("municipio", "Araguari"))
                .andExpect(redirectedUrl("/comercial/produtores/" + maria.getId() + "/editar"));
        Propriedade sitio = propriedadeRepository.findByProdutorIdOrderByNome(maria.getId()).get(0);

        // Abertura
        mockMvc.perform(get("/comercial/projetos/novo").param("propriedadeId", sitio.getId().toString()).with(anaLogada()))
                .andExpect(status().isOk())
                .andExpect(TrilhaDeNavegacao.daSecao("/projetos", "Projetos", "Abrir projeto"))
                .andExpect(view().name("comercial/abrirProjeto"))
                .andExpect(model().attribute("abertura", hasProperty("produtorId", is(maria.getId()))))
                .andExpect(model().attribute("abertura", hasProperty("propriedadeId", is(sitio.getId()))));
        mockMvc.perform(post("/comercial/projetos").with(csrf()).with(anaLogada())
                        .param("produtorId", maria.getId().toString())
                        .param("propriedadeId", sitio.getId().toString()))
                .andExpect(flash().attribute("sucesso",
                        "Projeto PRJ-2026-0001 aberto. O próximo passo é agendar a visita técnica."));
        Long id = projetoService.pesquisar(null, "PRJ-2026-0001").get(0).getId();

        // Agendamento da visita para quarta-feira
        mockMvc.perform(get("/projetos/" + id).with(anaLogada()))
                .andExpect(content().string(containsString("A visita ainda não foi agendada.")))
                .andExpect(content().string(containsString("/comercial/projetos/" + id + "/visita")));
        mockMvc.perform(post("/comercial/projetos/" + id + "/visita").with(csrf()).with(anaLogada())
                        .param("dataAgendada", "2026-11-18T08:30"))
                .andExpect(redirectedUrl("/projetos/" + id))
                .andExpect(flash().attribute("sucesso",
                        "Visita do projeto PRJ-2026-0001 agendada para 18/11/2026 às 08:30."));

        // Página central do projeto
        mockMvc.perform(get("/projetos/" + id).with(anaLogada()))
                .andExpect(status().isOk())
                .andExpect(view().name("projetos/detalheProjeto"))
                .andExpect(content().string(containsString("PRJ-2026-0001")))
                .andExpect(content().string(containsString("Maria Aparecida Souza")))
                .andExpect(content().string(containsString("111.444.777-35")))
                .andExpect(content().string(containsString("Sítio Esperança")))
                .andExpect(content().string(containsString("Ana Vendedora")))
                .andExpect(content().string(containsString("situacao-EM_LEVANTAMENTO")))
                .andExpect(content().string(containsString("Projeto aberto em")))
                .andExpect(content().string(containsString("16/11/2026 09:00")))
                .andExpect(content().string(containsString("Quarta-feira, 18/11/2026 às 08:30")))
                .andExpect(content().string(containsString("Reagendar visita")))
                .andExpect(content().string(containsString("Lista técnica")))
                .andExpect(content().string(containsString("Encerramento")));

        // Abre na aba da fase atual, e só nela: sem levantamento, a aba Levantamento
        String detalhe = mockMvc.perform(get("/projetos/" + id).with(anaLogada()))
                .andExpect(model().attribute("abaAtual", AbaProjeto.LEVANTAMENTO))
                .andExpect(TrilhaDeNavegacao.doProprioProjeto())
                .andReturn().getResponse().getContentAsString();
        assertThat(detalhe)
                .containsPattern("class=\"nav-link active\"\\s+id=\"aba-levantamento-tab\" data-aba=\"levantamento\"")
                .containsPattern("class=\"tab-pane fade show active\"\\s+id=\"aba-levantamento\"")
                .containsPattern("data-aba=\"lista\"[^>]*aria-selected=\"false\"");
        assertThat(detalhe.split("nav-link active", -1)).hasSize(2);
        assertThat(detalhe.split("show active", -1)).hasSize(2);
        assertThat(detalhe).containsOnlyOnce("(fase atual)").doesNotContain("(concluída)");

        // Próximo passo: a vendedora vê que está com o campo, sem o botão; o técnico vê o botão
        assertThat(detalhe)
                .contains("Próximo passo:", "fazer a visita e preencher o levantamento", "Aguardando a equipe de campo.",
                        "Visita agendada para quarta-feira, 18/11/2026 às 08:30.")
                .doesNotContain("proximo-passo-comigo");
        mockMvc.perform(get("/projetos/" + id).with(logado("tiago")))
                .andExpect(content().string(containsString("proximo-passo-comigo")))
                .andExpect(content().string(containsString("Com a equipe de campo.")))
                .andExpect(content().string(containsString(">Preencher levantamento</a>")));

        // O que ficou gravado
        Projeto projeto = relido(id);
        assertThat(projeto.getCodigo()).isEqualTo("PRJ-2026-0001");
        assertThat(projeto.getSituacao()).isEqualTo(SituacaoProjeto.EM_LEVANTAMENTO);
        assertThat(projeto.getDataAbertura()).isEqualTo(LocalDate.of(2026, 11, 16));
        assertThat(projeto.getResponsavel().getId()).isEqualTo(ana.getId());
        assertThat(projeto.getPropriedade().getId()).isEqualTo(sitio.getId());

        List<HistoricoProjeto> historico = historicoRepository.listarPorProjeto(id);
        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).getSituacaoAnterior()).isNull();
        assertThat(historico.get(0).getSituacaoNova()).isEqualTo(SituacaoProjeto.EM_LEVANTAMENTO);
        assertThat(historico.get(0).getUsuario().getId()).isEqualTo(ana.getId());
        assertThat(historico.get(0).getDataHora()).isEqualTo(RelogioDeTesteConfig.INICIO);

        Levantamento levantamento = levantamentoRepository.findByProjetoId(id).orElseThrow();
        assertThat(levantamento.getDataAgendada()).isEqualTo(LocalDateTime.of(2026, 11, 18, 8, 30));
        assertThat(levantamento.getDataVisita()).isNull();
        assertThat(levantamento.getCultura()).isNull();

        // A visita aparece na agenda da semana, na quarta-feira
        mockMvc.perform(get("/projetos/agenda").with(logado("tiago")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Quarta-feira, 18/11")))
                .andExpect(content().string(containsString("PRJ-2026-0001")))
                .andExpect(content().string(containsString("08:30")))
                .andExpect(content().string(containsString("Sítio Esperança, Araguari")));
    }

    // ---------- Abertura ----------

    @Test
    @DisplayName("Código PRJ-AAAA-NNNN sequencial no ano, que recomeça em 0001 na virada do ano")
    void testCodigoNaViradaDoAno() throws Exception {
        relogio.definir(LocalDateTime.of(2026, 12, 31, 23, 58));
        Long primeiro = abrirProjeto(joao, fazenda);
        Long segundo = abrirProjeto(joao, fazenda);
        relogio.definir(LocalDateTime.of(2027, 1, 1, 0, 0));
        Long terceiro = abrirProjeto(joao, fazenda);
        Long quarto = abrirProjeto(joao, fazenda);

        assertThat(relido(primeiro).getCodigo()).isEqualTo("PRJ-2026-0001");
        assertThat(relido(segundo).getCodigo()).isEqualTo("PRJ-2026-0002");
        assertThat(relido(terceiro).getCodigo()).isEqualTo("PRJ-2027-0001");
        assertThat(relido(quarto).getCodigo()).isEqualTo("PRJ-2027-0002");
        assertThat(relido(segundo).getDataAbertura()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(relido(terceiro).getDataAbertura()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(historicoRepository.listarPorProjeto(terceiro).get(0).getDataHora())
                .isEqualTo(LocalDateTime.of(2027, 1, 1, 0, 0));

        // De volta a 2026, a numeração continua do maior código do ano
        relogio.definir(LocalDateTime.of(2026, 12, 31, 23, 59));
        assertThat(relido(abrirProjeto(joao, fazenda)).getCodigo()).isEqualTo("PRJ-2026-0003");
    }

    @Test
    @DisplayName("Sem produtor ou propriedade, a abertura volta ao formulário com o erro no campo")
    void testAberturaSemEscolha() throws Exception {
        mockMvc.perform(post("/comercial/projetos").with(csrf()).with(anaLogada())
                        .param("produtorId", "")
                        .param("propriedadeId", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("comercial/abrirProjeto"))
                .andExpect(model().attributeHasFieldErrors("abertura", "produtorId", "propriedadeId"))
                .andExpect(content().string(containsString("Escolha o produtor.")))
                .andExpect(content().string(containsString("Escolha a propriedade.")));

        assertThat(projetoRepository.count()).isZero();
    }

    @Test
    @DisplayName("Propriedade de outro produtor é recusada com mensagem no topo")
    void testAberturaComPropriedadeDeOutroProdutor() throws Exception {
        Produtor maria = produtorService.criar("Maria Aparecida Souza", "111.444.777-35", null, null);

        mockMvc.perform(post("/comercial/projetos").with(csrf()).with(anaLogada())
                        .header(HttpHeaders.REFERER, "http://localhost/comercial/projetos/novo")
                        .param("produtorId", maria.getId().toString())
                        .param("propriedadeId", fazenda.getId().toString()))
                .andExpect(redirectedUrl("/comercial/projetos/novo"))
                .andExpect(flash().attribute("erro", "A propriedade escolhida não pertence a esse produtor."));

        assertThat(projetoRepository.count()).isZero();
    }

    @Test
    @DisplayName("O formulário de abertura liga cada propriedade ao seu produtor")
    void testFormularioDeAbertura() throws Exception {
        mockMvc.perform(get("/comercial/projetos/novo").with(anaLogada()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("João Batista Ferreira (529.982.247-25)")))
                .andExpect(content().string(containsString("data-grupo=\"" + joao.getId() + "\"")))
                .andExpect(content().string(containsString("Fazenda Boa Vista (Uberlândia)")));
    }

    @Test
    @DisplayName("Técnico de campo vê o projeto, mas não abre projeto nem agenda visita")
    void testTecnicoNaoAbreNemAgenda() throws Exception {
        Long id = abrirProjeto(joao, fazenda);

        mockMvc.perform(get("/projetos/" + id).with(logado("tiago")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PRJ-2026-0001")))
                .andExpect(content().string(not(containsString("/comercial/projetos/" + id + "/visita"))));
        mockMvc.perform(get("/comercial/projetos/novo").with(logado("tiago")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/comercial/projetos").with(csrf()).with(logado("tiago"))
                        .param("produtorId", joao.getId().toString())
                        .param("propriedadeId", fazenda.getId().toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/comercial/projetos/" + id + "/visita").with(csrf()).with(logado("tiago"))
                        .param("dataAgendada", "2026-11-18T08:30"))
                .andExpect(status().isForbidden());

        assertThat(projetoRepository.count()).isEqualTo(1);
        assertThat(levantamentoRepository.findByProjetoId(id)).isEmpty();
    }

    // ---------- Agendamento ----------

    @Test
    @DisplayName("Reagendar antes da visita troca a data do mesmo levantamento")
    void testReagendar() throws Exception {
        Long id = abrirProjeto(joao, fazenda);
        mockMvc.perform(post("/comercial/projetos/" + id + "/visita").with(csrf()).with(anaLogada())
                .param("dataAgendada", "2026-11-18T08:30"));

        mockMvc.perform(get("/comercial/projetos/" + id + "/visita").with(anaLogada()))
                .andExpect(status().isOk())
                .andExpect(TrilhaDeNavegacao.doProjeto(id, "Reagendar visita técnica"))
                .andExpect(content().string(containsString("Reagendar visita técnica")))
                .andExpect(content().string(containsString("value=\"2026-11-18T08:30:00\"")));
        mockMvc.perform(post("/comercial/projetos/" + id + "/visita").with(csrf()).with(anaLogada())
                        .param("dataAgendada", "2026-11-19T14:00"))
                .andExpect(redirectedUrl("/projetos/" + id));

        em.flush();
        em.clear();
        assertThat(levantamentoRepository.count()).isEqualTo(1);
        assertThat(levantamentoRepository.findByProjetoId(id).orElseThrow().getDataAgendada())
                .isEqualTo(LocalDateTime.of(2026, 11, 19, 14, 0));
    }

    @Test
    @DisplayName("Data vazia aparece no campo; data que já passou volta com a mensagem no topo")
    void testAgendamentoInvalido() throws Exception {
        Long id = abrirProjeto(joao, fazenda);
        String url = "/comercial/projetos/" + id + "/visita";

        mockMvc.perform(post(url).with(csrf()).with(anaLogada()).param("dataAgendada", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("comercial/agendarVisita"))
                .andExpect(model().attributeHasFieldErrors("agendamento", "dataAgendada"))
                .andExpect(content().string(containsString("Informe a data e a hora da visita.")));

        mockMvc.perform(post(url).with(csrf()).with(anaLogada())
                        .header(HttpHeaders.REFERER, "http://localhost" + url)
                        .param("dataAgendada", "2026-11-16T08:59"))
                .andExpect(redirectedUrl(url))
                .andExpect(flash().attribute("erro",
                        "A visita não pode ser agendada para uma data e hora que já passaram."));

        assertThat(levantamentoRepository.findByProjetoId(id)).isEmpty();
    }

    @Test
    @DisplayName("Fora de EM_LEVANTAMENTO a página não oferece o agendamento e o POST é recusado")
    void testAgendamentoForaDoLevantamento() throws Exception {
        Long id = abrirProjeto(joao, fazenda);
        projetoService.transitar(id, SituacaoProjeto.ORCANDO, null);
        String url = "/comercial/projetos/" + id + "/visita";

        mockMvc.perform(get("/projetos/" + id).with(anaLogada()))
                .andExpect(content().string(containsString("situacao-ORCANDO")))
                .andExpect(content().string(not(containsString(url))));
        mockMvc.perform(get(url).with(anaLogada()))
                .andExpect(redirectedUrl("/projetos/" + id))
                .andExpect(flash().attributeExists("erro"));
        mockMvc.perform(post(url).with(csrf()).with(anaLogada())
                        .header(HttpHeaders.REFERER, "http://localhost/projetos/" + id)
                        .param("dataAgendada", "2026-11-18T08:30"))
                .andExpect(redirectedUrl("/projetos/" + id))
                .andExpect(flash().attribute("erro", "A visita só pode ser agendada com o projeto em "
                        + "\"Em levantamento\". O projeto PRJ-2026-0001 está em \"Orçando\"."));

        assertThat(levantamentoRepository.findByProjetoId(id)).isEmpty();
    }

    // ---------- Situação e histórico no banco ----------

    @Test
    @DisplayName("transitar grava a nova situação e o histórico na mesma transação, e a linha do tempo mostra os dois")
    void testTransitarGravaHistorico() throws Exception {
        Long id = abrirProjeto(joao, fazenda);
        relogio.definir(LocalDateTime.of(2026, 11, 20, 15, 45));

        projetoService.transitar(id, SituacaoProjeto.ORCANDO, "Levantamento e lista técnica completos.");

        assertThat(relido(id).getSituacao()).isEqualTo(SituacaoProjeto.ORCANDO);
        List<HistoricoProjeto> historico = historicoRepository.listarPorProjeto(id);
        assertThat(historico).extracting(HistoricoProjeto::getSituacaoAnterior)
                .containsExactly(null, SituacaoProjeto.EM_LEVANTAMENTO);
        assertThat(historico).extracting(HistoricoProjeto::getSituacaoNova)
                .containsExactly(SituacaoProjeto.EM_LEVANTAMENTO, SituacaoProjeto.ORCANDO);
        assertThat(historico.get(1).getDataHora()).isEqualTo(LocalDateTime.of(2026, 11, 20, 15, 45));
        assertThat(historico.get(1).getUsuario()).isNull();

        mockMvc.perform(get("/projetos/" + id).with(anaLogada()))
                .andExpect(content().string(containsString("20/11/2026 15:45")))
                .andExpect(content().string(containsString("Levantamento e lista técnica completos.")))
                .andExpect(content().string(containsString("rotina automática")));
    }

    @Test
    @DisplayName("EM_LEVANTAMENTO -> ENCERRADO é recusada e nada muda no banco")
    void testTransicaoProibidaNoBanco() throws Exception {
        Long id = abrirProjeto(joao, fazenda);

        assertThatThrownBy(() -> projetoService.transitar(id, SituacaoProjeto.ENCERRADO, null))
                .isInstanceOf(TransicaoInvalidaException.class);

        assertThat(relido(id).getSituacao()).isEqualTo(SituacaoProjeto.EM_LEVANTAMENTO);
        assertThat(historicoRepository.listarPorProjeto(id)).hasSize(1);
    }

    @Test
    @DisplayName("Projeto cancelado não muda mais de situação")
    void testCanceladoEFinalNoBanco() throws Exception {
        Long id = abrirProjeto(joao, fazenda);
        projetoService.transitar(id, SituacaoProjeto.ORCANDO, null);
        projetoService.transitar(id, SituacaoProjeto.AGUARDANDO_RESPOSTA, null);
        projetoService.transitar(id, SituacaoProjeto.CANCELADO, "Cliente recusou a proposta.");

        for (SituacaoProjeto destino : SituacaoProjeto.values()) {
            assertThatThrownBy(() -> projetoService.transitar(id, destino, null))
                    .isInstanceOf(TransicaoInvalidaException.class)
                    .hasMessageContaining("situação final");
        }
        assertThat(relido(id).getSituacao()).isEqualTo(SituacaoProjeto.CANCELADO);
        assertThat(historicoRepository.listarPorProjeto(id)).hasSize(4);
    }

    // ---------- Lista de projetos ----------

    @Test
    @DisplayName("A lista filtra por situação e busca por código ou nome do produtor")
    void testListaDeProjetos() throws Exception {
        Produtor maria = produtorService.criar("Maria Aparecida Souza", "111.444.777-35", null, null);
        Propriedade sitio = produtorService.criarPropriedade(maria.getId(), "Sítio Esperança", "Araguari", null, null,
                null);
        Long doJoao = abrirProjeto(joao, fazenda);
        abrirProjeto(maria, sitio);
        projetoService.transitar(doJoao, SituacaoProjeto.ORCANDO, null);

        mockMvc.perform(get("/projetos").with(logado("tiago")))
                .andExpect(status().isOk())
                .andExpect(view().name("projetos/listarProjetos"))
                .andExpect(content().string(containsString("PRJ-2026-0001")))
                .andExpect(content().string(containsString("PRJ-2026-0002")))
                .andExpect(content().string(containsString("badge badge-situacao situacao-ORCANDO")))
                .andExpect(content().string(containsString("badge badge-situacao situacao-EM_LEVANTAMENTO")))
                // o técnico de campo não abre projeto
                .andExpect(content().string(not(containsString("/comercial/projetos/novo"))));

        mockMvc.perform(get("/projetos").param("situacao", "ORCANDO").with(anaLogada()))
                .andExpect(content().string(containsString("PRJ-2026-0001")))
                .andExpect(content().string(not(containsString("PRJ-2026-0002"))))
                .andExpect(content().string(containsString("/comercial/projetos/novo")))
                // O filtro agrupa as situações pela fase
                .andExpect(content().string(containsString("label=\"Proposta\"")))
                .andExpect(content().string(containsString("label=\"Encerrados e cancelados\"")));

        // Cada projeto diz o próximo passo; o técnico só acompanha os dois
        mockMvc.perform(get("/projetos").with(logado("tiago")))
                .andExpect(content().string(containsString("Gerar o orçamento e a proposta")))
                .andExpect(content().string(containsString("Agendar a visita técnica")))
                .andExpect(content().string(containsString("Aguardando o comercial")))
                .andExpect(content().string(not(containsString(">Gerar orçamento</a>"))));
        mockMvc.perform(get("/projetos").param("meus", "true").with(logado("tiago")))
                .andExpect(content().string(not(containsString("PRJ-2026-0001"))))
                .andExpect(content().string(containsString("Nenhum projeto esperando por você agora.")));
        // Para a vendedora os dois passos são dela, com o botão de cada um
        mockMvc.perform(get("/projetos").param("meus", "true").with(anaLogada()))
                .andExpect(content().string(containsString("PRJ-2026-0001")))
                .andExpect(content().string(containsString("PRJ-2026-0002")))
                .andExpect(content().string(containsString(">Gerar orçamento</a>")))
                .andExpect(content().string(containsString(">Agendar visita</a>")));

        assertThat(projetoService.pesquisar(null, "maria")).extracting(Projeto::getCodigo)
                .containsExactly("PRJ-2026-0002");
        assertThat(projetoService.pesquisar(null, "prj-2026-0001")).extracting(Projeto::getCodigo)
                .containsExactly("PRJ-2026-0001");
        assertThat(projetoService.pesquisar(SituacaoProjeto.ORCANDO, "maria")).isEmpty();
        assertThat(projetoService.pesquisar(null, "")).extracting(Projeto::getCodigo)
                .containsExactly("PRJ-2026-0002", "PRJ-2026-0001");
    }

    @Test
    @DisplayName("Projeto que não existe responde 404")
    void testProjetoInexistente() throws Exception {
        mockMvc.perform(get("/projetos/99999").with(anaLogada())).andExpect(status().isNotFound());
        mockMvc.perform(get("/comercial/projetos/99999/visita").with(anaLogada())).andExpect(status().isNotFound());
    }

    // ---------- Agenda ----------

    @Test
    @DisplayName("A agenda mostra a semana de segunda a domingo e navega para a anterior e a próxima")
    void testAgendaDaSemana() throws Exception {
        Long domingo = abrirProjeto(joao, fazenda);
        Long segundaSeguinte = abrirProjeto(joao, fazenda);
        mockMvc.perform(post("/comercial/projetos/" + domingo + "/visita").with(csrf()).with(anaLogada())
                .param("dataAgendada", "2026-11-22T23:30"));
        mockMvc.perform(post("/comercial/projetos/" + segundaSeguinte + "/visita").with(csrf()).with(anaLogada())
                .param("dataAgendada", "2026-11-23T07:00"));

        mockMvc.perform(get("/projetos/agenda").with(anaLogada()))
                .andExpect(status().isOk())
                .andExpect(view().name("projetos/agenda"))
                .andExpect(content().string(containsString("16/11 a 22/11/2026 · 1 visita(s)")))
                .andExpect(content().string(containsString("Segunda-feira, 16/11")))
                .andExpect(content().string(containsString("Domingo, 22/11")))
                .andExpect(content().string(containsString("PRJ-2026-0001")))
                .andExpect(content().string(not(containsString("PRJ-2026-0002"))))
                .andExpect(content().string(containsString("/projetos/agenda?semana=2026-11-09")))
                .andExpect(content().string(containsString("/projetos/agenda?semana=2026-11-23")));

        mockMvc.perform(get("/projetos/agenda").param("semana", "2026-11-25").with(anaLogada()))
                .andExpect(content().string(containsString("23/11 a 29/11/2026 · 1 visita(s)")))
                .andExpect(content().string(containsString("PRJ-2026-0002")))
                .andExpect(content().string(not(containsString("PRJ-2026-0001"))));
    }

    @Test
    @DisplayName("A grade põe a visita na altura do horário, e o calendário do mês troca de mês sem mudar a semana")
    void testCalendarioDaAgenda() throws Exception {
        Long id = abrirProjeto(joao, fazenda);
        mockMvc.perform(post("/comercial/projetos/" + id + "/visita").with(csrf()).with(anaLogada())
                .param("dataAgendada", "2026-11-18T08:30"));

        mockMvc.perform(get("/projetos/agenda").with(anaLogada()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Novembro de 2026</h2>")))
                // 08:30 são 510 minutos desde 0h
                .andExpect(content().string(containsString("--inicio: 510")))
                .andExpect(content().string(containsString("Segunda-feira, 16/11, hoje")))
                .andExpect(content().string(containsString("/projetos/agenda?semana=2026-11-16&amp;mes=2026-12")))
                .andExpect(model().attribute("miniCalendario",
                        hasProperty("primeiroDia", is(LocalDate.of(2026, 11, 1)))));

        // O mês ao lado vai para dezembro, e a semana aberta continua a de 16/11
        mockMvc.perform(get("/projetos/agenda").param("semana", "2026-11-16").param("mes", "2026-12")
                .with(anaLogada()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dezembro de 2026</h2>")))
                .andExpect(content().string(containsString("16/11 a 22/11/2026 · 1 visita(s)")))
                .andExpect(content().string(containsString("PRJ-2026-0001")));

        // Semana entre dois meses
        mockMvc.perform(get("/projetos/agenda").param("semana", "2026-12-02").with(anaLogada()))
                .andExpect(content().string(containsString("Novembro e dezembro de 2026")));
    }
}
