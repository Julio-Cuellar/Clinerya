package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.ClinicalReviewKind;
import com.jclinical.records.domain.model.PatientClinicalReview;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientClinicalReviewRepositoryPort {
    PatientClinicalReview save(PatientClinicalReview review);
    List<PatientClinicalReview> findByClinicIdAndPatientId(UUID clinicId, UUID patientId);
    Optional<PatientClinicalReview> findByClinicIdAndPatientIdAndKind(UUID clinicId, UUID patientId, ClinicalReviewKind kind);
}
