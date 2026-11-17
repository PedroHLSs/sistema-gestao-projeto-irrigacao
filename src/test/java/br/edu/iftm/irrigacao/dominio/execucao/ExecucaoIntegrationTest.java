package br.edu.iftm.irrigacao.dominio.execucao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.config.RelogioDeTesteConfig;
import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioAutenticado;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;
import br.edu.iftm.irrigacao.dominio.comercial.Produtor;
import br.edu.iftm.irrigacao.dominio.comercial.ProdutorService;
import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;
import br.edu.iftm.irrigacao.dominio.estoque.EstoqueService;
import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.Material;
import br.edu.iftm.irrigacao.dominio.estoque.MaterialService;
import br.edu.iftm.irrigacao.dominio.estoque.MovimentacaoEstoque;
import br.edu.iftm.irrigacao.dominio.estoque.SobraItem;
import br.edu.iftm.irrigacao.dominio.estoque.SobrasService;
import br.edu.iftm.irrigacao.dominio.estoque.TipoMovimentacao;
import br.edu.iftm.irrigacao.dominio.projeto.HistoricoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.TipoFoto;
import br.edu.iftm.irrigacao.persistencia.HistoricoProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.ItemProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.MaterialRepository;
import br.edu.iftm.irrigacao.persistencia.OrdemServicoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.RelogioAjustavel;
import jakarta.persistence.EntityManager;

/**
 * Execução do projeto (RF15 a RF19, G6, G7, G8), com o banco H2.
 *
 * <p>O projeto começa em PROGRAMANDO_EXECUCAO, com pressão de 30 mca, vazão de 6 m³/h e
 * tolerância de 2% (faixa de 29,4 a 30,6 mca e de 5,88 a 6,12 m³/h). A separação já tirou do
 * estoque 20 barras de tubo (de 50) e 400 gotejadores (de 400). O filtro existe no cadastro, mas
 * não está na lista técnica.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(RelogioDeTesteConfig.class)
class ExecucaoIntegrationTest {

    private static final String SENHA = "senha-de-teste-1";
    private static final LocalDate HOJE = RelogioDeTesteConfig.INICIO.toLocalDate();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RelogioAjustavel relogio;

    @Autowired
    private OrdemServicoService ordemServicoService;

    @Autowired
    private ApontamentoService apontamentoService;

    @Autowired
    private MedicaoService medicaoService;

    @Autowired
    private AceiteService aceiteService;

    @Autowired
    private SobrasService sobrasService;

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
    private OrdemServicoRepository ordemRepository;

    @Autowired
    private HistoricoProjetoRepository historicoRepository;

    @Autowired
    private EntityManager em;

    private Usuario pedro;
    private Usuario tito;
    private Usuario tania;
    private Usuario almir;
    private Long projetoId;
    private Long tuboId;
    private Long gotejadorId;
    private Long filtroId;
    private Long tuboItem;
    private Long gotejadorItem;

    @BeforeEach
    void levarOProjetoAProgramacao() {
        relogio.definir(RelogioDeTesteConfig.INICIO);
        Usuario ana = usuarioService.criar("Ana Vendedora", "ana", SENHA, Perfil.VENDEDOR);
        pedro = usuarioService.criar("Pedro Projetista", "pedro", SENHA, Perfil.PROJETISTA);
        tito = usuarioService.criar("Tito Técnico", "tito", SENHA, Perfil.TECNICO_CAMPO);
        tania = usuarioService.criar("Tânia Técnica", "tania", SENHA, Perfil.TECNICO_CAMPO);
        almir = usuarioService.criar("Almir Almoxarife", "almir", SENHA, Perfil.ALMOXARIFE);
        usuarioService.criar("Carlos Comprador", "carlos", SENHA, Perfil.COMPRADOR);
        Produtor joao = produtorService.criar("João Batista Ferreira", "529.982.247-25", null, null);
        Propriedade fazenda = produtorService.criarPropriedade(joao.getId(), "Fazenda Boa Vista", "Uberlândia", null,
                null, null);
        tuboId = materialService.criar("TB-PVC-050", "Tubo PVC 50 mm", "barra", new BigDecimal("62.90"),
                new BigDecimal("44.80")).getId();
        gotejadorId = materialService.criar("GT-AUT-2L", "Gotejador 2 L/h", "un", new BigDecimal("0.95"),
                new BigDecimal("0.52")).getId();
        filtroId = materialService.criar("FT-DSC-120", "Filtro de disco 120 mesh", "un", new BigDecimal("180.00"),
                new BigDecimal("120.00")).getId();

        Projeto projeto = new Projeto();
        projeto.setCodigo("PRJ-2026-0001");
        projeto.setPropriedade(fazenda);
        projeto.setResponsavel(ana);
        projeto.setDataAbertura(LocalDate.of(2026, 11, 2));
        projeto.setSituacao(SituacaoProjeto.PROGRAMANDO_EXECUCAO);
        projeto.setPressaoProjetoMca(30.0);
        projeto.setVazaoProjetoM3h(6.0);
        projeto.setToleranciaPercentual(2.0);
        projeto.setSeparacaoConferida(true);
        projetoId = projetoRepository.save(projeto).getId();

        estoqueService.entradaCompra(tuboId, new BigDecimal("50"));
        estoqueService.entradaCompra(gotejadorId, new BigDecimal("400"));
        estoqueService.reservar(tuboId, projetoId, new BigDecimal("20"));
        estoqueService.reservar(gotejadorId, projetoId, new BigDecimal("400"));
        estoqueService.baixarReservaParaProjeto(tuboId, projetoId, new BigDecimal("20"));
        estoqueService.baixarReservaParaProjeto(gotejadorId, projetoId, new BigDecimal("400"));

        Projeto referencia = projetoRepository.getReferenceById(projetoId);
        tuboItem = itemProjetoRepository.save(new ItemProjeto(null, referencia, materialRepository.getReferenceById(tuboId),
                new BigDecimal("20"), new BigDecimal("20"), BigDecimal.ZERO, null)).getId();
        gotejadorItem = itemProjetoRepository.save(new ItemProjeto(null, referencia,
                materialRepository.getReferenceById(gotejadorId), new BigDecimal("400"), new BigDecimal("400"),
                BigDecimal.ZERO, null)).getId();
        em.flush();
        em.clear();
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

    private Projeto projeto() {
        em.flush();
        em.clear();
        return projetoRepository.findById(projetoId).orElseThrow();
    }

    private ItemProjeto item(Long id) {
        em.flush();
        em.clear();
        return itemProjetoRepository.findById(id).orElseThrow();
    }

    private Material material(Long id) {
        em.flush();
        em.clear();
        return materialRepository.findById(id).orElseThrow();
    }

    private List<HistoricoProjeto> historico() {
        em.flush();
        em.clear();
        return historicoRepository.listarPorProjeto(projetoId);
    }

    private Long emitir() {
        logar(pedro);
        Long ordemId = ordemServicoService.emitir(projetoId, HOJE, List.of(tito.getId())).getId();
        logar(tito);
        return ordemId;
    }

    /** Instalação com 8 h, 17 barras de tubo e os 400 gotejadores, concluída e esperando a medição. */
    private Long instalar() {
        Long ordemId = emitir();
        apontamentoService.apontarHoras(ordemId, new BigDecimal("8"));
        apontamentoService.apontarMaterial(ordemId, tuboId, new BigDecimal("17"));
        apontamentoService.apontarMaterial(ordemId, gotejadorId, new BigDecimal("400"));
        ordemServicoService.concluirInstalacao(ordemId);
        return ordemId;
    }

    private Long chegarAoAceite() {
        Long ordemId = instalar();
        medicaoService.registrar(projetoId, 30.0, 6.0, null, null, null);
        return ordemId;
    }

    private Long finalizar() {
        Long ordemId = chegarAoAceite();
        aceiteService.registrarAceite(projetoId, "João Batista Ferreira", assinatura());
        return ordemId;
    }

    /** PNG com um traço, como o canvas manda. */
    private static String assinatura() {
        BufferedImage imagem = new BufferedImage(300, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pincel = imagem.createGraphics();
        pincel.setColor(Color.DARK_GRAY);
        pincel.setStroke(new BasicStroke(3));
        pincel.drawLine(20, 80, 280, 40);
        pincel.dispose();
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png(imagem));
    }

    /** Canvas limpo: PNG só com pixels transparentes. */
    private static String assinaturaEmBranco() {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(
                png(new BufferedImage(300, 120, BufferedImage.TYPE_INT_ARGB)));
    }

    private static byte[] png(BufferedImage imagem) {
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            ImageIO.write(imagem, "png", saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<MovimentacaoEstoque> devolucoes(Long materialId) {
        em.flush();
        em.clear();
        return estoqueService.extrato(materialId, null, null, projetoId, PageRequest.of(0, 50)).getContent().stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.DEVOLUCAO)
                .toList();
    }

    private SobraItem sobra(Long itemId) {
        em.flush();
        em.clear();
        return sobrasService.sobras(projetoId).stream()
                .filter(s -> s.getItem().getId().equals(itemId))
                .findFirst()
                .orElseThrow();
    }

    // ---------- Ordem de serviço (RF15) ----------

    @Test
    @DisplayName("Emitir a OS numera a ordem e leva o projeto a Executando instalação (transição 8)")
    void testEmitirOrdem() throws Exception {
        mockMvc.perform(post("/engenharia/projetos/{id}/programacao", projetoId)
                .param("dataProgramada", "2026-11-18")
                .param("tecnicos", tito.getId().toString(), tania.getId().toString())
                .with(csrf()).with(logado("pedro")))
                .andExpect(redirectedUrl("/projetos/" + projetoId))
                .andExpect(flash().attribute("sucesso", containsString("Ordem de serviço 1 emitida")));

        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.EXECUTANDO_INSTALACAO);
        List<OrdemServico> ordens = ordemServicoService.doProjeto(projetoId);
        assertThat(ordens).hasSize(1);
        assertThat(ordens.get(0).getNumero()).isEqualTo(1);
        assertThat(ordens.get(0).getDataProgramada()).isEqualTo(LocalDate.of(2026, 11, 18));
        assertThat(ordens.get(0).getDataConclusao()).isNull();
        assertThat(ordens.get(0).getTecnicos()).extracting(Usuario::getLogin).containsExactlyInAnyOrder("tito", "tania");
        HistoricoProjeto ultimo = historico().get(historico().size() - 1);
        assertThat(ultimo.getSituacaoAnterior()).isEqualTo(SituacaoProjeto.PROGRAMANDO_EXECUCAO);
        assertThat(ultimo.getSituacaoNova()).isEqualTo(SituacaoProjeto.EXECUTANDO_INSTALACAO);
        assertThat(ultimo.getObservacao()).contains("Ordem de serviço 1", "18/11/2026", "Tito Técnico");
    }

    @Test
    @DisplayName("Sem técnico ou sem data, a programação volta com o erro ao lado do campo")
    void testProgramacaoSemTecnico() throws Exception {
        mockMvc.perform(post("/engenharia/projetos/{id}/programacao", projetoId)
                .param("dataProgramada", "")
                .with(csrf()).with(logado("pedro")))
                .andExpect(status().isOk())
                .andExpect(view().name("engenharia/programacao"))
                .andExpect(model().attributeHasFieldErrors("programacao", "dataProgramada", "tecnicos"))
                .andExpect(content().string(containsString("Escolha pelo menos um técnico de campo.")));
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.PROGRAMANDO_EXECUCAO);
    }

    @Test
    @DisplayName("Só técnico de campo ativo entra na OS, a data não pode ter passado e não há segunda emissão")
    void testRegrasDaEmissao() {
        logar(pedro);
        assertThatThrownBy(() -> ordemServicoService.emitir(projetoId, HOJE, List.of(almir.getId())))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Técnico de campo");
        assertThatThrownBy(() -> ordemServicoService.emitir(projetoId, HOJE.minusDays(1), List.of(tito.getId())))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("de hoje em diante");

        ordemServicoService.emitir(projetoId, HOJE, List.of(tito.getId()));
        assertThatThrownBy(() -> ordemServicoService.emitir(projetoId, HOJE, List.of(tito.getId())))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Programando execução");
    }

    @Test
    @DisplayName("Minhas OS mostra só as ordens abertas do técnico; a de outra equipe responde 404")
    void testMinhasOrdens() throws Exception {
        Long ordemId = emitir();

        mockMvc.perform(get("/campo/minhas-os").with(logado("tito")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("OS 1 · PRJ-2026-0001")))
                .andExpect(content().string(containsString("Hoje")))
                .andExpect(content().string(containsString("/campo/ordens/" + ordemId)));
        mockMvc.perform(get("/campo/minhas-os").with(logado("tania")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nenhuma ordem de serviço aberta para você.")));
        mockMvc.perform(get("/campo/ordens/{id}", ordemId).with(logado("tania")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/campo/ordens/{id}", ordemId).with(logado("pedro")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A programação é do projetista: técnico e almoxarife não entram")
    void testPermissaoDaProgramacao() throws Exception {
        mockMvc.perform(get("/engenharia/projetos/{id}/programacao", projetoId).with(logado("tito")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/engenharia/projetos/{id}/programacao", projetoId)
                .param("dataProgramada", "2026-11-18").param("tecnicos", tito.getId().toString())
                .with(csrf()).with(logado("almir")))
                .andExpect(status().isForbidden());
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.PROGRAMANDO_EXECUCAO);
    }

    // ---------- Apontamentos (RF16) ----------

    @Test
    @DisplayName("Horas guardam o custo da hora do momento e o material soma no apontado do item")
    void testApontamentos() {
        Long ordemId = emitir();

        ApontamentoHoras horas = apontamentoService.apontarHoras(ordemId, new BigDecimal("7.5"));
        apontamentoService.apontarMaterial(ordemId, tuboId, new BigDecimal("5"));
        apontamentoService.apontarMaterial(ordemId, tuboId, new BigDecimal("3.5"));

        assertThat(horas.getValorHora()).isEqualByComparingTo("45.00");
        assertThat(horas.getUsuario().getLogin()).isEqualTo("tito");
        assertThat(horas.getDataHora()).isEqualTo(RelogioDeTesteConfig.INICIO);
        assertThat(item(tuboItem).getQuantidadeApontada()).isEqualByComparingTo("8.5");
        assertThat(apontamentoService.materiaisDaOrdem(ordemId)).hasSize(2);
        assertThat(apontamentoService.horasDoProjeto(projetoId)).hasSize(1);
    }

    @Test
    @DisplayName("Material fora da lista técnica é rejeitado, no service e na tela")
    void testMaterialForaDaLista() throws Exception {
        Long ordemId = emitir();

        assertThatThrownBy(() -> apontamentoService.apontarMaterial(ordemId, filtroId, BigDecimal.ONE))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não está na lista técnica");

        mockMvc.perform(post("/campo/ordens/{id}/materiais", ordemId)
                .param("materialId", filtroId.toString()).param("quantidade", "1")
                .header(HttpHeaders.REFERER, "http://localhost/campo/ordens/" + ordemId)
                .with(csrf()).with(logado("tito")))
                .andExpect(redirectedUrl("/campo/ordens/" + ordemId))
                .andExpect(flash().attribute("erro", containsString("não está na lista técnica")));
        assertThat(apontamentoService.materiaisDaOrdem(ordemId)).isEmpty();
    }

    @Test
    @DisplayName("Apontar mais que o separado avisa, mas grava")
    void testAvisoDeExcesso() throws Exception {
        Long ordemId = emitir();

        mockMvc.perform(post("/campo/ordens/{id}/materiais", ordemId)
                .param("materialId", tuboId.toString()).param("quantidade", "20")
                .with(csrf()).with(logado("tito")))
                .andExpect(redirectedUrl("/campo/ordens/" + ordemId))
                .andExpect(flash().attributeCount(1));
        mockMvc.perform(post("/campo/ordens/{id}/materiais", ordemId)
                .param("materialId", tuboId.toString()).param("quantidade", "2")
                .with(csrf()).with(logado("tito")))
                .andExpect(redirectedUrl("/campo/ordens/" + ordemId))
                .andExpect(flash().attribute("sucesso", containsString("2 barra de TB-PVC-050 apontados")))
                .andExpect(flash().attribute("aviso", containsString("passou do separado (20)")));

        assertThat(item(tuboItem).getQuantidadeApontada()).isEqualByComparingTo("22");
    }

    @Test
    @DisplayName("Excluir o último lançamento tira o material do apontado; depois sai a hora")
    void testExcluirUltimo() {
        Long ordemId = emitir();
        apontamentoService.apontarHoras(ordemId, new BigDecimal("4"));
        relogio.avancar(Duration.ofMinutes(10));
        apontamentoService.apontarMaterial(ordemId, tuboId, new BigDecimal("6"));

        Apontamento primeiro = apontamentoService.excluirUltimo(ordemId);
        assertThat(primeiro).isInstanceOf(ApontamentoMaterial.class);
        assertThat(item(tuboItem).getQuantidadeApontada()).isEqualByComparingTo("0");

        Apontamento segundo = apontamentoService.excluirUltimo(ordemId);
        assertThat(segundo).isInstanceOf(ApontamentoHoras.class);
        em.flush();
        assertThat(apontamentoService.horasDaOrdem(ordemId)).isEmpty();
        assertThatThrownBy(() -> apontamentoService.excluirUltimo(ordemId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não tem lançamento");
    }

    @Test
    @DisplayName("Concluída a instalação, não se aponta mais; a ordem de outro técnico nem é encontrada")
    void testApontarForaDaInstalacao() {
        Long ordemId = emitir();
        ordemServicoService.concluirInstalacao(ordemId);
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.VALIDANDO_MEDICAO);

        assertThatThrownBy(() -> apontamentoService.apontarHoras(ordemId, BigDecimal.ONE))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Executando instalação");
        logar(tania);
        assertThatThrownBy(() -> apontamentoService.apontarHoras(ordemId, BigDecimal.ONE))
                .isInstanceOf(RegistroNaoEncontradoException.class);
    }

    // ---------- Medição (RF17, G6) ----------

    @Test
    @DisplayName("Tolerância: exatamente no limite conta como dentro; logo depois, fora")
    void testLimitesDaTolerancia() {
        assertThat(medicaoService.dentroDaTolerancia(30.6, 30, 2)).isTrue();
        assertThat(medicaoService.dentroDaTolerancia(29.4, 30, 2)).isTrue();
        assertThat(medicaoService.dentroDaTolerancia(30.61, 30, 2)).isFalse();
        assertThat(medicaoService.dentroDaTolerancia(29.39, 30, 2)).isFalse();
        assertThat(medicaoService.dentroDaTolerancia(6.12, 6, 2)).isTrue();
        assertThat(medicaoService.dentroDaTolerancia(5.88, 6, 2)).isTrue();
        assertThat(medicaoService.dentroDaTolerancia(6.121, 6, 2)).isFalse();
        assertThat(medicaoService.dentroDaTolerancia(5.879, 6, 2)).isFalse();
        // Em double, 35,7 - 35 dá 0,7000000000000028 e passaria do limite de 0,7
        assertThat(medicaoService.dentroDaTolerancia(35.7, 35, 2)).isTrue();
        assertThat(medicaoService.desvioPercentual(30.6, 30)).isEqualByComparingTo("2.00");
        assertThat(medicaoService.desvioPercentual(29.39, 30)).isEqualByComparingTo("-2.03");
    }

    @Test
    @DisplayName("A faixa aceita sai dos parâmetros do projeto")
    void testFaixa() {
        FaixaMedicao faixa = medicaoService.faixa(projeto());
        assertThat(faixa.getPressaoMinima()).isEqualByComparingTo("29.4");
        assertThat(faixa.getPressaoMaxima()).isEqualByComparingTo("30.6");
        assertThat(faixa.getVazaoMinima()).isEqualByComparingTo("5.88");
        assertThat(faixa.getVazaoMaxima()).isEqualByComparingTo("6.12");
    }

    @Test
    @DisplayName("Medição exatamente no limite aprova e leva a Colhendo aceite (transição 10)")
    void testMedicaoNoLimite() {
        instalar();
        Medicao medicao = medicaoService.registrar(projetoId, 30.6, 5.88, null, -18.9113, -48.2622);

        assertThat(medicao.isAprovada()).isTrue();
        assertThat(medicao.getUsuario().getLogin()).isEqualTo("tito");
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.COLHENDO_ACEITE);
        assertThat(historico().get(historico().size() - 1).getObservacao())
                .contains("dentro do parâmetro", "+2%", "-2%");
    }

    @Test
    @DisplayName("Medição logo fora do limite reprova e leva a Corrigindo instalação (transição 11)")
    void testMedicaoLogoFora() {
        instalar();
        Medicao medicao = medicaoService.registrar(projetoId, 30.0, 6.121, null, null, null);

        assertThat(medicao.isAprovada()).isFalse();
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.CORRIGINDO_INSTALACAO);
    }

    @Test
    @DisplayName("Ciclo reprovada, correção, nova instalação e nova medição aprovada")
    void testCicloDeCorrecao() {
        Long ordemId = instalar();
        medicaoService.registrar(projetoId, 27.0, 6.0, null, null, null);
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.CORRIGINDO_INSTALACAO);
        assertThatThrownBy(() -> medicaoService.registrar(projetoId, 30.0, 6.0, null, null, null))
                .isInstanceOf(RegraNegocioException.class);

        ordemServicoService.registrarCorrecao(ordemId, "Troca do registro da linha 2");
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.EXECUTANDO_INSTALACAO);
        assertThat(historico().get(historico().size() - 1).getObservacao())
                .isEqualTo("Correção registrada: Troca do registro da linha 2");

        apontamentoService.apontarHoras(ordemId, new BigDecimal("1.5"));
        ordemServicoService.concluirInstalacao(ordemId);
        relogio.avancar(Duration.ofHours(1));
        medicaoService.registrar(projetoId, 30.2, 6.05, null, null, null);
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.COLHENDO_ACEITE);

        List<MedicaoAvaliada> medicoes = medicaoService.doProjeto(projetoId);
        assertThat(medicoes).hasSize(2);
        assertThat(medicoes.get(0).getMedicao().isAprovada()).isTrue();
        assertThat(medicoes.get(1).getMedicao().isAprovada()).isFalse();
        assertThat(medicoes.get(1).getDesvioPressao()).isEqualByComparingTo("-10.00");
        assertThat(apontamentoService.horasDaOrdem(ordemId)).hasSize(2);
    }

    @Test
    @DisplayName("Foto da medição: gravada em medicao/{projeto} e servida pelo projeto")
    void testFotoDaMedicao() throws Exception {
        Long ordemId = instalar();
        byte[] foto = png(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB));

        mockMvc.perform(multipart("/campo/ordens/{id}/medicoes", ordemId)
                .file(new MockMultipartFile("foto", "manometro.png", "image/png", foto))
                .param("pressaoMedidaMca", "30").param("vazaoMedidaM3h", "6")
                .with(csrf()).with(logado("tito")))
                .andExpect(redirectedUrl("/campo/ordens/" + ordemId))
                .andExpect(flash().attribute("sucesso", containsString("dentro do parâmetro")));

        Medicao medicao = medicaoService.doProjeto(projetoId).get(0).getMedicao();
        assertThat(medicao.getFoto()).startsWith("medicao/" + projetoId + "/").endsWith(".png");
        mockMvc.perform(get("/projetos/{id}/medicoes/{m}/foto", projetoId, medicao.getId()).with(logado("almir")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(foto));
    }

    @Test
    @DisplayName("Arquivo que não é foto volta com o erro no campo e nada é gravado")
    void testMedicaoComArquivoInvalido() throws Exception {
        Long ordemId = instalar();

        mockMvc.perform(multipart("/campo/ordens/{id}/medicoes", ordemId)
                .file(new MockMultipartFile("foto", "laudo.pdf", "application/pdf", "%PDF-1.4".getBytes()))
                .param("pressaoMedidaMca", "30").param("vazaoMedidaM3h", "6")
                .with(csrf()).with(logado("tito")))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("medicaoForm", "foto"));
        assertThat(medicaoService.doProjeto(projetoId)).isEmpty();
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.VALIDANDO_MEDICAO);
    }

    // ---------- Aceite (RF18, G7) ----------

    @Test
    @DisplayName("Aceite sem assinatura não salva: campo vazio na tela e canvas em branco no service")
    void testAceiteSemAssinatura() throws Exception {
        Long ordemId = chegarAoAceite();

        mockMvc.perform(post("/campo/ordens/{id}/aceite", ordemId)
                .param("nomeResponsavel", "João Batista Ferreira").param("assinatura", "")
                .with(csrf()).with(logado("tito")))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("aceiteForm", "assinatura"))
                .andExpect(content().string(containsString("Sem a assinatura do cliente o aceite não é gravado.")));

        assertThatThrownBy(() -> aceiteService.registrarAceite(projetoId, "João", assinaturaEmBranco()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Sem a assinatura");
        assertThatThrownBy(() -> aceiteService.registrarAceite(projetoId, "João", "data:image/png;base64,bm9wZQ=="))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não é uma imagem válida");
        assertThatThrownBy(() -> aceiteService.registrarAceite(projetoId, " ", assinatura()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("nome");

        assertThat(aceiteService.aceiteDoProjeto(projetoId)).isEmpty();
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.COLHENDO_ACEITE);
    }

    @Test
    @DisplayName("Aceite assinado conclui a ordem hoje e leva a Finalizando execução (transição 13)")
    void testAceiteAssinado() throws Exception {
        Long ordemId = chegarAoAceite();
        relogio.avancar(Duration.ofDays(1));

        mockMvc.perform(post("/campo/ordens/{id}/aceite", ordemId)
                .param("nomeResponsavel", "João Batista Ferreira").param("assinatura", assinatura())
                .with(csrf()).with(logado("tito")))
                .andExpect(redirectedUrl("/campo/ordens/" + ordemId))
                .andExpect(flash().attribute("sucesso", containsString("Aceite gravado")));

        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.FINALIZANDO_EXECUCAO);
        Aceite aceite = aceiteService.aceiteDoProjeto(projetoId).orElseThrow();
        assertThat(aceite.getNomeResponsavel()).isEqualTo("João Batista Ferreira");
        assertThat(aceite.getAssinatura()).doesNotStartWith("data:");
        assertThat(TipoFoto.detectar(Base64.getDecoder().decode(aceite.getAssinatura()))).contains(TipoFoto.PNG);
        assertThat(ordemRepository.findById(ordemId).orElseThrow().getDataConclusao()).isEqualTo(HOJE.plusDays(1));
        mockMvc.perform(get("/campo/minhas-os").with(logado("tito")))
                .andExpect(content().string(not(containsString("OS 1 · PRJ-2026-0001"))));
    }

    // ---------- Pendência ----------

    @Test
    @DisplayName("Pendência técnica volta para a medição (transições 14 e 16)")
    void testPendenciaTecnica() {
        chegarAoAceite();
        aceiteService.registrarPendencia(projetoId, TipoPendencia.TECNICA, "Gotejador entupido na linha 3",
                LocalDateTime.of(2026, 11, 18, 8, 0));
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.TRATANDO_PENDENCIA);
        assertThat(aceiteService.pendenciaAberta(projetoId)).isPresent();

        aceiteService.resolverPendencia(projetoId);

        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.VALIDANDO_MEDICAO);
        assertThat(aceiteService.pendenciaAberta(projetoId)).isEmpty();
        assertThat(aceiteService.pendenciasDoProjeto(projetoId)).singleElement()
                .satisfies(p -> assertThat(p.isResolvida()).isTrue());
    }

    @Test
    @DisplayName("Pendência administrativa volta para o aceite (transições 14 e 15)")
    void testPendenciaAdministrativa() {
        chegarAoAceite();
        aceiteService.registrarPendencia(projetoId, TipoPendencia.ADMINISTRATIVA, "Dono viajando, só o caseiro na fazenda",
                LocalDateTime.of(2026, 11, 20, 14, 0));

        aceiteService.resolverPendencia(projetoId);

        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.COLHENDO_ACEITE);
        assertThat(historico().get(historico().size() - 1).getObservacao()).contains("administrativa");
    }

    @Test
    @DisplayName("Pendência sem tipo, sem descrição ou com data que já passou é rejeitada")
    void testRegrasDaPendencia() {
        chegarAoAceite();
        LocalDateTime amanha = LocalDateTime.of(2026, 11, 17, 9, 0);
        assertThatThrownBy(() -> aceiteService.registrarPendencia(projetoId, null, "x", amanha))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> aceiteService.registrarPendencia(projetoId, TipoPendencia.TECNICA, " ", amanha))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> aceiteService.registrarPendencia(projetoId, TipoPendencia.TECNICA, "x",
                LocalDateTime.of(2026, 11, 16, 8, 59)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("de agora em diante");
        assertThatThrownBy(() -> aceiteService.resolverPendencia(projetoId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Tratando pendência");
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.COLHENDO_ACEITE);
    }

    // ---------- Sobras (RF19, G8) ----------

    @Test
    @DisplayName("Sobra é separada menos apontada e nunca fica negativa")
    void testCalculoDasSobras() {
        Long ordemId = emitir();
        apontamentoService.apontarMaterial(ordemId, tuboId, new BigDecimal("17.25"));
        apontamentoService.apontarMaterial(ordemId, gotejadorId, new BigDecimal("410"));

        assertThat(sobra(tuboItem).getSobra()).isEqualByComparingTo("2.75");
        assertThat(sobra(tuboItem).isTemSobra()).isTrue();
        assertThat(sobra(gotejadorItem).getSobra()).isEqualByComparingTo("0");
        assertThat(sobra(gotejadorItem).isTemSobra()).isFalse();
    }

    @Test
    @DisplayName("Devolver menos que a sobra: o que voltou entra no estoque e o projeto fica com as sobras tratadas")
    void testDevolverSobra() {
        finalizar();
        logar(almir);

        sobrasService.devolver(projetoId, Map.of(tuboItem, new BigDecimal("2")));

        assertThat(projeto().isSobrasDevolvidas()).isTrue();
        assertThat(material(tuboId).getSaldo()).isEqualByComparingTo("32");
        assertThat(devolucoes(tuboId)).singleElement()
                .satisfies(m -> assertThat(m.getQuantidade()).isEqualByComparingTo("2"));
        assertThat(devolucoes(gotejadorId)).isEmpty();
        assertThat(sobra(tuboItem).getDevolvida()).isEqualByComparingTo("2");
        assertThat(sobra(tuboItem).getNaoDevolvida()).isEqualByComparingTo("1");
        assertThatThrownBy(() -> sobrasService.devolver(projetoId, Map.of(tuboItem, BigDecimal.ONE)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já foram tratadas");
    }

    @Test
    @DisplayName("Devolução acima da sobra ou sem a quantidade de um item com sobra é rejeitada")
    void testDevolucaoInvalida() {
        finalizar();
        logar(almir);

        assertThatThrownBy(() -> sobrasService.devolver(projetoId, Map.of(tuboItem, new BigDecimal("3.001"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("A sobra de TB-PVC-050 é 3 barra");
        assertThatThrownBy(() -> sobrasService.devolver(projetoId, Map.of()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Informe quanto voltou de TB-PVC-050");
        assertThat(projeto().isSobrasDevolvidas()).isFalse();
        assertThat(devolucoes(tuboId)).isEmpty();
    }

    @Test
    @DisplayName("Sem sobra, confirmar sem sobras marca o projeto sem mexer no estoque")
    void testSemSobras() {
        Long ordemId = emitir();
        apontamentoService.apontarMaterial(ordemId, tuboId, new BigDecimal("20"));
        apontamentoService.apontarMaterial(ordemId, gotejadorId, new BigDecimal("400"));
        ordemServicoService.concluirInstalacao(ordemId);
        medicaoService.registrar(projetoId, 30.0, 6.0, null, null, null);
        aceiteService.registrarAceite(projetoId, "João Batista Ferreira", assinatura());
        logar(almir);

        sobrasService.devolver(projetoId, Map.of());

        assertThat(projeto().isSobrasDevolvidas()).isTrue();
        assertThat(devolucoes(tuboId)).isEmpty();
        assertThat(material(tuboId).getSaldo()).isEqualByComparingTo("30");
    }

    @Test
    @DisplayName("Sobras só depois do aceite, e só o almoxarife confirma")
    void testPermissoesDasSobras() throws Exception {
        instalar();
        assertThatThrownBy(() -> sobrasService.devolver(projetoId, Map.of(tuboItem, BigDecimal.ONE)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Finalizando execução");
        medicaoService.registrar(projetoId, 30.0, 6.0, null, null, null);
        aceiteService.registrarAceite(projetoId, "João Batista Ferreira", assinatura());
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/estoque/projetos/{id}/sobras", projetoId).with(logado("carlos")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Confirmar devolução"))));
        mockMvc.perform(post("/estoque/projetos/{id}/sobras", projetoId)
                .param("devolvidas[" + tuboItem + "]", "3")
                .with(csrf()).with(logado("carlos")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/estoque/projetos/{id}/sobras", projetoId)
                .param("devolvidas[" + tuboItem + "]", "3")
                .with(csrf()).with(logado("tito")))
                .andExpect(status().isForbidden());
        assertThat(projeto().isSobrasDevolvidas()).isFalse();
    }

    // ---------- Critério de aceite: o fluxo inteiro pelas telas ----------

    @Test
    @DisplayName("Fluxo pelas telas: OS, apontamentos, medição fora e dentro, pendência, aceite e sobra")
    void testFluxoCompletoPelasTelas() throws Exception {
        // Projetista programa a execução
        mockMvc.perform(get("/engenharia/projetos/{id}/programacao", projetoId).with(logado("pedro")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tito Técnico")));
        mockMvc.perform(post("/engenharia/projetos/{id}/programacao", projetoId)
                .param("dataProgramada", "2026-11-16").param("tecnicos", tito.getId().toString())
                .with(csrf()).with(logado("pedro")))
                .andExpect(redirectedUrl("/projetos/" + projetoId));
        Long ordemId = ordemServicoService.doProjeto(projetoId).get(0).getId();
        String ordem = "/campo/ordens/" + ordemId;

        // Técnico abre a ordem no celular e aponta
        mockMvc.perform(get(ordem).with(logado("tito")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("tela-campo")))
                .andExpect(content().string(containsString("Apontar horas")))
                .andExpect(content().string(containsString("Concluir instalação")));
        mockMvc.perform(post(ordem + "/horas").param("horas", "6.5").with(csrf()).with(logado("tito")))
                .andExpect(redirectedUrl(ordem));
        mockMvc.perform(post(ordem + "/materiais").param("materialId", tuboId.toString()).param("quantidade", "18")
                .with(csrf()).with(logado("tito")))
                .andExpect(redirectedUrl(ordem));
        mockMvc.perform(post(ordem + "/materiais").param("materialId", gotejadorId.toString()).param("quantidade", "400")
                .with(csrf()).with(logado("tito")))
                .andExpect(redirectedUrl(ordem));
        mockMvc.perform(get(ordem).with(logado("tito")))
                .andExpect(content().string(containsString("6,5 h")))
                .andExpect(content().string(containsString("18 barra de TB-PVC-050")))
                .andExpect(content().string(containsString("Excluir último lançamento")));
        mockMvc.perform(post(ordem + "/conclusao").with(csrf()).with(logado("tito")))
                .andExpect(redirectedUrl(ordem));

        // Primeira medição fora, correção, segunda dentro
        mockMvc.perform(get(ordem).with(logado("tito")))
                .andExpect(content().string(containsString("29,40 a 30,60 mca")))
                .andExpect(content().string(containsString("5,88 a 6,12 m³/h")));
        mockMvc.perform(multipart(ordem + "/medicoes").param("pressaoMedidaMca", "31.5").param("vazaoMedidaM3h", "6")
                .param("latitude", "-18.9113").param("longitude", "-48.2622")
                .with(csrf()).with(logado("tito")))
                .andExpect(redirectedUrl(ordem))
                .andExpect(flash().attribute("erro", containsString("fora do parâmetro")));
        mockMvc.perform(get(ordem).with(logado("tito")))
                .andExpect(content().string(containsString("Fora do parâmetro")))
                .andExpect(content().string(containsString("(+5,00%)")))
                .andExpect(content().string(containsString("Registrar correção")));
        mockMvc.perform(post(ordem + "/correcao").param("descricao", "Regulagem da válvula reguladora")
                .with(csrf()).with(logado("tito")))
                .andExpect(redirectedUrl(ordem));
        mockMvc.perform(post(ordem + "/conclusao").with(csrf()).with(logado("tito")))
                .andExpect(redirectedUrl(ordem));
        relogio.avancar(Duration.ofMinutes(30));
        mockMvc.perform(multipart(ordem + "/medicoes").param("pressaoMedidaMca", "30.4").param("vazaoMedidaM3h", "5.95")
                .with(csrf()).with(logado("tito")))
                .andExpect(flash().attribute("sucesso", containsString("dentro do parâmetro")));

        // Cliente não assina: pendência administrativa, resolvida, e depois o aceite
        mockMvc.perform(get(ordem).with(logado("tito")))
                .andExpect(content().string(containsString("quadroAssinatura")))
                .andExpect(content().string(containsString("Cliente não assinou")));
        mockMvc.perform(post(ordem + "/pendencia").param("tipo", "ADMINISTRATIVA")
                .param("descricao", "Responsável não estava na fazenda").param("dataReprogramada", "2026-11-17T10:00")
                .with(csrf()).with(logado("tito")))
                .andExpect(redirectedUrl(ordem));
        mockMvc.perform(get(ordem).with(logado("tito")))
                .andExpect(content().string(containsString("Pendência administrativa")))
                .andExpect(content().string(containsString("Resolver pendência")));
        mockMvc.perform(post(ordem + "/pendencia/resolucao").with(csrf()).with(logado("tito")))
                .andExpect(redirectedUrl(ordem))
                .andExpect(flash().attribute("sucesso", containsString("Colha o aceite")));
        mockMvc.perform(post(ordem + "/aceite").param("nomeResponsavel", "João Batista Ferreira")
                .param("assinatura", assinatura()).with(csrf()).with(logado("tito")))
                .andExpect(redirectedUrl(ordem));
        assertThat(projeto().getSituacao()).isEqualTo(SituacaoProjeto.FINALIZANDO_EXECUCAO);

        // Almoxarife devolve a sobra de 2 barras
        mockMvc.perform(get("/estoque/projetos/{id}/sobras", projetoId).with(logado("almir")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("devolvidas[" + tuboItem + "]")))
                .andExpect(content().string(containsString("Confirmar devolução")));
        mockMvc.perform(post("/estoque/projetos/{id}/sobras", projetoId).param("devolvidas[" + tuboItem + "]", "2")
                .with(csrf()).with(logado("almir")))
                .andExpect(redirectedUrl("/projetos/" + projetoId))
                .andExpect(flash().attribute("sucesso", containsString("Devolução das sobras registrada")));
        assertThat(projeto().isSobrasDevolvidas()).isTrue();
        assertThat(material(tuboId).getSaldo()).isEqualByComparingTo("32");

        // A aba Execução mostra tudo
        mockMvc.perform(get("/projetos/{id}", projetoId).with(logado("ana")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("programada para 16/11/2026")))
                .andExpect(content().string(containsString("6,5 h no total")))
                .andExpect(content().string(containsString("Dentro do parâmetro")))
                .andExpect(content().string(containsString("Fora do parâmetro")))
                .andExpect(content().string(containsString("Assinado por João Batista Ferreira")))
                .andExpect(content().string(containsString("data:image/png;base64,")))
                .andExpect(content().string(containsString("Pendência administrativa")))
                .andExpect(content().string(containsString("Resolvida")))
                .andExpect(content().string(containsString("Não devolvida")));
        assertThat(historico()).extracting(HistoricoProjeto::getSituacaoNova).containsSubsequence(
                SituacaoProjeto.EXECUTANDO_INSTALACAO, SituacaoProjeto.VALIDANDO_MEDICAO,
                SituacaoProjeto.CORRIGINDO_INSTALACAO, SituacaoProjeto.EXECUTANDO_INSTALACAO,
                SituacaoProjeto.VALIDANDO_MEDICAO, SituacaoProjeto.COLHENDO_ACEITE, SituacaoProjeto.TRATANDO_PENDENCIA,
                SituacaoProjeto.COLHENDO_ACEITE, SituacaoProjeto.FINALIZANDO_EXECUCAO);
    }
}
