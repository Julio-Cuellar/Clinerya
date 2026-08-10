package com.jclinical.records.domain.ports.out;

import com.jclinical.records.domain.model.TemporaryRecordShare;

import java.util.Optional;

public interface TemporaryRecordShareRepositoryPort {
    TemporaryRecordShare save(TemporaryRecordShare share);
    Optional<TemporaryRecordShare> findByToken(String token);
}
