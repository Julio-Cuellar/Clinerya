package com.jclinical.automation.domain.ports.out;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AppointmentRequestPort {

    /**
     * Aparta el cupo y deja la solicitud en manos del medico. Nunca crea la cita: eso solo ocurre
     * cuando el medico la aprueba.
     *
     * @throws SlotNoLongerAvailableException si el cupo se ocupo mientras el paciente elegia.
     */
    UUID submit(NewAppointmentRequest request);

    record NewAppointmentRequest(
            UUID clinicId,
            UUID conversationId,
            UUID patientId,
            UUID doctorStaffId,
            LocalDateTime start,
            LocalDateTime end,
            String patientPhone
    ) {}

    class SlotNoLongerAvailableException extends RuntimeException {
        public SlotNoLongerAvailableException() {
            super("El horario elegido ya no esta disponible.");
        }
    }
}
