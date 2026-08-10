package com.jclinical.accounting.domain.ports.out;

import com.jclinical.accounting.domain.model.OpeningBalanceSetup;

import java.util.Optional;
import java.util.UUID;

public interface OpeningBalanceSetupRepositoryPort {
    OpeningBalanceSetup save(OpeningBalanceSetup setup);

    Optional<OpeningBalanceSetup> findByClinicId(UUID clinicId);

    boolean existsByClinicId(UUID clinicId);
}
