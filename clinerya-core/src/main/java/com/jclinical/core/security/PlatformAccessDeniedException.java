package com.jclinical.core.security;

/**
 * El llamante no tiene permiso de plataforma (superadmin).
 *
 * <p>Deliberadamente separada de {@link ClinicAccessDeniedException}: esa cubre el acceso
 * a una clínica concreta, mientras que esta cubre operaciones globales al tenant
 * —configuración del sistema, respaldos— donde el ámbito de clínica no aplica.
 * Mantenerlas distintas evita que un chequeo de plataforma se "arregle" un día
 * concediendo membresía de clínica.
 */
public class PlatformAccessDeniedException extends RuntimeException {

    public PlatformAccessDeniedException(String message) {
        super(message);
    }
}
