package com.jclinical.auth.infra.adapters.in.web;

import com.jclinical.auth.domain.ports.in.LoginUseCase;
import com.jclinical.auth.domain.ports.in.LogoutUseCase;
import com.jclinical.auth.domain.ports.in.RefreshTokenUseCase;
import com.jclinical.auth.infra.adapters.in.web.dto.*;
import com.jclinical.auth.infra.adapters.out.JwtTokenProvider;
import com.jclinical.users.domain.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(LoginUseCase loginUseCase, LogoutUseCase logoutUseCase,
                          RefreshTokenUseCase refreshTokenUseCase, JwtTokenProvider jwtTokenProvider) {
        this.loginUseCase = loginUseCase;
        this.logoutUseCase = logoutUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        User user = loginUseCase.login(request.email(), request.password());
        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());

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

        LoginResponse loginResponse = new LoginResponse(token, refreshToken, "Bearer", userResponse);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            logoutUseCase.logout(token);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@RequestBody RefreshRequest request) {
        String newToken = refreshTokenUseCase.refresh(request.refreshToken());
        TokenRefreshResponse response = new TokenRefreshResponse(newToken, request.refreshToken(), "Bearer");
        return ResponseEntity.ok(response);
    }
}
