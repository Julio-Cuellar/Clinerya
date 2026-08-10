package com.jclinical.records.domain.ports.in;

import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.TemporaryRecordShare;

import java.util.List;
import java.util.UUID;

public interface ManageTemporaryShareUseCase {
    TemporaryRecordShare createShareLink(UUID clinicId, UUID patientId, String email, int daysValid, UUID requestingUserId);
    
    SharedRecordSummary getSharedRecord(String token);

    record SharedRecordSummary(
            UUID patientId,
            String patientFullName,
            String patientCurp,
            String patientPhone,
            String patientEmail,
            String clinicName,
            List<ClinicalNote> clinicalNotes
    ) {}
}
