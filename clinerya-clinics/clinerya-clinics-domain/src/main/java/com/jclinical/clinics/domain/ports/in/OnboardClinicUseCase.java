package com.jclinical.clinics.domain.ports.in;

import java.util.UUID;

public interface OnboardClinicUseCase {
    void createPendingClinic(UUID ownerUserId, String email, String fullName, String clinicName);
    void activateClinic(UUID ownerUserId);
}
