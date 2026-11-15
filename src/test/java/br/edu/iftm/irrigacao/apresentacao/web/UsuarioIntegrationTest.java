package br.edu.iftm.irrigacao.apresentacao.web;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
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
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;
import br.edu.iftm.irrigacao.persistencia.UsuarioRepository;
import jakarta.persistence.EntityManager;

/**
 * Gestão de usuários (RF23) de ponta a ponta: controller, service e banco H2, com o administrador
 * logado do mesmo jeito que no login pelo formulário.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UsuarioIntegrationTest {

    private static final String SENHA = "senha-inicial-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager em;

    private Usuario carla;

    @BeforeEach
    void criaAAdministradora() {
        carla = usuarioService.criar("Carla Chefe", "carla", SENHA, Perfil.ADMINISTRADOR);
    }

    /** A administradora na sessão, com o mesmo principal que o login pelo formulário monta. */
    private RequestPostProcessor carlaLogada() {
        return user(userDetailsService.loadUserByUsername("carla"));
    }

    private Usuario relido(Long id) {
        em.flush();
        em.clear();
        return usuarioRepository.findById(id).orElseThrow();
    }

    @Test
    @DisplayName("Teste de integração - Administrador cria usuário, com senha em BCrypt e autoria gravada")
    void testCriarUsuarioIntegration() throws Exception {
        mockMvc.perform(post("/admin/usuarios")
                        .with(csrf()).with(carlaLogada())
                        .param("nome", "  João Silva ")
                        .param("login", "joao.silva")
                        .param("perfil", "TECNICO_CAMPO")
                        .param("senha", "senha-do-joao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"));

        Usuario joao = relido(usuarioRepository.findByLogin("joao.silva").orElseThrow().getId());
        assertThat(joao.getNome()).isEqualTo("João Silva");
        assertThat(joao.getPerfil()).isEqualTo(Perfil.TECNICO_CAMPO);
        assertThat(joao.isAtivo()).isTrue();
        assertThat(joao.getSenhaHash()).isNotEqualTo("senha-do-joao");
        assertThat(passwordEncoder.matches("senha-do-joao", joao.getSenhaHash())).isTrue();
        // RNF03: quem criou foi a administradora logada
        assertThat(joao.getCriadoPor()).isEqualTo(carla.getId());
        assertThat(joao.getCriadoEm()).isNotNull();

        mockMvc.perform(formLogin("/login").user("joao.silva").password("senha-do-joao"))
                .andExpect(authenticated().withUsername("joao.silva"));
    }

    @Test
    @DisplayName("Teste de integração - Login repetido é recusado e nada é gravado")
    void testCriarUsuarioLoginRepetidoIntegration() throws Exception {
        long antes = usuarioRepository.count();

        mockMvc.perform(post("/admin/usuarios")
                        .with(csrf()).with(carlaLogada())
                        .param("nome", "Outra Carla")
                        .param("login", "carla")
                        .param("perfil", "VENDEDOR")
                        .param("senha", "senha-da-outra"))
                .andExpect(status().isOk())
                .andExpect(view().name("acesso/cadastrarUsuario"))
                .andExpect(model().attributeHasFieldErrors("usuario", "login"));

        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    @Test
    @DisplayName("Teste de integração - Lista mostra os usuários e não oferece desativar o próprio usuário")
    void testListarUsuariosIntegration() throws Exception {
        Usuario pedro = usuarioService.criar("Pedro Alves", "pedro", SENHA, Perfil.ALMOXARIFE);

        mockMvc.perform(get("/admin/usuarios").with(carlaLogada()))
                .andExpect(status().isOk())
                .andExpect(view().name("acesso/listarUsuarios"))
                .andExpect(content().string(containsString("Carla Chefe")))
                .andExpect(content().string(containsString("Pedro Alves")))
                .andExpect(content().string(containsString("Almoxarife")))
                .andExpect(content().string(containsString("/admin/usuarios/" + pedro.getId() + "/desativar")))
                .andExpect(content().string(not(containsString("/admin/usuarios/" + carla.getId() + "/desativar"))));
    }

    @Test
    @DisplayName("Teste de integração - Administrador edita nome e perfil de outro usuário")
    void testEditarUsuarioIntegration() throws Exception {
        Usuario pedro = usuarioService.criar("Pedro Alves", "pedro", SENHA, Perfil.ALMOXARIFE);

        mockMvc.perform(post("/admin/usuarios/" + pedro.getId())
                        .with(csrf()).with(carlaLogada())
                        .param("nome", "Pedro Alves Filho")
                        .param("login", "pedro")
                        .param("perfil", "COMPRADOR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"));

        Usuario lido = relido(pedro.getId());
        assertThat(lido.getNome()).isEqualTo("Pedro Alves Filho");
        assertThat(lido.getLogin()).isEqualTo("pedro");
        assertThat(lido.getPerfil()).isEqualTo(Perfil.COMPRADOR);

        mockMvc.perform(formLogin("/login").user("pedro").password(SENHA))
                .andExpect(authenticated().withAuthorities(List.of(new SimpleGrantedAuthority("COMPRADOR"))));
    }

    @Test
    @DisplayName("Teste de integração - Editar usuário inexistente responde 404")
    void testEditarUsuarioInexistenteIntegration() throws Exception {
        mockMvc.perform(get("/admin/usuarios/99999/editar").with(carlaLogada()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Teste de integração - Administrador não tira o próprio perfil de ADMINISTRADOR")
    void testNaoTiraOProprioPerfilIntegration() throws Exception {
        mockMvc.perform(post("/admin/usuarios/" + carla.getId())
                        .with(csrf()).with(carlaLogada())
                        .header(HttpHeaders.REFERER, "http://localhost/admin/usuarios/" + carla.getId() + "/editar")
                        .param("nome", "Carla Chefe")
                        .param("login", "carla")
                        .param("perfil", "VENDEDOR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios/" + carla.getId() + "/editar"))
                .andExpect(flash().attribute("erro", "Você não pode tirar o seu próprio perfil de Administrador."));

        assertThat(relido(carla.getId()).getPerfil()).isEqualTo(Perfil.ADMINISTRADOR);
    }

    @Test
    @DisplayName("Teste de integração - Administrador altera o próprio nome mantendo o perfil")
    void testAlteraOProprioNomeIntegration() throws Exception {
        mockMvc.perform(post("/admin/usuarios/" + carla.getId())
                        .with(csrf()).with(carlaLogada())
                        .param("nome", "Carla Chefe de Souza")
                        .param("login", "carla")
                        .param("perfil", "ADMINISTRADOR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attributeExists("sucesso"));

        assertThat(relido(carla.getId()).getNome()).isEqualTo("Carla Chefe de Souza");
    }

    @Test
    @DisplayName("Teste de integração - Administrador pode tirar o perfil de ADMINISTRADOR de outro usuário")
    void testTiraOPerfilDeOutroAdministradorIntegration() throws Exception {
        Usuario outro = usuarioService.criar("Otávio Admin", "otavio", SENHA, Perfil.ADMINISTRADOR);

        mockMvc.perform(post("/admin/usuarios/" + outro.getId())
                        .with(csrf()).with(carlaLogada())
                        .param("nome", "Otávio Admin")
                        .param("login", "otavio")
                        .param("perfil", "FINANCEIRO"))
                .andExpect(redirectedUrl("/admin/usuarios"));

        assertThat(relido(outro.getId()).getPerfil()).isEqualTo(Perfil.FINANCEIRO);
    }

    @Test
    @DisplayName("Teste de integração - Administrador não desativa a si mesmo")
    void testNaoDesativaASiMesmoIntegration() throws Exception {
        mockMvc.perform(post("/admin/usuarios/" + carla.getId() + "/desativar")
                        .with(csrf()).with(carlaLogada())
                        .header(HttpHeaders.REFERER, "http://localhost/admin/usuarios"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attribute("erro", "Você não pode desativar o seu próprio usuário."));

        assertThat(relido(carla.getId()).isAtivo()).isTrue();
    }

    @Test
    @DisplayName("Teste de integração - Usuário desativado não entra mais, e reativado volta a entrar")
    void testDesativarEAtivarIntegration() throws Exception {
        Usuario pedro = usuarioService.criar("Pedro Alves", "pedro", SENHA, Perfil.ALMOXARIFE);

        mockMvc.perform(post("/admin/usuarios/" + pedro.getId() + "/desativar").with(csrf()).with(carlaLogada()))
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attributeExists("sucesso"));

        assertThat(relido(pedro.getId()).isAtivo()).isFalse();
        mockMvc.perform(formLogin("/login").user("pedro").password(SENHA))
                .andExpect(unauthenticated());

        mockMvc.perform(post("/admin/usuarios/" + pedro.getId() + "/ativar").with(csrf()).with(carlaLogada()))
                .andExpect(redirectedUrl("/admin/usuarios"));

        assertThat(relido(pedro.getId()).isAtivo()).isTrue();
        mockMvc.perform(formLogin("/login").user("pedro").password(SENHA))
                .andExpect(authenticated().withUsername("pedro"));
    }

    @Test
    @DisplayName("Teste de integração - Senha redefinida passa a valer e a antiga deixa de valer")
    void testRedefinirSenhaIntegration() throws Exception {
        Usuario pedro = usuarioService.criar("Pedro Alves", "pedro", SENHA, Perfil.ALMOXARIFE);
        String hashAntigo = pedro.getSenhaHash();

        mockMvc.perform(post("/admin/usuarios/" + pedro.getId() + "/senha")
                        .with(csrf()).with(carlaLogada())
                        .param("novaSenha", "nova-senha-456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"));

        assertThat(relido(pedro.getId()).getSenhaHash()).isNotEqualTo(hashAntigo);
        mockMvc.perform(formLogin("/login").user("pedro").password(SENHA))
                .andExpect(unauthenticated());
        mockMvc.perform(formLogin("/login").user("pedro").password("nova-senha-456"))
                .andExpect(authenticated().withUsername("pedro"));
    }

    @Test
    @DisplayName("Teste de integração - Senha curta não é gravada")
    void testRedefinirSenhaCurtaIntegration() throws Exception {
        Usuario pedro = usuarioService.criar("Pedro Alves", "pedro", SENHA, Perfil.ALMOXARIFE);

        mockMvc.perform(post("/admin/usuarios/" + pedro.getId() + "/senha")
                        .with(csrf()).with(carlaLogada())
                        .param("novaSenha", "1234567"))
                .andExpect(status().isOk())
                .andExpect(view().name("acesso/editarUsuario"))
                .andExpect(model().attributeHasFieldErrors("senha", "novaSenha"));

        mockMvc.perform(formLogin("/login").user("pedro").password(SENHA))
                .andExpect(authenticated().withUsername("pedro"));
    }
}
