package com.jclinical.records.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataMedicalHistoryRepository extends JpaRepository<MedicalHistoryEntity, UUID> {
    Optional<MedicalHistoryEntity> findByPatientIdAndTemplateIdAndClinicId(UUID patientId, UUID templateId, UUID clinicId);
    List<MedicalHistoryEntity> findByPatientIdAndClinicId(UUID patientId, UUID clinicId);
}
