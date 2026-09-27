package com.jclinical.automation.domain.agent;

import java.util.List;

/** Lo que propone el modelo en cada paso: contestar al paciente o usar herramientas antes. */
public sealed interface ModelStep {

    /** Uno o varios mensajes de WhatsApp, en orden. */
    record Reply(List<String> bubbles) implements ModelStep {
        public Reply {
            bubbles = bubbles == null ? List.of() : List.copyOf(bubbles);
        }
    }

    record CallTools(List<AgentMessage.ToolCall> calls) implements ModelStep {
        public CallTools {
            calls = calls == null ? List.of() : List.copyOf(calls);
        }
    }
}
