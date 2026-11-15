package br.edu.iftm.irrigacao.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import br.edu.iftm.irrigacao.dominio.estoque.Material;

/**
 * Restrições de saldo do material (RNF05).
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(ConfiguracaoJpaTeste.class)
class MaterialRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private MaterialRepository materialRepository;

    @Test
    void gravaMaterialComSaldoEReservaValidos() {
        Material gravado = materialRepository.saveAndFlush(DadosTeste.material("MT-01", "Filtro", "50", "20"));
        em.clear();

        Material lido = materialRepository.findByCodigo("MT-01").orElseThrow();

        assertThat(lido.getId()).isEqualTo(gravado.getId());
        assertThat(lido.getSaldo()).isEqualByComparingTo("50");
        assertThat(lido.getSaldoReservado()).isEqualByComparingTo("20");
        assertThat(materialRepository.existsByCodigo("MT-01")).isTrue();
    }

    @Test
    void bancoRecusaSaldoNegativo() {
        Material material = DadosTeste.material("MT-02", "Filtro", "-1", "0");

        assertThatThrownBy(() -> materialRepository.saveAndFlush(material))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoRecusaReservaNegativa() {
        Material material = DadosTeste.material("MT-03", "Filtro", "10", "-1");

        assertThatThrownBy(() -> materialRepository.saveAndFlush(material))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoRecusaReservaMaiorQueOSaldo() {
        Material material = DadosTeste.material("MT-04", "Filtro", "10", "10.001");

        assertThatThrownBy(() -> materialRepository.saveAndFlush(material))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void salvarOCadastroNaoAlteraOSaldo() {
        Material material = materialRepository.saveAndFlush(DadosTeste.material("MT-05", "Filtro", "50", "20"));

        material.setDescricao("Filtro de disco");
        material.setSaldo(new BigDecimal("999"));
        material.setSaldoReservado(new BigDecimal("0"));
        materialRepository.saveAndFlush(material);
        em.clear();

        Material lido = materialRepository.findById(material.getId()).orElseThrow();
        assertThat(lido.getDescricao()).isEqualTo("Filtro de disco");
        assertThat(lido.getSaldo()).isEqualByComparingTo("50");
        assertThat(lido.getSaldoReservado()).isEqualByComparingTo("20");
    }

    @Test
    void codigoDoMaterialNaoSeRepete() {
        materialRepository.saveAndFlush(DadosTeste.material("MT-06", "Filtro", "1", "0"));

        assertThatThrownBy(() -> materialRepository.saveAndFlush(DadosTeste.material("MT-06", "Outro", "1", "0")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
