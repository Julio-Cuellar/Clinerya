package com.jclinical.records.infra.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataMedicalHistoryVersionRepository extends JpaRepository<MedicalHistoryVersionEntity, UUID> {
    List<MedicalHistoryVersionEntity> findByMedicalHistoryIdOrderByVersionDesc(UUID medicalHistoryId);
    Optional<MedicalHistoryVersionEntity> findByMedicalHistoryIdAndVersion(UUID medicalHistoryId, int version);
    Optional<MedicalHistoryVersionEntity> findTopByMedicalHistoryIdOrderByVersionDesc(UUID medicalHistoryId);
}
