package com.jclinical.auth.infra.security;

import com.jclinical.auth.domain.ports.in.ValidateTokenUseCase;
import com.jclinical.auth.infra.adapters.out.JwtTokenProvider;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El frontend solo renueva el token de acceso ante un 401. Si una peticion sin sesion valida
 * (sin token o con uno vencido) responde 403, la renovacion nunca se dispara y la app queda
 * rota a los 30 minutos aunque la cookie de refresco siga vigente. El 403 queda reservado para
 * un usuario con sesion que no tiene permiso.
 */
@SpringJUnitWebConfig(SecurityConfigTest.TestConfig.class)
class SecurityConfigTest {

    private static final String VALID = "token-valido";
    private static final String EXPIRED = "token-vencido";

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestConfig {

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            JwtTokenProvider provider = mock(JwtTokenProvider.class);
            when(provider.validateAccessToken(anyString())).thenAnswer(call -> VALID.equals(call.getArgument(0)));
            when(provider.getEmailFromToken(VALID)).thenReturn("demo@medicloud.local");
            return provider;
        }

        @Bean
        ValidateTokenUseCase validateTokenUseCase() {
            return token -> true;
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider provider, ValidateTokenUseCase validity) {
            return new JwtAuthenticationFilter(provider, validity);
        }

        @Bean
        RateLimitingFilter rateLimitingFilter() {
            return new RateLimitingFilter(1);
        }

        @Bean
        ProbeController probeController() {
            return new ProbeController();
        }
    }

    @RestController
    static class ProbeController {

        @GetMapping("/api/v1/probe")
        String probe() {
            return "ok";
        }

        @GetMapping("/api/v1/probe/forbidden")
        String forbidden() {
            throw new AccessDeniedException("Sin permiso");
        }

        @PostMapping("/api/v1/auth/refresh")
        String refresh() {
            return "renovado";
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
    }

    @Test
    void aRequestWithoutTokenIsUnauthorizedSoTheClientKnowsToRefresh() throws Exception {
        mvc.perform(get("/api/v1/probe")).andExpect(status().isUnauthorized());
    }

    @Test
    void anExpiredOrInvalidTokenIsUnauthorizedNotForbidden() throws Exception {
        mvc.perform(get("/api/v1/probe").header("Authorization", "Bearer " + EXPIRED))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aValidTokenReachesTheEndpoint() throws Exception {
        mvc.perform(get("/api/v1/probe").header("Authorization", "Bearer " + VALID))
                .andExpect(status().isOk());
    }

    @Test
    void theRefreshEndpointStaysOpenWithAnExpiredAccessToken() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh").header("Authorization", "Bearer " + EXPIRED))
                .andExpect(status().isOk());
    }

    @Test
    void anAuthenticatedUserWithoutPermissionIsStillForbidden() throws Exception {
        mvc.perform(get("/api/v1/probe/forbidden").header("Authorization", "Bearer " + VALID))
                .andExpect(status().isForbidden());
    }
}
