package br.edu.iftm.irrigacao.apresentacao.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
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
import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.LevantamentoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.RelogioAjustavel;
import jakarta.persistence.EntityManager;

/**
 * Levantamento em campo (RF03, RNF02, RNF06) e G1 (RF04) de ponta a ponta: upload de verdade,
 * banco H2 e pasta de uploads em {@code target/test-uploads}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(RelogioDeTesteConfig.class)
class LevantamentoIntegrationTest {

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
    private ProjetoRepository projetoRepository;

    @Autowired
    private LevantamentoRepository levantamentoRepository;

    @Autowired
    private EntityManager em;

    @Value("${irrigacao.upload.dir}")
    private String pastaDeUploads;

    private Long projetoId;
    private String formulario;

    @BeforeEach
    void prepararDados() {
        relogio.definir(RelogioDeTesteConfig.INICIO);
        Usuario ana = usuarioService.criar("Ana Vendedora", "ana", SENHA, Perfil.VENDEDOR);
        usuarioService.criar("Tiago Técnico", "tiago", SENHA, Perfil.TECNICO_CAMPO);
        usuarioService.criar("Paulo Projetista", "paulo", SENHA, Perfil.PROJETISTA);
        Produtor joao = produtorService.criar("João Batista Ferreira", "529.982.247-25", null, null);
        Propriedade fazenda = produtorService.criarPropriedade(joao.getId(), "Fazenda Boa Vista", "Uberlândia", null,
                null, null);
        Projeto projeto = new Projeto();
        projeto.setCodigo("PRJ-2026-0001");
        projeto.setPropriedade(fazenda);
        projeto.setResponsavel(ana);
        projeto.setDataAbertura(LocalDate.of(2026, 11, 16));
        projetoId = projetoRepository.save(projeto).getId();
        formulario = "/campo/projetos/" + projetoId + "/levantamento";
    }

    @AfterEach
    void apagarFotos() {
        FileSystemUtils.deleteRecursively(pastaDoProjeto().toFile());
    }

    private Path pastaDoProjeto() {
        return Path.of(pastaDeUploads, "levantamento", projetoId.toString());
    }

    private RequestPostProcessor logado(String login) {
        return user(userDetailsService.loadUserByUsername(login));
    }

    /** Envio do formulário pelo técnico, com o botão informado. */
    private MockMultipartHttpServletRequestBuilder envio(String acao, MockMultipartFile... fotos) {
        MockMultipartHttpServletRequestBuilder requisicao = multipart(formulario);
        for (MockMultipartFile foto : fotos) {
            requisicao.file(foto);
        }
        requisicao.with(csrf()).with(logado("tiago")).param("acao", acao);
        return requisicao;
    }

    /** Todos os campos do G1 preenchidos. */
    private MockMultipartHttpServletRequestBuilder completo(MockMultipartHttpServletRequestBuilder requisicao) {
        return (MockMultipartHttpServletRequestBuilder) requisicao
                .param("dataVisita", "2026-11-16T08:40")
                .param("cultura", "Café")
                .param("areaIrrigadaHa", "12.5")
                .param("fonteAgua", "POCO_ARTESIANO")
                .param("vazaoFonte", "8")
                .param("topografia", "Plana, com leve declive para o córrego")
                .param("latitude", "-18.9113")
                .param("longitude", "-48.2622");
    }

    private Levantamento levantamento() {
        em.flush();
        em.clear();
        Levantamento levantamento = levantamentoRepository.findByProjetoId(projetoId).orElseThrow();
        levantamento.getFotos().size();
        return levantamento;
    }

    // ---------- Tela ----------

    @Test
    @DisplayName("A tela de campo tem câmera, GPS, os dois botões e a data da visita já em agora")
    void testTelaDeCampo() throws Exception {
        mockMvc.perform(get(formulario).with(logado("tiago")))
                .andExpect(status().isOk())
                .andExpect(view().name("campo/levantamento"))
                .andExpect(TrilhaDeNavegacao.doProjeto(projetoId, "Levantamento"))
                .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
                .andExpect(content().string(containsString("accept=\"image/*\" capture=\"environment\" multiple")))
                .andExpect(content().string(containsString("Usar minha localização")))
                .andExpect(content().string(containsString("value=\"enviar\"")))
                .andExpect(content().string(containsString("Salvar rascunho")))
                .andExpect(content().string(containsString("value=\"2026-11-16T09:00:00\"")))
                .andExpect(content().string(containsString("Poço semiartesiano")))
                .andExpect(content().string(containsString("Para enviar, falta:")));
    }

    // ---------- Rascunho e envio ----------

    @Test
    @DisplayName("Rascunho grava o que veio, com a foto num nome gerado pelo sistema")
    void testRascunhoComFoto() throws Exception {
        byte[] jpeg = FotosDeTeste.jpeg();

        mockMvc.perform(envio("rascunho", FotosDeTeste.arquivo("../../config/minha foto.jpg", "image/jpeg", jpeg))
                        .param("cultura", "Café"))
                .andExpect(redirectedUrl(formulario))
                .andExpect(flash().attribute("sucesso", "Rascunho salvo. 1 foto adicionada."));

        Levantamento levantamento = levantamento();
        assertThat(levantamento.getCultura()).isEqualTo("Café");
        assertThat(levantamento.getAreaIrrigadaHa()).isNull();
        assertThat(levantamento.getFotos()).singleElement().asString()
                .matches("levantamento/" + projetoId + "/[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.jpg");
        Path gravada = Path.of(pastaDeUploads).resolve(levantamento.getFotos().get(0));
        assertThat(Files.readAllBytes(gravada)).isEqualTo(jpeg);
        assertThat(Files.exists(Path.of(pastaDeUploads, "config"))).isFalse();
        assertThat(projetoRepository.findById(projetoId).orElseThrow().getSituacao())
                .isEqualTo(SituacaoProjeto.EM_LEVANTAMENTO);
    }

    @Test
    @DisplayName("Envio incompleto: continua em levantamento, a tela diz o que falta e a página do projeto também")
    void testEnvioIncompleto() throws Exception {
        mockMvc.perform(envio("enviar", FotosDeTeste.arquivo("a.png", "image/png", FotosDeTeste.png()))
                        .param("dataVisita", "2026-11-16T08:40")
                        .param("cultura", "Milho")
                        .param("fonteAgua", "RIO_CORREGO"))
                .andExpect(redirectedUrl(formulario))
                .andExpect(flash().attribute("erro", "Levantamento salvo, mas incompleto. Falta: área irrigada, "
                        + "vazão da fonte, topografia, latitude, longitude."));

        assertThat(projetoRepository.findById(projetoId).orElseThrow().getSituacao())
                .isEqualTo(SituacaoProjeto.EM_LEVANTAMENTO);
        mockMvc.perform(get("/projetos/" + projetoId).with(logado("ana")))
                .andExpect(content().string(containsString("Levantamento incompleto. Falta:")))
                .andExpect(content().string(containsString("<li>vazão da fonte</li>")))
                .andExpect(content().string(containsString("<li>topografia</li>")))
                .andExpect(content().string(not(containsString("<li>cultura</li>"))))
                .andExpect(content().string(not(containsString("Levantamento completo"))));
        mockMvc.perform(get("/projetos/" + projetoId).with(logado("paulo")))
                .andExpect(model().attribute("abaAtual", AbaProjeto.LEVANTAMENTO))
                .andExpect(content().string(not(containsString("/engenharia/projetos/" + projetoId + "/dimensionamento"))));
    }

    @Test
    @DisplayName("Envio completo: volta para o projeto, com fotos, mapa e o dimensionamento liberado")
    void testEnvioCompleto() throws Exception {
        mockMvc.perform(completo(envio("enviar", FotosDeTeste.arquivo("fonte.jpg", "image/jpeg", FotosDeTeste.jpeg()),
                        FotosDeTeste.arquivo("area.png", "image/png", FotosDeTeste.png()))))
                .andExpect(redirectedUrl("/projetos/" + projetoId))
                .andExpect(flash().attribute("sucesso", "Levantamento completo e enviado. 2 fotos adicionadas. "
                        + "O dimensionamento está liberado para o projetista."));

        Levantamento levantamento = levantamento();
        assertThat(levantamento.getDataVisita()).isEqualTo(LocalDateTime.of(2026, 11, 16, 8, 40));
        assertThat(levantamento.getTopografia()).isEqualTo("Plana, com leve declive para o córrego");
        assertThat(levantamento.getFotos()).hasSize(2);
        assertThat(levantamento.getFotos().get(0)).endsWith(".jpg");
        assertThat(levantamento.getFotos().get(1)).endsWith(".png");

        // O próximo passo é o dimensionamento: a página abre na aba Lista técnica
        mockMvc.perform(get("/projetos/" + projetoId).with(logado("paulo")))
                .andExpect(model().attribute("abaAtual", AbaProjeto.LISTA))
                .andExpect(content().string(containsString("Levantamento completo: o dimensionamento está liberado.")))
                .andExpect(content().string(containsString("Poço artesiano")))
                .andExpect(content().string(containsString("12,50 ha")))
                .andExpect(content().string(containsString("https://www.google.com/maps?q=-18.9113")))
                .andExpect(content().string(containsString("/projetos/" + projetoId + "/levantamento/fotos/1")))
                .andExpect(content().string(containsString("/engenharia/projetos/" + projetoId + "/dimensionamento")));
    }

    @Test
    @DisplayName("Fotos de envios diferentes se somam, na ordem em que chegaram")
    void testFotosSeSomam() throws Exception {
        mockMvc.perform(envio("rascunho", FotosDeTeste.arquivo("1.jpg", "image/jpeg", FotosDeTeste.jpeg())));
        mockMvc.perform(envio("rascunho", FotosDeTeste.arquivo("2.png", "image/png", FotosDeTeste.png())));

        Levantamento levantamento = levantamento();
        assertThat(levantamento.getFotos()).hasSize(2);
        assertThat(levantamento.getFotos().get(0)).endsWith(".jpg");
        assertThat(levantamento.getFotos().get(1)).endsWith(".png");
    }

    // ---------- Arquivo inválido ----------

    @Test
    @DisplayName("Texto com nome de foto, GIF e foto de mais de 5 MB são recusados ao lado do campo, sem gravar nada")
    void testArquivoInvalido() throws Exception {
        MockMultipartFile disfarcado = FotosDeTeste.arquivo("foto.jpg", "image/jpeg", "<script>alert(1)</script>".getBytes());
        MockMultipartFile gif = FotosDeTeste.arquivo("animada.gif", "image/gif", new byte[] { 'G', 'I', 'F', '8', '9', 'a' });
        MockMultipartFile grande = FotosDeTeste.arquivo("grande.jpg", "image/jpeg",
                Arrays.copyOf(FotosDeTeste.jpeg(), 5 * 1024 * 1024 + 1));

        for (MockMultipartFile invalido : new MockMultipartFile[] { disfarcado, gif }) {
            mockMvc.perform(completo(envio("enviar", invalido)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("campo/levantamento"))
                    .andExpect(model().attributeHasFieldErrors("dados", "novasFotos"))
                    .andExpect(content().string(containsString("Envie só fotos JPEG ou PNG.")));
        }
        mockMvc.perform(completo(envio("enviar", grande)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("dados", "novasFotos"))
                .andExpect(content().string(containsString("Cada foto pode ter até 5 MB.")));

        em.flush();
        assertThat(levantamentoRepository.findByProjetoId(projetoId)).isEmpty();
        assertThat(Files.exists(pastaDoProjeto())).isFalse();
    }

    @Test
    @DisplayName("Coordenada fora da faixa aparece ao lado do campo")
    void testCoordenadaInvalida() throws Exception {
        mockMvc.perform(envio("rascunho").param("latitude", "95").param("longitude", "-200"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("dados", "latitude", "longitude"))
                .andExpect(content().string(containsString("A latitude vai de -90 a 90.")));
    }

    // ---------- Fotos servidas ----------

    @Test
    @DisplayName("A foto é servida pela posição, para qualquer usuário logado; fora disso, 404 ou login")
    void testServirFoto() throws Exception {
        byte[] jpeg = FotosDeTeste.jpeg();
        mockMvc.perform(envio("rascunho", FotosDeTeste.arquivo("fonte.jpg", "image/jpeg", jpeg)));
        String foto = "/projetos/" + projetoId + "/levantamento/fotos/";

        mockMvc.perform(get(foto + "0").with(logado("ana")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(jpeg));
        mockMvc.perform(get(foto + "1").with(logado("ana"))).andExpect(status().isNotFound());
        mockMvc.perform(get(foto + "abc").with(logado("ana"))).andExpect(status().isNotFound());
        mockMvc.perform(get("/projetos/99999/levantamento/fotos/0").with(logado("ana"))).andExpect(status().isNotFound());
        mockMvc.perform(get(foto + "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ---------- Bloqueios ----------

    @Test
    @DisplayName("Fora de EM_LEVANTAMENTO a tela não abre e o envio não grava")
    void testForaDoLevantamento() throws Exception {
        Projeto projeto = projetoRepository.findById(projetoId).orElseThrow();
        projeto.setSituacao(SituacaoProjeto.ORCANDO);
        projetoRepository.saveAndFlush(projeto);

        mockMvc.perform(get(formulario).with(logado("tiago")))
                .andExpect(redirectedUrl("/projetos/" + projetoId))
                .andExpect(flash().attribute("erro", "O levantamento do projeto PRJ-2026-0001 só pode ser preenchido "
                        + "com o projeto em \"Em levantamento\"."));
        mockMvc.perform(completo(envio("enviar", FotosDeTeste.arquivo("a.jpg", "image/jpeg", FotosDeTeste.jpeg())))
                        .header(HttpHeaders.REFERER, "http://localhost/projetos/" + projetoId))
                .andExpect(redirectedUrl("/projetos/" + projetoId))
                .andExpect(flash().attribute("erro", "O levantamento só pode ser alterado com o projeto em "
                        + "\"Em levantamento\". O projeto PRJ-2026-0001 está em \"Orçando\"."));
        mockMvc.perform(get("/projetos/" + projetoId).with(logado("tiago")))
                .andExpect(content().string(not(containsString(formulario))));

        em.flush();
        assertThat(levantamentoRepository.findByProjetoId(projetoId)).isEmpty();
        assertThat(Files.exists(pastaDoProjeto())).isFalse();
    }

    @Test
    @DisplayName("Só campo e projetista preenchem o levantamento; o vendedor recebe acesso negado")
    void testPermissoes() throws Exception {
        mockMvc.perform(get(formulario).with(logado("ana"))).andExpect(status().isForbidden());
        mockMvc.perform(multipart(formulario).with(csrf()).with(logado("ana")).param("cultura", "Soja"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(formulario).with(logado("paulo"))).andExpect(status().isOk());
        mockMvc.perform(get("/projetos/" + projetoId).with(logado("ana")))
                .andExpect(content().string(not(containsString(formulario))));
        mockMvc.perform(get("/projetos/" + projetoId).with(logado("tiago")))
                .andExpect(content().string(containsString(formulario)));
    }

    @Test
    @DisplayName("O envio exige o token CSRF")
    void testSemCsrf() throws Exception {
        mockMvc.perform(multipart(formulario).with(logado("tiago")).param("cultura", "Soja"))
                .andExpect(status().isForbidden());
        assertThat(levantamentoRepository.findByProjetoId(projetoId)).isEmpty();
    }
}
