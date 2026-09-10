package com.jclinical.records.domain.ports.in;

import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.SharedSection;
import com.jclinical.records.domain.model.TemporaryRecordShare;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ManageTemporaryShareUseCase {

    TemporaryRecordShare createShareLink(UUID clinicId, UUID patientId, String email, int daysValid,
                                        Set<SharedSection> sections, UUID requestingUserId);

    /**
     * Canje del enlace publico. El token viaja en el cuerpo de la peticion, no en
     * el query string, y cada consulta se registra en la bitacora de acceso.
     */
    SharedRecordSummary getSharedRecord(String token, String ipAddress, String userAgent);

    List<ShareLinkView> listActiveShares(UUID clinicId, UUID patientId, UUID requestingUserId);

    void revokeShare(UUID clinicId, UUID shareId, UUID requestingUserId);

    record SharedRecordSummary(
            UUID patientId,
            String patientFullName,
            String patientCurp,
            String patientPhone,
            String patientEmail,
            String clinicName,
            Set<SharedSection> sections,
            List<ClinicalNote> clinicalNotes,
            List<MedicalHistoryView> medicalHistories,
            List<PrescriptionView> prescriptions
    ) {}

    record MedicalHistoryView(
            String templateName,
            String schemaJson,
            String answersJson,
            LocalDateTime updatedAt
    ) {}

    record PrescriptionView(
            LocalDateTime createdAt,
            String notes,
            List<PrescriptionItemView> items
    ) {}

    record PrescriptionItemView(
            String medicationName,
            String dosage,
            String frequency,
            String duration,
            String instructions
    ) {}

    record ShareLinkView(
            UUID id,
            String email,
            UUID createdByUserId,
            LocalDateTime createdAt,
            LocalDateTime expiresAt,
            LocalDateTime lastAccessedAt,
            int accessCount,
            Set<SharedSection> sections
    ) {}
}
