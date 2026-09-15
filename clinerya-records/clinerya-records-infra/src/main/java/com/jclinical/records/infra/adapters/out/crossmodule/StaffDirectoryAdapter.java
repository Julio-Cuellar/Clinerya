package com.jclinical.records.infra.adapters.out.crossmodule;

import com.jclinical.records.domain.ports.out.StaffDirectoryPort;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase.StaffSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StaffDirectoryAdapter implements StaffDirectoryPort {

    private final ManageClinicStaffUseCase clinicStaffUseCase;

    @Override
    public Optional<String> staffName(UUID staffId, UUID clinicId) {
        if (staffId == null || clinicId == null) {
            return Optional.empty();
        }
        return clinicStaffUseCase.getActiveStaffById(staffId, clinicId)
                .map(StaffSummary::fullName)
                .filter(name -> name != null && !name.isBlank());
    }

    @Override
    public Optional<String> userName(UUID userId, UUID clinicId) {
        if (userId == null || clinicId == null) {
            return Optional.empty();
        }
        return clinicStaffUseCase.getActiveStaffByUserAndClinic(userId, clinicId)
                .map(StaffSummary::fullName)
                .filter(name -> name != null && !name.isBlank());
    }
}
