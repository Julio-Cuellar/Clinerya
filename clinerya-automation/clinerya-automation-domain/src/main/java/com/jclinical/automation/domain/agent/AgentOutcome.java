package com.jclinical.automation.domain.agent;

import com.jclinical.automation.domain.model.ConversationOption;

import java.util.List;

/**
 * Lo que sale de un turno del agente: los mensajes para el paciente, las opciones reales que se le
 * ofrecen y las senales para el codigo (no entendio, pasar a una persona, el modelo fallo).
 */
public record AgentOutcome(List<String> bubbles, List<ConversationOption> options, boolean notUnderstood,
                           boolean handoff, boolean failed) {

    public AgentOutcome {
        bubbles = bubbles == null ? List.of() : List.copyOf(bubbles);
        options = options == null ? List.of() : List.copyOf(options);
    }
}
