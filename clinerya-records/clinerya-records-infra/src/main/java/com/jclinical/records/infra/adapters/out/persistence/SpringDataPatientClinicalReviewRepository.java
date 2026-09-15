package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.ClinicalReviewKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPatientClinicalReviewRepository extends JpaRepository<PatientClinicalReviewEntity, UUID> {
    List<PatientClinicalReviewEntity> findByClinicIdAndPatientId(UUID clinicId, UUID patientId);
    Optional<PatientClinicalReviewEntity> findByClinicIdAndPatientIdAndKind(UUID clinicId, UUID patientId, ClinicalReviewKind kind);
}
