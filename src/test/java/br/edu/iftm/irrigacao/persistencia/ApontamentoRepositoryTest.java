package br.edu.iftm.irrigacao.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.estoque.Material;
import br.edu.iftm.irrigacao.dominio.execucao.Apontamento;
import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoHoras;
import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoMaterial;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServico;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;

/**
 * Herança de Apontamento em tabela única e a tabela auxiliar dos técnicos da ordem de serviço.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(ConfiguracaoJpaTeste.class)
class ApontamentoRepositoryTest {

    private static final LocalDateTime MANHA = LocalDateTime.of(2026, 3, 20, 8, 0);
    private static final LocalDateTime TARDE = LocalDateTime.of(2026, 3, 20, 14, 0);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private OrdemServicoRepository ordemServicoRepository;

    @Autowired
    private ApontamentoRepository apontamentoRepository;

    @Autowired
    private ApontamentoHorasRepository apontamentoHorasRepository;

    @Autowired
    private ApontamentoMaterialRepository apontamentoMaterialRepository;

    private Projeto projeto;
    private Usuario tecnico;
    private Usuario ajudante;
    private OrdemServico ordemServico;

    @BeforeEach
    void preparaOrdemDeServicoComDoisApontamentos() {
        projeto = DadosTeste.projetoGravado(em, "PRJ-0201");
        tecnico = em.persist(DadosTeste.usuario("tecnico", Perfil.TECNICO_CAMPO));
        ajudante = em.persist(DadosTeste.usuario("ajudante", Perfil.TECNICO_CAMPO));
        Material tubo = em.persist(DadosTeste.material("TB-21", "Tubo PVC 50 mm", "100", "0"));

        ordemServico = em.persist(new OrdemServico(null, projeto, 1, LocalDate.of(2026, 3, 20), null,
                List.of(tecnico, ajudante)));
        em.persist(new ApontamentoHoras(null, ordemServico, MANHA, tecnico, new BigDecimal("6.50"),
                new BigDecimal("45.00")));
        em.persist(new ApontamentoMaterial(null, ordemServico, TARDE, tecnico, tubo, new BigDecimal("12.000")));
        em.flush();
        em.clear();
    }

    @Test
    void consultaPelaClasseBaseDevolveCadaApontamentoNaSuaSubclasse() {
        List<Apontamento> apontamentos = apontamentoRepository
                .findByOrdemServicoIdOrderByDataHora(ordemServico.getId());

        assertThat(apontamentos).hasSize(2);
        assertThat(apontamentos.get(0)).isInstanceOf(ApontamentoHoras.class);
        assertThat(apontamentos.get(1)).isInstanceOf(ApontamentoMaterial.class);

        ApontamentoHoras horas = (ApontamentoHoras) apontamentos.get(0);
        assertThat(horas.getHoras()).isEqualByComparingTo("6.50");
        assertThat(horas.getValorHora()).isEqualByComparingTo("45.00");
        assertThat(horas.getUsuario().getLogin()).isEqualTo("tecnico");

        ApontamentoMaterial material = (ApontamentoMaterial) apontamentos.get(1);
        assertThat(material.getQuantidade()).isEqualByComparingTo("12");
        assertThat(material.getMaterial().getCodigo()).isEqualTo("TB-21");
    }

    @Test
    void asDuasSubclassesFicamNaMesmaTabelaComODiscriminador() {
        List<?> tipos = em.getEntityManager()
                .createNativeQuery("select tipo from apontamento order by data_hora")
                .getResultList();

        assertThat(tipos).hasSize(2);
        assertThat(tipos.get(0)).hasToString("HORAS");
        assertThat(tipos.get(1)).hasToString("MATERIAL");
    }

    @Test
    void repositorioDeCadaSubclasseSoTrazOSeuTipo() {
        List<ApontamentoHoras> horas = apontamentoHorasRepository.listarPorProjeto(projeto.getId());
        List<ApontamentoMaterial> materiais = apontamentoMaterialRepository
                .listarPorProjetoComMaterial(projeto.getId());

        assertThat(horas).hasSize(1);
        assertThat(materiais).hasSize(1);
        assertThat(Hibernate.isInitialized(materiais.get(0).getMaterial())).isTrue();
    }

    @Test
    void ordemDeServicoGuardaOsTecnicosNaTabelaAuxiliar() {
        OrdemServico lida = ordemServicoRepository
                .findFirstByProjetoIdAndDataConclusaoIsNullOrderByNumeroDesc(projeto.getId()).orElseThrow();

        assertThat(lida.getTecnicos()).extracting(Usuario::getLogin)
                .containsExactlyInAnyOrder("tecnico", "ajudante");

        Number linhas = (Number) em.getEntityManager()
                .createNativeQuery("select count(*) from ordem_servico_tecnico where ordem_servico_id = :id")
                .setParameter("id", ordemServico.getId())
                .getSingleResult();
        assertThat(linhas.intValue()).isEqualTo(2);
    }
}
