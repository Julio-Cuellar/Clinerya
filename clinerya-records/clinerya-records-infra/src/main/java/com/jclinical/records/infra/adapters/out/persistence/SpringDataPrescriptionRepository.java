package com.jclinical.records.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPrescriptionRepository extends JpaRepository<PrescriptionEntity, UUID> {
    List<PrescriptionEntity> findByClinicIdAndPatientIdOrderByCreatedAtDesc(UUID clinicId, UUID patientId);
    Optional<PrescriptionEntity> findByClinicIdAndId(UUID clinicId, UUID id);
}
