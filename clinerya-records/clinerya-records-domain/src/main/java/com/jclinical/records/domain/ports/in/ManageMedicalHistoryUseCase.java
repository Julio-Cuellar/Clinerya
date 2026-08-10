package com.jclinical.records.domain.ports.in;

import com.jclinical.records.domain.model.MedicalHistory;
import com.jclinical.records.domain.model.MedicalHistoryVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageMedicalHistoryUseCase {

    Optional<MedicalHistory> getMedicalHistoryByTemplate(UUID patientId, UUID templateId, UUID clinicId, UUID requestingUserId);

    List<MedicalHistory> getMedicalHistories(UUID patientId, UUID clinicId, UUID requestingUserId);

    MedicalHistory saveMedicalHistory(UUID patientId, UUID clinicId, SaveHistoryCommand command, UUID requestingUserId);

    List<MedicalHistoryVersion> getMedicalHistoryVersions(UUID patientId, UUID templateId, UUID clinicId, UUID requestingUserId);

    Optional<MedicalHistoryVersion> getMedicalHistoryVersion(UUID patientId, UUID templateId, int versionNumber, UUID clinicId, UUID requestingUserId);

    record SaveHistoryCommand(
        UUID templateId,
        String answersJson,
        String changedByUserName,
        String ipAddress,
        String userAgent
    ) {}
}
