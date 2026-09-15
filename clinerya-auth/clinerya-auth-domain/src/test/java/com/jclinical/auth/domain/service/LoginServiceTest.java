package com.jclinical.auth.domain.service;

import com.jclinical.users.domain.model.User;
import com.jclinical.users.domain.ports.out.PasswordHasherPort;
import com.jclinical.users.domain.ports.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regresion de la enumeracion de usuarios: los mensajes distintos para "no existe",
 * "no activo" y "bloqueada" permitian averiguar que correos estan registrados y en que
 * estado, sin conocer ninguna credencial.
 */
class LoginServiceTest {

    private static final String PASSWORD = "Correcta123!";

    private FakeUserRepository userRepository;
    private LoginService service;

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        service = new LoginService(userRepository, new FakePasswordHasher());
    }

    private User givenUser(boolean active, LocalDateTime lockedUntil) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("doctora@clinica.mx")
                .fullName("Doctora Demo")
                .passwordHash(FakePasswordHasher.hashOf(PASSWORD))
                .active(active)
                .emailVerified(active)
                .failedLoginAttempts(0)
                .lockedUntil(lockedUntil)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
        return user;
    }

    @Test
    void usesTheSameMessageForUnknownEmailAndWrongPassword() {
        givenUser(true, null);

        String unknownEmail = assertThrows(IllegalArgumentException.class,
                () -> service.login("nadie@clinica.mx", PASSWORD)).getMessage();
        String wrongPassword = assertThrows(IllegalArgumentException.class,
                () -> service.login("doctora@clinica.mx", "otra")).getMessage();

        assertEquals(unknownEmail, wrongPassword);
        assertEquals("Credenciales incorrectas", unknownEmail);
    }

    @Test
    void doesNotRevealInactiveAccountsToSomeoneWithoutThePassword() {
        givenUser(false, null);

        String message = assertThrows(IllegalArgumentException.class,
                () -> service.login("doctora@clinica.mx", "otra")).getMessage();

        assertEquals("Credenciales incorrectas", message);
    }

    @Test
    void doesNotRevealLockedAccountsToSomeoneWithoutThePassword() {
        givenUser(true, LocalDateTime.now().plusMinutes(10));

        String message = assertThrows(IllegalArgumentException.class,
                () -> service.login("doctora@clinica.mx", "otra")).getMessage();

        assertEquals("Credenciales incorrectas", message);
    }

    @Test
    void explainsTheRealReasonOnceThePasswordIsProven() {
        givenUser(false, null);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.login("doctora@clinica.mx", PASSWORD));

        assertTrue(error.getMessage().contains("no está activo"));
    }

    @Test
    void doesNotExtendTheLockWithFurtherFailedAttempts() {
        User user = givenUser(true, LocalDateTime.now().plusMinutes(10));
        LocalDateTime lockedUntilBefore = user.getLockedUntil();

        assertThrows(IllegalArgumentException.class, () -> service.login("doctora@clinica.mx", "otra"));

        User stored = userRepository.findByEmail("doctora@clinica.mx").orElseThrow();
        assertEquals(0, stored.getFailedLoginAttempts());
        assertEquals(lockedUntilBefore, stored.getLockedUntil());
    }

    @Test
    void countsFailedAttemptsWhileTheAccountIsNotLocked() {
        givenUser(true, null);

        assertThrows(IllegalArgumentException.class, () -> service.login("doctora@clinica.mx", "otra"));

        assertEquals(1, userRepository.findByEmail("doctora@clinica.mx").orElseThrow().getFailedLoginAttempts());
    }

    @Test
    void logsInAndClearsFailedAttempts() {
        givenUser(true, null);
        assertThrows(IllegalArgumentException.class, () -> service.login("doctora@clinica.mx", "otra"));

        User logged = service.login("  Doctora@Clinica.MX  ", PASSWORD);

        assertEquals(0, logged.getFailedLoginAttempts());
        assertTrue(logged.getLastLoginAt() != null);
    }

    /** Hash reversible de mentira: los tests no necesitan el coste real de bcrypt. */
    private static final class FakePasswordHasher implements PasswordHasherPort {
        static String hashOf(String raw) {
            return "hash:" + raw;
        }

        @Override
        public String hash(String rawPassword) {
            return hashOf(rawPassword);
        }

        @Override
        public boolean matches(String rawPassword, String securedPassword) {
            return securedPassword != null && securedPassword.equals(hashOf(rawPassword));
        }
    }

    private static final class FakeUserRepository implements UserRepositoryPort {
        private final Map<String, User> byEmail = new HashMap<>();

        @Override
        public User save(User user) {
            byEmail.put(user.getEmail().toLowerCase(), user);
            return user;
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.ofNullable(byEmail.get(email == null ? null : email.toLowerCase()));
        }

        @Override
        public Optional<User> findById(UUID id) {
            return byEmail.values().stream().filter(user -> user.getId().equals(id)).findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public Optional<User> findByPasswordResetTokenHash(String tokenHash) {
            return Optional.empty();
        }
    }
}
