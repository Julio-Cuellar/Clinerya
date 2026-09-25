package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase;
import com.jclinical.automation.domain.ports.out.ChannelConnectionCheckPort;
import com.jclinical.automation.domain.ports.out.ChannelSettingsAuditPort;
import com.jclinical.automation.domain.ports.out.ChannelSettingsRepositoryPort;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

public class ChannelSettingsService implements ManageChannelSettingsUseCase {

    public ChannelSettingsService(ChannelSettingsRepositoryPort settings, ChannelSettingsAuditPort audit,
                                  ChannelConnectionCheckPort checks, StaffPermissionCheckerPort permissions,
                                  Supplier<String> randomTokens, Clock clock) {
    }

    @Override
    public ChannelSettingsView getSettings(UUID actingUserId, UUID clinicId) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public ChannelSettingsView updateWhatsApp(UUID actingUserId, UUID clinicId, WhatsAppCredentials credentials) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public ChannelSettingsView updateGemini(UUID actingUserId, UUID clinicId, GeminiCredentials credentials) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public ChannelSettingsView updateAssistant(UUID actingUserId, UUID clinicId, AssistantPreferences preferences) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public ChannelSettingsView removeSecret(UUID actingUserId, UUID clinicId, SecretKind kind) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public ChannelSettingsView regenerateVerifyToken(UUID actingUserId, UUID clinicId) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public ConnectionTestResult testConnection(UUID actingUserId, UUID clinicId) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public ChannelSettingsView setEnabled(UUID actingUserId, UUID clinicId, boolean enabled) {
        throw new UnsupportedOperationException("pendiente");
    }
}
