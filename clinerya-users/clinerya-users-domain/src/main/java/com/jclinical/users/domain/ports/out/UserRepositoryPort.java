package com.jclinical.users.domain.ports.out;

import com.jclinical.users.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
    Optional<User> findByPasswordResetTokenHash(String tokenHash);
    boolean existsByEmail(String email);
}
