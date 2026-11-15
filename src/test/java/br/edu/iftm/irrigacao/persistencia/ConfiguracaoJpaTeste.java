package br.edu.iftm.irrigacao.persistencia;

import java.time.LocalDateTime;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import br.edu.iftm.irrigacao.config.AuditoriaConfig;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioLogado;

/**
 * O {@code @DataJpaTest} não carrega as classes {@code @Configuration} nem os componentes da
 * aplicação. Esta configuração traz a auditoria para dentro do teste e troca o relógio do sistema
 * por um que o teste controla.
 */
@TestConfiguration
@Import({ AuditoriaConfig.class, UsuarioLogado.class })
public class ConfiguracaoJpaTeste {

    /** Data e hora em que o relógio dos testes começa. */
    public static final LocalDateTime AGORA = LocalDateTime.of(2026, 3, 10, 9, 30);

    @Bean
    public RelogioAjustavel clock() {
        return new RelogioAjustavel(AGORA);
    }
}
