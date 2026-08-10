package com.jclinical.records.infra.adapters.out.persistence;

import com.jclinical.records.domain.model.PrivacyConsent;
import com.jclinical.records.domain.ports.out.PrivacyConsentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqlPrivacyConsentRepository implements PrivacyConsentRepositoryPort {

    private final SpringDataPrivacyConsentRepository repository;
    private final PrivacyConsentMapper mapper;

    @Override
    public PrivacyConsent save(PrivacyConsent consent) {
        PrivacyConsentEntity entity = mapper.toEntity(consent);
        PrivacyConsentEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PrivacyConsent> findByPatientIdAndClinicId(UUID patientId, UUID clinicId) {
        return repository.findByPatientIdAndClinicId(patientId, clinicId)
                .map(mapper::toDomain);
    }
}
