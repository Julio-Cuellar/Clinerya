package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.ClinicalNoteAddendum;
import com.jclinical.records.domain.ports.out.ClinicalNoteAddendumRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlClinicalNoteAddendumRepository implements ClinicalNoteAddendumRepositoryPort {

    private final SpringDataClinicalNoteAddendumRepository repository;

    @Override
    public ClinicalNoteAddendum save(ClinicalNoteAddendum addendum) {
        return toDomain(repository.save(toEntity(addendum)));
    }

    @Override
    public List<ClinicalNoteAddendum> findByClinicalNoteIdAndClinicIdOrderByCreatedAtAsc(UUID clinicalNoteId, UUID clinicId) {
        return repository.findByClinicalNoteIdAndClinicIdOrderByCreatedAtAsc(clinicalNoteId, clinicId).stream()
                .map(SqlClinicalNoteAddendumRepository::toDomain)
                .toList();
    }

    private static ClinicalNoteAddendumEntity toEntity(ClinicalNoteAddendum domain) {
        return ClinicalNoteAddendumEntity.builder()
                .id(domain.getId())
                .clinicalNoteId(domain.getClinicalNoteId())
                .patientId(domain.getPatientId())
                .clinicId(domain.getClinicId())
                .content(domain.getContent())
                .createdByUserId(domain.getCreatedByUserId())
                .createdByUserName(domain.getCreatedByUserName())
                .ipAddress(domain.getIpAddress())
                .userAgent(domain.getUserAgent())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    private static ClinicalNoteAddendum toDomain(ClinicalNoteAddendumEntity entity) {
        return ClinicalNoteAddendum.builder()
                .id(entity.getId())
                .clinicalNoteId(entity.getClinicalNoteId())
                .patientId(entity.getPatientId())
                .clinicId(entity.getClinicId())
                .content(entity.getContent())
                .createdByUserId(entity.getCreatedByUserId())
                .createdByUserName(entity.getCreatedByUserName())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
