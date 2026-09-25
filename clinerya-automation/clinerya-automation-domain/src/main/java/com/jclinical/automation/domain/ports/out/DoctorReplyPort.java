package com.jclinical.automation.domain.ports.out;

import java.util.UUID;

/**
 * Si quien escribe es un medico de la clinica, se le recuerda que responde en Clinerya y su mensaje
 * no entra a la conversacion de pacientes.
 */
public interface DoctorReplyPort {

    /** @return true si el remitente es un medico y ya se le contesto */
    boolean replyIfDoctor(UUID clinicId, String fromPhone);
}
