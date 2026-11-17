package br.edu.iftm.irrigacao.config;

import static br.edu.iftm.irrigacao.dominio.acesso.Perfil.ADMINISTRADOR;
import static br.edu.iftm.irrigacao.dominio.acesso.Perfil.ALMOXARIFE;
import static br.edu.iftm.irrigacao.dominio.acesso.Perfil.COMPRADOR;
import static br.edu.iftm.irrigacao.dominio.acesso.Perfil.DIRETORIA;
import static br.edu.iftm.irrigacao.dominio.acesso.Perfil.FINANCEIRO;
import static br.edu.iftm.irrigacao.dominio.acesso.Perfil.PROJETISTA;
import static br.edu.iftm.irrigacao.dominio.acesso.Perfil.TECNICO_CAMPO;
import static br.edu.iftm.irrigacao.dominio.acesso.Perfil.VENDEDOR;

import java.util.stream.Stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;

/**
 * Autenticação e autorização por perfil (RNF04).
 *
 * <p>O login é por formulário, em página própria, com a senha conferida em BCrypt. Cada área do
 * sistema tem um prefixo de URL, liberado para os perfis da tabela abaixo. O ADMINISTRADOR acessa
 * todas. A authority é o nome do {@link Perfil}.
 *
 * <pre>
 * /projetos/**    qualquer usuário logado
 * /comercial/**   VENDEDOR, DIRETORIA
 *                 (GET /comercial/projetos/{id}/faltantes também para COMPRADOR, e o POST das
 *                 cotações só para COMPRADOR)
 * /engenharia/**  PROJETISTA
 * /estoque/**     ALMOXARIFE, COMPRADOR
 *                 (GET /estoque/materiais/busca também para PROJETISTA, que monta a lista técnica;
 *                 POST da requisição de compra só para COMPRADOR e da separação só para ALMOXARIFE)
 * /campo/**       TECNICO_CAMPO, PROJETISTA
 * /financeiro/**  FINANCEIRO, DIRETORIA
 * /diretoria/**   DIRETORIA
 * /admin/**       ADMINISTRADOR
 * </pre>
 *
 * <p>Só {@code /css/**}, {@code /js/**} e {@code /login} abrem sem login. Não há auto-registro:
 * quem cria usuário é o administrador (RF23). A proteção CSRF fica ligada, no padrão do Spring
 * Security.
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final BCryptPasswordEncoder passwordEncoder;

    public SecurityConfig(UserDetailsService userDetailsService, BCryptPasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/login").permitAll()

                // Faltantes e cotações (RF08): o comercial vê, e quem registra a cotação é o comprador
                .requestMatchers(HttpMethod.GET, "/comercial/projetos/*/faltantes")
                    .hasAnyAuthority(comAdministrador(VENDEDOR, DIRETORIA, COMPRADOR))
                .requestMatchers(HttpMethod.POST, "/comercial/projetos/*/faltantes/cotacoes")
                    .hasAnyAuthority(comAdministrador(COMPRADOR))
                .requestMatchers("/comercial/**").hasAnyAuthority(comAdministrador(VENDEDOR, DIRETORIA))
                .requestMatchers("/engenharia/**").hasAnyAuthority(comAdministrador(PROJETISTA))
                // A busca de materiais em JSON serve a lista técnica, que é do projetista (RF05)
                .requestMatchers(HttpMethod.GET, "/estoque/materiais/busca")
                    .hasAnyAuthority(comAdministrador(ALMOXARIFE, COMPRADOR, PROJETISTA))
                // Material do projeto aprovado (RF13, RF14): o comprador requisita, o almoxarife separa e confere
                .requestMatchers(HttpMethod.POST, "/estoque/projetos/*/requisicoes")
                    .hasAnyAuthority(comAdministrador(COMPRADOR))
                .requestMatchers(HttpMethod.POST, "/estoque/projetos/*/separacao", "/estoque/projetos/*/separacao/**")
                    .hasAnyAuthority(comAdministrador(ALMOXARIFE))
                .requestMatchers("/estoque/**").hasAnyAuthority(comAdministrador(ALMOXARIFE, COMPRADOR))
                .requestMatchers("/campo/**").hasAnyAuthority(comAdministrador(TECNICO_CAMPO, PROJETISTA))
                .requestMatchers("/financeiro/**").hasAnyAuthority(comAdministrador(FINANCEIRO, DIRETORIA))
                .requestMatchers("/diretoria/**").hasAnyAuthority(comAdministrador(DIRETORIA))
                .requestMatchers("/admin/**").hasAnyAuthority(comAdministrador())

                .requestMatchers("/projetos/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/")
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/acesso-negado")
            );
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    /** Authorities dos perfis informados, mais a do ADMINISTRADOR, que acessa tudo. */
    private static String[] comAdministrador(Perfil... perfis) {
        return Stream.concat(Stream.of(perfis), Stream.of(ADMINISTRADOR))
                .map(Perfil::name)
                .toArray(String[]::new);
    }
}
