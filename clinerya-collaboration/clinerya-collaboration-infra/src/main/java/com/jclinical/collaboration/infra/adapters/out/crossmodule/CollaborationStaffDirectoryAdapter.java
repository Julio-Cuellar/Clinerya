package com.jclinical.collaboration.infra.adapters.out.crossmodule;

import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import com.jclinical.collaboration.domain.ports.out.ClinicStaffDirectoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CollaborationStaffDirectoryAdapter implements ClinicStaffDirectoryPort {

    private final ManageClinicStaffUseCase clinicStaffUseCase;

    @Override
    public Optional<UUID> findActiveStaffId(UUID userId, UUID clinicId) {
        return clinicStaffUseCase.getActiveStaffByUserAndClinic(userId, clinicId)
                .map(ManageClinicStaffUseCase.StaffSummary::staffId);
    }
}
