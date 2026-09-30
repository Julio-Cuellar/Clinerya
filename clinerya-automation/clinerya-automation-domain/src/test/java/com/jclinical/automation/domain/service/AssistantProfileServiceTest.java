package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.AssistantProfile;
import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.domain.ports.out.AssistantProfileStorePort;
import com.jclinical.automation.domain.service.ChannelSettingsServiceTest.InMemorySettings;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plan v2, S5: Ajustes del asistente. Quien administra las integraciones define como se presenta
 * (nombre), que preguntas frecuentes responde y si comparte precios; queda en la bitacora. Chats sabe si
 * el asistente esta apagado para avisar que los pacientes no reciben respuesta.
 */
class AssistantProfileServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 30, 12, 0);

    private final UUID clinicId = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();
    private final UUID viewer = UUID.randomUUID();
    private final UUID receptionist = UUID.randomUUID();
    private final Map<UUID, Set<StaffPermission>> permissions = new HashMap<>();
    private final InMemoryProfiles profiles = new InMemoryProfiles();
    private final InMemorySettings settings = new InMemorySettings();
    private final List<String> audit = new ArrayList<>();

    private AssistantProfileService service;

    @BeforeEach
    void setUp() {
        permissions.put(admin, Set.of(StaffPermission.MANAGE_INTEGRATIONS, StaffPermission.VIEW_CLINIC_SETTINGS));
        permissions.put(viewer, Set.of(StaffPermission.VIEW_CLINIC_SETTINGS));
        permissions.put(receptionist, Set.of(StaffPermission.VIEW_PATIENTS));
        service = new AssistantProfileService(profiles, settings,
                (clinic, user, permission) -> permissions.getOrDefault(user, Set.of()).contains(permission),
                (clinic, user, action, at) -> audit.add(action), Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    @Test
    void theAdminSavesHowTheAssistantPresentsItselfAndItIsAudited() {
        AssistantProfile saved = service.update(admin, clinicId,
                new AssistantProfile("  Sofi  ", "Aceptamos efectivo y tarjeta.", false));

        assertEquals(new AssistantProfile("Sofi", "Aceptamos efectivo y tarjeta.", false), saved);
        assertEquals(saved, service.get(viewer, clinicId));
        assertEquals(List.of("ASSISTANT_PROFILE_UPDATED"), audit);
    }

    @Test
    void anEmptyNameMeansItSpeaksForTheClinic() {
        AssistantProfile saved = service.update(admin, clinicId, new AssistantProfile("   ", "   ", true));

        assertEquals(new AssistantProfile(null, null, true), saved);
    }

    @Test
    void tooLongTextsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.update(admin, clinicId,
                new AssistantProfile("x".repeat(AssistantProfileService.MAX_NAME + 1), null, true)));
        assertThrows(IllegalArgumentException.class, () -> service.update(admin, clinicId,
                new AssistantProfile(null, "x".repeat(AssistantProfileService.MAX_FAQ + 1), true)));
    }

    @Test
    void theReminderSettingsAreKeptAndChecked() {
        AssistantProfile saved = service.update(admin, clinicId,
                new AssistantProfile("Sofi", null, true, false, 48, "  recordatorio_cita  "));

        assertEquals(new AssistantProfile("Sofi", null, true, false, 48, "recordatorio_cita"), saved);
        assertEquals(saved, profiles.byClinic.get(clinicId));
        assertThrows(IllegalArgumentException.class, () -> service.update(admin, clinicId,
                new AssistantProfile(null, null, true, true, AssistantProfileService.MAX_REMINDER_HOURS + 1, null)));
        assertThrows(IllegalArgumentException.class, () -> service.update(admin, clinicId,
                new AssistantProfile(null, null, true, true, 24, "Recordatorio Cita!")));
    }

    @Test
    void onlyWhoManagesIntegrationsChangesItAndOnlySettingsViewersReadIt() {
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.update(viewer, clinicId, new AssistantProfile("Sofi", null, true)));
        assertThrows(ClinicAccessDeniedException.class, () -> service.get(receptionist, clinicId));
    }

    @Test
    void chatsKnowWhetherTheAssistantIsOn() {
        assertFalse(service.assistantEnabled(receptionist, clinicId), "sin configurar esta apagado");

        settings.save(ChannelSettings.unconfigured(clinicId).toBuilder().enabled(true).build());

        assertTrue(service.assistantEnabled(receptionist, clinicId));
        assertThrows(ClinicAccessDeniedException.class, () -> service.assistantEnabled(UUID.randomUUID(), clinicId));
    }

    static final class InMemoryProfiles implements AssistantProfileStorePort {
        final Map<UUID, AssistantProfile> byClinic = new HashMap<>();

        @Override
        public AssistantProfile find(UUID clinicId) {
            return byClinic.getOrDefault(clinicId, AssistantProfile.EMPTY);
        }

        @Override
        public void save(UUID clinicId, AssistantProfile profile) {
            byClinic.put(clinicId, profile);
        }
    }
}
