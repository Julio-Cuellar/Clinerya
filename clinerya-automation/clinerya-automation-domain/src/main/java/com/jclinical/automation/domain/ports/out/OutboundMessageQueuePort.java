package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.PatientNotification;

/**
 * Mensajes al paciente que salen por iniciativa de la automatizacion (no como respuesta inmediata a
 * un mensaje suyo). Quedan en cola hasta que el canal (WhatsApp, entrega 5) los envie.
 */
public interface OutboundMessageQueuePort {

    void enqueue(PatientNotification notification);
}
