package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.AssistantProfile;

import java.util.UUID;

/** Donde se guarda el perfil del asistente de cada clinica (Ajustes). */
public interface AssistantProfileStorePort extends AssistantProfilePort {

    /** @throws IllegalStateException si la clinica aun no configuro su asistente. */
    void save(UUID clinicId, AssistantProfile profile);
}
