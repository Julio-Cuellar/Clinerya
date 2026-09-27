package com.jclinical.automation.domain.agent;

import com.jclinical.automation.domain.model.ConversationOption;

import java.util.List;
import java.util.Map;

/**
 * Resultado de una herramienta: lo que ve el modelo ({@code content}), las opciones reales que el
 * paciente podra tocar (lista o botones), los datos que la respuesta puede citar ({@code facts}) y,
 * si aplica, un texto que debe llegar tal cual ({@code verbatim}, por ejemplo la autorizacion de
 * contacto): lo escribe el codigo y sale como su propio mensaje, nunca parafraseado por el modelo.
 */
public record ToolOutcome(Map<String, Object> content, List<ConversationOption> options, List<String> facts, String verbatim) {

    public ToolOutcome {
        content = content == null ? Map.of() : Map.copyOf(content);
        options = options == null ? List.of() : List.copyOf(options);
        facts = facts == null ? List.of() : List.copyOf(facts);
    }

    public ToolOutcome(Map<String, Object> content, List<ConversationOption> options, List<String> facts) {
        this(content, options, facts, null);
    }

    public static ToolOutcome of(Map<String, Object> content) {
        return new ToolOutcome(content, List.of(), List.of(), null);
    }

    public ToolOutcome withOptions(List<ConversationOption> offered) {
        return new ToolOutcome(content, offered, facts, verbatim);
    }

    public ToolOutcome withFacts(List<String> citable) {
        return new ToolOutcome(content, options, citable, verbatim);
    }

    public ToolOutcome withVerbatim(String text) {
        return new ToolOutcome(content, options, facts, text);
    }
}
