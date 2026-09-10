package com.jclinical.records.domain.ports.in;

import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.TemporaryRecordShare;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ManageTemporaryShareUseCase {

    TemporaryRecordShare createShareLink(UUID clinicId, UUID patientId, String email, int daysValid, UUID requestingUserId);

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
            List<ClinicalNote> clinicalNotes
    ) {}

    record ShareLinkView(
            UUID id,
            String email,
            UUID createdByUserId,
            LocalDateTime createdAt,
            LocalDateTime expiresAt,
            LocalDateTime lastAccessedAt,
            int accessCount
    ) {}
}
