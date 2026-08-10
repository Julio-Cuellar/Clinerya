package com.jclinical.records.domain.ports.in;

import com.jclinical.records.domain.model.MedicalHistoryTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageHistoryTemplateUseCase {

    MedicalHistoryTemplate createTemplate(UUID clinicId, CreateTemplateCommand command);

    MedicalHistoryTemplate updateTemplate(UUID templateId, UUID clinicId, UpdateTemplateCommand command);

    Optional<MedicalHistoryTemplate> getTemplate(UUID templateId, UUID clinicId);

    List<MedicalHistoryTemplate> getTemplatesByClinic(UUID clinicId);

    void deleteTemplate(UUID templateId, UUID clinicId);

    record CreateTemplateCommand(
        String name,
        String description,
        String schemaJson
    ) {}

    record UpdateTemplateCommand(
        String name,
        String description,
        String schemaJson,
        boolean active
    ) {}
}
