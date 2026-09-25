package com.jclinical.automation.domain.model;

/**
 * Estados de la conversacion con el paciente (ver documentacion/automatizacion/flujo-citas-whatsapp.md).
 * Solo existen las transiciones que el motor implementa; los estados terminales no aceptan mensajes y
 * el siguiente mensaje del mismo celular abre una conversacion nueva.
 */
public enum ConversationState {
    IDENTIFICANDO,
    ELEGIR_PACIENTE,
    MENU,
    ELEGIR_MEDICO,
    ELEGIR_CUPO,
    ESPERANDO_MEDICO,
    /** El medico propuso otros horarios; el paciente elige uno o los declina. */
    ELEGIR_OPCION_MEDICO,
    CERRADA(true),
    EXPIRADA(true);

    private final boolean terminal;

    ConversationState() {
        this(false);
    }

    ConversationState(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean isTerminal() {
        return terminal;
    }
}
