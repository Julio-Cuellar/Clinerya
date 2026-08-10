package com.jclinical.records.infra.adapters.in.web.dto;

import java.util.UUID;

public record SignPrivacyConsentRequest(
        UUID clinicId,
        String privacyNoticeText,
        String signerName,
        String signatureImage
) {}
