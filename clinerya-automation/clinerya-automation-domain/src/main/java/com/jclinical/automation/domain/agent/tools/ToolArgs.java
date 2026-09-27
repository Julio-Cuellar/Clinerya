package com.jclinical.automation.domain.agent.tools;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

/**
 * Lectura tolerante de los argumentos que manda el modelo (un numero puede llegar como texto o como
 * decimal) y utilidades de texto comunes a las herramientas.
 */
final class ToolArgs {

    private ToolArgs() {
    }

    static String text(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        return value == null ? "" : String.valueOf(value).trim();
    }

    static int integer(Map<String, Object> arguments, String name, int fallback) {
        Object value = arguments.get(name);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(text(arguments, name));
        } catch (NumberFormatException notANumber) {
            return fallback;
        }
    }

    /** AAAA-MM-DD, o null si no es una fecha. */
    static LocalDate date(Map<String, Object> arguments, String name) {
        try {
            return LocalDate.parse(text(arguments, name));
        } catch (DateTimeParseException notADate) {
            return null;
        }
    }

    /** Minusculas y sin acentos, para comparar lo que escribe el paciente con el catalogo. */
    static String normalize(String text) {
        return text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
    }

    /** "$650" o "$1,200.50". */
    static String price(BigDecimal price) {
        String pattern = price.stripTrailingZeros().scale() <= 0 ? "#,##0" : "#,##0.00";
        return "$" + new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US)).format(price);
    }
}
