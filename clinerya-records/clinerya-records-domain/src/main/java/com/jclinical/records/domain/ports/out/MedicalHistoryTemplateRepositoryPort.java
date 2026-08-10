package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.MedicalHistoryTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicalHistoryTemplateRepositoryPort {
    MedicalHistoryTemplate save(MedicalHistoryTemplate template);
    Optional<MedicalHistoryTemplate> findByIdAndClinicId(UUID id, UUID clinicId);
    List<MedicalHistoryTemplate> findByClinicId(UUID clinicId);
    boolean existsByIdAndClinicId(UUID id, UUID clinicId);
}
