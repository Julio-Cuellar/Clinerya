package com.jclinical.collaboration.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

public interface ClinicStaffDirectoryPort {

    Optional<UUID> findActiveStaffId(UUID userId, UUID clinicId);
}
