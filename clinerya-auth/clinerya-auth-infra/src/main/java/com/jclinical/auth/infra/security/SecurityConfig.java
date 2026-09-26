package com.jclinical.auth.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, RateLimitingFilter rateLimitingFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // API stateless: JWT is sent in the Authorization header, not in browser cookies.
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/users/register",
                    "/api/v1/users/verify-email",
                    "/api/v1/users/resend-verification",
                    "/api/v1/users/password-reset/request",
                    "/api/v1/users/password-reset/confirm",
                    "/api/v1/users/register-staff",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/public/shared-history",
                    "/api/v1/public/shared-history/**",
                    "/api/v1/integrations/google-calendar/callback",
                    // Webhook de WhatsApp: Meta no manda JWT; lo protege la firma HMAC de cada clinica.
                    "/api/v1/public/whatsapp/**",
                    // Handshake del WebSocket: el JWT se valida en el CONNECT de STOMP (StompAuthInterceptor).
                    "/ws",
                    "/ws/**",
                    "/api/v1/info",
                    "/actuator/health",
                    "/actuator/health/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            // Sin sesion valida (sin token, vencido o revocado) -> 401: es la senal con la que el
            // frontend renueva el token de acceso. El default de Spring seria 403, que se reserva
            // para un usuario con sesion que no tiene permiso.
            .exceptionHandling(errors -> errors.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitingFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
