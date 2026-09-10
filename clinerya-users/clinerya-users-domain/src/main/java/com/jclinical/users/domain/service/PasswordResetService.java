package com.jclinical.users.domain.service;

import com.jclinical.users.domain.model.User;
import com.jclinical.users.domain.ports.out.EmailSenderPort;
import com.jclinical.users.domain.ports.out.PasswordHasherPort;
import com.jclinical.users.domain.ports.out.UserRepositoryPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

public class PasswordResetService {
    private static final int TOKEN_EXPIRATION_MINUTES = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final EmailSenderPort emailSender;
    private final String frontendBaseUrl;

    public PasswordResetService(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher,
                                EmailSenderPort emailSender, String frontendBaseUrl) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.emailSender = emailSender;
        this.frontendBaseUrl = frontendBaseUrl.replaceFirst("/+$", "");
    }

    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        userRepository.findByEmail(email.trim().toLowerCase()).filter(User::isActive).ifPresent(user -> {
            String token = generateToken();
            user.issuePasswordReset(hash(token), TOKEN_EXPIRATION_MINUTES);
            userRepository.save(user);
            emailSender.sendPasswordReset(
                    user.getEmail(), user.getFullName(),
                    frontendBaseUrl + "/reset-password?token=" + token,
                    user.getPasswordResetTokenExpiresAt());
        });
    }

    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank() || newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("La solicitud de recuperación es inválida.");
        }
        User user = userRepository.findByPasswordResetTokenHash(hash(token))
                .orElseThrow(() -> new IllegalArgumentException("El enlace de recuperación es inválido o ya fue utilizado."));
        user.resetPassword(passwordHasher.hash(newPassword));
        userRepository.save(user);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible.", exception);
        }
    }
}
