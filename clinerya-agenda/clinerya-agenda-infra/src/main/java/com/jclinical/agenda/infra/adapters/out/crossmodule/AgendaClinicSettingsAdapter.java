package com.jclinical.agenda.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.out.ClinicSettingsPort;
import com.jclinical.clinics.domain.ports.in.GetClinicSettingsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AgendaClinicSettingsAdapter implements ClinicSettingsPort {

    private static final int DEFAULT_LEAD_DAYS = 3;

    private final GetClinicSettingsUseCase getClinicSettingsUseCase;

    @Override
    public int getMaterialReservationLeadDays(UUID clinicId) {
        return getClinicSettingsUseCase.getSettings(clinicId)
                .map(GetClinicSettingsUseCase.ClinicSettings::materialReservationLeadDays)
                .orElse(DEFAULT_LEAD_DAYS);
    }
}
