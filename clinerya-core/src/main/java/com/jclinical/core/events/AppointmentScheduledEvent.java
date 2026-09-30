package com.jclinical.core.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentScheduledEvent(
        UUID eventId,
        UUID clinicId,
        UUID appointmentId,
        UUID doctorStaffId,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        LocalDateTime scheduledAt,
        UUID patientId,
        /** Servicio del catalogo (puede faltar); precio solo si es fijo; tipo FIXED o VARIES_BY_PATIENT. */
        UUID serviceId,
        String serviceName,
        BigDecimal servicePrice,
        String servicePricing
) {
    public AppointmentScheduledEvent(UUID eventId, UUID clinicId, UUID appointmentId, UUID doctorStaffId,
                                     LocalDateTime scheduledStart, LocalDateTime scheduledEnd, LocalDateTime scheduledAt,
                                     UUID patientId) {
        this(eventId, clinicId, appointmentId, doctorStaffId, scheduledStart, scheduledEnd, scheduledAt, patientId,
                null, null, null, null);
    }
}
