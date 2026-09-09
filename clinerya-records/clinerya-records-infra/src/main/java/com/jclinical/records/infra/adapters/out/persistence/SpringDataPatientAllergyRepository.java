package com.jclinical.records.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPatientAllergyRepository extends JpaRepository<PatientAllergyEntity, UUID> {
    List<PatientAllergyEntity> findByClinicIdAndPatientIdOrderByNotedAtDesc(UUID clinicId, UUID patientId);
    Optional<PatientAllergyEntity> findByIdAndClinicId(UUID id, UUID clinicId);
    void deleteByIdAndClinicId(UUID id, UUID clinicId);
}
