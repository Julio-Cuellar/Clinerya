package com.jclinical.automation.domain.ports.in;

/** Barrido que le recuerda al medico las solicitudes que llevan horas sin respuesta. */
public interface RemindPendingRequestsUseCase {

    /** @return cuantos recordatorios se encolaron */
    int remindPending();
}
