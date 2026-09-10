package com.jclinical.auth.domain.service;

import com.jclinical.auth.domain.ports.in.LoginUseCase;
import com.jclinical.users.domain.model.User;
import com.jclinical.users.domain.ports.out.PasswordHasherPort;
import com.jclinical.users.domain.ports.out.UserRepositoryPort;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class LoginService implements LoginUseCase {

    /**
     * Un único mensaje para "no existe", "contraseña incorrecta" y cualquier otro fallo
     * previo a comprobar la contraseña. Antes se distinguía entre esos casos, lo que
     * permitía averiguar qué correos están registrados y en qué estado, sin conocer
     * ninguna credencial.
     */
    private static final String GENERIC_FAILURE = "Credenciales incorrectas";

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;

    /**
     * Hash de descarte para gastar el mismo tiempo de bcrypt cuando el correo no existe.
     * Sin él, un correo no registrado responde muchísimo más rápido que uno registrado y
     * el mensaje unificado no sirve de nada: el reloj delata la diferencia.
     */
    private final String dummyHash;

    public LoginService(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.dummyHash = passwordHasher.hash(UUID.randomUUID().toString());
    }

    @Override
    public User login(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("El correo y la contraseña son obligatorios");
        }

        Optional<User> found = userRepository.findByEmail(email.trim().toLowerCase());
        if (found.isEmpty()) {
            passwordHasher.matches(password, dummyHash);
            throw new IllegalArgumentException(GENERIC_FAILURE);
        }

        User user = found.get();
        LocalDateTime now = LocalDateTime.now();
        boolean locked = user.isLocked(now);

        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            // Una cuenta ya bloqueada no debe seguir acumulando intentos: eso permitiría
            // extender el bloqueo indefinidamente desde fuera.
            if (!locked) {
                user.registerFailedLoginAttempt(now);
                userRepository.save(user);
            }
            throw new IllegalArgumentException(GENERIC_FAILURE);
        }

        // A partir de aquí quien llama demostró conocer la contraseña, así que un mensaje
        // específico ya no revela nada que no supiera: puede decirle por qué no entra.
        if (locked) {
            throw new IllegalStateException("Cuenta bloqueada temporalmente debido a múltiples intentos fallidos");
        }
        if (!user.isActive()) {
            throw new IllegalStateException("El usuario no está activo o no ha confirmado su correo electrónico");
        }

        user.resetFailedLoginAttempts();
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }
}
