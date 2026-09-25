package com.jclinical.core.domain;

import java.util.regex.Pattern;

/**
 * Cedula profesional que un administrador debe registrar para atender pacientes. Solo se revisa
 * el formato (numerica, 5 a 8 digitos, que cubre tambien las cedulas antiguas); no se consulta el
 * registro de la SEP, por eso el perfil medico queda EN_TRAMITE.
 */
public final class CedulaProfesional {

    private static final Pattern FORMAT = Pattern.compile("\\d{5,8}");

    private CedulaProfesional() {
    }

    /** Devuelve la cedula sin espacios o lanza {@link IllegalArgumentException} si no es valida. */
    public static String require(String value) {
        return require(value, "Para atender pacientes captura tu cédula profesional.");
    }

    public static String require(String value, String missingMessage) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", "");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(missingMessage);
        }
        if (!FORMAT.matcher(normalized).matches()) {
            throw new IllegalArgumentException("La cédula profesional debe tener entre 5 y 8 dígitos.");
        }
        return normalized;
    }
}
