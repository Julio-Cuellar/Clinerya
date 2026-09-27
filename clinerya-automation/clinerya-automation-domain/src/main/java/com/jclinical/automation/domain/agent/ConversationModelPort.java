package com.jclinical.automation.domain.agent;

import java.util.List;
import java.util.UUID;

/**
 * El modelo de lenguaje de la clinica (Gemini, con su propia clave). Solo propone el siguiente paso;
 * el codigo decide que se ejecuta. Una falla se reporta como excepcion.
 */
public interface ConversationModelPort {

    ModelStep next(UUID clinicId, String systemInstruction, List<AgentMessage> transcript, List<ToolSpec> tools);
}
