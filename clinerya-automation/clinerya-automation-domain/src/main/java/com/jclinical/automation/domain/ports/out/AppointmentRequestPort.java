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

    /**
     * El paciente eligio una de las opciones que propuso el medico: se agenda directo (el medico ya
     * la aprobo al proponerla) y se liberan las demas.
     *
     * @return id de la cita.
     * @throws SlotNoLongerAvailableException si la agenda ya no la acepta.
     */
    UUID chooseOption(UUID clinicId, UUID requestId, LocalDateTime start, LocalDateTime end);

    /** Ninguna opcion le sirve al paciente: se liberan todas. */
    void declineOptions(UUID clinicId, UUID requestId);

    record NewAppointmentRequest(
            UUID clinicId,
            UUID conversationId,
            UUID patientId,
            UUID doctorStaffId,
            LocalDateTime start,
            LocalDateTime end,
            String patientPhone,
            String patientName,
            String doctorName
    ) {}

    class SlotNoLongerAvailableException extends RuntimeException {
        public SlotNoLongerAvailableException() {
            super("El horario elegido ya no esta disponible.");
        }

        public SlotNoLongerAvailableException(String message) {
            super(message);
        }
    }
}
