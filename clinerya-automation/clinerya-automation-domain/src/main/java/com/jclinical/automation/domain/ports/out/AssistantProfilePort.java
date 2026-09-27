package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.AssistantProfile;

import java.util.UUID;

/** Perfil del asistente que configura cada clinica en Ajustes. */
@FunctionalInterface
public interface AssistantProfilePort {

    AssistantProfile find(UUID clinicId);
}
