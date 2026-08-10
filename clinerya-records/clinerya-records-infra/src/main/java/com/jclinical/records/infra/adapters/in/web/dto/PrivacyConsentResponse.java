package com.jclinical.records.infra.adapters.in.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PrivacyConsentResponse(
        UUID id,
        UUID patientId,
        UUID clinicId,
        String privacyNoticeText,
        String documentHash,
        String signerName,
        String signatureImage,
        String signatureImageHash,
        String ipAddress,
        String userAgent,
        LocalDateTime signedAt,
        LocalDateTime createdAt
) {}
