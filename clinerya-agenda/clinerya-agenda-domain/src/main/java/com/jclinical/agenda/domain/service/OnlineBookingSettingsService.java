package com.jclinical.agenda.domain.service;

import com.jclinical.agenda.domain.ports.in.ManageOnlineBookingSettingsUseCase;
import com.jclinical.agenda.domain.ports.out.OnlineBookingSettingsPort;
import com.jclinical.agenda.domain.ports.out.OnlineBookingSettingsRepositoryPort;
import com.jclinical.agenda.domain.ports.out.StaffValidatorPort;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.util.UUID;

public class OnlineBookingSettingsService implements ManageOnlineBookingSettingsUseCase, OnlineBookingSettingsPort {

    public OnlineBookingSettingsService(OnlineBookingSettingsRepositoryPort repository, StaffValidatorPort staffValidator,
                                        StaffPermissionCheckerPort permissionChecker) {
    }

    @Override
    public OnlineBookingSettings getSettings(UUID actingUserId, UUID clinicId) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public void updateSlotMinutes(UUID actingUserId, UUID clinicId, int minutes) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public void updateDoctorLeadMinutes(UUID actingUserId, UUID clinicId, UUID doctorStaffId, int minutes) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public int slotMinutes(UUID clinicId) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public int minLeadMinutes(UUID clinicId, UUID doctorStaffId) {
        throw new UnsupportedOperationException("pendiente");
    }
}
