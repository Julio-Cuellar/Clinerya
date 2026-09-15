package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.ClinicalNoteDiagnosis;
import com.jclinical.records.domain.ports.out.ClinicalNoteDiagnosisRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlClinicalNoteDiagnosisRepository implements ClinicalNoteDiagnosisRepositoryPort {

    private final SpringDataClinicalNoteDiagnosisRepository repository;

    @Override
    public List<ClinicalNoteDiagnosis> saveAll(List<ClinicalNoteDiagnosis> diagnoses) {
        List<ClinicalNoteDiagnosisEntity> entities = diagnoses.stream()
                .map(SqlClinicalNoteDiagnosisRepository::toEntity)
                .toList();
        return repository.saveAll(entities).stream()
                .map(SqlClinicalNoteDiagnosisRepository::toDomain)
                .toList();
    }

    @Override
    public List<ClinicalNoteDiagnosis> findByClinicalNoteIdAndClinicId(UUID clinicalNoteId, UUID clinicId) {
        return repository.findByClinicalNoteIdAndClinicIdOrderByCreatedAtAsc(clinicalNoteId, clinicId).stream()
                .map(SqlClinicalNoteDiagnosisRepository::toDomain)
                .toList();
    }

    private static ClinicalNoteDiagnosisEntity toEntity(ClinicalNoteDiagnosis domain) {
        return ClinicalNoteDiagnosisEntity.builder()
                .id(domain.getId())
                .clinicalNoteId(domain.getClinicalNoteId())
                .clinicId(domain.getClinicId())
                .icd10Code(domain.getIcd10Code())
                .kind(domain.getKind())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    private static ClinicalNoteDiagnosis toDomain(ClinicalNoteDiagnosisEntity entity) {
        return ClinicalNoteDiagnosis.builder()
                .id(entity.getId())
                .clinicalNoteId(entity.getClinicalNoteId())
                .clinicId(entity.getClinicId())
                .icd10Code(entity.getIcd10Code())
                .kind(entity.getKind())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
