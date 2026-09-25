package com.jclinical.automation.domain.ports.in;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Celular del medico para avisos. Lo configura el propio medico ("mine") o quien tiene
 * MANAGE_INTEGRATIONS para cualquier medico de la clinica.
 */
public interface ManageDoctorChannelUseCase {

    DoctorChannelView getMine(UUID actingUserId, UUID clinicId);

    DoctorChannelView updateMine(UUID actingUserId, UUID clinicId, DoctorChannelUpdate update);

    DoctorChannelView get(UUID actingUserId, UUID clinicId, UUID staffId);

    DoctorChannelView update(UUID actingUserId, UUID clinicId, UUID staffId, DoctorChannelUpdate update);

    record DoctorChannelUpdate(String phone, boolean consent, boolean active) {}

    record DoctorChannelView(UUID staffId, String phone, boolean active, LocalDateTime consentAt, LocalDateTime updatedAt) {}
}
