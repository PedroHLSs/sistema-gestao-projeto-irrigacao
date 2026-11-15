package br.edu.iftm.irrigacao.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioAutenticado;
import br.edu.iftm.irrigacao.dominio.comercial.Produtor;
import jakarta.persistence.EntityManager;

/**
 * A auditoria do JPA alimentada pelo usuário do {@code SecurityContext} (RNF03), com a aplicação
 * inteira: {@code AuditoriaConfig}, {@code UsuarioLogado} de verdade e o banco H2.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditoriaUsuarioLogadoTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProdutorRepository produtorRepository;

    @Autowired
    private EntityManager em;

    @AfterEach
    void limpaOLogin() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockUser(username = "maria", authorities = { "VENDEDOR" })
    void inclusaoGravaOUsuarioLogadoEmCriadoPor() {
        Usuario maria = usuarioRepository.saveAndFlush(DadosTeste.usuario("maria", Perfil.VENDEDOR));

        Long id = produtorRepository.saveAndFlush(DadosTeste.produtor("444.444.444-44")).getId();
        em.clear();

        Produtor lido = produtorRepository.findById(id).orElseThrow();
        assertThat(lido.getCriadoPor()).isEqualTo(maria.getId());
        assertThat(lido.getAlteradoPor()).isEqualTo(maria.getId());
        assertThat(lido.getCriadoEm()).isNotNull();
    }

    @Test
    @WithMockUser(username = "maria", authorities = { "VENDEDOR" })
    void alteracaoGravaOUsuarioLogadoEmAlteradoPorEMantemCriadoPor() {
        // incluído antes de a Maria existir no cadastro: fica sem autoria
        Produtor produtor = produtorRepository.saveAndFlush(DadosTeste.produtor("555.555.555-55"));
        Usuario maria = usuarioRepository.saveAndFlush(DadosTeste.usuario("maria", Perfil.VENDEDOR));

        produtor.setTelefone("(34) 3333-0000");
        produtorRepository.saveAndFlush(produtor);
        em.clear();

        Produtor lido = produtorRepository.findById(produtor.getId()).orElseThrow();
        assertThat(lido.getTelefone()).isEqualTo("(34) 3333-0000");
        assertThat(lido.getCriadoPor()).isNull();
        assertThat(lido.getAlteradoPor()).isEqualTo(maria.getId());
    }

    /**
     * No login pelo formulário o principal é o {@code UsuarioAutenticado}, que já traz o id. O
     * login dele aqui não existe no banco de propósito: o id tem de sair da sessão, sem consulta.
     */
    @Test
    void loginPeloFormularioUsaOIdGuardadoNoPrincipal() {
        Usuario joana = new Usuario(123L, "Joana Projetista", "joana.fora.do.banco", "hash", Perfil.PROJETISTA, true);
        UsuarioAutenticado principal = new UsuarioAutenticado(joana);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));

        Produtor produtor = produtorRepository.saveAndFlush(DadosTeste.produtor("777.777.777-77"));
        produtor.setTelefone("(34) 3333-1111");
        produtorRepository.saveAndFlush(produtor);
        em.clear();

        Produtor lido = produtorRepository.findById(produtor.getId()).orElseThrow();
        assertThat(lido.getCriadoPor()).isEqualTo(123L);
        assertThat(lido.getAlteradoPor()).isEqualTo(123L);
    }

    @Test
    void semLoginAAutoriaFicaVazia() {
        Long id = produtorRepository.saveAndFlush(DadosTeste.produtor("666.666.666-66")).getId();
        em.clear();

        Produtor lido = produtorRepository.findById(id).orElseThrow();
        assertThat(lido.getCriadoPor()).isNull();
        assertThat(lido.getAlteradoPor()).isNull();
        assertThat(lido.getCriadoEm()).isNotNull();
    }
}
