package com.jclinical.records.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.records.domain.model.MedicalHistory;
import com.jclinical.records.domain.model.MedicalHistoryVersion;
import com.jclinical.records.domain.ports.in.ManageMedicalHistoryUseCase;
import com.jclinical.records.domain.ports.out.MedicalHistoryRepositoryPort;
import com.jclinical.records.domain.ports.out.MedicalHistoryTemplateRepositoryPort;
import com.jclinical.records.domain.ports.out.MedicalHistoryVersionRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.records.domain.ports.out.PatientValidatorPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MedicalHistoryService implements ManageMedicalHistoryUseCase {

    private final MedicalHistoryRepositoryPort historyRepository;
    private final MedicalHistoryTemplateRepositoryPort templateRepository;
    private final PatientValidatorPort patientValidator;
    private final PatientAccessAuthorizationPort accessAuthorizationPort;
    private final MedicalHistoryVersionRepositoryPort versionRepository;

    public MedicalHistoryService(MedicalHistoryRepositoryPort historyRepository,
                                 MedicalHistoryTemplateRepositoryPort templateRepository,
                                 PatientValidatorPort patientValidator,
                                 PatientAccessAuthorizationPort accessAuthorizationPort,
                                 MedicalHistoryVersionRepositoryPort versionRepository) {
        this.historyRepository = historyRepository;
        this.templateRepository = templateRepository;
        this.patientValidator = patientValidator;
        this.accessAuthorizationPort = accessAuthorizationPort;
        this.versionRepository = versionRepository;
    }

    @Override
    public Optional<MedicalHistory> getMedicalHistoryByTemplate(UUID patientId, UUID templateId, UUID clinicId, UUID requestingUserId) {
        authorize(requestingUserId, patientId, clinicId, false);
        return historyRepository.findByPatientIdAndTemplateIdAndClinicId(patientId, templateId, clinicId);
    }

    @Override
    public List<MedicalHistory> getMedicalHistories(UUID patientId, UUID clinicId, UUID requestingUserId) {
        authorize(requestingUserId, patientId, clinicId, false);
        return historyRepository.findByPatientIdAndClinicId(patientId, clinicId);
    }

    @Override
    public MedicalHistory saveMedicalHistory(UUID patientId, UUID clinicId, SaveHistoryCommand command, UUID requestingUserId) {
        authorize(requestingUserId, patientId, clinicId, true);

        // Validar que la plantilla existe y pertenece a esta clínica
        if (!templateRepository.existsByIdAndClinicId(command.templateId(), clinicId)) {
            throw new IllegalArgumentException("La plantilla de historia clínica no existe para esta clínica.");
        }

        Optional<MedicalHistory> existingOpt = historyRepository.findByPatientIdAndTemplateIdAndClinicId(
                patientId, command.templateId(), clinicId);

        MedicalHistory history;
        int nextVersion = 1;
        if (existingOpt.isPresent()) {
            history = existingOpt.get();
            nextVersion = history.getVersion() + 1;
            history.setAnswersJson(command.answersJson());
            history.setVersion(nextVersion);
            history.setUpdatedAt(LocalDateTime.now());
        } else {
            history = MedicalHistory.builder()
                    .id(UUID.randomUUID())
                    .patientId(patientId)
                    .clinicId(clinicId)
                    .templateId(command.templateId())
                    .answersJson(command.answersJson())
                    .version(nextVersion)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        MedicalHistory savedHistory = historyRepository.save(history);

        // Guardar instantánea de la versión
        MedicalHistoryVersion historyVersion = MedicalHistoryVersion.builder()
                .id(UUID.randomUUID())
                .medicalHistoryId(savedHistory.getId())
                .version(nextVersion)
                .answersJson(command.answersJson())
                .changedByUserId(requestingUserId)
                .changedByUserName(command.changedByUserName() != null ? command.changedByUserName() : "Médico")
                .ipAddress(command.ipAddress())
                .userAgent(command.userAgent())
                .createdAt(LocalDateTime.now())
                .build();
        versionRepository.save(historyVersion);

        return savedHistory;
    }

    @Override
    public List<MedicalHistoryVersion> getMedicalHistoryVersions(UUID patientId, UUID templateId, UUID clinicId, UUID requestingUserId) {
        authorize(requestingUserId, patientId, clinicId, false);
        Optional<MedicalHistory> historyOpt = historyRepository.findByPatientIdAndTemplateIdAndClinicId(patientId, templateId, clinicId);
        if (historyOpt.isEmpty()) {
            return List.of();
        }
        return versionRepository.findByMedicalHistoryIdOrderByVersionDesc(historyOpt.get().getId());
    }

    @Override
    public Optional<MedicalHistoryVersion> getMedicalHistoryVersion(UUID patientId, UUID templateId, int versionNumber, UUID clinicId, UUID requestingUserId) {
        authorize(requestingUserId, patientId, clinicId, false);
        Optional<MedicalHistory> historyOpt = historyRepository.findByPatientIdAndTemplateIdAndClinicId(patientId, templateId, clinicId);
        if (historyOpt.isEmpty()) {
            return Optional.empty();
        }
        return versionRepository.findByMedicalHistoryIdAndVersion(historyOpt.get().getId(), versionNumber);
    }

    private void authorize(UUID requestingUserId, UUID patientId, UUID clinicId, boolean requireWrite) {
        if (!patientValidator.existsByIdAndClinicId(patientId, clinicId)) {
            throw new IllegalArgumentException("El paciente no existe en esta clínica.");
        }
        AccessDecision decision = accessAuthorizationPort.resolveAccess(requestingUserId, clinicId, patientId);
        if (decision.level() == AccessLevel.NONE || (requireWrite && decision.level() != AccessLevel.READ_WRITE)) {
            throw new ClinicAccessDeniedException("No tienes acceso a este expediente.");
        }
    }
}
