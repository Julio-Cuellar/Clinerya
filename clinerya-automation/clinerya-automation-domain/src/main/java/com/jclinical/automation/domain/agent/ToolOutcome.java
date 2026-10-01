package com.jclinical.automation.domain.agent;

import com.jclinical.automation.domain.model.ConversationOption;

import java.util.List;
import java.util.Map;

/**
 * Resultado de una herramienta: lo que ve el modelo ({@code content}), las opciones reales que el
 * paciente podra tocar (lista o botones), los datos que la respuesta puede citar ({@code facts}) y,
 * si aplica, un texto que debe llegar tal cual ({@code verbatim}, por ejemplo la autorizacion de
 * contacto): lo escribe el codigo y sale como su propio mensaje, nunca parafraseado por el modelo.
 * {@code fallback}: lo que se le dice al paciente si el modelo deja de responder despues de que la
 * herramienta ya hizo algo real (registrarlo, dejar lista su cita); sale con las opciones de la herramienta.
 * {@code closing}: la accion ya termino (cita agendada, solicitud enviada, cita cancelada) y este texto, escrito
 * por el codigo, es toda la respuesta: reemplaza lo que diga el modelo, para que nunca contradiga lo que paso.
 */
public record ToolOutcome(Map<String, Object> content, List<ConversationOption> options, List<String> facts, String verbatim,
                          String fallback, String closing) {

    public ToolOutcome {
        content = content == null ? Map.of() : Map.copyOf(content);
        options = options == null ? List.of() : List.copyOf(options);
        facts = facts == null ? List.of() : List.copyOf(facts);
    }

    public ToolOutcome(Map<String, Object> content, List<ConversationOption> options, List<String> facts, String verbatim,
                       String fallback) {
        this(content, options, facts, verbatim, fallback, null);
    }

    public ToolOutcome(Map<String, Object> content, List<ConversationOption> options, List<String> facts, String verbatim) {
        this(content, options, facts, verbatim, null, null);
    }

    public ToolOutcome(Map<String, Object> content, List<ConversationOption> options, List<String> facts) {
        this(content, options, facts, null);
    }

    public static ToolOutcome of(Map<String, Object> content) {
        return new ToolOutcome(content, List.of(), List.of(), null, null, null);
    }

    public ToolOutcome withOptions(List<ConversationOption> offered) {
        return new ToolOutcome(content, offered, facts, verbatim, fallback, closing);
    }

    public ToolOutcome withFacts(List<String> citable) {
        return new ToolOutcome(content, options, citable, verbatim, fallback, closing);
    }

    public ToolOutcome withVerbatim(String text) {
        return new ToolOutcome(content, options, facts, text, fallback, closing);
    }

    public ToolOutcome withFallback(String text) {
        return new ToolOutcome(content, options, facts, verbatim, text, closing);
    }

    public ToolOutcome withClosing(String text) {
        return new ToolOutcome(content, options, facts, verbatim, fallback, text);
    }
}
