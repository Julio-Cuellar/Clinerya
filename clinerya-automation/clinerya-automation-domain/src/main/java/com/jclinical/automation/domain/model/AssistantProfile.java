package com.jclinical.automation.domain.model;

/**
 * Como se presenta el asistente de una clinica y que puede compartir: nombre (vacio: habla a nombre
 * de la clinica), preguntas frecuentes escritas por la clinica y si se muestran precios de lista.
 */
public record AssistantProfile(String assistantName, String faq, boolean showPrices) {

    public static final AssistantProfile EMPTY = new AssistantProfile(null, null, false);
}
