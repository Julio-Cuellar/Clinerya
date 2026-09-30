package com.jclinical.automation.domain.ports.out;

import java.util.UUID;

/** El paciente confirma su propia cita (ruta publica de la agenda). */
@FunctionalInterface
public interface AppointmentConfirmationPort {

    /** @return false si la agenda ya no la deja confirmar (paso, se cancelo o no es suya) */
    boolean confirm(UUID clinicId, UUID appointmentId, UUID patientId);
}
