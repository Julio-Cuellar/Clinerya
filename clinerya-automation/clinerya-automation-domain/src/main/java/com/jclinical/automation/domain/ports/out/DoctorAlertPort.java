package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.AppointmentRequest;

import java.time.LocalDateTime;
import java.util.UUID;

/** Avisa al medico de lo que pasa con sus citas por el chat (solicitudes nuevas, cancelaciones del paciente). */
public interface DoctorAlertPort {

    void newRequest(AppointmentRequest request);

    /** El paciente cancelo por WhatsApp una cita del medico. Sin datos del paciente. */
    default void appointmentCancelled(UUID clinicId, UUID doctorStaffId, LocalDateTime start) {
    }
}
