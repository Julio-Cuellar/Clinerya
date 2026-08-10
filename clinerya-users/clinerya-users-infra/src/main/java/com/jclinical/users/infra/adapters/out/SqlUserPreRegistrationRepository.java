package com.jclinical.users.infra.adapters.out;

import com.jclinical.users.domain.model.UserPreRegistration;
import com.jclinical.users.domain.ports.out.UserPreRegistrationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlUserPreRegistrationRepository implements UserPreRegistrationRepositoryPort {

    private final SpringDataUserPreRegistrationRepository springDataRepository;

    @Override
    public UserPreRegistration save(UserPreRegistration domain) {
        UserPreRegistrationEntity entity = UserPreRegistrationEntity.builder()
                .id(domain.getId())
                .email(domain.getEmail())
                .fullName(domain.getFullName())
                .passwordHash(domain.getPasswordHash())
                .clinicName(domain.getClinicName())
                .verificationToken(domain.getVerificationToken())
                .verificationTokenExpiresAt(domain.getVerificationTokenExpiresAt())
                .createdAt(domain.getCreatedAt())
                .build();
        UserPreRegistrationEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<UserPreRegistration> findByVerificationToken(String token) {
        return springDataRepository.findByVerificationToken(token).map(this::toDomain);
    }

    @Override
    public Optional<UserPreRegistration> findByEmail(String email) {
        return springDataRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataRepository.existsByEmail(email);
    }

    @Override
    public void deleteById(UUID id) {
        springDataRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteExpiredBefore(LocalDateTime dateTime) {
        springDataRepository.deleteByVerificationTokenExpiresAtBefore(dateTime);
    }

    private UserPreRegistration toDomain(UserPreRegistrationEntity entity) {
        return UserPreRegistration.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .fullName(entity.getFullName())
                .passwordHash(entity.getPasswordHash())
                .clinicName(entity.getClinicName())
                .verificationToken(entity.getVerificationToken())
                .verificationTokenExpiresAt(entity.getVerificationTokenExpiresAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
