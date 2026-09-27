package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.ClinicInfo;
import com.jclinical.automation.domain.model.ClinicInfo.OpeningHours;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Arma el mensaje de "Informacion de la clinica" con los datos tal como estan en Clinerya. Lo que
 * falta no se menciona; el horario agrupa dias seguidos iguales ("Lunes a viernes: 09:00 a 18:00").
 */
public final class ClinicInfoMessage {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> DAY_NAMES =
            List.of("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo");

    private ClinicInfoMessage() {
    }

    static String format(ClinicInfo info) {
        List<String> lines = new ArrayList<>();
        lines.add(info.name());
        addIfPresent(lines, "Dirección: ", info.address());
        addIfPresent(lines, "Teléfono: ", info.phone());
        addIfPresent(lines, "Correo: ", info.email());
        List<String> schedule = scheduleLines(info.hours());
        if (!schedule.isEmpty()) {
            lines.add("Horario:");
            lines.addAll(schedule);
        }
        return String.join("\n", lines);
    }

    private static void addIfPresent(List<String> lines, String label, String value) {
        if (value != null && !value.isBlank()) {
            lines.add(label + value.trim());
        }
    }

    public static List<String> scheduleLines(List<OpeningHours> hours) {
        List<OpeningHours> week = hours.stream().sorted(Comparator.comparing(OpeningHours::day)).toList();
        List<String> lines = new ArrayList<>();
        int from = 0;
        while (from < week.size()) {
            int to = from;
            while (to + 1 < week.size() && isNextDay(week.get(to), week.get(to + 1)) && sameHours(week.get(from), week.get(to + 1))) {
                to++;
            }
            lines.add(capitalize(daysLabel(week.get(from).day(), week.get(to).day(), to - from + 1)) + ": "
                    + hoursLabel(week.get(from)));
            from = to + 1;
        }
        return lines;
    }

    private static boolean isNextDay(OpeningHours day, OpeningHours next) {
        return next.day().getValue() == day.day().getValue() + 1;
    }

    private static boolean sameHours(OpeningHours a, OpeningHours b) {
        return a.open() == b.open() && Objects.equals(a.start(), b.start()) && Objects.equals(a.end(), b.end());
    }

    private static String daysLabel(DayOfWeek first, DayOfWeek last, int count) {
        String firstName = DAY_NAMES.get(first.getValue() - 1);
        if (count == 1) {
            return firstName;
        }
        String lastName = DAY_NAMES.get(last.getValue() - 1);
        return firstName + (count == 2 ? " y " : " a ") + lastName;
    }

    private static String hoursLabel(OpeningHours day) {
        if (!day.open() || day.start() == null || day.end() == null) {
            return "cerrado";
        }
        return TIME.format(day.start()) + " a " + TIME.format(day.end());
    }

    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
