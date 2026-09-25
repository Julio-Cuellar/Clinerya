package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.ports.out.DoctorIdentityPort;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.util.UUID;

public class RealtimeAccessPolicy {

    public RealtimeAccessPolicy(StaffPermissionCheckerPort permissions, DoctorIdentityPort identity) {
    }

    public boolean canSubscribe(UUID userId, String destination) {
        throw new UnsupportedOperationException("pendiente");
    }
}
