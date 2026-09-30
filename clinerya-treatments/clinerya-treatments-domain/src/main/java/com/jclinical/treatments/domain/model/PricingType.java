package com.jclinical.treatments.domain.model;

/** Como se cobra un servicio del catalogo. */
public enum PricingType {
    /** Siempre cuesta lo mismo: se copia a la cita y cuenta en el ingreso estimado. */
    FIXED,
    /** Depende de la valoracion: el precio del catalogo es solo referencia "desde" (opcional). */
    VARIES_BY_PATIENT
}
