package com.jclinical.automation.domain.ports.in;

import java.time.LocalDateTime;
import java.util.UUID;

/** Recordatorios de cita: se programan con los eventos de la agenda y los envia un barrido periodico. */
public interface AppointmentRemindersUseCase {

    void appointmentScheduled(UUID clinicId, UUID appointmentId, UUID patientId, UUID doctorStaffId,
                              LocalDateTime startsAt, String serviceName);

    void appointmentRescheduled(UUID clinicId, UUID appointmentId, UUID patientId, UUID doctorStaffId,
                                LocalDateTime newStart);

    void appointmentCancelled(UUID appointmentId);

    /** @return cuantos recordatorios salieron en este barrido */
    int sendDue();
}
