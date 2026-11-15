package br.edu.iftm.irrigacao.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.Material;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(ConfiguracaoJpaTeste.class)
class ItemProjetoRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ItemProjetoRepository itemProjetoRepository;

    @Test
    void mesmoMaterialNaoEntraDuasVezesNoMesmoProjeto() {
        Projeto projeto = DadosTeste.projetoGravado(em, "PRJ-0101");
        Material tubo = em.persist(DadosTeste.material("TB-01", "Tubo", "100", "0"));
        itemProjetoRepository.saveAndFlush(item(projeto, tubo, "10"));

        assertThatThrownBy(() -> itemProjetoRepository.saveAndFlush(item(projeto, tubo, "5")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void mesmoMaterialPodeEntrarEmProjetosDiferentes() {
        Projeto primeiro = DadosTeste.projetoGravado(em, "PRJ-0102");
        Projeto segundo = DadosTeste.projetoGravado(em, "PRJ-0103");
        Material tubo = em.persist(DadosTeste.material("TB-02", "Tubo", "100", "0"));

        itemProjetoRepository.saveAndFlush(item(primeiro, tubo, "10"));
        itemProjetoRepository.saveAndFlush(item(segundo, tubo, "5"));

        assertThat(itemProjetoRepository.existsByProjetoId(primeiro.getId())).isTrue();
        assertThat(itemProjetoRepository.findByProjetoIdAndMaterialId(segundo.getId(), tubo.getId()))
                .isPresent();
    }

    @Test
    void listaTecnicaVemOrdenadaPelaDescricaoComOMaterialCarregado() {
        Projeto projeto = DadosTeste.projetoGravado(em, "PRJ-0104");
        Material tubo = em.persist(DadosTeste.material("TB-03", "Tubo PVC 50 mm", "100", "0"));
        Material aspersor = em.persist(DadosTeste.material("AS-03", "Aspersor setorial", "30", "0"));
        em.persist(item(projeto, tubo, "12.500"));
        em.persist(item(projeto, aspersor, "8"));
        em.flush();
        em.clear();

        List<ItemProjeto> itens = itemProjetoRepository.listarPorProjetoComMaterial(projeto.getId());

        assertThat(itens).hasSize(2);
        assertThat(itens).allSatisfy(i -> assertThat(Hibernate.isInitialized(i.getMaterial())).isTrue());
        assertThat(itens).extracting(i -> i.getMaterial().getDescricao())
                .containsExactly("Aspersor setorial", "Tubo PVC 50 mm");
        ItemProjeto itemTubo = itens.get(1);
        assertThat(itemTubo.getQuantidadePrevista()).isEqualByComparingTo("12.500");
        assertThat(itemTubo.getQuantidadeSeparada()).isEqualByComparingTo("0");
        assertThat(itemTubo.getQuantidadeApontada()).isEqualByComparingTo("0");
        assertThat(itemTubo.getPrecoUnitario()).isNull();
    }

    private ItemProjeto item(Projeto projeto, Material material, String quantidadePrevista) {
        ItemProjeto item = new ItemProjeto();
        item.setProjeto(projeto);
        item.setMaterial(material);
        item.setQuantidadePrevista(new BigDecimal(quantidadePrevista));
        return item;
    }
}
