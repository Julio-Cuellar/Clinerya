package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.TemporaryRecordShare;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemporaryRecordShareRepositoryPort {
    TemporaryRecordShare save(TemporaryRecordShare share);

    Optional<TemporaryRecordShare> findByTokenHash(String tokenHash);

    Optional<TemporaryRecordShare> findById(UUID id);

    List<TemporaryRecordShare> findActiveByClinicAndPatient(UUID clinicId, UUID patientId);
}
