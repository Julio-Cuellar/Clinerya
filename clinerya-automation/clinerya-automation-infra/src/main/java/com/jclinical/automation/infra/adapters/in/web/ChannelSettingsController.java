package com.jclinical.automation.infra.adapters.in.web;

import com.jclinical.automation.domain.model.PromptMode;
import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase;
import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase.AssistantPreferences;
import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase.ChannelSettingsView;
import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase.ConnectionTestResult;
import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase.GeminiCredentials;
import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase.SecretKind;
import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase.TemplateSettings;
import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase.WhatsAppCredentials;
import com.jclinical.users.infra.security.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Ajustes > Clinica e Integraciones > Asistente de WhatsApp. Los secretos entran, nunca salen. */
@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/automation/settings")
@RequiredArgsConstructor
public class ChannelSettingsController {

    private final ManageChannelSettingsUseCase settings;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public ChannelSettingsView get(@PathVariable UUID clinicId) {
        return settings.getSettings(currentUserResolver.getCurrentUserId(), clinicId);
    }

    @PutMapping("/whatsapp")
    public ChannelSettingsView updateWhatsApp(@PathVariable UUID clinicId, @RequestBody WhatsAppCredentials body) {
        return settings.updateWhatsApp(currentUserResolver.getCurrentUserId(), clinicId, required(body));
    }

    @PutMapping("/gemini")
    public ChannelSettingsView updateGemini(@PathVariable UUID clinicId, @RequestBody GeminiCredentials body) {
        return settings.updateGemini(currentUserResolver.getCurrentUserId(), clinicId, required(body));
    }

    @PutMapping("/assistant")
    public ChannelSettingsView updateAssistant(@PathVariable UUID clinicId, @RequestBody AssistantRequest body) {
        AssistantRequest request = required(body);
        return settings.updateAssistant(currentUserResolver.getCurrentUserId(), clinicId,
                new AssistantPreferences(request.promptMode(), request.customPrompt(),
                        request.chatRetentionMonths() == null ? 12 : request.chatRetentionMonths()));
    }

    /** Plantillas aprobadas en Meta para escribir fuera de la ventana de 24 h (paciente y medico). */
    @PutMapping("/templates")
    public ChannelSettingsView updateTemplates(@PathVariable UUID clinicId, @RequestBody TemplateSettings body) {
        return settings.updateTemplates(currentUserResolver.getCurrentUserId(), clinicId, required(body));
    }

    @DeleteMapping("/secrets/{kind}")
    public ChannelSettingsView removeSecret(@PathVariable UUID clinicId, @PathVariable SecretKind kind) {
        return settings.removeSecret(currentUserResolver.getCurrentUserId(), clinicId, kind);
    }

    @PostMapping("/verify-token")
    public ChannelSettingsView regenerateVerifyToken(@PathVariable UUID clinicId) {
        return settings.regenerateVerifyToken(currentUserResolver.getCurrentUserId(), clinicId);
    }

    @PostMapping("/test-connection")
    public ConnectionTestResult testConnection(@PathVariable UUID clinicId) {
        return settings.testConnection(currentUserResolver.getCurrentUserId(), clinicId);
    }

    @PutMapping("/enabled")
    public ChannelSettingsView setEnabled(@PathVariable UUID clinicId, @RequestBody EnabledRequest body) {
        return settings.setEnabled(currentUserResolver.getCurrentUserId(), clinicId, required(body).enabled());
    }

    private static <T> T required(T body) {
        if (body == null) {
            throw new IllegalArgumentException("Falta el cuerpo de la petición.");
        }
        return body;
    }

    public record AssistantRequest(PromptMode promptMode, String customPrompt, Integer chatRetentionMonths) {}

    public record EnabledRequest(boolean enabled) {}
}
