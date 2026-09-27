package com.jclinical.automation.domain.agent;

/**
 * Quien contesta el WhatsApp de la clinica. {@code assistantName} vacio: se habla a nombre de la
 * clinica. {@code tone} es la preferencia de estilo que escribio la clinica (puede faltar).
 */
public record AgentPersona(String clinicName, String assistantName, String tone) {

    public boolean hasName() {
        return assistantName != null && !assistantName.isBlank();
    }
}
