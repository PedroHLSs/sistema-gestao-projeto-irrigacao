package br.edu.iftm.irrigacao.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import br.edu.iftm.irrigacao.dominio.acesso.UsuarioLogado;

/**
 * Liga a auditoria do Spring Data JPA usada por {@code EntidadeAuditada} (RNF03).
 *
 * <p>Fica separada do {@code AppConfig} para que os testes {@code @DataJpaTest} possam importar
 * só a auditoria e usar um relógio próprio.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "dateTimeProvider")
public class AuditoriaConfig {

    /** O auditor é o id do usuário logado. Sem login, os campos de autoria ficam vazios. */
    @Bean
    public AuditorAware<Long> auditorAware(UsuarioLogado usuarioLogado) {
        return usuarioLogado::idAtual;
    }

    /** As datas da auditoria saem do mesmo {@code Clock} usado pelo resto da aplicação. */
    @Bean
    public DateTimeProvider dateTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }
}
