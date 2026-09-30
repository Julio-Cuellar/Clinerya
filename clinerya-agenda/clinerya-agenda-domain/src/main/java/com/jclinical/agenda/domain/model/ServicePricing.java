package com.jclinical.agenda.domain.model;

/** Como se cobra el servicio de una cita (copia del tipo de precio del catalogo al agendar). */
public enum ServicePricing {
    /** Precio fijo: la cita guarda el precio y cuenta en el ingreso estimado. */
    FIXED,
    /** Varia por paciente: la cita queda con precio por definir. */
    VARIES_BY_PATIENT
}
