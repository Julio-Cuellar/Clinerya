package com.jclinical.auth.infra.config;

import com.jclinical.auth.domain.ports.in.LoginUseCase;
import com.jclinical.auth.domain.ports.in.LogoutUseCase;
import com.jclinical.auth.domain.ports.in.RefreshTokenUseCase;
import com.jclinical.auth.domain.ports.in.ValidateTokenUseCase;
import com.jclinical.auth.domain.ports.out.TokenProviderPort;
import com.jclinical.auth.domain.ports.out.TokenRepositoryPort;
import com.jclinical.auth.domain.service.LoginService;
import com.jclinical.auth.domain.service.LogoutService;
import com.jclinical.auth.domain.service.RefreshTokenService;
import com.jclinical.auth.domain.service.ValidateTokenService;
import com.jclinical.users.domain.ports.out.PasswordHasherPort;
import com.jclinical.users.domain.ports.out.UserRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthDomainConfig {

    @Bean
    public LoginUseCase loginUseCase(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher) {
        return new LoginService(userRepository, passwordHasher);
    }

    @Bean
    public LogoutUseCase logoutUseCase(TokenRepositoryPort tokenRepository) {
        return new LogoutService(tokenRepository);
    }

    @Bean
    public ValidateTokenUseCase validateTokenUseCase(TokenRepositoryPort tokenRepository) {
        return new ValidateTokenService(tokenRepository);
    }

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(TokenProviderPort tokenProvider) {
        return new RefreshTokenService(tokenProvider);
    }
}
