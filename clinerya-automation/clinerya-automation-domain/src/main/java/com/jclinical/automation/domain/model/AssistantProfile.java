package com.jclinical.automation.domain.model;

/**
 * Como se presenta el asistente de una clinica y que puede compartir: nombre (vacio: habla a nombre
 * de la clinica), preguntas frecuentes escritas por la clinica y si se muestran precios de lista.
 * Tambien el recordatorio de citas: si sale, cuantas horas antes y con que plantilla aprobada en Meta.
 */
public record AssistantProfile(String assistantName, String faq, boolean showPrices, boolean remindersEnabled,
                               int reminderHoursBefore, String reminderTemplateName) {

    public static final int DEFAULT_REMINDER_HOURS = 24;
    public static final AssistantProfile EMPTY = new AssistantProfile(null, null, true);

    public AssistantProfile {
        if (reminderHoursBefore <= 0) {
            reminderHoursBefore = DEFAULT_REMINDER_HOURS;
        }
    }

    public AssistantProfile(String assistantName, String faq, boolean showPrices) {
        this(assistantName, faq, showPrices, true, DEFAULT_REMINDER_HOURS, null);
    }
}
