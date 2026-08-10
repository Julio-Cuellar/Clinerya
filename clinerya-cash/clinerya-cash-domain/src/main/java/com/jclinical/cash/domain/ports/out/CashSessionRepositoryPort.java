package com.jclinical.cash.domain.ports.out;

import com.jclinical.cash.domain.model.CashSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashSessionRepositoryPort {

    CashSession save(CashSession session);

    Optional<CashSession> findOpenByClinicId(UUID clinicId);

    Optional<CashSession> findOpenByClinicIdForUpdate(UUID clinicId);

    Optional<CashSession> findByIdAndClinicId(UUID sessionId, UUID clinicId);

    List<CashSession> findByClinicId(UUID clinicId);
}
