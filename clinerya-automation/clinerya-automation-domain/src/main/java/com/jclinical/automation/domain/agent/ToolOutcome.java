package com.jclinical.automation.domain.agent;

import com.jclinical.automation.domain.model.ConversationOption;

import java.util.List;
import java.util.Map;

/**
 * Resultado de una herramienta: lo que ve el modelo ({@code content}), las opciones reales que el
 * paciente podra tocar (lista o botones) y los datos que la respuesta puede citar ({@code facts}).
 */
public record ToolOutcome(Map<String, Object> content, List<ConversationOption> options, List<String> facts) {

    public ToolOutcome {
        content = content == null ? Map.of() : Map.copyOf(content);
        options = options == null ? List.of() : List.copyOf(options);
        facts = facts == null ? List.of() : List.copyOf(facts);
    }

    public static ToolOutcome of(Map<String, Object> content) {
        return new ToolOutcome(content, List.of(), List.of());
    }

    public ToolOutcome withOptions(List<ConversationOption> offered) {
        return new ToolOutcome(content, offered, facts);
    }

    public ToolOutcome withFacts(List<String> citable) {
        return new ToolOutcome(content, options, citable);
    }
}
