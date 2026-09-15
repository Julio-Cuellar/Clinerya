package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.ClinicalDataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPatientMedicationRepository extends JpaRepository<PatientMedicationEntity, UUID> {
    List<PatientMedicationEntity> findByClinicIdAndPatientIdOrderByNotedAtDesc(UUID clinicId, UUID patientId);
    Optional<PatientMedicationEntity> findByIdAndClinicId(UUID id, UUID clinicId);
    void deleteByIdAndClinicId(UUID id, UUID clinicId);
    void deleteByClinicIdAndPatientIdAndSource(UUID clinicId, UUID patientId, ClinicalDataSource source);
}
