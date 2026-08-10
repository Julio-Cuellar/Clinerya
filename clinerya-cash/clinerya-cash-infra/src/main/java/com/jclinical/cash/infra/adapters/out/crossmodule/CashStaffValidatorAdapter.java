package com.jclinical.cash.infra.adapters.out.crossmodule;

import com.jclinical.cash.domain.ports.out.CashStaffValidatorPort;
import com.jclinical.staff.domain.ports.in.ManageClinicStaffUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CashStaffValidatorAdapter implements CashStaffValidatorPort {

    private final ManageClinicStaffUseCase clinicStaffUseCase;

    @Override
    public Optional<StaffSnapshot> findActiveStaff(UUID staffId, UUID clinicId) {
        return clinicStaffUseCase.getActiveStaffById(staffId, clinicId)
                .map(staff -> new StaffSnapshot(staff.staffId(), staff.fullName()));
    }
}
