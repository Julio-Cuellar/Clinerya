package com.jclinical.core.domain;

/**
 * Perfil de especialidad de una clínica.
 *
 * <p>Determina presentación, no modelo: vocabulario del módulo de tratamientos, visibilidad del
 * localizador clínico (número de diente), catálogo de servicios sugerido y qué plantilla de
 * historia clínica se recomienda. Ninguna entidad de negocio cambia de forma según este valor.
 *
 * <p>Vive en {@code clinerya-core} porque lo leen varios módulos (clínicas, tratamientos y,
 * más adelante, expedientes y agenda).
 */
public enum ClinicSpecialty {

    /**
     * Estado inicial de una clínica recién dada de alta que todavía no pasó por el onboarding.
     * No es un hueco: es lo que permite saber qué clínicas nunca terminaron de configurarse.
     */
    SIN_CONFIGURAR,

    ODONTOLOGIA,
    MEDICINA_GENERAL,
    NUTRICION,
    FISIOTERAPIA,
    OFTALMOLOGIA,
    OTRA;

    /**
     * Único perfil que hoy expone el localizador clínico (número de diente en notación FDI).
     */
    public boolean usaLocalizadorDental() {
        return this == ODONTOLOGIA;
    }
}
