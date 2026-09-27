package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Accion con efecto real que el agente propuso y espera la confirmacion del paciente en un mensaje
 * posterior. {@code appointmentId} aplica a cancelar o reprogramar.
 */
public record PendingAction(UUID conversationId, Kind kind, UUID patientId, String patientName, UUID doctorStaffId,
                            String doctorName, LocalDateTime start, LocalDateTime end, UUID appointmentId,
                            LocalDateTime proposedAt) {

    public enum Kind {
        BOOK,
        CANCEL,
        RESCHEDULE,
        /** Se le mostro la autorizacion de contacto; falta su respuesta. */
        CONSENT,
        /** El paciente acepto la autorizacion; se puede registrar. */
        CONSENT_ACCEPTED
    }
}
