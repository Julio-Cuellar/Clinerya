package com.jclinical.automation.domain.ports.in;

import com.jclinical.automation.domain.model.AssistantProfile;

import java.util.UUID;

/**
 * Ajustes del asistente (plan v2, S5): nombre, preguntas frecuentes y compartir precios. Lo ve quien ve la
 * configuracion de la clinica y lo cambia quien administra las integraciones. Chats consulta si esta
 * encendido (VIEW_PATIENTS) para avisar cuando los pacientes no reciben respuesta.
 */
public interface ManageAssistantProfileUseCase {

    AssistantProfile get(UUID actingUserId, UUID clinicId);

    AssistantProfile update(UUID actingUserId, UUID clinicId, AssistantProfile profile);

    boolean assistantEnabled(UUID actingUserId, UUID clinicId);
}
