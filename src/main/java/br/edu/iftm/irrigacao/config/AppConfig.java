package br.edu.iftm.irrigacao.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Beans de infraestrutura compartilhados pela aplicação.
 */
@Configuration
public class AppConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Relógio do sistema. Toda data e hora da aplicação deve vir deste bean, e não de
     * {@code LocalDate.now()} sem argumento, para que os testes possam controlar o tempo.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
