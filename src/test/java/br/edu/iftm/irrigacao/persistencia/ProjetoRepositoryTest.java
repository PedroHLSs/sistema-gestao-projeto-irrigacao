package br.edu.iftm.irrigacao.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
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

import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(ConfiguracaoJpaTeste.class)
class ProjetoRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Test
    void gravaProjetoComPropriedadeEProdutor() {
        Long id = DadosTeste.projetoGravado(em, "PRJ-0001").getId();
        em.flush();
        em.clear();

        Projeto lido = projetoRepository.buscarComPropriedadeEProdutor(id).orElseThrow();

        assertThat(lido.getCodigo()).isEqualTo("PRJ-0001");
        assertThat(lido.getDataAbertura()).isEqualTo(LocalDate.of(2026, 3, 2));
        assertThat(Hibernate.isInitialized(lido.getPropriedade())).isTrue();
        assertThat(Hibernate.isInitialized(lido.getPropriedade().getProdutor())).isTrue();
        assertThat(Hibernate.isInitialized(lido.getResponsavel())).isTrue();
        assertThat(lido.getPropriedade().getNome()).isEqualTo("Fazenda Boa Vista");
        assertThat(lido.getPropriedade().getProdutor().getCpfCnpj()).isEqualTo("cpf-PRJ-0001");
        assertThat(lido.getResponsavel().getLogin()).isEqualTo("vendedor-PRJ-0001");
    }

    @Test
    void projetoNovoNasceComOsValoresPadrao() {
        Long id = DadosTeste.projetoGravado(em, "PRJ-0002").getId();
        em.flush();
        em.clear();

        Projeto lido = projetoRepository.findById(id).orElseThrow();

        assertThat(lido.getSituacao()).isEqualTo(SituacaoProjeto.EM_LEVANTAMENTO);
        assertThat(lido.getToleranciaPercentual()).isEqualTo(2.0);
        assertThat(lido.isSeparacaoConferida()).isFalse();
        assertThat(lido.isSobrasDevolvidas()).isFalse();
        assertThat(lido.getPressaoProjetoMca()).isNull();
        assertThat(lido.getCustoOrcado()).isNull();
        assertThat(lido.getDataInicioGarantia()).isNull();
    }

    @Test
    void listaTrazOsMaisNovosPrimeiroComAsAssociacoesCarregadas() {
        Projeto antigo = DadosTeste.projetoGravado(em, "PRJ-0003");
        antigo.setDataAbertura(LocalDate.of(2026, 1, 15));
        Projeto novo = DadosTeste.projetoGravado(em, "PRJ-0004");
        novo.setSituacao(SituacaoProjeto.ORCANDO);
        em.flush();
        em.clear();

        List<Projeto> todos = projetoRepository.listarComPropriedadeEProdutor();
        List<Projeto> orcando = projetoRepository
                .listarPorSituacaoComPropriedadeEProdutor(SituacaoProjeto.ORCANDO);

        assertThat(todos).extracting(Projeto::getCodigo).containsExactly("PRJ-0004", "PRJ-0003");
        assertThat(todos).allSatisfy(
                p -> assertThat(Hibernate.isInitialized(p.getPropriedade().getProdutor())).isTrue());
        assertThat(orcando).extracting(Projeto::getCodigo).containsExactly("PRJ-0004");
    }

    @Test
    void codigoDoProjetoNaoSeRepete() {
        Projeto primeiro = DadosTeste.projetoGravado(em, "PRJ-0005");
        em.flush();

        Projeto repetido = DadosTeste.projeto("PRJ-0005", primeiro.getPropriedade(), primeiro.getResponsavel());

        assertThatThrownBy(() -> projetoRepository.saveAndFlush(repetido))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
