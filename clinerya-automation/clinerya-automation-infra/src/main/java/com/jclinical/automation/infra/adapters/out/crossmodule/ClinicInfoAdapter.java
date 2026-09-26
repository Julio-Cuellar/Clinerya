package com.jclinical.automation.infra.adapters.out.crossmodule;

import com.jclinical.agenda.domain.ports.in.ManageClinicScheduleUseCase;
import com.jclinical.automation.domain.model.ClinicInfo;
import com.jclinical.automation.domain.ports.out.ClinicInfoPort;
import com.jclinical.clinics.domain.ports.in.GetClinicPublicProfileUseCase;

import java.util.Optional;
import java.util.UUID;

public class ClinicInfoAdapter implements ClinicInfoPort {

    public ClinicInfoAdapter(GetClinicPublicProfileUseCase profiles, ManageClinicScheduleUseCase schedules) {
    }

    @Override
    public Optional<ClinicInfo> find(UUID clinicId) {
        throw new UnsupportedOperationException("pendiente");
    }
}
