package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.PrivacyConsent;

public interface PrivacyConsentMapper {
    PrivacyConsentEntity toEntity(PrivacyConsent domain);
    PrivacyConsent toDomain(PrivacyConsentEntity entity);
}
