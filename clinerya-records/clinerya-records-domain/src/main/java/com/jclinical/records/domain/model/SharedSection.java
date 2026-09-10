package com.jclinical.records.domain.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Secciones del expediente que un enlace temporal puede exponer. El emisor elige
 * el subconjunto al generar el enlace; se persiste como CSV de estos nombres.
 */
public enum SharedSection {
    CLINICAL_NOTES,
    MEDICAL_HISTORY,
    STUDIES,
    VITAL_SIGNS,
    PRESCRIPTIONS;

    /** Todas las secciones: valor por defecto cuando no se especifica ninguna. */
    public static Set<SharedSection> all() {
        return EnumSet.allOf(SharedSection.class);
    }

    /**
     * Convierte un CSV a conjunto, ignorando nombres desconocidos o en blanco.
     * Un CSV nulo o sin ninguna clave válida se interpreta como "todas".
     */
    public static Set<SharedSection> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return all();
        }
        Set<SharedSection> parsed = Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(SharedSection::fromNameOrNull)
                .filter(section -> section != null)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SharedSection.class)));
        return parsed.isEmpty() ? all() : parsed;
    }

    /** Serializa un conjunto a CSV en orden de declaración del enum. */
    public static String toCsv(Set<SharedSection> sections) {
        Set<SharedSection> effective = (sections == null || sections.isEmpty()) ? all() : sections;
        return EnumSet.copyOf(effective).stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    private static SharedSection fromNameOrNull(String name) {
        try {
            return SharedSection.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
