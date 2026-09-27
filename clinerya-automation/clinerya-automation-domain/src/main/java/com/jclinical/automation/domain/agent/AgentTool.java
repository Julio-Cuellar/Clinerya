package com.jclinical.automation.domain.agent;

import java.util.Map;

/**
 * Algo que el agente puede consultar o hacer. Cada herramienta envuelve un caso de uso publico de
 * otro modulo (a traves de un puerto): el agente nunca lee la base de datos.
 */
public interface AgentTool {

    ToolSpec spec();

    ToolOutcome run(ToolContext context, Map<String, Object> arguments);
}
