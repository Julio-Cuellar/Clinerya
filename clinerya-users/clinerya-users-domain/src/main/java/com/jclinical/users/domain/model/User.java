package com.jclinical.users.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private String passwordHash;
    private String avatarUrl;
    private boolean emailVerified;
    private int failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private Theme themePreference;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;
    private String verificationToken;
    private LocalDateTime verificationTokenExpiresAt;

    // Métodos de negocio
    public void verifyEmail() {
        this.emailVerified = true;
        this.active = true;
        this.verificationToken = null;
        this.verificationTokenExpiresAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void generateVerificationToken(String token, int expirationMinutes) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("El token no puede ser vacío");
        }
        this.verificationToken = token;
        this.verificationTokenExpiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);
        this.updatedAt = LocalDateTime.now();
    }

    public void verifyEmailWithToken(String token) {
        if (this.verificationToken == null || !this.verificationToken.equals(token)) {
            throw new IllegalArgumentException("Token de verificación inválido");
        }
        if (this.verificationTokenExpiresAt == null || LocalDateTime.now().isAfter(this.verificationTokenExpiresAt)) {
            throw new IllegalStateException("El token de verificación ha expirado");
        }
        verifyEmail();
    }

    public void registerFailedLoginAttempt(LocalDateTime lockTime) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = lockTime.plusMinutes(15);
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isLocked(LocalDateTime now) {
        if (this.lockedUntil == null) {
            return false;
        }
        return now.isBefore(this.lockedUntil);
    }

    public void changeTheme(Theme newTheme) {
        this.themePreference = newTheme;
        this.updatedAt = LocalDateTime.now();
    }
}
