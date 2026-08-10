package com.jclinical.clinics.domain.ports.in;

import java.util.Optional;
import java.util.UUID;

public interface GetClinicSettingsUseCase {

    Optional<ClinicSettings> getSettings(UUID clinicId);

    record ClinicSettings(UUID clinicId, int materialReservationLeadDays) {}
}
