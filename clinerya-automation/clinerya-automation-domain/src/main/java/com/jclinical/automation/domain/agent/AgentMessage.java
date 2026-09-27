package com.jclinical.automation.domain.agent;

import java.util.Map;

/**
 * Un turno de la conversacion tal como lo ve el modelo: lo que escribio el paciente, lo que se le
 * contesto, y las llamadas a herramientas con su resultado dentro del turno actual.
 */
public sealed interface AgentMessage {

    record User(String text) implements AgentMessage {}

    record Assistant(String text) implements AgentMessage {}

    /** El modelo pide usar una herramienta. Sus argumentos nunca deciden de quien son los datos. */
    record ToolCall(String id, String name, Map<String, Object> arguments) implements AgentMessage {
        public ToolCall {
            arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
        }
    }

    record ToolResult(String callId, String name, Map<String, Object> content) implements AgentMessage {
        public ToolResult {
            content = content == null ? Map.of() : Map.copyOf(content);
        }
    }
}
