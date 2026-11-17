package br.edu.iftm.irrigacao.config;

import java.time.LocalDateTime;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import br.edu.iftm.irrigacao.persistencia.RelogioAjustavel;

/**
 * Troca o relógio da aplicação inteira por um que o teste controla, nos testes
 * {@code @SpringBootTest}. O teste que importa esta configuração deve chamar
 * {@code relogio.definir(...)} no {@code @BeforeEach}, porque o contexto do Spring (e o relógio) é
 * reaproveitado entre as classes de teste.
 */
@TestConfiguration
public class RelogioDeTesteConfig {

    /** Segunda-feira, 16/11/2026, 09:00. */
    public static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 16, 9, 0);

    @Bean
    @Primary
    public RelogioAjustavel relogioAjustavel() {
        return new RelogioAjustavel(INICIO);
    }
}
