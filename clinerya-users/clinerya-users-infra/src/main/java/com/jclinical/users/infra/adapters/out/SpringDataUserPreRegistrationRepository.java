package com.jclinical.users.infra.adapters.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUserPreRegistrationRepository extends JpaRepository<UserPreRegistrationEntity, UUID> {
    Optional<UserPreRegistrationEntity> findByVerificationToken(String token);
    Optional<UserPreRegistrationEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    void deleteByVerificationTokenExpiresAtBefore(LocalDateTime dateTime);
}
