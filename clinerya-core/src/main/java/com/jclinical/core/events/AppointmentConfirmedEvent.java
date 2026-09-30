package com.jclinical.core.events;

import java.time.LocalDateTime;
import java.util.UUID;

/** Una cita quedo confirmada (por el personal o por el paciente desde el recordatorio de WhatsApp). */
public record AppointmentConfirmedEvent(
        UUID eventId,
        UUID clinicId,
        UUID appointmentId,
        LocalDateTime confirmedAt,
        UUID patientId,
        UUID doctorStaffId,
        LocalDateTime scheduledStart
) {}
