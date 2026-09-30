package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.AssistantProfile;
import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.domain.ports.in.ManageAssistantProfileUseCase;
import com.jclinical.automation.domain.ports.out.AssistantProfileStorePort;
import com.jclinical.automation.domain.ports.out.ChannelSettingsAuditPort;
import com.jclinical.automation.domain.ports.out.ChannelSettingsRepositoryPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/** Ajustes del asistente: como se presenta, que responde y si comparte precios. */
public class AssistantProfileService implements ManageAssistantProfileUseCase {

    public static final int MAX_NAME = 60;
    public static final int MAX_FAQ = 4000;

    private final AssistantProfileStorePort profiles;
    private final ChannelSettingsRepositoryPort settings;
    private final StaffPermissionCheckerPort permissions;
    private final ChannelSettingsAuditPort audit;
    private final Clock clock;

    public AssistantProfileService(AssistantProfileStorePort profiles, ChannelSettingsRepositoryPort settings,
                                   StaffPermissionCheckerPort permissions, ChannelSettingsAuditPort audit, Clock clock) {
        this.profiles = profiles;
        this.settings = settings;
        this.permissions = permissions;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public AssistantProfile get(UUID actingUserId, UUID clinicId) {
        if (!can(actingUserId, clinicId, StaffPermission.MANAGE_INTEGRATIONS)
                && !can(actingUserId, clinicId, StaffPermission.VIEW_CLINIC_SETTINGS)) {
            throw new ClinicAccessDeniedException("No tienes permiso para ver la configuración de la clínica.");
        }
        return profiles.find(clinicId);
    }

    @Override
    public AssistantProfile update(UUID actingUserId, UUID clinicId, AssistantProfile profile) {
        if (!can(actingUserId, clinicId, StaffPermission.MANAGE_INTEGRATIONS)) {
            throw new ClinicAccessDeniedException("Solo quien administra las integraciones puede configurar el asistente.");
        }
        String name = blankToNull(profile.assistantName());
        String faq = blankToNull(profile.faq());
        if (name != null && name.length() > MAX_NAME) {
            throw new IllegalArgumentException("El nombre del asistente admite hasta " + MAX_NAME + " caracteres.");
        }
        if (faq != null && faq.length() > MAX_FAQ) {
            throw new IllegalArgumentException("Las preguntas frecuentes admiten hasta " + MAX_FAQ + " caracteres.");
        }
        AssistantProfile cleaned = new AssistantProfile(name, faq, profile.showPrices());
        profiles.save(clinicId, cleaned);
        audit.record(clinicId, actingUserId, "ASSISTANT_PROFILE_UPDATED", LocalDateTime.now(clock));
        return cleaned;
    }

    @Override
    public boolean assistantEnabled(UUID actingUserId, UUID clinicId) {
        if (!can(actingUserId, clinicId, StaffPermission.VIEW_PATIENTS)) {
            throw new ClinicAccessDeniedException("No tienes permiso para ver los chats de WhatsApp de esta clínica.");
        }
        return settings.findByClinicId(clinicId).map(ChannelSettings::enabled).orElse(false);
    }

    private boolean can(UUID actingUserId, UUID clinicId, StaffPermission permission) {
        return actingUserId != null && permissions.hasPermission(clinicId, actingUserId, permission);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
