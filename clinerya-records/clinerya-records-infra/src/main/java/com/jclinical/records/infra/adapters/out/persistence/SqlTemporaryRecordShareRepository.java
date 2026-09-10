package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.TemporaryRecordShare;
import com.jclinical.records.domain.ports.out.TemporaryRecordShareRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SqlTemporaryRecordShareRepository implements TemporaryRecordShareRepositoryPort {

    private final SpringDataTemporaryRecordShareRepository springRepository;

    @Override
    public TemporaryRecordShare save(TemporaryRecordShare share) {
        return toDomain(springRepository.save(toEntity(share)));
    }

    @Override
    public Optional<TemporaryRecordShare> findByTokenHash(String tokenHash) {
        return springRepository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public Optional<TemporaryRecordShare> findById(UUID id) {
        return springRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<TemporaryRecordShare> findActiveByClinicAndPatient(UUID clinicId, UUID patientId) {
        return springRepository.findByClinicIdAndPatientIdAndRevokedAtIsNull(clinicId, patientId).stream()
                .map(this::toDomain)
                .toList();
    }

    private TemporaryRecordShareEntity toEntity(TemporaryRecordShare domain) {
        if (domain == null) return null;
        return TemporaryRecordShareEntity.builder()
                .id(domain.getId())
                .clinicId(domain.getClinicId())
                .patientId(domain.getPatientId())
                .email(domain.getEmail())
                .tokenHash(domain.getTokenHash())
                .createdByUserId(domain.getCreatedByUserId())
                .expiresAt(domain.getExpiresAt())
                .createdAt(domain.getCreatedAt())
                .revokedAt(domain.getRevokedAt())
                .recipientVerifiedAt(domain.getRecipientVerifiedAt())
                .lastAccessedAt(domain.getLastAccessedAt())
                .accessCount(domain.getAccessCount())
                .build();
    }

    private TemporaryRecordShare toDomain(TemporaryRecordShareEntity entity) {
        if (entity == null) return null;
        return TemporaryRecordShare.builder()
                .id(entity.getId())
                .clinicId(entity.getClinicId())
                .patientId(entity.getPatientId())
                .email(entity.getEmail())
                .tokenHash(entity.getTokenHash())
                .createdByUserId(entity.getCreatedByUserId())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .revokedAt(entity.getRevokedAt())
                .recipientVerifiedAt(entity.getRecipientVerifiedAt())
                .lastAccessedAt(entity.getLastAccessedAt())
                .accessCount(entity.getAccessCount())
                .build();
    }
}
