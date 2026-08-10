package com.jclinical.agenda.domain.ports.out;

import java.util.UUID;

public interface ClinicSettingsPort {

    int getMaterialReservationLeadDays(UUID clinicId);
}
