package com.jclinical.clinics.infra.adapters.out.security;

import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import com.jclinical.core.security.ClinicMembershipPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClinicMembershipAdapter implements ClinicMembershipPort {

    private final ManageClinicStaffUseCase clinicStaffUseCase;

    @Override
    public boolean isActiveStaffMember(UUID userId, UUID clinicId) {
        return clinicStaffUseCase.getActiveStaffByUserAndClinic(userId, clinicId).isPresent();
    }
}
