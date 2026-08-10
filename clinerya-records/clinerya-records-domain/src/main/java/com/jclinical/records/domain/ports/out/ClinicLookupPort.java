package com.jclinical.records.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

public interface ClinicLookupPort {
    Optional<String> findClinicName(UUID clinicId);
}
