package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.TemporaryRecordShare;
import com.jclinical.records.domain.ports.out.TemporaryRecordShareRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SqlTemporaryRecordShareRepository implements TemporaryRecordShareRepositoryPort {

    private final SpringDataTemporaryRecordShareRepository springRepository;

    @Override
    public TemporaryRecordShare save(TemporaryRecordShare share) {
        TemporaryRecordShareEntity entity = toEntity(share);
        TemporaryRecordShareEntity saved = springRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<TemporaryRecordShare> findByToken(String token) {
        return springRepository.findByToken(token)
                .map(this::toDomain);
    }

    private TemporaryRecordShareEntity toEntity(TemporaryRecordShare domain) {
        if (domain == null) return null;
        return TemporaryRecordShareEntity.builder()
                .id(domain.getId())
                .clinicId(domain.getClinicId())
                .patientId(domain.getPatientId())
                .email(domain.getEmail())
                .token(domain.getToken())
                .expiresAt(domain.getExpiresAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    private TemporaryRecordShare toDomain(TemporaryRecordShareEntity entity) {
        if (entity == null) return null;
        return TemporaryRecordShare.builder()
                .id(entity.getId())
                .clinicId(entity.getClinicId())
                .patientId(entity.getPatientId())
                .email(entity.getEmail())
                .token(entity.getToken())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
