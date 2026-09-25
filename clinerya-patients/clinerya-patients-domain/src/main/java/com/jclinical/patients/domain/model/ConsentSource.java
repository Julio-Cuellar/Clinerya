package com.jclinical.patients.domain.model;

/** Via por la que se registro la decision de contacto. */
public enum ConsentSource {
    /** Casilla marcada por el personal al dar de alta al paciente. */
    CLINIC_REGISTRATION,
    /** Cambio posterior desde la ficha del paciente (otorgar o revocar). */
    CLINIC_UPDATE,
    /** Aceptado por el propio paciente en el chat de WhatsApp (pre-registro). */
    WHATSAPP_CHAT
}
