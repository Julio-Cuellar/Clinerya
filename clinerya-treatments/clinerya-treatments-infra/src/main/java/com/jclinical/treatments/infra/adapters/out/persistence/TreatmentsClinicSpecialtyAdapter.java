package com.jclinical.treatments.infra.adapters.out.persistence;

import com.jclinical.clinics.domain.ports.in.GetClinicSettingsUseCase;
import com.jclinical.core.domain.ClinicSpecialty;
import com.jclinical.treatments.domain.ports.out.ClinicSpecialtyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TreatmentsClinicSpecialtyAdapter implements ClinicSpecialtyPort {

    private final GetClinicSettingsUseCase getClinicSettingsUseCase;

    @Override
    public Optional<ClinicSpecialty> findByClinicId(UUID clinicId) {
        return getClinicSettingsUseCase.getSettings(clinicId)
                .map(GetClinicSettingsUseCase.ClinicSettings::specialty);
    }
}
