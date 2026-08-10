package com.jclinical.records.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataClinicalNoteRepository extends JpaRepository<ClinicalNoteEntity, UUID> {
    Optional<ClinicalNoteEntity> findByIdAndPatientIdAndClinicId(UUID id, UUID patientId, UUID clinicId);
    List<ClinicalNoteEntity> findByPatientIdAndClinicIdOrderByCreatedAtDesc(UUID patientId, UUID clinicId);
}
