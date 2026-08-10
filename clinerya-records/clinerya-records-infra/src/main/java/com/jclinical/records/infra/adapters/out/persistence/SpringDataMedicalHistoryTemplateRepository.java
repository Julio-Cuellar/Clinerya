package com.jclinical.records.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataMedicalHistoryTemplateRepository extends JpaRepository<MedicalHistoryTemplateEntity, UUID> {
    Optional<MedicalHistoryTemplateEntity> findByIdAndClinicId(UUID id, UUID clinicId);
    List<MedicalHistoryTemplateEntity> findByClinicId(UUID clinicId);
    boolean existsByIdAndClinicId(UUID id, UUID clinicId);
}
