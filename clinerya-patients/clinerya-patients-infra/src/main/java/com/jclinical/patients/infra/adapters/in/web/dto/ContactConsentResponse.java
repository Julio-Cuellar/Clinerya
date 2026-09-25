package com.jclinical.patients.infra.adapters.in.web.dto;

import com.jclinical.patients.domain.model.ConsentSource;
import com.jclinical.patients.domain.model.ContactConsent;

import java.time.LocalDateTime;
import java.util.UUID;

public record ContactConsentResponse(
        boolean granted,
        String textVersion,
        ConsentSource source,
        UUID recordedByUserId,
        LocalDateTime recordedAt
) {
    public static ContactConsentResponse from(ContactConsent consent) {
        return new ContactConsentResponse(consent.granted(), consent.textVersion(), consent.source(),
                consent.recordedByUserId(), consent.recordedAt());
    }
}
