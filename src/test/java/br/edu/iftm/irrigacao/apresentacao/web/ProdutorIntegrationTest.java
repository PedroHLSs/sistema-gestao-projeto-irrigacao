package br.edu.iftm.irrigacao.apresentacao.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.config.RelogioDeTesteConfig;
import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;
import br.edu.iftm.irrigacao.dominio.comercial.Produtor;
import br.edu.iftm.irrigacao.dominio.comercial.ProdutorService;
import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;
import br.edu.iftm.irrigacao.persistencia.ProdutorRepository;
import br.edu.iftm.irrigacao.persistencia.PropriedadeRepository;
import jakarta.persistence.EntityManager;

/**
 * Cadastro de produtor e propriedade (RF01) de ponta a ponta: controller, service e banco H2, com
 * uma vendedora logada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(RelogioDeTesteConfig.class)
class ProdutorIntegrationTest {

    private static final String SENHA = "senha-da-ana-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private ProdutorService produtorService;

    @Autowired
    private ProdutorRepository produtorRepository;

    @Autowired
    private PropriedadeRepository propriedadeRepository;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void criaAVendedora() {
        usuarioService.criar("Ana Vendedora", "ana", SENHA, Perfil.VENDEDOR);
    }

    private RequestPostProcessor anaLogada() {
        return user(userDetailsService.loadUserByUsername("ana"));
    }

    private MockHttpServletRequestBuilder postProdutor(String url, String nome, String cpfCnpj) {
        return post(url).with(csrf()).with(anaLogada())
                .param("nome", nome)
                .param("cpfCnpj", cpfCnpj)
                .param("telefone", "(34) 99999-0000")
                .param("email", "contato@exemplo.com");
    }

    private void limparCache() {
        em.flush();
        em.clear();
    }

    // ---------- Produtor ----------

    @Test
    @DisplayName("Cria produtor com o CPF gravado sem pontuação e mostrado formatado")
    void testCriarProdutor() throws Exception {
        mockMvc.perform(postProdutor("/comercial/produtores", "  João Batista Ferreira ", "529.982.247-25"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("sucesso",
                        "Produtor João Batista Ferreira cadastrado. Agora cadastre a propriedade dele."));

        limparCache();
        List<Produtor> produtores = produtorRepository.findAllByOrderByNome();
        assertThat(produtores).hasSize(1);
        Produtor joao = produtores.get(0);
        assertThat(joao.getNome()).isEqualTo("João Batista Ferreira");
        assertThat(joao.getCpfCnpj()).isEqualTo("52998224725");
        assertThat(joao.getCriadoPor()).isNotNull();

        mockMvc.perform(get("/comercial/produtores").with(anaLogada()))
                .andExpect(status().isOk())
                .andExpect(view().name("comercial/listarProdutores"))
                .andExpect(content().string(containsString("João Batista Ferreira")))
                .andExpect(content().string(containsString("529.982.247-25")));
    }

    @Test
    @DisplayName("CPF com dígito verificador errado aparece como erro no campo e nada é gravado")
    void testCriarProdutorCpfInvalido() throws Exception {
        mockMvc.perform(postProdutor("/comercial/produtores", "João", "529.982.247-24"))
                .andExpect(status().isOk())
                .andExpect(view().name("comercial/formProdutor"))
                .andExpect(model().attributeHasFieldErrors("produtor", "cpfCnpj"))
                .andExpect(content().string(containsString("CPF ou CNPJ inválido. Confira os números digitados.")));

        assertThat(produtorRepository.count()).isZero();
    }

    @Test
    @DisplayName("CPF já cadastrado, mesmo digitado sem pontuação, aparece como erro no campo")
    void testCriarProdutorCpfRepetido() throws Exception {
        produtorService.criar("João Batista Ferreira", "529.982.247-25", null, null);

        mockMvc.perform(postProdutor("/comercial/produtores", "Outro João", "52998224725"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("produtor", "cpfCnpj"))
                .andExpect(content().string(containsString("Já existe um produtor com este CPF/CNPJ.")));

        assertThat(produtorRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Nome vazio, CPF vazio e e-mail fora do padrão voltam ao formulário")
    void testCriarProdutorCamposInvalidos() throws Exception {
        mockMvc.perform(post("/comercial/produtores").with(csrf()).with(anaLogada())
                        .param("nome", "")
                        .param("cpfCnpj", "")
                        .param("email", "sem-arroba"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("produtor", "nome", "cpfCnpj", "email"))
                .andExpect(content().string(containsString("Informe o nome.")))
                .andExpect(content().string(containsString("Informe o CPF ou o CNPJ.")));
    }

    @Test
    @DisplayName("CNPJ, inclusive o alfanumérico, é aceito e mostrado formatado")
    void testCriarProdutorCnpj() throws Exception {
        mockMvc.perform(postProdutor("/comercial/produtores", "Agropecuária Santa Rita Ltda", "11.222.333/0001-81"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(postProdutor("/comercial/produtores", "Irrigação Nova Ltda", "12.abc.345/01de-35"))
                .andExpect(status().is3xxRedirection());

        limparCache();
        assertThat(produtorRepository.findAllByOrderByNome()).extracting(Produtor::getCpfCnpj)
                .containsExactly("11222333000181", "12ABC34501DE35");
        mockMvc.perform(get("/comercial/produtores").with(anaLogada()))
                .andExpect(content().string(containsString("11.222.333/0001-81")))
                .andExpect(content().string(containsString("12.ABC.345/01DE-35")));
    }

    @Test
    @DisplayName("Busca por parte do nome, sem diferença de maiúsculas, e por parte do CPF/CNPJ")
    void testBuscarProdutor() throws Exception {
        produtorService.criar("João Batista Ferreira", "529.982.247-25", null, null);
        produtorService.criar("Maria Aparecida Souza", "111.444.777-35", null, null);
        produtorService.criar("Agropecuária Santa Rita Ltda", "11.222.333/0001-81", null, null);

        assertThat(produtorService.pesquisar("BATISTA")).extracting(Produtor::getNome)
                .containsExactly("João Batista Ferreira");
        assertThat(produtorService.pesquisar("98224")).extracting(Produtor::getNome)
                .containsExactly("João Batista Ferreira");
        assertThat(produtorService.pesquisar("111.444")).extracting(Produtor::getNome)
                .containsExactly("Maria Aparecida Souza");
        assertThat(produtorService.pesquisar("11.222.333/0001")).extracting(Produtor::getNome)
                .containsExactly("Agropecuária Santa Rita Ltda");
        assertThat(produtorService.pesquisar("  ")).hasSize(3);

        mockMvc.perform(get("/comercial/produtores").param("busca", "maria").with(anaLogada()))
                .andExpect(content().string(containsString("Maria Aparecida Souza")))
                .andExpect(content().string(not(containsString("João Batista Ferreira"))));
        mockMvc.perform(get("/comercial/produtores").param("busca", "Ninguém").with(anaLogada()))
                .andExpect(content().string(containsString("Nenhum produtor encontrado para essa busca.")));
    }

    @Test
    @DisplayName("Edita o produtor mantendo o próprio CPF, e não aceita o CPF de outro produtor")
    void testEditarProdutor() throws Exception {
        Produtor joao = produtorService.criar("João Batista Ferreira", "529.982.247-25", null, null);
        produtorService.criar("Maria Aparecida Souza", "111.444.777-35", null, null);
        String url = "/comercial/produtores/" + joao.getId();

        mockMvc.perform(get(url + "/editar").with(anaLogada()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"529.982.247-25\"")))
                .andExpect(content().string(containsString("Nova propriedade")));

        mockMvc.perform(postProdutor(url, "João Batista Ferreira Filho", "52998224725"))
                .andExpect(redirectedUrl(url + "/editar"))
                .andExpect(flash().attribute("sucesso", "Produtor João Batista Ferreira Filho alterado."));

        mockMvc.perform(postProdutor(url, "João", "111.444.777-35"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("produtor", "cpfCnpj"));

        limparCache();
        Produtor lido = produtorRepository.findById(joao.getId()).orElseThrow();
        assertThat(lido.getNome()).isEqualTo("João Batista Ferreira Filho");
        assertThat(lido.getCpfCnpj()).isEqualTo("52998224725");
        assertThat(lido.getTelefone()).isEqualTo("(34) 99999-0000");
    }

    @Test
    @DisplayName("Produtor que não existe responde 404")
    void testProdutorInexistente() throws Exception {
        mockMvc.perform(get("/comercial/produtores/99999/editar").with(anaLogada()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/comercial/produtores/99999/propriedades/nova").with(anaLogada()))
                .andExpect(status().isNotFound());
    }

    // ---------- Propriedade ----------

    @Test
    @DisplayName("Cria e edita a propriedade do produtor, que aparece na página dele")
    void testCriarEEditarPropriedade() throws Exception {
        Produtor joao = produtorService.criar("João Batista Ferreira", "529.982.247-25", null, null);
        String base = "/comercial/produtores/" + joao.getId();

        mockMvc.perform(post(base + "/propriedades").with(csrf()).with(anaLogada())
                        .param("nome", "Fazenda Boa Vista")
                        .param("municipio", "Uberlândia")
                        .param("latitude", "-18.9113")
                        .param("longitude", "-48.2622")
                        .param("areaTotalHa", "42.5"))
                .andExpect(redirectedUrl(base + "/editar"))
                .andExpect(flash().attribute("sucesso", "Propriedade Fazenda Boa Vista cadastrada."));

        limparCache();
        Propriedade fazenda = propriedadeRepository.findByProdutorIdOrderByNome(joao.getId()).get(0);
        assertThat(fazenda.getProdutor().getId()).isEqualTo(joao.getId());
        assertThat(fazenda.getLatitude()).isEqualTo(-18.9113);
        assertThat(fazenda.getAreaTotalHa()).isEqualTo(42.5);

        mockMvc.perform(get(base + "/editar").with(anaLogada()))
                .andExpect(content().string(containsString("Fazenda Boa Vista")))
                .andExpect(content().string(containsString("42,50")))
                .andExpect(content().string(containsString("/comercial/projetos/novo?propriedadeId=" + fazenda.getId())));

        mockMvc.perform(post(base + "/propriedades/" + fazenda.getId()).with(csrf()).with(anaLogada())
                        .param("nome", "Fazenda Boa Vista II")
                        .param("municipio", "Araguari"))
                .andExpect(redirectedUrl(base + "/editar"));

        limparCache();
        Propriedade lida = propriedadeRepository.findById(fazenda.getId()).orElseThrow();
        assertThat(lida.getNome()).isEqualTo("Fazenda Boa Vista II");
        assertThat(lida.getMunicipio()).isEqualTo("Araguari");
        assertThat(lida.getLatitude()).isNull();
    }

    @Test
    @DisplayName("Coordenada fora da faixa, área negativa e texto em campo numérico aparecem no campo")
    void testPropriedadeCamposInvalidos() throws Exception {
        Produtor joao = produtorService.criar("João Batista Ferreira", "529.982.247-25", null, null);

        mockMvc.perform(post("/comercial/produtores/" + joao.getId() + "/propriedades").with(csrf()).with(anaLogada())
                        .param("nome", "")
                        .param("municipio", "Uberlândia")
                        .param("latitude", "95")
                        .param("longitude", "abc")
                        .param("areaTotalHa", "-3"))
                .andExpect(status().isOk())
                .andExpect(view().name("comercial/formPropriedade"))
                .andExpect(model().attributeHasFieldErrors("propriedade", "nome", "latitude", "longitude", "areaTotalHa"))
                .andExpect(content().string(containsString("A latitude vai de -90 a 90.")))
                .andExpect(content().string(containsString("Informe um número, como -18.9113.")))
                .andExpect(content().string(containsString("A área deve ser maior que zero.")));

        assertThat(propriedadeRepository.count()).isZero();
    }

    @Test
    @DisplayName("Propriedade de outro produtor responde 404, para ver e para alterar")
    void testPropriedadeDeOutroProdutor() throws Exception {
        Produtor joao = produtorService.criar("João Batista Ferreira", "529.982.247-25", null, null);
        Produtor maria = produtorService.criar("Maria Aparecida Souza", "111.444.777-35", null, null);
        Propriedade daMaria = produtorService.criarPropriedade(maria.getId(), "Sítio Esperança", "Araguari", null,
                null, null);
        String urlErrada = "/comercial/produtores/" + joao.getId() + "/propriedades/" + daMaria.getId();

        mockMvc.perform(get(urlErrada + "/editar").with(anaLogada()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(urlErrada).with(csrf()).with(anaLogada())
                        .param("nome", "Tomada")
                        .param("municipio", "Uberlândia"))
                .andExpect(status().isNotFound());

        limparCache();
        assertThat(propriedadeRepository.findById(daMaria.getId()).orElseThrow().getNome()).isEqualTo("Sítio Esperança");
    }

    // ---------- Acesso ----------

    @Test
    @DisplayName("Só o comercial cadastra produtor: projetista e técnico de campo recebem acesso negado")
    void testAcessoAoCadastro() throws Exception {
        usuarioService.criar("Paulo Projetista", "paulo", SENHA, Perfil.PROJETISTA);
        usuarioService.criar("Tiago Técnico", "tiago", SENHA, Perfil.TECNICO_CAMPO);
        usuarioService.criar("Diana Diretora", "diana", SENHA, Perfil.DIRETORIA);

        for (String login : List.of("paulo", "tiago")) {
            RequestPostProcessor logado = user(userDetailsService.loadUserByUsername(login));
            mockMvc.perform(get("/comercial/produtores").with(logado))
                    .andExpect(status().isForbidden())
                    .andExpect(forwardedUrl("/acesso-negado"));
            mockMvc.perform(post("/comercial/produtores").with(csrf()).with(logado)
                            .param("nome", "Intruso")
                            .param("cpfCnpj", "529.982.247-25"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/comercial/produtores").with(user(userDetailsService.loadUserByUsername("diana"))))
                .andExpect(status().isOk());

        assertThat(produtorRepository.count()).isZero();
    }
}
