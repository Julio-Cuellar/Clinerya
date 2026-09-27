package com.jclinical.automation.domain.ports.out;

import java.util.UUID;

/** El paciente cancela su propia cita, por la ruta interna de la agenda. */
@FunctionalInterface
public interface AppointmentCancellationPort {

    /** @throws NotCancellableException si la cita no es suya, ya paso o ya no esta vigente. */
    void cancel(UUID clinicId, UUID appointmentId, UUID patientId, String reason);

    class NotCancellableException extends RuntimeException {
        public NotCancellableException(String message) {
            super(message);
        }
    }
}
