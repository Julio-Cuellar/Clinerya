package com.jclinical.clinics.domain.ports.out;

import com.jclinical.clinics.domain.model.ClinicRoom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicRoomRepositoryPort {
    ClinicRoom save(ClinicRoom room);
    Optional<ClinicRoom> findByIdAndClinicId(UUID id, UUID clinicId);
    List<ClinicRoom> findByClinicId(UUID clinicId);
    List<ClinicRoom> findActiveByClinicId(UUID clinicId);
    boolean existsByNameAndClinicId(String name, UUID clinicId, UUID excludeId);
}
