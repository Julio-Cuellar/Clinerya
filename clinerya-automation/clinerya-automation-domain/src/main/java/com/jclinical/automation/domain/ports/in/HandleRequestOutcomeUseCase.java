package com.jclinical.automation.domain.ports.in;

import com.jclinical.automation.domain.model.AppointmentRequestResolvedEvent;
import com.jclinical.automation.domain.model.PatientNotification;

import java.util.Optional;

/** La conversacion del paciente reacciona a la respuesta del medico (o al vencimiento). */
public interface HandleRequestOutcomeUseCase {

    /**
     * @return el mensaje para el paciente, o vacio si el evento no corresponde al paso actual de la
     * conversacion (repetido, de una solicitud anterior o de una conversacion que ya no existe).
     */
    Optional<PatientNotification> onRequestResolved(AppointmentRequestResolvedEvent event);
}
