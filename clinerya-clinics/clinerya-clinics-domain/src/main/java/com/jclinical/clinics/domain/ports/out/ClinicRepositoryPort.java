package com.jclinical.clinics.domain.ports.out;

import com.jclinical.clinics.domain.model.Clinic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicRepositoryPort {
    Clinic save(Clinic clinic);
    Optional<Clinic> findById(UUID id);
    List<Clinic> findByOwnerUserId(UUID ownerUserId);
}
