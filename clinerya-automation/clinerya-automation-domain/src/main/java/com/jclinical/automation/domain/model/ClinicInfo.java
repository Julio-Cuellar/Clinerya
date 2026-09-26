package com.jclinical.automation.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Lo que el asistente puede decir de la clinica: sus datos publicos y su horario, tal como estan en
 * Clinerya. Sin IA: si un dato falta, no se menciona.
 */
public record ClinicInfo(String name, String address, String phone, String email, String privacyNoticeUrl,
                         List<OpeningHours> hours) {

    public ClinicInfo {
        hours = hours == null ? List.of() : List.copyOf(hours);
    }

    /** Un dia de la semana: abierto con su horario, o cerrado. */
    public record OpeningHours(DayOfWeek day, boolean open, LocalTime start, LocalTime end) {}
}
