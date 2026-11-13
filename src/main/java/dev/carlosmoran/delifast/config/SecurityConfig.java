package dev.carlosmoran.delifast.config;

import dev.carlosmoran.delifast.security.ProblemDetailSecurityHandler;
import dev.carlosmoran.delifast.security.SupabaseJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * La API es un resource server: cada petición lleva el access token de Supabase Auth
 * (Authorization: Bearer). La firma se comprueba con las claves públicas (JWKS) del
 * proyecto Supabase, configuradas en spring.security.oauth2.resourceserver.jwt.
 * El rol sale del claim user_rol y se convierte en ROLE_&lt;rol&gt;.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        ProblemDetailSecurityHandler errores = new ProblemDetailSecurityHandler();
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/localidades").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/negocio", "/negocio/**").hasRole("NEGOCIO")
                        .requestMatchers(HttpMethod.POST, "/pedidos/*/aceptar", "/pedidos/*/rechazar",
                                "/pedidos/*/listo", "/pedidos/*/asignar").hasRole("NEGOCIO")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new SupabaseJwtAuthenticationConverter()))
                        .authenticationEntryPoint(errores)
                        .accessDeniedHandler(errores))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(errores)
                        .accessDeniedHandler(errores));
        return http.build();
    }
}
