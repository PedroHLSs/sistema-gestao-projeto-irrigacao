package br.edu.iftm.irrigacao.sistema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import br.edu.iftm.irrigacao.config.RelogioDeTesteConfig;
import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;
import br.edu.iftm.irrigacao.dominio.comercial.Produtor;
import br.edu.iftm.irrigacao.dominio.comercial.ProdutorService;
import br.edu.iftm.irrigacao.dominio.comercial.Proposta;
import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;
import br.edu.iftm.irrigacao.dominio.estoque.EstoqueService;
import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.ListaTecnicaService;
import br.edu.iftm.irrigacao.dominio.estoque.Material;
import br.edu.iftm.irrigacao.dominio.estoque.MaterialService;
import br.edu.iftm.irrigacao.dominio.estoque.MovimentacaoEstoque;
import br.edu.iftm.irrigacao.dominio.estoque.TipoMovimentacao;
import br.edu.iftm.irrigacao.dominio.projeto.HistoricoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.persistencia.HistoricoProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.MaterialRepository;
import br.edu.iftm.irrigacao.persistencia.MovimentacaoEstoqueRepository;
import br.edu.iftm.irrigacao.persistencia.OrdemServicoRepository;
import br.edu.iftm.irrigacao.persistencia.ProdutorRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.PropostaRepository;
import br.edu.iftm.irrigacao.persistencia.PropriedadeRepository;
import br.edu.iftm.irrigacao.persistencia.RelogioAjustavel;
import br.edu.iftm.irrigacao.persistencia.UsuarioRepository;

/**
 * Apoio dos testes de sistema: cada passo do processo TO-BE é uma requisição HTTP feita pelo perfil
 * que responde por ele, com CSRF, como o navegador faria.
 *
 * <p>Sem {@code @Transactional}: cada requisição abre e fecha a própria transação, como em produção.
 * Por isso estes testes usam um banco H2 só deles, e cada teste cria os próprios materiais e projetos.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sistema;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
        + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelogioDeTesteConfig.class)
abstract class ProcessoSistemaBase {

    protected static final String VENDEDOR = "vera.vendas";
    protected static final String PROJETISTA = "paulo.projetos";
    protected static final String COMPRADOR = "carla.compras";
    protected static final String ALMOXARIFE = "alberto.almox";
    protected static final String TECNICO = "tiago.campo";
    protected static final String FINANCEIRO = "fernanda.fin";
    protected static final String DIRETORIA = "dora.diretoria";
    protected static final String ADMINISTRADOR = "adm.sistema";

    protected static final String CLIENTE = "João Batista Ferreira";

    private static final String SENHA = "senha-de-teste-1";
    private static final String CPF_DO_PRODUTOR = "52998224725";
    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    @Autowired protected MockMvc mockMvc;
    @Autowired protected RelogioAjustavel relogio;
    @Autowired protected UserDetailsService userDetailsService;
    @Autowired protected UsuarioService usuarioService;
    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected ProdutorService produtorService;
    @Autowired protected ProdutorRepository produtorRepository;
    @Autowired protected PropriedadeRepository propriedadeRepository;
    @Autowired protected MaterialService materialService;
    @Autowired protected MaterialRepository materialRepository;
    @Autowired protected EstoqueService estoqueService;
    @Autowired protected ListaTecnicaService listaTecnicaService;
    @Autowired protected ProjetoRepository projetoRepository;
    @Autowired protected PropostaRepository propostaRepository;
    @Autowired protected OrdemServicoRepository ordemRepository;
    @Autowired protected HistoricoProjetoRepository historicoRepository;
    @Autowired protected MovimentacaoEstoqueRepository movimentacaoRepository;

    protected Long produtorId;
    protected Long propriedadeId;

    @BeforeEach
    void prepararCenario() {
        relogio.definir(RelogioDeTesteConfig.INICIO);
        garantirUsuario("Vera Vendedora", VENDEDOR, Perfil.VENDEDOR);
        garantirUsuario("Paulo Projetista", PROJETISTA, Perfil.PROJETISTA);
        garantirUsuario("Carla Compradora", COMPRADOR, Perfil.COMPRADOR);
        garantirUsuario("Alberto Almoxarife", ALMOXARIFE, Perfil.ALMOXARIFE);
        garantirUsuario("Tiago Técnico", TECNICO, Perfil.TECNICO_CAMPO);
        garantirUsuario("Fernanda Financeiro", FINANCEIRO, Perfil.FINANCEIRO);
        garantirUsuario("Dora Diretora", DIRETORIA, Perfil.DIRETORIA);
        garantirUsuario("Adm do Sistema", ADMINISTRADOR, Perfil.ADMINISTRADOR);
        if (!produtorRepository.existsByCpfCnpj(CPF_DO_PRODUTOR)) {
            Produtor produtor = produtorService.criar(CLIENTE, CPF_DO_PRODUTOR, null, null);
            produtorService.criarPropriedade(produtor.getId(), "Fazenda Boa Vista", "Uberlândia", null, null, null);
        }
        Propriedade fazenda = propriedadeRepository.listarComProdutor().stream()
                .filter(p -> CPF_DO_PRODUTOR.equals(p.getProdutor().getCpfCnpj()))
                .findFirst().orElseThrow();
        produtorId = fazenda.getProdutor().getId();
        propriedadeId = fazenda.getId();
    }

    private void garantirUsuario(String nome, String login, Perfil perfil) {
        if (!usuarioRepository.existsByLogin(login)) {
            usuarioService.criar(nome, login, SENHA, perfil);
        }
    }

    // ---------- Requisições ----------

    protected RequestPostProcessor logado(String login) {
        return user(userDetailsService.loadUserByUsername(login));
    }

    private static MockHttpServletRequestBuilder comParametros(MockHttpServletRequestBuilder requisicao,
            String... parametros) {
        for (int i = 0; i < parametros.length; i += 2) {
            requisicao.param(parametros[i], parametros[i + 1]);
        }
        return requisicao;
    }

    /** POST que precisa dar certo: redireciona sem mensagem de erro. @return o destino do redirecionamento */
    protected String postar(String login, String url, String... parametros) throws Exception {
        return mockMvc.perform(comParametros(post(url), parametros).with(csrf()).with(logado(login)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("erro", nullValue()))
                .andReturn().getResponse().getRedirectedUrl();
    }

    /** POST que a regra de negócio recusa: volta com a mensagem de erro no topo da página. */
    protected void postarRecusado(String login, String url, String trechoDoErro, String... parametros) throws Exception {
        mockMvc.perform(comParametros(post(url), parametros).with(csrf()).with(logado(login)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("erro", containsString(trechoDoErro)));
    }

    /** POST que o perfil não pode fazer (RNF04). */
    protected void postarProibido(String login, String url, String... parametros) throws Exception {
        mockMvc.perform(comParametros(post(url), parametros).with(csrf()).with(logado(login)))
                .andExpect(status().isForbidden());
    }

    /** GET de uma tela, que precisa abrir. @return o HTML */
    protected String abrir(String login, String url) throws Exception {
        return mockMvc.perform(get(url).with(logado(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    protected static Long idNoFim(String url) {
        return Long.valueOf(url.substring(url.lastIndexOf('/') + 1));
    }

    protected void passarTempo() {
        relogio.avancar(Duration.ofMinutes(10));
    }

    protected LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    // ---------- Materiais ----------

    /** Material novo, com código único neste banco e saldo de entrada. */
    protected Long materialComSaldo(String preco, String custo, String saldo) {
        String codigo = "MAT-" + SEQUENCIA.incrementAndGet();
        Long id = materialService.criar(codigo, "Material " + codigo, "un", new BigDecimal(preco), new BigDecimal(custo))
                .getId();
        if (new BigDecimal(saldo).signum() > 0) {
            estoqueService.entradaCompra(id, new BigDecimal(saldo));
        }
        return id;
    }

    // ---------- Comercial: atividades 1 e 2 ----------

    protected Long abrirProjeto() throws Exception {
        return idNoFim(postar(VENDEDOR, "/comercial/projetos", "produtorId", produtorId.toString(), "propriedadeId",
                propriedadeId.toString()));
    }

    protected void agendarVisita(Long projetoId) throws Exception {
        postar(VENDEDOR, "/comercial/projetos/" + projetoId + "/visita", "dataAgendada",
                LocalDateTime.now(relogio).plusDays(1).withSecond(0).withNano(0).toString());
    }

    // ---------- Campo e engenharia: atividades 3 a 5 ----------

    /** Atividade 3 com todos os campos que o G1 exige. */
    protected void preencherLevantamento(Long projetoId) throws Exception {
        MockMultipartFile foto = new MockMultipartFile("novasFotos", "fonte.png", "image/png", png(Color.GREEN));
        mockMvc.perform(comParametros(multipart("/campo/projetos/" + projetoId + "/levantamento").file(foto),
                "acao", "enviar",
                "dataVisita", LocalDateTime.now(relogio).withSecond(0).withNano(0).toString(),
                "cultura", "Café",
                "areaIrrigadaHa", "12.5",
                "fonteAgua", "POCO_ARTESIANO",
                "vazaoFonte", "8",
                "topografia", "Plana",
                "latitude", "-18.91",
                "longitude", "-48.26")
                .with(csrf()).with(logado(TECNICO)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("erro", nullValue()));
    }

    /** Atividade 5: 30 mca, 6 m³/h e tolerância de 2% (faixa de 29,4 a 30,6 e de 5,88 a 6,12). */
    protected void registrarDimensionamento(Long projetoId) throws Exception {
        postar(PROJETISTA, "/engenharia/projetos/" + projetoId + "/dimensionamento", "pressaoProjetoMca", "30",
                "vazaoProjetoM3h", "6", "toleranciaPercentual", "2");
    }

    protected void adicionarItem(Long projetoId, Long materialId, String quantidade) throws Exception {
        postar(PROJETISTA, "/engenharia/projetos/" + projetoId + "/itens", "materialId", materialId.toString(),
                "quantidadePrevista", quantidade);
    }

    protected void concluirListaTecnica(Long projetoId) throws Exception {
        postar(PROJETISTA, "/engenharia/projetos/" + projetoId + "/concluir");
    }

    /** Atividades 1 a 5 com um único material na lista técnica: o projeto termina em ORCANDO. */
    protected Long projetoOrcando(Long materialId, String quantidade) throws Exception {
        Long projetoId = abrirProjeto();
        agendarVisita(projetoId);
        preencherLevantamento(projetoId);
        registrarDimensionamento(projetoId);
        adicionarItem(projetoId, materialId, quantidade);
        concluirListaTecnica(projetoId);
        return projetoId;
    }

    // ---------- Proposta: atividades 8 a 11 ----------

    /** @return o id da proposta gerada */
    protected Long gerarProposta(Long projetoId, String desconto) throws Exception {
        return idNoFim(postar(VENDEDOR, "/comercial/projetos/" + projetoId + "/propostas", "horasPrevistas", "10",
                "percentualDesconto", desconto));
    }

    protected void enviarProposta(Long propostaId) throws Exception {
        postar(VENDEDOR, "/comercial/propostas/" + propostaId + "/enviar");
    }

    protected void aprovarProposta(Long propostaId) throws Exception {
        postar(VENDEDOR, "/comercial/propostas/" + propostaId + "/aprovacao-cliente");
    }

    /** Proposta sem desconto enviada e aprovada: o projeto termina em PREPARANDO_MATERIAL. */
    protected Long projetoAprovado(Long materialId, String quantidade) throws Exception {
        Long projetoId = projetoOrcando(materialId, quantidade);
        Long propostaId = gerarProposta(projetoId, "0");
        enviarProposta(propostaId);
        aprovarProposta(propostaId);
        return projetoId;
    }

    // ---------- Almoxarifado: atividades 13 e 14 ----------

    protected ItemProjeto item(Long projetoId, Long materialId) {
        return listaTecnicaService.listar(projetoId).stream()
                .filter(item -> item.getMaterial().getId().equals(materialId))
                .findFirst().orElseThrow();
    }

    protected void separar(Long projetoId, Long materialId, String quantidade) throws Exception {
        postar(ALMOXARIFE, "/estoque/projetos/" + projetoId + "/separacao",
                "quantidades[" + item(projetoId, materialId).getId() + "]", quantidade);
    }

    /** G5: libera a execução com o que já foi separado (pelo menos um item). */
    protected void liberarExecucao(Long projetoId) throws Exception {
        postar(ALMOXARIFE, "/estoque/projetos/" + projetoId + "/separacao/liberacao");
    }

    /** Separa exatamente o previsto e libera a execução: o projeto termina em PROGRAMANDO_EXECUCAO. */
    protected Long projetoSeparado(Long materialId, String quantidade) throws Exception {
        Long projetoId = projetoAprovado(materialId, quantidade);
        separar(projetoId, materialId, quantidade);
        liberarExecucao(projetoId);
        return projetoId;
    }

    // ---------- Execução: atividades 15 a 21 ----------

    /** @return o id da ordem de serviço emitida para o técnico */
    protected Long emitirOrdem(Long projetoId) throws Exception {
        Long tecnicoId = usuarioRepository.findByLogin(TECNICO).orElseThrow().getId();
        postar(PROJETISTA, "/engenharia/projetos/" + projetoId + "/programacao", "dataProgramada", hoje().toString(),
                "tecnicos", tecnicoId.toString());
        return ordemRepository.findByProjetoIdOrderByNumero(projetoId).getLast().getId();
    }

    protected void apontarMaterial(Long ordemId, Long materialId, String quantidade) throws Exception {
        postar(TECNICO, "/campo/ordens/" + ordemId + "/materiais", "materialId", materialId.toString(), "quantidade",
                quantidade);
    }

    protected void apontarHoras(Long ordemId, String horas) throws Exception {
        postar(TECNICO, "/campo/ordens/" + ordemId + "/horas", "horas", horas);
    }

    protected void concluirInstalacao(Long ordemId) throws Exception {
        postar(TECNICO, "/campo/ordens/" + ordemId + "/conclusao");
    }

    protected void medirDentroDoParametro(Long ordemId) throws Exception {
        postar(TECNICO, "/campo/ordens/" + ordemId + "/medicoes", "pressaoMedidaMca", "30", "vazaoMedidaM3h", "6");
    }

    protected void colherAceite(Long ordemId) throws Exception {
        postar(TECNICO, "/campo/ordens/" + ordemId + "/aceite", "nomeResponsavel", CLIENTE, "assinatura", assinatura());
    }

    /** Instalação concluída e medição aprovada: o projeto termina em COLHENDO_ACEITE. @return a ordem */
    protected Long ordemComMedicaoAprovada(Long projetoId, Long materialId, String apontado) throws Exception {
        Long ordemId = emitirOrdem(projetoId);
        apontarHoras(ordemId, "8");
        apontarMaterial(ordemId, materialId, apontado);
        concluirInstalacao(ordemId);
        medirDentroDoParametro(ordemId);
        return ordemId;
    }

    // ---------- Consultas para conferir o resultado ----------

    protected SituacaoProjeto situacao(Long projetoId) {
        return projeto(projetoId).getSituacao();
    }

    protected Projeto projeto(Long projetoId) {
        return projetoRepository.findById(projetoId).orElseThrow();
    }

    protected Proposta proposta(Long propostaId) {
        return propostaRepository.findById(propostaId).orElseThrow();
    }

    protected Material material(Long materialId) {
        return materialRepository.findById(materialId).orElseThrow();
    }

    protected void conferirSaldo(Long materialId, String saldo, String reservado) {
        Material material = material(materialId);
        assertThat(material.getSaldo()).as("saldo de %s", material.getCodigo()).isEqualByComparingTo(saldo);
        assertThat(material.getSaldoReservado()).as("reservado de %s", material.getCodigo())
                .isEqualByComparingTo(reservado);
    }

    /** Soma das movimentações do projeto, por tipo, de um material. */
    protected Map<TipoMovimentacao, BigDecimal> movimentos(Long projetoId, Long materialId) {
        return movimentacaoRepository.listarPorProjetoComMaterial(projetoId).stream()
                .filter(movimento -> movimento.getMaterial().getId().equals(materialId))
                .collect(Collectors.toMap(MovimentacaoEstoque::getTipo, MovimentacaoEstoque::getQuantidade,
                        BigDecimal::add));
    }

    /** As situações por onde o projeto passou, em ordem. */
    protected List<SituacaoProjeto> caminho(Long projetoId) {
        return historicoRepository.listarPorProjeto(projetoId).stream().map(HistoricoProjeto::getSituacaoNova).toList();
    }

    // ---------- Imagens ----------

    /** Assinatura de verdade: PNG em base64 com um traço, como o quadro da tela envia. */
    protected static String assinatura() {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png(Color.DARK_GRAY));
    }

    private static byte[] png(Color cor) {
        BufferedImage imagem = new BufferedImage(120, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pincel = imagem.createGraphics();
        pincel.setColor(cor);
        pincel.drawLine(5, 40, 115, 15);
        pincel.dispose();
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            ImageIO.write(imagem, "png", saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
