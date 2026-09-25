package com.jclinical.automation.domain.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Como se nombra un horario en los mensajes: "Mar 29/09 10:00". */
final class SlotLabel {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("EEE dd/MM HH:mm", Locale.forLanguageTag("es-MX"));

    private SlotLabel() {
    }

    static String of(LocalDateTime start) {
        String label = FORMAT.format(start).replace(".", "");
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }
}
