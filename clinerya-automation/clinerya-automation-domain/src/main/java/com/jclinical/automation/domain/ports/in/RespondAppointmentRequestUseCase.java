package com.jclinical.automation.domain.ports.in;

import com.jclinical.automation.domain.model.AppointmentRequest;
import com.jclinical.automation.domain.model.AvailableSlot;

import java.util.List;
import java.util.UUID;

/**
 * Bandeja del medico: solo el medico al que pertenece la solicitud puede verla y responderla
 * (decision: toda aprobacion pasa por el medico de la cita).
 */
public interface RespondAppointmentRequestUseCase {

    List<AppointmentRequest> listPending(UUID actingUserId, UUID clinicId);

    /** Cupos libres del medico que puede proponer en lugar del solicitado. */
    List<AvailableSlot> proposableSlots(UUID actingUserId, UUID clinicId, UUID requestId);

    AppointmentRequest accept(UUID actingUserId, UUID clinicId, UUID requestId);

    AppointmentRequest reject(UUID actingUserId, UUID clinicId, UUID requestId, String reason);

    AppointmentRequest proposeOptions(UUID actingUserId, UUID clinicId, UUID requestId, List<AvailableSlot> options);
}
