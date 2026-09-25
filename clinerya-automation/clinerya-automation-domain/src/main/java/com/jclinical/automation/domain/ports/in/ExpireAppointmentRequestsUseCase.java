package com.jclinical.automation.domain.ports.in;

/** Vence las solicitudes sin respuesta del medico (o sin eleccion del paciente) en su plazo. */
public interface ExpireAppointmentRequestsUseCase {

    /** @return cuantas solicitudes vencieron. */
    int expireOverdue();
}
