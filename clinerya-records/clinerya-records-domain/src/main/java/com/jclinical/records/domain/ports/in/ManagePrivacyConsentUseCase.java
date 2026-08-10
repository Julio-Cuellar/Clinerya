package com.jclinical.records.domain.ports.in;

import com.jclinical.records.domain.model.PrivacyConsent;

import java.util.Optional;
import java.util.UUID;

public interface ManagePrivacyConsentUseCase {

    PrivacyConsent saveConsent(UUID patientId, UUID clinicId, SignConsentCommand command, UUID currentUserId);

    Optional<PrivacyConsent> getConsent(UUID patientId, UUID clinicId, UUID currentUserId);

    record SignConsentCommand(
            String privacyNoticeText,
            String signerName,
            String signatureImage,
            String ipAddress,
            String userAgent
    ) {}
}
