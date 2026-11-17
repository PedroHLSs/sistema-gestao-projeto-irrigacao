package br.edu.iftm.irrigacao.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;

/**
 * Login, logout e regras de acesso por perfil (RNF04), com a aplicação inteira no H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SegurancaIntegrationTest {

    private static final String SENHA = "senha-da-ana-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    private static RequestPostProcessor comPerfil(Perfil perfil) {
        return user("usuario.teste").authorities(new SimpleGrantedAuthority(perfil.name()));
    }

    // ---------- Login e logout ----------

    @Test
    @DisplayName("Login válido autentica com o perfil como authority e vai para o início")
    void testLoginValido() throws Exception {
        usuarioService.criar("Ana Vendedora", "ana", SENHA, Perfil.VENDEDOR);

        mockMvc.perform(formLogin("/login").user("ana").password(SENHA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("ana")
                        .withAuthorities(List.of(new SimpleGrantedAuthority("VENDEDOR"))));
    }

    @Test
    @DisplayName("Login com a senha errada volta para a página de login com erro")
    void testLoginSenhaErrada() throws Exception {
        usuarioService.criar("Ana Vendedora", "ana", SENHA, Perfil.VENDEDOR);

        mockMvc.perform(formLogin("/login").user("ana").password("senha-errada"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("Login que não existe volta para a página de login com erro")
    void testLoginInexistente() throws Exception {
        mockMvc.perform(formLogin("/login").user("ninguem").password(SENHA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("Usuário inativo é bloqueado mesmo com a senha certa, e volta a entrar quando reativado")
    void testUsuarioInativoBloqueado() throws Exception {
        Usuario ana = usuarioService.criar("Ana Vendedora", "ana", SENHA, Perfil.VENDEDOR);
        usuarioService.desativar(ana.getId());

        mockMvc.perform(formLogin("/login").user("ana").password(SENHA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());

        usuarioService.ativar(ana.getId());

        mockMvc.perform(formLogin("/login").user("ana").password(SENHA))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("ana"));
    }

    @Test
    @DisplayName("POST /login sem o token CSRF não autentica")
    void testLoginSemCsrf() throws Exception {
        usuarioService.criar("Ana Vendedora", "ana", SENHA, Perfil.VENDEDOR);

        mockMvc.perform(post("/login").param("username", "ana").param("password", SENHA))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("GET /login - Página de login é pública e tem o formulário")
    void testPaginaDeLoginPublica() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("acesso/login"))
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                // sem login o cabeçalho não mostra menu nem botão de sair
                .andExpect(content().string(not(containsString("Projetos</a>"))))
                .andExpect(content().string(not(containsString("action=\"/logout\""))));
    }

    @Test
    @DisplayName("GET /login?error - Mostra a mensagem de login inválido")
    void testPaginaDeLoginComErro() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login ou senha inválidos.")));
    }

    @Test
    @DisplayName("CSS e JS abrem sem login")
    void testEstaticosPublicos() throws Exception {
        mockMvc.perform(get("/css/app.css")).andExpect(status().isOk());
        mockMvc.perform(get("/js/app.js")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sem login, a página inicial manda para o login")
    void testInicioSemLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Não existe página de auto-registro")
    void testSemAutoRegistro() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/registro").with(comPerfil(Perfil.ADMINISTRADOR)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = { "VENDEDOR" })
    @DisplayName("POST /logout encerra a sessão e volta para o login")
    void testLogout() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(unauthenticated());
    }

    // ---------- Cabeçalho e menu por perfil ----------

    @Test
    @DisplayName("Depois do login o cabeçalho mostra o nome e o perfil, e o menu é o do perfil")
    void testCabecalhoEMenuDoVendedor() throws Exception {
        usuarioService.criar("Ana Vendedora", "ana", SENHA, Perfil.VENDEDOR);
        MvcResult login = mockMvc.perform(formLogin("/login").user("ana").password(SENHA)).andReturn();
        MockHttpSession sessao = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ana Vendedora")))
                .andExpect(content().string(containsString(">Vendedor</span>")))
                .andExpect(content().string(containsString("action=\"/logout\"")))
                .andExpect(content().string(containsString("Projetos</a>")))
                .andExpect(content().string(containsString("Comercial</a>")))
                .andExpect(content().string(not(containsString("Engenharia</a>"))))
                .andExpect(content().string(not(containsString("Estoque</a>"))))
                .andExpect(content().string(not(containsString("Campo</a>"))))
                .andExpect(content().string(not(containsString("Financeiro</a>"))))
                .andExpect(content().string(not(containsString("Diretoria</a>"))))
                .andExpect(content().string(not(containsString("Usuários</a>"))));
    }

    /** Itens de menu que cada perfil vê, além de Início e Projetos, que todos veem. */
    private static final Map<Perfil, Set<String>> MENU = Map.of(
            Perfil.VENDEDOR, Set.of("Comercial"),
            Perfil.PROJETISTA, Set.of("Engenharia", "Campo"),
            Perfil.COMPRADOR, Set.of("Estoque"),
            Perfil.ALMOXARIFE, Set.of("Estoque"),
            Perfil.TECNICO_CAMPO, Set.of("Campo"),
            Perfil.FINANCEIRO, Set.of("Financeiro"),
            Perfil.DIRETORIA, Set.of("Comercial", "Financeiro", "Diretoria"),
            Perfil.ADMINISTRADOR,
            Set.of("Comercial", "Engenharia", "Estoque", "Campo", "Financeiro", "Diretoria", "Usuários"));

    @ParameterizedTest(name = "menu do perfil {0}")
    @MethodSource("perfis")
    @DisplayName("Cada perfil vê no menu só as áreas que pode acessar")
    void testMenuPorPerfil(Perfil perfil) throws Exception {
        String pagina = mockMvc.perform(get("/").with(comPerfil(perfil)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(pagina).contains("Início</a>", "Projetos</a>");
        for (String item : MENU.get(Perfil.ADMINISTRADOR)) {
            if (MENU.get(perfil).contains(item)) {
                assertThat(pagina).contains(item + "</a>");
            } else {
                assertThat(pagina).doesNotContain(item + "</a>");
            }
        }
    }

    static Stream<Perfil> perfis() {
        return Stream.of(Perfil.values());
    }

    // ---------- Acesso negado por prefixo ----------

    @Test
    @DisplayName("/projetos/** - Sem login é negado e vai para a página de login")
    void testProjetosSemLogin() throws Exception {
        mockMvc.perform(get("/projetos/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @ParameterizedTest(name = "{0} nega o perfil {1}")
    @CsvSource({
            "/comercial/propostas,    PROJETISTA",
            "/engenharia/projetos,    VENDEDOR",
            "/estoque/materiais,      FINANCEIRO",
            "/campo/ordens,           ALMOXARIFE",
            "/financeiro/faturamento, VENDEDOR",
            "/diretoria/aprovacoes,   FINANCEIRO",
            "/admin/usuarios,         DIRETORIA" })
    @DisplayName("Perfil fora da tabela recebe acesso negado no prefixo")
    void testAcessoNegadoPorPrefixo(String url, Perfil perfil) throws Exception {
        mockMvc.perform(get(url).with(comPerfil(perfil)))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acesso-negado"));
    }

    @ParameterizedTest(name = "POST {0} nega o perfil {1}")
    @CsvSource({
            "/comercial/propostas,    PROJETISTA",
            "/admin/usuarios,         DIRETORIA" })
    @DisplayName("O acesso negado vale também para POST")
    void testAcessoNegadoEmPost(String url, Perfil perfil) throws Exception {
        mockMvc.perform(post(url).with(csrf()).with(comPerfil(perfil)))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acesso-negado"));
    }

    /** Perfis liberados em cada prefixo, sem contar o ADMINISTRADOR, que entra em todos. */
    private static final Map<String, Set<Perfil>> TABELA = Map.of(
            "/projetos", Set.of(Perfil.values()),
            "/comercial", Set.of(Perfil.VENDEDOR, Perfil.DIRETORIA),
            "/engenharia", Set.of(Perfil.PROJETISTA),
            "/estoque", Set.of(Perfil.ALMOXARIFE, Perfil.COMPRADOR),
            "/campo", Set.of(Perfil.TECNICO_CAMPO, Perfil.PROJETISTA),
            "/financeiro", Set.of(Perfil.FINANCEIRO, Perfil.DIRETORIA),
            "/diretoria", Set.of(Perfil.DIRETORIA),
            "/admin", Set.of());

    static Stream<Arguments> prefixosEPerfis() {
        List<Arguments> combinacoes = new ArrayList<>();
        for (Map.Entry<String, Set<Perfil>> linha : TABELA.entrySet()) {
            for (Perfil perfil : Perfil.values()) {
                boolean liberado = perfil == Perfil.ADMINISTRADOR || linha.getValue().contains(perfil);
                combinacoes.add(Arguments.of(linha.getKey(), perfil, liberado));
            }
        }
        return combinacoes.stream();
    }

    /**
     * A tabela inteira: 8 prefixos por 8 perfis, no próprio prefixo e numa página que não existe.
     * Quem passa pela segurança recebe a página (200) ou 404, se ela não existe; quem é barrado
     * recebe 403.
     */
    @ParameterizedTest(name = "{0} com o perfil {1}: liberado = {2}")
    @MethodSource("prefixosEPerfis")
    @DisplayName("Tabela de permissões por prefixo e perfil")
    void testTabelaDePermissoes(String prefixo, Perfil perfil, boolean liberado) throws Exception {
        for (String url : List.of(prefixo, prefixo + "/pagina-que-nao-existe")) {
            int status = mockMvc.perform(get(url).with(comPerfil(perfil))).andReturn().getResponse().getStatus();
            if (liberado) {
                assertThat(status).as(url).isIn(200, 404);
            } else {
                assertThat(status).as(url).isEqualTo(403);
            }
        }
    }

    @Test
    @WithMockUser(authorities = { "VENDEDOR" })
    @DisplayName("GET /acesso-negado - Página de acesso negado no visual do sistema")
    void testPaginaDeAcessoNegado() throws Exception {
        mockMvc.perform(get("/acesso-negado"))
                .andExpect(status().isOk())
                .andExpect(view().name("acesso/acessoNegado"))
                .andExpect(content().string(containsString("Acesso negado")));
    }

    @Test
    @WithMockUser(authorities = { "VENDEDOR" })
    @DisplayName("GET /login - Quem já está logado vai para o início")
    void testLoginJaLogado() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}
