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
 */
public record ToolOutcome(Map<String, Object> content, List<ConversationOption> options, List<String> facts, String verbatim,
                          String fallback) {

    public ToolOutcome {
        content = content == null ? Map.of() : Map.copyOf(content);
        options = options == null ? List.of() : List.copyOf(options);
        facts = facts == null ? List.of() : List.copyOf(facts);
    }

    public ToolOutcome(Map<String, Object> content, List<ConversationOption> options, List<String> facts, String verbatim) {
        this(content, options, facts, verbatim, null);
    }

    public ToolOutcome(Map<String, Object> content, List<ConversationOption> options, List<String> facts) {
        this(content, options, facts, null);
    }

    public static ToolOutcome of(Map<String, Object> content) {
        return new ToolOutcome(content, List.of(), List.of(), null, null);
    }

    public ToolOutcome withOptions(List<ConversationOption> offered) {
        return new ToolOutcome(content, offered, facts, verbatim, fallback);
    }

    public ToolOutcome withFacts(List<String> citable) {
        return new ToolOutcome(content, options, citable, verbatim, fallback);
    }

    public ToolOutcome withVerbatim(String text) {
        return new ToolOutcome(content, options, facts, text, fallback);
    }

    public ToolOutcome withFallback(String text) {
        return new ToolOutcome(content, options, facts, verbatim, text);
    }
}
