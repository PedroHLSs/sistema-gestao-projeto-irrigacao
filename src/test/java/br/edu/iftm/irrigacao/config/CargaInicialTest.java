package br.edu.iftm.irrigacao.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;
import br.edu.iftm.irrigacao.persistencia.MaterialRepository;
import br.edu.iftm.irrigacao.persistencia.UsuarioRepository;

/**
 * Criação do primeiro administrador pela carga inicial (RF23). No perfil {@code test} a carga não
 * roda sozinha, então o teste a monta com a senha que quer experimentar.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CargaInicialTest {

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private void rodarCarga(String senhaAdministrador) {
        new CargaInicial(materialRepository, usuarioRepository, usuarioService, senhaAdministrador)
                .run(new DefaultApplicationArguments());
    }

    @Test
    void semNenhumAdministradorCriaOLoginAdminComASenhaInformada() {
        rodarCarga("senha-do-ambiente");

        Usuario admin = usuarioRepository.findByLogin("admin").orElseThrow();
        assertThat(admin.getPerfil()).isEqualTo(Perfil.ADMINISTRADOR);
        assertThat(admin.isAtivo()).isTrue();
        assertThat(passwordEncoder.matches("senha-do-ambiente", admin.getSenhaHash())).isTrue();
    }

    @Test
    void rodarDeNovoNaoDuplicaNemTrocaASenha() {
        rodarCarga("senha-do-ambiente");
        long usuarios = usuarioRepository.count();
        long materiais = materialRepository.count();

        rodarCarga("outra-senha-qualquer");

        assertThat(usuarioRepository.count()).isEqualTo(usuarios);
        assertThat(materialRepository.count()).isEqualTo(materiais);
        Usuario admin = usuarioRepository.findByLogin("admin").orElseThrow();
        assertThat(passwordEncoder.matches("senha-do-ambiente", admin.getSenhaHash())).isTrue();
    }

    @Test
    void comOutroAdministradorCadastradoNaoCriaOLoginAdmin() {
        usuarioService.criar("Carla Chefe", "carla", "senha-da-carla", Perfil.ADMINISTRADOR);

        rodarCarga("senha-do-ambiente");

        assertThat(usuarioRepository.existsByLogin("admin")).isFalse();
    }

    @Test
    void semASenhaNaoCriaOAdministradorMasInsereOsMateriais() {
        rodarCarga("");

        assertThat(usuarioRepository.existsByPerfil(Perfil.ADMINISTRADOR)).isFalse();
        assertThat(materialRepository.count()).isPositive();
    }

    @Test
    void senhaComMenosDeOitoCaracteresNaoCriaOAdministrador() {
        rodarCarga("1234567");

        assertThat(usuarioRepository.existsByPerfil(Perfil.ADMINISTRADOR)).isFalse();
    }

    @Test
    void loginAdminJaUsadoPorOutroPerfilNaoEAlterado() {
        usuarioService.criar("Admilson Vendedor", "admin", "senha-do-admilson", Perfil.VENDEDOR);

        rodarCarga("senha-do-ambiente");

        assertThat(usuarioRepository.findByLogin("admin").orElseThrow().getPerfil()).isEqualTo(Perfil.VENDEDOR);
        assertThat(usuarioRepository.existsByPerfil(Perfil.ADMINISTRADOR)).isFalse();
    }
}
