package com.jclinical.automation.domain.agent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Revisa que ninguna cifra (precio, hora, fecha, telefono), enlace o correo de una respuesta salga
 * de la nada: debe estar en las fuentes de confianza (lo consultado, lo que ya dijo la clinica, la
 * fecha que da el codigo) o, salvo un precio, en lo que dijo el paciente. Los digitos sueltos son
 * conversacion ("te muestro 3 opciones"), no dato.
 */
public final class GroundingGuard {

    private static final Pattern FIGURE = Pattern.compile(
            "https?://[^\\s<>]+|www\\.[^\\s<>]+|[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+|\\$\\s?\\d[\\d.,:/]*|\\d[\\d.,:/]*");
    private static final Pattern DIGIT_GROUP_SEPARATOR = Pattern.compile("(?<=\\d)[\\s-](?=\\d)");
    private static final Pattern DECIMAL = Pattern.compile("\\d+\\.\\d+");

    private GroundingGuard() {
    }

    public static List<String> inventedFigures(String reply, List<String> trusted) {
        return inventedFigures(reply, trusted, List.of());
    }

    /** Cifras, enlaces o correos de {@code reply} que no respaldan las fuentes, en el orden en que aparecen. */
    public static List<String> inventedFigures(String reply, List<String> trusted, List<String> patientSaid) {
        Set<String> fromTrusted = values(trusted);
        Set<String> fromPatient = values(patientSaid);
        Set<String> invented = new LinkedHashSet<>();
        for (Figure figure : figures(reply)) {
            boolean backed = fromTrusted.contains(figure.value())
                    || (!figure.price() && fromPatient.contains(figure.value()));
            if (!backed) {
                invented.add(figure.value());
            }
        }
        return List.copyOf(invented);
    }

    private static Set<String> values(List<String> sources) {
        Set<String> values = new HashSet<>();
        for (String source : sources) {
            if (source == null) {
                continue;
            }
            figures(source).forEach(figure -> values.add(figure.value()));
            // "222 555 0101" tambien respalda "2225550101".
            figures(DIGIT_GROUP_SEPARATOR.matcher(source).replaceAll("")).forEach(figure -> values.add(figure.value()));
        }
        return values;
    }

    private static List<Figure> figures(String text) {
        List<Figure> figures = new ArrayList<>();
        if (text == null) {
            return figures;
        }
        Matcher matcher = FIGURE.matcher(text);
        while (matcher.find()) {
            String raw = matcher.group();
            if (raw.contains("@") || raw.startsWith("http") || raw.startsWith("www.")) {
                figures.add(new Figure(stripTrailing(raw).toLowerCase(Locale.ROOT), false));
                continue;
            }
            boolean price = raw.startsWith("$");
            String value = stripTrailing(raw.replace("$", "").replace(",", "").replace(" ", ""));
            if (DECIMAL.matcher(value).matches()) {
                value = value.replaceAll("0+$", "").replaceAll("\\.$", "");
            }
            if (value.length() > 1) {
                figures.add(new Figure(value, price));
            }
        }
        return figures;
    }

    private static String stripTrailing(String value) {
        return value.replaceAll("[.,:;/)]+$", "");
    }

    private record Figure(String value, boolean price) {}
}
