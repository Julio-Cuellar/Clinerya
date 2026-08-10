package com.jclinical.auth.domain.service;

import com.jclinical.auth.domain.ports.in.LoginUseCase;
import com.jclinical.users.domain.model.User;
import com.jclinical.users.domain.ports.out.PasswordHasherPort;
import com.jclinical.users.domain.ports.out.UserRepositoryPort;
import java.time.LocalDateTime;

public class LoginService implements LoginUseCase {
    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;

    public LoginService(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public User login(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("El correo y la contraseña son obligatorios");
        }
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales incorrectas"));

        if (!user.isActive()) {
            throw new IllegalStateException("El usuario no está activo o no ha confirmado su correo electrónico");
        }

        if (user.isLocked(LocalDateTime.now())) {
            throw new IllegalStateException("Cuenta bloqueada temporalmente debido a múltiples intentos fallidos");
        }

        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            user.registerFailedLoginAttempt(LocalDateTime.now());
            userRepository.save(user);
            throw new IllegalArgumentException("Credenciales incorrectas");
        }

        user.resetFailedLoginAttempts();
        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}
