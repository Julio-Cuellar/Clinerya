package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.MedicalHistoryVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicalHistoryVersionRepositoryPort {
    MedicalHistoryVersion save(MedicalHistoryVersion version);
    List<MedicalHistoryVersion> findByMedicalHistoryIdOrderByVersionDesc(UUID medicalHistoryId);
    Optional<MedicalHistoryVersion> findByMedicalHistoryIdAndVersion(UUID medicalHistoryId, int version);
    Optional<MedicalHistoryVersion> findTopByMedicalHistoryIdOrderByVersionDesc(UUID medicalHistoryId);
}
