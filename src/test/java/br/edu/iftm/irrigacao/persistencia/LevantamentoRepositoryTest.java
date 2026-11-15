package br.edu.iftm.irrigacao.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.TipoFonteAgua;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(ConfiguracaoJpaTeste.class)
class LevantamentoRepositoryTest {

    private static final LocalDateTime AGENDADA = LocalDateTime.of(2026, 3, 12, 8, 30);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private LevantamentoRepository levantamentoRepository;

    @Test
    void levantamentoNasceSoComADataAgendada() {
        Projeto projeto = DadosTeste.projetoGravado(em, "PRJ-0301");
        levantamentoRepository.saveAndFlush(agendado(projeto));
        em.clear();

        Levantamento lido = levantamentoRepository.findByProjetoId(projeto.getId()).orElseThrow();

        assertThat(lido.getDataAgendada()).isEqualTo(AGENDADA);
        assertThat(lido.getDataVisita()).isNull();
        assertThat(lido.getCultura()).isNull();
        assertThat(lido.getFonteAgua()).isNull();
        assertThat(lido.getFotos()).isEmpty();
    }

    @Test
    void fotosDoLevantamentoVaoParaATabelaAuxiliar() {
        Projeto projeto = DadosTeste.projetoGravado(em, "PRJ-0302");
        Levantamento levantamento = agendado(projeto);
        levantamento.setDataVisita(AGENDADA.plusHours(2));
        levantamento.setCultura("Café");
        levantamento.setAreaIrrigadaHa(12.5);
        levantamento.setFonteAgua(TipoFonteAgua.POCO_ARTESIANO);
        levantamento.setVazaoFonte(18.0);
        levantamento.getFotos().add("levantamento/302/fonte.jpg");
        levantamento.getFotos().add("levantamento/302/area.jpg");
        levantamento.getFotos().add("levantamento/302/casa-de-bombas.jpg");
        Long id = levantamentoRepository.saveAndFlush(levantamento).getId();
        em.clear();

        Levantamento lido = levantamentoRepository.findByProjetoId(projeto.getId()).orElseThrow();

        assertThat(lido.getFonteAgua()).isEqualTo(TipoFonteAgua.POCO_ARTESIANO);
        assertThat(lido.getFotos()).containsExactlyInAnyOrder("levantamento/302/fonte.jpg",
                "levantamento/302/area.jpg", "levantamento/302/casa-de-bombas.jpg");

        Number linhas = (Number) em.getEntityManager()
                .createNativeQuery("select count(*) from levantamento_foto where levantamento_id = :id")
                .setParameter("id", id)
                .getSingleResult();
        assertThat(linhas.intValue()).isEqualTo(3);
    }

    @Test
    void removerUmaFotoApagaALinhaDela() {
        Projeto projeto = DadosTeste.projetoGravado(em, "PRJ-0303");
        Levantamento levantamento = agendado(projeto);
        levantamento.getFotos().add("levantamento/303/fonte.jpg");
        levantamento.getFotos().add("levantamento/303/area.jpg");
        levantamentoRepository.saveAndFlush(levantamento);

        levantamento.getFotos().remove("levantamento/303/area.jpg");
        levantamentoRepository.saveAndFlush(levantamento);
        em.clear();

        Levantamento lido = levantamentoRepository.findByProjetoId(projeto.getId()).orElseThrow();
        assertThat(lido.getFotos()).containsExactly("levantamento/303/fonte.jpg");
    }

    @Test
    void projetoTemUmLevantamentoSo() {
        Projeto projeto = DadosTeste.projetoGravado(em, "PRJ-0304");
        levantamentoRepository.saveAndFlush(agendado(projeto));

        assertThatThrownBy(() -> levantamentoRepository.saveAndFlush(agendado(projeto)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Levantamento agendado(Projeto projeto) {
        Levantamento levantamento = new Levantamento();
        levantamento.setProjeto(projeto);
        levantamento.setDataAgendada(AGENDADA);
        return levantamento;
    }
}
