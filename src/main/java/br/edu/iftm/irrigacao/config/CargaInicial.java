package br.edu.iftm.irrigacao.config;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;
import br.edu.iftm.irrigacao.dominio.estoque.Material;
import br.edu.iftm.irrigacao.persistencia.MaterialRepository;
import br.edu.iftm.irrigacao.persistencia.UsuarioRepository;

/**
 * Carga inicial: o cadastro de materiais (RF06), para a aplicação já subir com o que orçar, e o
 * primeiro administrador (RF23), para alguém conseguir entrar e criar os outros usuários.
 *
 * <p>É idempotente: só insere o material cujo código ainda não existe e só cria o administrador
 * quando não há nenhum, então subir a aplicação de novo não duplica nem altera nada. Fica
 * desligada no perfil {@code test}.
 */
@Component
@Profile("!test")
public class CargaInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaInicial.class);

    /** Login do administrador criado pela carga inicial. */
    public static final String LOGIN_ADMINISTRADOR = "admin";

    private final MaterialRepository materialRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final String senhaAdministrador;

    /**
     * @param senhaAdministrador valor da variável de ambiente {@code IRRIGACAO_ADMIN_SENHA}. Só o
     *                           perfil {@code dev} tem um valor padrão; fora dele, sem a variável,
     *                           chega vazio
     */
    public CargaInicial(MaterialRepository materialRepository, UsuarioRepository usuarioRepository,
            UsuarioService usuarioService, @Value("${irrigacao.admin.senha:}") String senhaAdministrador) {
        this.materialRepository = materialRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.senhaAdministrador = senhaAdministrador;}

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        criarAdministradorSeNaoHouver();
        inserirMateriais();
    }

    /**
     * Cria o login "admin" quando não existe nenhum usuário com perfil ADMINISTRADOR (RF23). A
     * senha vem de fora do código e nunca é escrita no log.
     */
    private void criarAdministradorSeNaoHouver() {
        if (usuarioRepository.existsByPerfil(Perfil.ADMINISTRADOR)) {
            return;
        }
        if (senhaAdministrador == null || senhaAdministrador.length() < UsuarioService.SENHA_TAMANHO_MINIMO) {
            log.warn("Carga inicial: não há nenhum usuário ADMINISTRADOR e ele NÃO foi criado, porque a "
                    + "variável de ambiente IRRIGACAO_ADMIN_SENHA está vazia ou tem menos de {} caracteres. "
                    + "Defina a variável (ou use o perfil dev) e suba a aplicação de novo.",
                    UsuarioService.SENHA_TAMANHO_MINIMO);
            return;
        }
        if (usuarioRepository.existsByLogin(LOGIN_ADMINISTRADOR)) {
            log.warn("Carga inicial: não há nenhum usuário ADMINISTRADOR e ele NÃO foi criado, porque o "
                    + "login \"{}\" já pertence a um usuário de outro perfil.", LOGIN_ADMINISTRADOR);
            return;
        }
        usuarioService.criar("Administrador", LOGIN_ADMINISTRADOR, senhaAdministrador, Perfil.ADMINISTRADOR);
        log.warn("Carga inicial: criado o usuário \"{}\", com perfil ADMINISTRADOR e a senha de "
                + "IRRIGACAO_ADMIN_SENHA (no perfil dev, sem a variável, vale a senha padrão de "
                + "desenvolvimento). Troque a senha em Usuários depois do primeiro acesso.", LOGIN_ADMINISTRADOR);
    }

    private void inserirMateriais() {
        int inseridos = 0;
        for (Material material : materiais()) {
            if (!materialRepository.existsByCodigo(material.getCodigo())) {
                materialRepository.save(material);
                inseridos++;
            }
        }
        log.info("Carga inicial: {} material(is) inserido(s).", inseridos);
    }

    private List<Material> materiais() {
        return List.of(
                // Tubos de PVC (barra de 6 m)
                material("TB-PVC-050", "Tubo PVC irrigação PN 40 DN 50 mm, barra 6 m", "barra", "62.90", "44.80", "120"),
                material("TB-PVC-075", "Tubo PVC irrigação PN 40 DN 75 mm, barra 6 m", "barra", "118.50", "86.20", "80"),
                material("TB-PVC-100", "Tubo PVC irrigação PN 40 DN 100 mm, barra 6 m", "barra", "189.90", "139.00", "60"),
                // Tubos de polietileno (por metro)
                material("TB-PEBD-016", "Tubo PEBD 16 mm para linha lateral", "m", "1.45", "0.92", "5000"),
                material("TB-PEAD-032", "Tubo PEAD PN 6 DN 32 mm", "m", "5.80", "3.90", "1500"),
                material("TB-PEAD-050", "Tubo PEAD PN 6 DN 50 mm", "m", "12.40", "8.60", "800"),
                // Gotejamento
                material("GT-AUT-2L", "Gotejador autocompensante 2 L/h", "un", "0.95", "0.52", "8000"),
                material("GT-AUT-4L", "Gotejador autocompensante 4 L/h", "un", "1.05", "0.58", "6000"),
                material("TG-16-30", "Tubo gotejador 16 mm, 1,6 L/h, espaçamento 30 cm", "m", "1.89", "1.20", "10000"),
                // Aspersão
                material("AS-SET-34", "Aspersor setorial de impacto 3/4\"", "un", "48.90", "31.50", "150"),
                material("AS-ROT-12", "Aspersor rotativo de impacto 1/2\"", "un", "29.90", "18.70", "200"),
                material("MA-BAI-40", "Microaspersor bailarina 40 L/h com estaca", "un", "4.50", "2.60", "1200"),
                // Conexões
                material("CX-TE-050", "Tê PVC soldável 50 mm", "un", "9.80", "5.90", "150"),
                material("CX-JO-050", "Joelho 90° PVC soldável 50 mm", "un", "6.40", "3.80", "200"),
                material("CX-LU-075", "Luva PVC soldável 75 mm", "un", "11.90", "7.40", "100"),
                material("CX-CI-016", "Conector inicial 16 mm com borracha de vedação", "un", "1.60", "0.85", "3000"),
                material("CX-FL-016", "Final de linha 16 mm", "un", "0.90", "0.45", "3000"),
                // Registros e válvulas
                material("RG-ESF-050", "Registro de esfera PVC soldável 50 mm", "un", "42.00", "27.30", "60"),
                material("RG-GAV-075", "Registro de gaveta em bronze 3\"", "un", "310.00", "228.00", "12"),
                material("VL-SOL-100", "Válvula solenoide 1\" 24 VCA", "un", "189.00", "134.00", "25"),
                // Filtros
                material("FT-DIS-200", "Filtro de disco 2\" 120 mesh", "un", "289.00", "205.00", "20"),
                material("FT-TEL-100", "Filtro de tela 1\" 120 mesh", "un", "79.90", "52.00", "40"));
    }

    private Material material(String codigo, String descricao, String unidade, String precoVenda,
            String custoUnitario, String saldo) {
        return new Material(null, codigo, descricao, unidade, new BigDecimal(precoVenda),
                new BigDecimal(custoUnitario), new BigDecimal(saldo), BigDecimal.ZERO);
    }
}
