package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.PrivacyConsent;

import java.util.Optional;
import java.util.UUID;

public interface PrivacyConsentRepositoryPort {
    PrivacyConsent save(PrivacyConsent consent);
    Optional<PrivacyConsent> findByPatientIdAndClinicId(UUID patientId, UUID clinicId);
}
