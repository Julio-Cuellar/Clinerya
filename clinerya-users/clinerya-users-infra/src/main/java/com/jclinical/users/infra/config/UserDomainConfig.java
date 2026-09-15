package com.jclinical.users.infra.config;


import com.jclinical.users.domain.model.Theme;
import com.jclinical.users.domain.model.User;
import com.jclinical.users.domain.ports.out.EventPublisherPort;
import com.jclinical.users.domain.ports.out.PasswordHasherPort;
import com.jclinical.users.domain.ports.out.UserPreRegistrationRepositoryPort;
import com.jclinical.users.domain.ports.out.UserRepositoryPort;

import com.jclinical.users.domain.ports.in.GetUserProfileUseCase;
import com.jclinical.users.domain.service.GetUserProfileService;
import com.jclinical.users.domain.service.RegisterUserService;
import com.jclinical.users.domain.service.ResendVerificationCodeService;
import com.jclinical.users.domain.service.VerifyUserEmailService;
import com.jclinical.users.domain.service.PasswordResetService;
import com.jclinical.users.infra.adapters.in.web.dto.UserResponse;
import com.jclinical.users.infra.adapters.out.UserEntity;
import com.jclinical.users.infra.adapters.out.UserMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDomainConfig {

    @Bean
    public RegisterUserService registerUserService(
            UserRepositoryPort userRepository,
            UserPreRegistrationRepositoryPort preRegistrationRepository,
            PasswordHasherPort passwordHasher,
            com.jclinical.users.domain.ports.out.EmailSenderPort emailSender) {
        return new RegisterUserService(userRepository, preRegistrationRepository, passwordHasher, emailSender);
    }

    @Bean
    public ResendVerificationCodeService resendVerificationCodeService(
            UserPreRegistrationRepositoryPort preRegistrationRepository,
            com.jclinical.users.domain.ports.out.EmailSenderPort emailSender) {
        return new ResendVerificationCodeService(preRegistrationRepository, emailSender);
    }

    @Bean
    public VerifyUserEmailService verifyUserEmailService(
            UserRepositoryPort userRepository,
            UserPreRegistrationRepositoryPort preRegistrationRepository,
            EventPublisherPort eventPublisher) {
        return new VerifyUserEmailService(userRepository, preRegistrationRepository, eventPublisher);
    }

    @Bean
    public PasswordResetService passwordResetService(
            UserRepositoryPort userRepository,
            PasswordHasherPort passwordHasher,
            com.jclinical.users.domain.ports.out.EmailSenderPort emailSender,
            @org.springframework.beans.factory.annotation.Value("${app.frontend-base-url}") String frontendBaseUrl) {
        return new PasswordResetService(userRepository, passwordHasher, emailSender, frontendBaseUrl);
    }

    @Bean
    @ConditionalOnMissingBean(GetUserProfileUseCase.class)
    public GetUserProfileUseCase getUserProfileUseCase(UserRepositoryPort userRepository) {
        return new GetUserProfileService(userRepository);
    }

    @Bean
    @ConditionalOnMissingBean(UserMapper.class)
    public UserMapper userMapper() {
        return new UserMapper() {
            @Override
            public UserEntity toEntity(User domain) {
                if (domain == null) {
                    return null;
                }
                return UserEntity.builder()
                        .id(domain.getId())
                        .email(domain.getEmail())
                        .fullName(domain.getFullName())
                        .phone(domain.getPhone())
                        .passwordHash(domain.getPasswordHash())
                        .avatarUrl(domain.getAvatarUrl())
                        .emailVerified(domain.isEmailVerified())
                        .failedLoginAttempts(domain.getFailedLoginAttempts())
                        .lockedUntil(domain.getLockedUntil())
                        .themePreference(toThemeValue(domain.getThemePreference()))
                        .lastLoginAt(domain.getLastLoginAt())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .active(domain.isActive())
                        .verificationToken(domain.getVerificationToken())
                        .verificationTokenExpiresAt(domain.getVerificationTokenExpiresAt())
                        .passwordResetTokenHash(domain.getPasswordResetTokenHash())
                        .passwordResetTokenExpiresAt(domain.getPasswordResetTokenExpiresAt())
                        .platformAdmin(domain.isPlatformAdmin())
                        .build();
            }

            @Override
            public User toDomain(UserEntity entity) {
                if (entity == null) {
                    return null;
                }
                return User.builder()
                        .id(entity.getId())
                        .email(entity.getEmail())
                        .fullName(entity.getFullName())
                        .phone(entity.getPhone())
                        .passwordHash(entity.getPasswordHash())
                        .avatarUrl(entity.getAvatarUrl())
                        .emailVerified(entity.isEmailVerified())
                        .failedLoginAttempts(entity.getFailedLoginAttempts())
                        .lockedUntil(entity.getLockedUntil())
                        .themePreference(toTheme(entity.getThemePreference()))
                        .lastLoginAt(entity.getLastLoginAt())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .active(entity.isActive())
                        .verificationToken(entity.getVerificationToken())
                        .verificationTokenExpiresAt(entity.getVerificationTokenExpiresAt())
                        .passwordResetTokenHash(entity.getPasswordResetTokenHash())
                        .passwordResetTokenExpiresAt(entity.getPasswordResetTokenExpiresAt())
                        .platformAdmin(entity.isPlatformAdmin())
                        .build();
            }

            @Override
            public UserResponse toResponse(User domain) {
                if (domain == null) {
                    return null;
                }
                return new UserResponse(
                        domain.getId(),
                        domain.getEmail(),
                        domain.getFullName(),
                        domain.getPhone(),
                        domain.getAvatarUrl(),
                        domain.isEmailVerified(),
                        toThemeValue(domain.getThemePreference()),
                        domain.isActive()
                );
            }
        };
    }

    private String toThemeValue(Theme theme) {
        return theme == null ? null : theme.name();
    }

    private Theme toTheme(String theme) {
        return theme == null || theme.isBlank() ? null : Theme.valueOf(theme);
    }
}
