package com.jclinical.automation.infra.config;

import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase;
import com.jclinical.automation.domain.service.ChannelSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** La configuracion y su renglon de bitacora se guardan juntos o no se guardan. */
@Service
@Primary
@RequiredArgsConstructor
public class TransactionalChannelSettingsUseCase implements ManageChannelSettingsUseCase {

    private final ChannelSettingsService settings;

    @Override
    @Transactional(readOnly = true)
    public ChannelSettingsView getSettings(UUID actingUserId, UUID clinicId) {
        return settings.getSettings(actingUserId, clinicId);
    }

    @Override
    @Transactional
    public ChannelSettingsView updateWhatsApp(UUID actingUserId, UUID clinicId, WhatsAppCredentials credentials) {
        return settings.updateWhatsApp(actingUserId, clinicId, credentials);
    }

    @Override
    @Transactional
    public ChannelSettingsView updateGemini(UUID actingUserId, UUID clinicId, GeminiCredentials credentials) {
        return settings.updateGemini(actingUserId, clinicId, credentials);
    }

    @Override
    @Transactional
    public ChannelSettingsView updateAssistant(UUID actingUserId, UUID clinicId, AssistantPreferences preferences) {
        return settings.updateAssistant(actingUserId, clinicId, preferences);
    }

    @Override
    @Transactional
    public ChannelSettingsView removeSecret(UUID actingUserId, UUID clinicId, SecretKind kind) {
        return settings.removeSecret(actingUserId, clinicId, kind);
    }

    @Override
    @Transactional
    public ChannelSettingsView regenerateVerifyToken(UUID actingUserId, UUID clinicId) {
        return settings.regenerateVerifyToken(actingUserId, clinicId);
    }

    @Override
    @Transactional
    public ConnectionTestResult testConnection(UUID actingUserId, UUID clinicId) {
        return settings.testConnection(actingUserId, clinicId);
    }

    @Override
    @Transactional
    public ChannelSettingsView setEnabled(UUID actingUserId, UUID clinicId, boolean enabled) {
        return settings.setEnabled(actingUserId, clinicId, enabled);
    }
}
