package com.jclinical.records.domain.service;

import com.jclinical.records.domain.model.MedicalHistoryTemplate;
import com.jclinical.records.domain.ports.in.ManageHistoryTemplateUseCase;
import com.jclinical.records.domain.ports.out.MedicalHistoryTemplateRepositoryPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class HistoryTemplateService implements ManageHistoryTemplateUseCase {

    private final MedicalHistoryTemplateRepositoryPort templateRepository;

    public HistoryTemplateService(MedicalHistoryTemplateRepositoryPort templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    public MedicalHistoryTemplate createTemplate(UUID clinicId, CreateTemplateCommand command) {
        MedicalHistoryTemplate template = MedicalHistoryTemplate.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .name(command.name())
                .description(command.description())
                .schemaJson(command.schemaJson())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return templateRepository.save(template);
    }

    @Override
    public MedicalHistoryTemplate updateTemplate(UUID templateId, UUID clinicId, UpdateTemplateCommand command) {
        MedicalHistoryTemplate template = templateRepository.findByIdAndClinicId(templateId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La plantilla no existe en esta clínica."));

        template.setName(command.name());
        template.setDescription(command.description());
        template.setSchemaJson(command.schemaJson());
        template.setActive(command.active());
        template.setUpdatedAt(LocalDateTime.now());

        return templateRepository.save(template);
    }

    @Override
    public Optional<MedicalHistoryTemplate> getTemplate(UUID templateId, UUID clinicId) {
        return templateRepository.findByIdAndClinicId(templateId, clinicId);
    }

    @Override
    public List<MedicalHistoryTemplate> getTemplatesByClinic(UUID clinicId) {
        return templateRepository.findByClinicId(clinicId);
    }

    @Override
    public void deleteTemplate(UUID templateId, UUID clinicId) {
        MedicalHistoryTemplate template = templateRepository.findByIdAndClinicId(templateId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La plantilla no existe en esta clínica."));
        template.setActive(false);
        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.save(template);
    }
}
