package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.ports.in.ManageDoctorChannelUseCase;
import com.jclinical.automation.domain.ports.out.DoctorChannelRepositoryPort;
import com.jclinical.automation.domain.ports.out.DoctorDirectoryPort;
import com.jclinical.automation.domain.ports.out.DoctorIdentityPort;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.time.Clock;
import java.util.UUID;

public class DoctorChannelService implements ManageDoctorChannelUseCase {

    public DoctorChannelService(DoctorChannelRepositoryPort channels, DoctorIdentityPort identity,
                                DoctorDirectoryPort doctors, StaffPermissionCheckerPort permissions, Clock clock) {
    }

    @Override
    public DoctorChannelView getMine(UUID actingUserId, UUID clinicId) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public DoctorChannelView updateMine(UUID actingUserId, UUID clinicId, DoctorChannelUpdate update) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public DoctorChannelView get(UUID actingUserId, UUID clinicId, UUID staffId) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public DoctorChannelView update(UUID actingUserId, UUID clinicId, UUID staffId, DoctorChannelUpdate update) {
        throw new UnsupportedOperationException("pendiente");
    }
}
