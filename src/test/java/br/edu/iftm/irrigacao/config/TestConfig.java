package br.edu.iftm.irrigacao.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;
import br.edu.iftm.irrigacao.dominio.comercial.PropostaService;

/**
 * Services mockados para os testes {@code @WebMvcTest}, que carregam só a camada web. Junto com
 * esta classe o teste importa o {@code SecurityConfig}, para as regras de acesso valerem; o
 * {@code UserDetailsService} e o encoder daqui são o que ele precisa para subir.
 */
@TestConfiguration
public class TestConfig {

    @Bean
    public UsuarioService usuarioService() {
        return Mockito.mock(UsuarioService.class);
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return Mockito.mock(UserDetailsService.class);
    }

    /** Usado pelo {@code AlertaPropostasAdvice}, que vale para todos os controllers. */
    @Bean
    public PropostaService propostaService() {
        return Mockito.mock(PropostaService.class);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
