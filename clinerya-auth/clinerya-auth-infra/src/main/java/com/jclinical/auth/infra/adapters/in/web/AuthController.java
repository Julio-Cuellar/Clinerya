package com.jclinical.auth.infra.adapters.in.web;

import com.jclinical.auth.domain.ports.in.LoginUseCase;
import com.jclinical.auth.domain.ports.in.LogoutUseCase;
import com.jclinical.auth.domain.ports.in.RefreshTokenUseCase;
import com.jclinical.auth.infra.adapters.in.web.dto.*;
import com.jclinical.auth.infra.adapters.out.JwtTokenProvider;
import com.jclinical.users.domain.model.User;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /**
     * El refresh token viaja en una cookie HttpOnly (no en el cuerpo JSON ni en
     * localStorage): un XSS que exfiltre localStorage ya no se lleva un token de 7 dias.
     * SameSite=Strict es la mitigacion de CSRF para esta cookie: el navegador no la manda
     * en peticiones cross-site, asi que no hace falta reactivar el filtro CSRF de Spring
     * (que rompería el resto del API, autenticado por cabecera Authorization).
     */
    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final JwtTokenProvider jwtTokenProvider;
    private final long refreshValidityInMilliseconds;
    private final boolean secureCookies;

    public AuthController(LoginUseCase loginUseCase, LogoutUseCase logoutUseCase,
                          RefreshTokenUseCase refreshTokenUseCase, JwtTokenProvider jwtTokenProvider,
                          @Value("${jwt.refresh-expiration:604800000}") long refreshValidityInMilliseconds,
                          @Value("${medicloud.security.secure-cookies:true}") boolean secureCookies) {
        this.loginUseCase = loginUseCase;
        this.logoutUseCase = logoutUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshValidityInMilliseconds = refreshValidityInMilliseconds;
        this.secureCookies = secureCookies;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        User user = loginUseCase.login(request.email(), request.password());
        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());

        response.addHeader(HttpHeaders.SET_COOKIE,
                buildRefreshCookie(refreshToken, Duration.ofMillis(refreshValidityInMilliseconds)).toString());

        String theme = user.getThemePreference() != null ? user.getThemePreference().name() : "LIGHT";
        AuthUserResponse userResponse = new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.isEmailVerified(),
                theme,
                user.isActive()
        );

        LoginResponse loginResponse = new LoginResponse(token, "Bearer", userResponse);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            logoutUseCase.logout(authHeader.substring(7));
        }
        // Sin esto, el refresh token sobrevive al logout y sigue emitiendo tokens de
        // acceso durante toda su vigencia.
        if (refreshToken != null && !refreshToken.isBlank()) {
            logoutUseCase.logout(refreshToken);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie("", Duration.ZERO).toString());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        String newToken = refreshTokenUseCase.refresh(refreshToken);
        return ResponseEntity.ok(new TokenRefreshResponse(newToken, "Bearer"));
    }

    private ResponseCookie buildRefreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
