package com.jclinical.automation.infra.adapters.in.web;

import com.jclinical.automation.domain.model.AssistantProfile;
import com.jclinical.automation.domain.ports.in.ManageAssistantProfileUseCase;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Ajustes del asistente (nombre, preguntas frecuentes, compartir precios) y si esta encendido. */
@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/automation")
@RequiredArgsConstructor
public class AssistantProfileController {

    private final ManageAssistantProfileUseCase profiles;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/assistant-profile")
    public AssistantProfile get(@PathVariable UUID clinicId) {
        return profiles.get(currentUserResolver.getCurrentUserId(), clinicId);
    }

    @PutMapping("/assistant-profile")
    public AssistantProfile update(@PathVariable UUID clinicId, @RequestBody AssistantProfile profile) {
        return profiles.update(currentUserResolver.getCurrentUserId(), clinicId, profile);
    }

    @GetMapping("/assistant-status")
    public AssistantStatus status(@PathVariable UUID clinicId) {
        return new AssistantStatus(profiles.assistantEnabled(currentUserResolver.getCurrentUserId(), clinicId));
    }

    public record AssistantStatus(boolean enabled) {}
}
