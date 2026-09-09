package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.ClinicalReviewKind;
import com.jclinical.records.domain.model.PatientClinicalReview;
import com.jclinical.records.domain.ports.out.PatientClinicalReviewRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlPatientClinicalReviewRepository implements PatientClinicalReviewRepositoryPort {

    private final SpringDataPatientClinicalReviewRepository repository;

    @Override
    public PatientClinicalReview save(PatientClinicalReview review) {
        return toDomain(repository.save(toEntity(review)));
    }

    @Override
    public List<PatientClinicalReview> findByClinicIdAndPatientId(UUID clinicId, UUID patientId) {
        return repository.findByClinicIdAndPatientId(clinicId, patientId).stream()
                .map(SqlPatientClinicalReviewRepository::toDomain)
                .toList();
    }

    @Override
    public Optional<PatientClinicalReview> findByClinicIdAndPatientIdAndKind(UUID clinicId, UUID patientId, ClinicalReviewKind kind) {
        return repository.findByClinicIdAndPatientIdAndKind(clinicId, patientId, kind)
                .map(SqlPatientClinicalReviewRepository::toDomain);
    }

    private static PatientClinicalReviewEntity toEntity(PatientClinicalReview domain) {
        return PatientClinicalReviewEntity.builder()
                .id(domain.getId())
                .clinicId(domain.getClinicId())
                .patientId(domain.getPatientId())
                .kind(domain.getKind())
                .noneReported(domain.isNoneReported())
                .reviewedByUserId(domain.getReviewedByUserId())
                .reviewedByUserName(domain.getReviewedByUserName())
                .reviewedAt(domain.getReviewedAt())
                .build();
    }

    private static PatientClinicalReview toDomain(PatientClinicalReviewEntity entity) {
        return PatientClinicalReview.builder()
                .id(entity.getId())
                .clinicId(entity.getClinicId())
                .patientId(entity.getPatientId())
                .kind(entity.getKind())
                .noneReported(entity.isNoneReported())
                .reviewedByUserId(entity.getReviewedByUserId())
                .reviewedByUserName(entity.getReviewedByUserName())
                .reviewedAt(entity.getReviewedAt())
                .build();
    }
}
