package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Accion con efecto real que el agente propuso y espera la confirmacion del paciente en un mensaje
 * posterior. {@code appointmentId} aplica a cancelar o reprogramar; {@code note} es el motivo que dio
 * el paciente, si dio uno.
 */
public record PendingAction(UUID conversationId, Kind kind, UUID patientId, String patientName, UUID doctorStaffId,
                            String doctorName, LocalDateTime start, LocalDateTime end, UUID appointmentId,
                            LocalDateTime proposedAt, String note) {

    public PendingAction(UUID conversationId, Kind kind, UUID patientId, String patientName, UUID doctorStaffId,
                         String doctorName, LocalDateTime start, LocalDateTime end, UUID appointmentId,
                         LocalDateTime proposedAt) {
        this(conversationId, kind, patientId, patientName, doctorStaffId, doctorName, start, end, appointmentId, proposedAt, null);
    }

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
