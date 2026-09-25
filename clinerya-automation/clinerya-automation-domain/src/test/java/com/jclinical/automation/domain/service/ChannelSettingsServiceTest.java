package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.AssistantPrompt;
import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.domain.model.PromptMode;
import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase.AssistantPreferences;
import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase.ChannelSettingsView;
import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase.ConnectionTestResult;
import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase.GeminiCredentials;
import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase.SecretKind;
import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase.WhatsAppCredentials;
import com.jclinical.automation.domain.ports.out.ChannelConnectionCheckPort;
import com.jclinical.automation.domain.ports.out.ChannelSettingsAuditPort;
import com.jclinical.automation.domain.ports.out.ChannelSettingsRepositoryPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entrega 5.1: cada clinica configura su propio asistente (su app de Meta, su clave de Gemini y su
 * prompt). Los secretos se guardan pero nunca se muestran; sin Gemini, o sin probar la conexion, el
 * asistente no se puede activar (decision D4).
 */
class ChannelSettingsServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 9, 0);
    private static final String TOKEN = "EAAGm0PX4ZCpsBAKZCZBtoken1234567890";
    private static final String APP_SECRET = "a1b2c3d4e5f6secret9876";
    private static final String GEMINI_KEY = "AIzaSyFakeGeminiKey4321";

    private final UUID clinicId = UUID.randomUUID();
    private final UUID adminUserId = UUID.randomUUID();
    private final UUID receptionistUserId = UUID.randomUUID();
    private final UUID strangerUserId = UUID.randomUUID();

    private final InMemorySettings repository = new InMemorySettings();
    private final List<String> audit = new ArrayList<>();
    private final FakeChecks checks = new FakeChecks();
    private final Map<UUID, Set<StaffPermission>> permissions = new HashMap<>();
    private final AtomicInteger tokens = new AtomicInteger();

    private ChannelSettingsService service;

    @BeforeEach
    void setUp() {
        permissions.put(adminUserId, Set.of(StaffPermission.MANAGE_INTEGRATIONS, StaffPermission.VIEW_CLINIC_SETTINGS));
        permissions.put(receptionistUserId, Set.of(StaffPermission.VIEW_CLINIC_SETTINGS));
        StaffPermissionCheckerPort checker =
                (clinic, user, permission) -> permissions.getOrDefault(user, Set.of()).contains(permission);
        ChannelSettingsAuditPort auditPort = (clinic, user, action, at) -> audit.add(action);
        service = new ChannelSettingsService(repository, auditPort, checks, checker,
                () -> "random-" + tokens.incrementAndGet(), Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    @Test
    void withoutConfigurationNothingIsSetAndTheAssistantIsOff() {
        ChannelSettingsView view = service.getSettings(adminUserId, clinicId);

        assertNull(view.whatsappPhoneNumberId());
        assertNull(view.accessTokenHint());
        assertNull(view.geminiApiKeyHint());
        assertNull(view.webhookPath());
        assertFalse(view.enabled());
        assertEquals(PromptMode.DEFAULT, view.promptMode());
        assertEquals(AssistantPrompt.DEFAULT, view.defaultPrompt());
        assertEquals(12, view.chatRetentionMonths());
        assertEquals(ChannelSettings.DEFAULT_GEMINI_MODEL, view.geminiModel());
    }

    @Test
    void savingWhatsAppStoresTheSecretsButOnlyShowsAHint() {
        ChannelSettingsView view = saveWhatsApp();

        assertEquals("106540352242922", view.whatsappPhoneNumberId());
        assertEquals("••••7890", view.accessTokenHint());
        assertEquals("••••9876", view.appSecretHint());
        assertEquals("/api/v1/public/whatsapp/random-1", view.webhookPath());
        assertEquals("random-2", view.verifyToken(), "quien administra integraciones lo necesita para Meta");
        assertEquals(TOKEN, repository.stored.get(clinicId).whatsappAccessToken());
        assertFalse(view.toString().contains(TOKEN));
        assertFalse(view.toString().contains(APP_SECRET));
    }

    @Test
    void aBlankSecretKeepsTheOneAlreadyStored() {
        saveWhatsApp();

        service.updateWhatsApp(adminUserId, clinicId, new WhatsAppCredentials("106540352242922", "102290129340398", " ", null));

        assertEquals(TOKEN, repository.stored.get(clinicId).whatsappAccessToken());
        assertEquals(APP_SECRET, repository.stored.get(clinicId).whatsappAppSecret());
        assertEquals("random-1", repository.stored.get(clinicId).webhookKey(), "la URL del webhook no cambia");
    }

    @Test
    void whatsAppIdsMustBeMetaIds() {
        assertThrows(IllegalArgumentException.class, () -> service.updateWhatsApp(adminUserId, clinicId,
                new WhatsAppCredentials("+52 55 1234", "102290129340398", TOKEN, APP_SECRET)));
        assertThrows(IllegalArgumentException.class, () -> service.updateWhatsApp(adminUserId, clinicId,
                new WhatsAppCredentials("106540352242922", "", TOKEN, APP_SECRET)));
        assertThrows(IllegalArgumentException.class, () -> service.updateWhatsApp(adminUserId, clinicId,
                new WhatsAppCredentials("106540352242922", "102290129340398", "token con espacios", APP_SECRET)));
    }

    @Test
    void aNumberConnectedToAnotherClinicCannotBeReused() {
        UUID otherClinic = UUID.randomUUID();
        repository.save(ChannelSettings.unconfigured(otherClinic).toBuilder().whatsappPhoneNumberId("106540352242922").build());

        assertThrows(IllegalArgumentException.class, this::saveWhatsApp);
    }

    @Test
    void onlyWhoManagesIntegrationsCanChangeTheSettings() {
        assertThrows(ClinicAccessDeniedException.class, () -> service.updateWhatsApp(receptionistUserId, clinicId,
                new WhatsAppCredentials("106540352242922", "102290129340398", TOKEN, APP_SECRET)));
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.updateGemini(receptionistUserId, clinicId, new GeminiCredentials(GEMINI_KEY, null)));
        assertThrows(ClinicAccessDeniedException.class, () -> service.updateAssistant(receptionistUserId, clinicId,
                new AssistantPreferences(PromptMode.DEFAULT, null, 12)));
        assertThrows(ClinicAccessDeniedException.class, () -> service.testConnection(receptionistUserId, clinicId));
        assertThrows(ClinicAccessDeniedException.class, () -> service.setEnabled(receptionistUserId, clinicId, true));
        assertThrows(ClinicAccessDeniedException.class, () -> service.regenerateVerifyToken(receptionistUserId, clinicId));
        assertThrows(ClinicAccessDeniedException.class,
                () -> service.removeSecret(receptionistUserId, clinicId, SecretKind.GEMINI_API_KEY));
        assertTrue(repository.stored.isEmpty());
    }

    @Test
    void readingNeedsClinicSettingsPermissionAndHidesTheVerifyToken() {
        saveWhatsApp();

        ChannelSettingsView view = service.getSettings(receptionistUserId, clinicId);

        assertNull(view.verifyToken());
        assertEquals("••••7890", view.accessTokenHint());
        assertThrows(ClinicAccessDeniedException.class, () -> service.getSettings(strangerUserId, clinicId));
    }

    @Test
    void testingTheConnectionRecordsWhichChannelWorks() {
        saveWhatsApp();
        service.updateGemini(adminUserId, clinicId, new GeminiCredentials(GEMINI_KEY, null));
        checks.geminiOk = false;

        ConnectionTestResult result = service.testConnection(adminUserId, clinicId);

        assertTrue(result.whatsappOk());
        assertEquals("Clínica Sonrisa", result.whatsappDetail());
        assertFalse(result.geminiOk());
        assertEquals(List.of("106540352242922|" + TOKEN), checks.whatsappCalls);
        assertEquals(List.of(GEMINI_KEY + "|" + ChannelSettings.DEFAULT_GEMINI_MODEL), checks.geminiCalls);
        assertEquals(NOW, repository.stored.get(clinicId).whatsappVerifiedAt());
        assertNull(repository.stored.get(clinicId).geminiVerifiedAt());
    }

    @Test
    void testingWithoutCredentialsDoesNotCallMetaOrGemini() {
        ConnectionTestResult result = service.testConnection(adminUserId, clinicId);

        assertFalse(result.whatsappOk());
        assertFalse(result.geminiOk());
        assertTrue(checks.whatsappCalls.isEmpty());
        assertTrue(checks.geminiCalls.isEmpty());
    }

    @Test
    void withoutAGeminiKeyTheAssistantCannotBeEnabled() {
        saveWhatsApp();
        service.testConnection(adminUserId, clinicId);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.setEnabled(adminUserId, clinicId, true));

        assertTrue(error.getMessage().contains("Gemini"), error.getMessage());
        assertFalse(repository.stored.get(clinicId).enabled());
    }

    @Test
    void untestedConnectionsKeepTheAssistantOff() {
        saveWhatsApp();
        service.updateGemini(adminUserId, clinicId, new GeminiCredentials(GEMINI_KEY, null));

        assertThrows(IllegalStateException.class, () -> service.setEnabled(adminUserId, clinicId, true));
        assertTrue(service.getSettings(adminUserId, clinicId).missingToEnable().size() >= 2);
    }

    @Test
    void configuredAndTestedTheAssistantCanBeEnabledAndDisabled() {
        configureEverything();

        assertTrue(service.setEnabled(adminUserId, clinicId, true).enabled());
        assertTrue(service.getSettings(adminUserId, clinicId).missingToEnable().isEmpty());
        assertFalse(service.setEnabled(adminUserId, clinicId, false).enabled());
    }

    @Test
    void changingCredentialsRequiresTestingAgainAndTurnsTheAssistantOff() {
        configureEverything();
        service.setEnabled(adminUserId, clinicId, true);

        service.updateWhatsApp(adminUserId, clinicId,
                new WhatsAppCredentials("106540352242922", "102290129340398", "EAAnuevoToken0000000000001111", null));

        ChannelSettings stored = repository.stored.get(clinicId);
        assertFalse(stored.enabled());
        assertNull(stored.whatsappVerifiedAt());
        assertEquals(NOW, stored.geminiVerifiedAt(), "Gemini no cambio");
    }

    @Test
    void removingASecretTurnsTheAssistantOff() {
        configureEverything();
        service.setEnabled(adminUserId, clinicId, true);

        ChannelSettingsView view = service.removeSecret(adminUserId, clinicId, SecretKind.GEMINI_API_KEY);

        assertNull(view.geminiApiKeyHint());
        assertFalse(view.enabled());
        assertNull(repository.stored.get(clinicId).geminiApiKey());
    }

    @Test
    void theClinicCanWriteItsOwnPromptOrGoBackToTheDefault() {
        ChannelSettingsView custom = service.updateAssistant(adminUserId, clinicId,
                new AssistantPreferences(PromptMode.CUSTOM, "  Somos Clínica Sonrisa, tutea al paciente.  ", 12));

        assertEquals(PromptMode.CUSTOM, custom.promptMode());
        assertEquals("Somos Clínica Sonrisa, tutea al paciente.", custom.customPrompt());

        ChannelSettingsView restored = service.updateAssistant(adminUserId, clinicId,
                new AssistantPreferences(PromptMode.DEFAULT, null, 12));
        assertEquals(PromptMode.DEFAULT, restored.promptMode());
        assertNull(restored.customPrompt());
    }

    @Test
    void aCustomPromptCannotBeEmptyOrTooLong() {
        assertThrows(IllegalArgumentException.class, () -> service.updateAssistant(adminUserId, clinicId,
                new AssistantPreferences(PromptMode.CUSTOM, "   ", 12)));
        assertThrows(IllegalArgumentException.class, () -> service.updateAssistant(adminUserId, clinicId,
                new AssistantPreferences(PromptMode.CUSTOM, "x".repeat(AssistantPrompt.MAX_LENGTH + 1), 12)));
    }

    @Test
    void chatRetentionIsConfigurableBetweenOneAndSixtyMonths() {
        assertEquals(24, service.updateAssistant(adminUserId, clinicId,
                new AssistantPreferences(PromptMode.DEFAULT, null, 24)).chatRetentionMonths());
        assertThrows(IllegalArgumentException.class, () -> service.updateAssistant(adminUserId, clinicId,
                new AssistantPreferences(PromptMode.DEFAULT, null, 0)));
        assertThrows(IllegalArgumentException.class, () -> service.updateAssistant(adminUserId, clinicId,
                new AssistantPreferences(PromptMode.DEFAULT, null, 61)));
    }

    @Test
    void theVerifyTokenCanBeRegenerated() {
        saveWhatsApp();

        ChannelSettingsView view = service.regenerateVerifyToken(adminUserId, clinicId);

        assertNotEquals("random-2", view.verifyToken());
        assertEquals(view.verifyToken(), repository.stored.get(clinicId).verifyToken());
    }

    @Test
    void everyChangeIsAuditedWithoutTheSecretValues() {
        configureEverything();
        service.setEnabled(adminUserId, clinicId, true);
        service.removeSecret(adminUserId, clinicId, SecretKind.WHATSAPP_APP_SECRET);

        assertEquals(List.of("WHATSAPP_UPDATED", "GEMINI_UPDATED", "CONNECTION_TESTED", "ENABLED",
                "SECRET_REMOVED:WHATSAPP_APP_SECRET"), audit);
        assertTrue(audit.stream().noneMatch(entry -> entry.contains(TOKEN) || entry.contains(GEMINI_KEY)));
    }

    // ---- utilidades -------------------------------------------------------------------------

    private ChannelSettingsView saveWhatsApp() {
        return service.updateWhatsApp(adminUserId, clinicId,
                new WhatsAppCredentials("106540352242922", "102290129340398", TOKEN, APP_SECRET));
    }

    private void configureEverything() {
        saveWhatsApp();
        service.updateGemini(adminUserId, clinicId, new GeminiCredentials(GEMINI_KEY, null));
        service.testConnection(adminUserId, clinicId);
    }

    static final class InMemorySettings implements ChannelSettingsRepositoryPort {
        final Map<UUID, ChannelSettings> stored = new HashMap<>();

        @Override
        public Optional<ChannelSettings> findByClinicId(UUID clinicId) {
            return Optional.ofNullable(stored.get(clinicId));
        }

        @Override
        public Optional<ChannelSettings> findByPhoneNumberId(String phoneNumberId) {
            return stored.values().stream().filter(s -> phoneNumberId.equals(s.whatsappPhoneNumberId())).findFirst();
        }

        @Override
        public Optional<ChannelSettings> findByWebhookKey(String webhookKey) {
            return stored.values().stream().filter(s -> webhookKey.equals(s.webhookKey())).findFirst();
        }

        @Override
        public ChannelSettings save(ChannelSettings settings) {
            stored.put(settings.clinicId(), settings);
            return settings;
        }
    }

    static final class FakeChecks implements ChannelConnectionCheckPort {
        final List<String> whatsappCalls = new ArrayList<>();
        final List<String> geminiCalls = new ArrayList<>();
        boolean whatsappOk = true;
        boolean geminiOk = true;

        @Override
        public CheckResult checkWhatsApp(String phoneNumberId, String accessToken) {
            whatsappCalls.add(phoneNumberId + "|" + accessToken);
            return new CheckResult(whatsappOk, whatsappOk ? "Clínica Sonrisa" : "Token inválido");
        }

        @Override
        public CheckResult checkGemini(String apiKey, String model) {
            geminiCalls.add(apiKey + "|" + model);
            return new CheckResult(geminiOk, geminiOk ? "Gemini respondió" : "Clave inválida");
        }
    }
}
