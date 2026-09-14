package com.jclinical.auth.infra.adapters.in.web;

import com.jclinical.auth.domain.ports.in.LoginUseCase;
import com.jclinical.auth.domain.ports.in.LogoutUseCase;
import com.jclinical.auth.domain.ports.in.RefreshTokenUseCase;
import com.jclinical.auth.infra.adapters.in.web.dto.LoginRequest;
import com.jclinical.auth.infra.adapters.in.web.dto.LoginResponse;
import com.jclinical.auth.infra.adapters.in.web.dto.TokenRefreshResponse;
import com.jclinical.auth.infra.adapters.out.JwtTokenProvider;
import com.jclinical.users.domain.model.Theme;
import com.jclinical.users.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El refresh token ya no viaja en el cuerpo JSON: se entrega/lee/borra como cookie
 * HttpOnly (ver AuthController). Estas pruebas fijan ese contrato sin levantar Spring.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final long REFRESH_TTL_MS = 604_800_000L;

    @Mock private LoginUseCase loginUseCase;
    @Mock private LogoutUseCase logoutUseCase;
    @Mock private RefreshTokenUseCase refreshTokenUseCase;
    @Mock private JwtTokenProvider jwtTokenProvider;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(loginUseCase, logoutUseCase, refreshTokenUseCase, jwtTokenProvider,
                REFRESH_TTL_MS, true);
    }

    private User user() {
        return User.builder()
                .id(UUID.randomUUID()).email("doc@example.com").fullName("Dra. Ejemplo")
                .themePreference(Theme.LIGHT).active(true).emailVerified(true)
                .build();
    }

    @Test
    void loginSetsRefreshTokenAsHttpOnlyCookieAndOmitsItFromTheBody() {
        User user = user();
        when(loginUseCase.login("doc@example.com", "secret")).thenReturn(user);
        when(jwtTokenProvider.createToken(user.getId(), user.getEmail())).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail())).thenReturn("refresh-token");
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        ResponseEntity<LoginResponse> response = controller.login(
                new LoginRequest("doc@example.com", "secret"), httpResponse);

        assertThat(response.getBody().token()).isEqualTo("access-token");

        String setCookie = httpResponse.getHeader("Set-Cookie");
        assertThat(setCookie).contains("refresh_token=refresh-token");
        assertThat(setCookie).containsIgnoringCase("HttpOnly");
        assertThat(setCookie).containsIgnoringCase("Secure");
        assertThat(setCookie).contains("SameSite=Strict");
        assertThat(setCookie).contains("Path=/api/v1/auth");
        assertThat(setCookie).contains("Max-Age=" + (REFRESH_TTL_MS / 1000));
    }

    @Test
    void refreshReadsTheTokenFromTheCookieParameterNotFromABody() {
        when(refreshTokenUseCase.refresh("refresh-token")).thenReturn("new-access-token");

        ResponseEntity<TokenRefreshResponse> response = controller.refresh("refresh-token");

        assertThat(response.getBody().token()).isEqualTo("new-access-token");
        verify(refreshTokenUseCase).refresh("refresh-token");
    }

    @Test
    void logoutBlacklistsBothTokensAndClearsTheCookie() {
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        controller.logout("Bearer access-token", "refresh-token", httpResponse);

        verify(logoutUseCase).logout("access-token");
        verify(logoutUseCase).logout("refresh-token");

        String setCookie = httpResponse.getHeader("Set-Cookie");
        assertThat(setCookie).contains("refresh_token=");
        assertThat(setCookie).contains("Max-Age=0");
    }

    @Test
    void logoutToleratesAMissingRefreshCookie() {
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        controller.logout("Bearer access-token", null, httpResponse);

        verify(logoutUseCase).logout("access-token");
        verify(logoutUseCase, org.mockito.Mockito.times(1)).logout(any());
    }
}
