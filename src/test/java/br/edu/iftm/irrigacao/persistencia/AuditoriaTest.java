package br.edu.iftm.irrigacao.persistencia;

import static br.edu.iftm.irrigacao.persistencia.ConfiguracaoJpaTeste.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import br.edu.iftm.irrigacao.dominio.acesso.UsuarioLogado;
import br.edu.iftm.irrigacao.dominio.comercial.Produtor;

/**
 * Preenchimento dos campos de {@code EntidadeAuditada} (RNF03).
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(ConfiguracaoJpaTeste.class)
class AuditoriaTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ProdutorRepository produtorRepository;

    @Autowired
    private RelogioAjustavel relogio;

    @MockitoBean
    private UsuarioLogado usuarioLogado;

    @BeforeEach
    void voltaORelogio() {
        relogio.definir(AGORA);
    }

    @Test
    void inclusaoGravaQuemCriouEQuando() {
        when(usuarioLogado.idAtual()).thenReturn(Optional.of(7L));

        Long id = produtorRepository.saveAndFlush(DadosTeste.produtor("111.111.111-11")).getId();
        em.clear();

        Produtor lido = produtorRepository.findById(id).orElseThrow();
        assertThat(lido.getCriadoPor()).isEqualTo(7L);
        assertThat(lido.getCriadoEm()).isEqualTo(AGORA);
        assertThat(lido.getAlteradoPor()).isEqualTo(7L);
        assertThat(lido.getAlteradoEm()).isEqualTo(AGORA);
    }

    @Test
    void alteracaoTrocaSoQuemAlterouEQuando() {
        when(usuarioLogado.idAtual()).thenReturn(Optional.of(7L));
        Produtor produtor = produtorRepository.saveAndFlush(DadosTeste.produtor("222.222.222-22"));

        LocalDateTime diaSeguinte = AGORA.plusDays(1);
        relogio.definir(diaSeguinte);
        when(usuarioLogado.idAtual()).thenReturn(Optional.of(9L));
        produtor.setTelefone("(34) 3333-0000");
        produtorRepository.saveAndFlush(produtor);
        em.clear();

        Produtor lido = produtorRepository.findById(produtor.getId()).orElseThrow();
        assertThat(lido.getTelefone()).isEqualTo("(34) 3333-0000");
        assertThat(lido.getCriadoPor()).isEqualTo(7L);
        assertThat(lido.getCriadoEm()).isEqualTo(AGORA);
        assertThat(lido.getAlteradoPor()).isEqualTo(9L);
        assertThat(lido.getAlteradoEm()).isEqualTo(diaSeguinte);
    }

    @Test
    void semUsuarioLogadoAAutoriaFicaVaziaEADataEGravada() {
        when(usuarioLogado.idAtual()).thenReturn(Optional.empty());

        Long id = produtorRepository.saveAndFlush(DadosTeste.produtor("333.333.333-33")).getId();
        em.clear();

        Produtor lido = produtorRepository.findById(id).orElseThrow();
        assertThat(lido.getCriadoPor()).isNull();
        assertThat(lido.getAlteradoPor()).isNull();
        assertThat(lido.getCriadoEm()).isEqualTo(AGORA);
        assertThat(lido.getAlteradoEm()).isEqualTo(AGORA);
    }
}
