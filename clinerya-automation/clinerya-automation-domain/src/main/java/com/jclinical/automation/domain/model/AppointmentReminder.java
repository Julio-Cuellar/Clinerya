package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Recordatorio pendiente de una cita: cuando sale y lo necesario para escribirlo sin volver a
 * preguntarle a la agenda. {@code sentAt} null: aun no sale.
 */
public record AppointmentReminder(
        UUID appointmentId,
        UUID clinicId,
        UUID patientId,
        UUID doctorStaffId,
        LocalDateTime startsAt,
        String serviceName,
        LocalDateTime sendAt,
        LocalDateTime sentAt
) {

    public AppointmentReminder sent(LocalDateTime at) {
        return new AppointmentReminder(appointmentId, clinicId, patientId, doctorStaffId, startsAt, serviceName, sendAt, at);
    }
}
