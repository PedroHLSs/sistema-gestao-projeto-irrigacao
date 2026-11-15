package br.edu.iftm.irrigacao.apresentacao.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import br.edu.iftm.irrigacao.config.SecurityConfig;
import br.edu.iftm.irrigacao.config.TestConfig;
import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;

/**
 * Controller da gestão de usuários (RF23), com o service mockado.
 */
@WebMvcTest(UsuarioController.class)
@Import({ TestConfig.class, SecurityConfig.class })
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @AfterEach
    void resetMocks() {
        reset(usuarioService);
    }

    private Usuario joao() {
        return new Usuario(1L, "João Silva", "joao.silva", "hash", Perfil.VENDEDOR, true);
    }

    private Usuario maria() {
        return new Usuario(2L, "Maria Santos", "maria.santos", "hash", Perfil.TECNICO_CAMPO, false);
    }

    // ---------- Listagem ----------

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("GET /admin/usuarios - Administrador vê a lista de usuários")
    void testListarUsuariosAdministrador() throws Exception {
        when(usuarioService.listar()).thenReturn(List.of(joao(), maria()));

        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("acesso/listarUsuarios"))
                .andExpect(model().attributeExists("usuarios"))
                .andExpect(content().string(containsString("João Silva")))
                .andExpect(content().string(containsString("joao.silva")))
                .andExpect(content().string(containsString("Maria Santos")))
                .andExpect(content().string(containsString("Técnico de campo")))
                .andExpect(content().string(containsString("Novo usuário")));

        verify(usuarioService).listar();
    }

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("GET /admin/usuarios - Usuário ativo tem o botão Desativar e o inativo tem o Ativar")
    void testListarMostraAcaoConformeASituacao() throws Exception {
        when(usuarioService.listar()).thenReturn(List.of(joao(), maria()));

        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(content().string(containsString("/admin/usuarios/1/desativar")))
                .andExpect(content().string(not(containsString("/admin/usuarios/1/ativar"))))
                .andExpect(content().string(containsString("/admin/usuarios/2/ativar")))
                .andExpect(content().string(not(containsString("/admin/usuarios/2/desativar"))));
    }

    @Test
    @WithMockUser(authorities = { "DIRETORIA" })
    @DisplayName("GET /admin/usuarios - Diretoria não acessa a gestão de usuários")
    void testListarUsuariosDiretoriaForbidden() throws Exception {
        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acesso-negado"));

        verify(usuarioService, never()).listar();
    }

    @Test
    @DisplayName("GET /admin/usuarios - Sem login vai para a página de login")
    void testListarUsuariosNotAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verify(usuarioService, never()).listar();
    }

    // ---------- Inclusão ----------

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("GET /admin/usuarios/novo - Formulário de inclusão com os oito perfis")
    void testNovoUsuarioForm() throws Exception {
        mockMvc.perform(get("/admin/usuarios/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("acesso/cadastrarUsuario"))
                .andExpect(model().attributeExists("usuario", "perfis"))
                .andExpect(content().string(containsString("value=\"ALMOXARIFE\"")))
                .andExpect(content().string(containsString("Técnico de campo")));
    }

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("POST /admin/usuarios - Administrador cria usuário")
    void testCriarUsuario() throws Exception {
        when(usuarioService.criar("Pedro Alves", "pedro.alves", "senha-forte-1", Perfil.PROJETISTA))
                .thenReturn(new Usuario(3L, "Pedro Alves", "pedro.alves", "hash", Perfil.PROJETISTA, true));

        mockMvc.perform(post("/admin/usuarios")
                        .with(csrf())
                        .param("nome", "Pedro Alves")
                        .param("login", "pedro.alves")
                        .param("perfil", "PROJETISTA")
                        .param("senha", "senha-forte-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attribute("sucesso", "Usuário pedro.alves criado."));

        verify(usuarioService).criar("Pedro Alves", "pedro.alves", "senha-forte-1", Perfil.PROJETISTA);
    }

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("POST /admin/usuarios - Senha com menos de 8 caracteres volta ao formulário")
    void testCriarUsuarioSenhaCurta() throws Exception {
        mockMvc.perform(post("/admin/usuarios")
                        .with(csrf())
                        .param("nome", "Pedro Alves")
                        .param("login", "pedro.alves")
                        .param("perfil", "PROJETISTA")
                        .param("senha", "1234567"))
                .andExpect(status().isOk())
                .andExpect(view().name("acesso/cadastrarUsuario"))
                .andExpect(model().attributeHasFieldErrors("usuario", "senha"))
                .andExpect(content().string(containsString("A senha deve ter de 8 a 72 caracteres.")))
                // a senha digitada não volta para a página
                .andExpect(content().string(not(containsString("1234567"))));

        verify(usuarioService, never()).criar(anyString(), anyString(), anyString(), any());
    }

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("POST /admin/usuarios - Login que já existe aparece como erro no campo")
    void testCriarUsuarioLoginRepetido() throws Exception {
        when(usuarioService.loginEmUso("joao.silva", null)).thenReturn(true);

        mockMvc.perform(post("/admin/usuarios")
                        .with(csrf())
                        .param("nome", "Outro João")
                        .param("login", "joao.silva")
                        .param("perfil", "VENDEDOR")
                        .param("senha", "senha-forte-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("acesso/cadastrarUsuario"))
                .andExpect(model().attributeHasFieldErrors("usuario", "login"))
                .andExpect(content().string(containsString("Já existe um usuário com este login.")));

        verify(usuarioService, never()).criar(anyString(), anyString(), anyString(), any());
    }

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("POST /admin/usuarios - Campos vazios e login fora do padrão são recusados")
    void testCriarUsuarioCamposInvalidos() throws Exception {
        mockMvc.perform(post("/admin/usuarios")
                        .with(csrf())
                        .param("nome", "")
                        .param("login", "João Silva")
                        .param("perfil", "")
                        .param("senha", "senha-forte-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("acesso/cadastrarUsuario"))
                .andExpect(model().attributeHasFieldErrors("usuario", "nome", "login", "perfil"))
                .andExpect(content().string(containsString("Informe o nome.")))
                .andExpect(content().string(containsString("Escolha o perfil.")));

        verify(usuarioService, never()).criar(anyString(), anyString(), anyString(), any());
    }

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("POST /admin/usuarios - Sem o token CSRF a inclusão é recusada")
    void testCriarUsuarioSemCsrf() throws Exception {
        mockMvc.perform(post("/admin/usuarios")
                        .param("nome", "Pedro Alves")
                        .param("login", "pedro.alves")
                        .param("perfil", "PROJETISTA")
                        .param("senha", "senha-forte-1"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).criar(anyString(), anyString(), anyString(), any());
    }

    @Test
    @WithMockUser(authorities = { "VENDEDOR" })
    @DisplayName("POST /admin/usuarios - Vendedor não pode criar usuário")
    void testCriarUsuarioVendedorForbidden() throws Exception {
        mockMvc.perform(post("/admin/usuarios")
                        .with(csrf())
                        .param("nome", "Pedro Alves")
                        .param("login", "pedro.alves")
                        .param("perfil", "ADMINISTRADOR")
                        .param("senha", "senha-forte-1"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).criar(anyString(), anyString(), anyString(), any());
    }

    // ---------- Edição ----------

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("GET /admin/usuarios/{id}/editar - Formulário de edição preenchido")
    void testEditarUsuarioForm() throws Exception {
        when(usuarioService.buscarPorId(1L)).thenReturn(joao());

        mockMvc.perform(get("/admin/usuarios/1/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("acesso/editarUsuario"))
                .andExpect(model().attributeExists("usuario", "senha", "perfis"))
                .andExpect(model().attribute("usuario", hasProperty("nome", is("João Silva"))))
                .andExpect(content().string(containsString("value=\"joao.silva\"")))
                .andExpect(content().string(containsString("value=\"VENDEDOR\" selected=\"selected\"")))
                .andExpect(content().string(containsString("action=\"/admin/usuarios/1\"")))
                .andExpect(content().string(containsString("action=\"/admin/usuarios/1/senha\"")));

        verify(usuarioService).buscarPorId(1L);
    }

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("GET /admin/usuarios/{id}/editar - Usuário inexistente responde 404")
    void testEditarUsuarioNaoExistente() throws Exception {
        when(usuarioService.buscarPorId(999L)).thenThrow(new RegistroNaoEncontradoException("Usuário não encontrado."));

        mockMvc.perform(get("/admin/usuarios/999/editar"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("POST /admin/usuarios/{id} - Administrador altera nome, login e perfil")
    void testAtualizarUsuario() throws Exception {
        when(usuarioService.atualizar(1L, "João da Silva", "joao.silva", Perfil.DIRETORIA))
                .thenReturn(new Usuario(1L, "João da Silva", "joao.silva", "hash", Perfil.DIRETORIA, true));

        mockMvc.perform(post("/admin/usuarios/1")
                        .with(csrf())
                        .param("nome", "João da Silva")
                        .param("login", "joao.silva")
                        .param("perfil", "DIRETORIA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attribute("sucesso", "Usuário joao.silva alterado."));

        // o id da URL entra na conferência de login único, para o login do próprio usuário não contar
        verify(usuarioService).loginEmUso("joao.silva", 1L);
        verify(usuarioService).atualizar(1L, "João da Silva", "joao.silva", Perfil.DIRETORIA);
    }

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("POST /admin/usuarios/{id} - Login de outro usuário volta ao formulário")
    void testAtualizarUsuarioLoginRepetido() throws Exception {
        when(usuarioService.loginEmUso("maria.santos", 1L)).thenReturn(true);

        mockMvc.perform(post("/admin/usuarios/1")
                        .with(csrf())
                        .param("nome", "João Silva")
                        .param("login", "maria.santos")
                        .param("perfil", "VENDEDOR"))
                .andExpect(status().isOk())
                .andExpect(view().name("acesso/editarUsuario"))
                .andExpect(model().attributeHasFieldErrors("usuario", "login"))
                .andExpect(model().attributeExists("senha"));

        verify(usuarioService, never()).atualizar(anyLong(), anyString(), anyString(), any());
    }

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("POST /admin/usuarios/{id} - Tirar o próprio perfil de administrador mostra o erro do service")
    void testAtualizarUsuarioRegraDeNegocio() throws Exception {
        when(usuarioService.atualizar(1L, "João Silva", "joao.silva", Perfil.VENDEDOR))
                .thenThrow(new RegraNegocioException("Você não pode tirar o seu próprio perfil de Administrador."));

        mockMvc.perform(post("/admin/usuarios/1")
                        .with(csrf())
                        .header(HttpHeaders.REFERER, "http://localhost/admin/usuarios/1/editar")
                        .param("nome", "João Silva")
                        .param("login", "joao.silva")
                        .param("perfil", "VENDEDOR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios/1/editar"))
                .andExpect(flash().attribute("erro", "Você não pode tirar o seu próprio perfil de Administrador."));
    }

    @Test
    @WithMockUser(authorities = { "PROJETISTA" })
    @DisplayName("POST /admin/usuarios/{id} - Projetista não pode alterar usuário")
    void testAtualizarUsuarioProjetistaForbidden() throws Exception {
        mockMvc.perform(post("/admin/usuarios/1")
                        .with(csrf())
                        .param("nome", "João Silva")
                        .param("login", "joao.silva")
                        .param("perfil", "ADMINISTRADOR"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).atualizar(anyLong(), anyString(), anyString(), any());
    }

    // ---------- Senha ----------

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("POST /admin/usuarios/{id}/senha - Administrador redefine a senha")
    void testRedefinirSenha() throws Exception {
        mockMvc.perform(post("/admin/usuarios/1/senha")
                        .with(csrf())
                        .param("novaSenha", "novaSenha123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attribute("sucesso", "Senha redefinida."));

        verify(usuarioService).redefinirSenha(1L, "novaSenha123");
    }

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("POST /admin/usuarios/{id}/senha - Senha curta ou vazia volta ao formulário")
    void testRedefinirSenhaCurta() throws Exception {
        when(usuarioService.buscarPorId(1L)).thenReturn(joao());

        for (String senha : List.of("", "curta12")) {
            mockMvc.perform(post("/admin/usuarios/1/senha")
                            .with(csrf())
                            .param("novaSenha", senha))
                    .andExpect(status().isOk())
                    .andExpect(view().name("acesso/editarUsuario"))
                    .andExpect(model().attributeHasFieldErrors("senha", "novaSenha"))
                    .andExpect(content().string(containsString("A senha deve ter de 8 a 72 caracteres.")));
        }

        verify(usuarioService, never()).redefinirSenha(anyLong(), anyString());
    }

    // ---------- Ativar e desativar ----------

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("POST /admin/usuarios/{id}/desativar - Administrador desativa usuário")
    void testDesativarUsuario() throws Exception {
        mockMvc.perform(post("/admin/usuarios/1/desativar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attributeExists("sucesso"));

        verify(usuarioService).desativar(1L);
    }

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("POST /admin/usuarios/{id}/desativar - Desativar a si mesmo mostra o erro do service")
    void testDesativarASiMesmo() throws Exception {
        doThrow(new RegraNegocioException("Você não pode desativar o seu próprio usuário."))
                .when(usuarioService).desativar(1L);

        mockMvc.perform(post("/admin/usuarios/1/desativar")
                        .with(csrf())
                        .header(HttpHeaders.REFERER, "http://localhost/admin/usuarios"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attribute("erro", "Você não pode desativar o seu próprio usuário."))
                .andExpect(flash().attributeCount(1));
    }

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("POST /admin/usuarios/{id}/ativar - Administrador reativa usuário")
    void testAtivarUsuario() throws Exception {
        mockMvc.perform(post("/admin/usuarios/2/ativar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attributeExists("sucesso"));

        verify(usuarioService).ativar(2L);
    }

    @Test
    @WithMockUser(authorities = { "ADMINISTRADOR" })
    @DisplayName("GET nas ações que alteram dado - Não existe: só POST")
    void testAcoesNaoAceitamGet() throws Exception {
        mockMvc.perform(get("/admin/usuarios/1/desativar")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(get("/admin/usuarios/1/ativar")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(get("/admin/usuarios/1/senha")).andExpect(status().isMethodNotAllowed());

        verify(usuarioService, never()).desativar(anyLong());
        verify(usuarioService, never()).ativar(anyLong());
    }

    @Test
    @WithMockUser(authorities = { "ALMOXARIFE" })
    @DisplayName("POST /admin/usuarios/{id}/desativar - Almoxarife não pode desativar usuário")
    void testDesativarUsuarioAlmoxarifeForbidden() throws Exception {
        mockMvc.perform(post("/admin/usuarios/1/desativar").with(csrf()))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).desativar(anyLong());
    }
}
