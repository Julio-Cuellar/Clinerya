package com.jclinical.users.domain.ports.out;

import com.jclinical.users.domain.model.UserPreRegistration;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserPreRegistrationRepositoryPort {
    UserPreRegistration save(UserPreRegistration preRegistration);
    Optional<UserPreRegistration> findByVerificationToken(String token);
    Optional<UserPreRegistration> findByEmail(String email);
    boolean existsByEmail(String email);
    void deleteById(UUID id);
    void deleteExpiredBefore(LocalDateTime dateTime);
}
