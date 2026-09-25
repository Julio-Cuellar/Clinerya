package com.jclinical.automation.domain.service;

import com.jclinical.automation.domain.model.AssistantPrompt;
import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.domain.model.PromptMode;
import com.jclinical.automation.domain.ports.in.ManageChannelSettingsUseCase;
import com.jclinical.automation.domain.ports.out.ChannelConnectionCheckPort;
import com.jclinical.automation.domain.ports.out.ChannelConnectionCheckPort.CheckResult;
import com.jclinical.automation.domain.ports.out.ChannelSettingsAuditPort;
import com.jclinical.automation.domain.ports.out.ChannelSettingsRepositoryPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Configuracion del asistente de cada clinica. Guarda los secretos tal cual (el adaptador los
 * cifra) pero nunca los devuelve; cualquier cambio de credenciales exige volver a probar la conexion
 * y apaga el asistente, y activarlo exige WhatsApp y Gemini configurados y probados (D4).
 */
public class ChannelSettingsService implements ManageChannelSettingsUseCase {

    static final String WEBHOOK_PATH_PREFIX = "/api/v1/public/whatsapp/";
    static final int MIN_RETENTION_MONTHS = 1;
    static final int MAX_RETENTION_MONTHS = 60;

    private static final Pattern META_ID = Pattern.compile("\\d{5,30}");
    private static final Pattern GEMINI_MODEL = Pattern.compile("[a-z0-9][a-z0-9.\\-]{2,59}");
    private static final Pattern NO_WHITESPACE = Pattern.compile("\\S+");
    private static final int MAX_TOKEN_LENGTH = 2048;
    private static final int MAX_SECRET_LENGTH = 512;

    private final ChannelSettingsRepositoryPort settings;
    private final ChannelSettingsAuditPort audit;
    private final ChannelConnectionCheckPort checks;
    private final StaffPermissionCheckerPort permissions;
    private final Supplier<String> randomTokens;
    private final Clock clock;

    public ChannelSettingsService(ChannelSettingsRepositoryPort settings, ChannelSettingsAuditPort audit,
                                  ChannelConnectionCheckPort checks, StaffPermissionCheckerPort permissions,
                                  Supplier<String> randomTokens, Clock clock) {
        this.settings = settings;
        this.audit = audit;
        this.checks = checks;
        this.permissions = permissions;
        this.randomTokens = randomTokens;
        this.clock = clock;
    }

    @Override
    public ChannelSettingsView getSettings(UUID actingUserId, UUID clinicId) {
        boolean manages = can(actingUserId, clinicId, StaffPermission.MANAGE_INTEGRATIONS);
        if (!manages && !can(actingUserId, clinicId, StaffPermission.VIEW_CLINIC_SETTINGS)) {
            throw new ClinicAccessDeniedException("No tienes permiso para ver la configuración de la clínica.");
        }
        return view(current(clinicId), manages);
    }

    @Override
    public ChannelSettingsView updateWhatsApp(UUID actingUserId, UUID clinicId, WhatsAppCredentials credentials) {
        requireManage(actingUserId, clinicId);
        String phoneNumberId = requireMetaId(credentials.phoneNumberId(), "El identificador del número de WhatsApp");
        String businessAccountId = requireMetaId(credentials.businessAccountId(), "El identificador de la cuenta de WhatsApp Business");
        settings.findByPhoneNumberId(phoneNumberId)
                .filter(other -> !other.clinicId().equals(clinicId))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("Ese número de WhatsApp ya está conectado a otra clínica.");
                });

        ChannelSettings existing = current(clinicId);
        String accessToken = secretOrKeep(credentials.accessToken(), existing.whatsappAccessToken(),
                MAX_TOKEN_LENGTH, "El token de acceso");
        String appSecret = secretOrKeep(credentials.appSecret(), existing.whatsappAppSecret(),
                MAX_SECRET_LENGTH, "El secreto de la app de Meta");
        boolean changed = !Objects.equals(phoneNumberId, existing.whatsappPhoneNumberId())
                || !Objects.equals(businessAccountId, existing.whatsappBusinessAccountId())
                || !Objects.equals(accessToken, existing.whatsappAccessToken())
                || !Objects.equals(appSecret, existing.whatsappAppSecret());

        ChannelSettings.Builder updated = existing.toBuilder()
                .whatsappPhoneNumberId(phoneNumberId)
                .whatsappBusinessAccountId(businessAccountId)
                .whatsappAccessToken(accessToken)
                .whatsappAppSecret(appSecret);
        if (existing.webhookKey() == null) {
            updated.webhookKey(randomTokens.get());
        }
        if (existing.verifyToken() == null) {
            updated.verifyToken(randomTokens.get());
        }
        if (changed) {
            updated.whatsappVerifiedAt(null).enabled(false);
        }
        return save(updated, actingUserId, clinicId, "WHATSAPP_UPDATED");
    }

    @Override
    public ChannelSettingsView updateGemini(UUID actingUserId, UUID clinicId, GeminiCredentials credentials) {
        requireManage(actingUserId, clinicId);
        String model = isBlank(credentials.model()) ? ChannelSettings.DEFAULT_GEMINI_MODEL : credentials.model().trim();
        if (!GEMINI_MODEL.matcher(model).matches()) {
            throw new IllegalArgumentException("El modelo de Gemini no es válido.");
        }
        ChannelSettings existing = current(clinicId);
        String apiKey = secretOrKeep(credentials.apiKey(), existing.geminiApiKey(), MAX_SECRET_LENGTH, "La clave de Gemini");
        boolean changed = !Objects.equals(apiKey, existing.geminiApiKey()) || !Objects.equals(model, existing.geminiModel());

        ChannelSettings.Builder updated = existing.toBuilder().geminiApiKey(apiKey).geminiModel(model);
        if (changed) {
            updated.geminiVerifiedAt(null).enabled(false);
        }
        return save(updated, actingUserId, clinicId, "GEMINI_UPDATED");
    }

    @Override
    public ChannelSettingsView updateAssistant(UUID actingUserId, UUID clinicId, AssistantPreferences preferences) {
        requireManage(actingUserId, clinicId);
        int retention = preferences.chatRetentionMonths();
        if (retention < MIN_RETENTION_MONTHS || retention > MAX_RETENTION_MONTHS) {
            throw new IllegalArgumentException("El historial se conserva entre " + MIN_RETENTION_MONTHS + " y "
                    + MAX_RETENTION_MONTHS + " meses.");
        }
        PromptMode mode = preferences.promptMode() == null ? PromptMode.DEFAULT : preferences.promptMode();
        String customPrompt = null;
        if (mode == PromptMode.CUSTOM) {
            if (isBlank(preferences.customPrompt())) {
                throw new IllegalArgumentException("Escribe el prompt personalizado o usa el predeterminado.");
            }
            customPrompt = preferences.customPrompt().trim();
            if (customPrompt.length() > AssistantPrompt.MAX_LENGTH) {
                throw new IllegalArgumentException("El prompt no puede pasar de " + AssistantPrompt.MAX_LENGTH + " caracteres.");
            }
        }
        ChannelSettings.Builder updated = current(clinicId).toBuilder()
                .promptMode(mode)
                .customPrompt(customPrompt)
                .chatRetentionMonths(retention);
        return save(updated, actingUserId, clinicId, "ASSISTANT_UPDATED");
    }

    @Override
    public ChannelSettingsView removeSecret(UUID actingUserId, UUID clinicId, SecretKind kind) {
        requireManage(actingUserId, clinicId);
        ChannelSettings.Builder updated = current(clinicId).toBuilder().enabled(false);
        switch (kind) {
            case WHATSAPP_ACCESS_TOKEN -> updated.whatsappAccessToken(null).whatsappVerifiedAt(null);
            case WHATSAPP_APP_SECRET -> updated.whatsappAppSecret(null);
            case GEMINI_API_KEY -> updated.geminiApiKey(null).geminiVerifiedAt(null);
        }
        return save(updated, actingUserId, clinicId, "SECRET_REMOVED:" + kind.name());
    }

    @Override
    public ChannelSettingsView updateTemplates(UUID actingUserId, UUID clinicId, TemplateSettings templates) {
        throw new UnsupportedOperationException("pendiente");
    }

    @Override
    public ChannelSettingsView regenerateVerifyToken(UUID actingUserId, UUID clinicId) {
        requireManage(actingUserId, clinicId);
        return save(current(clinicId).toBuilder().verifyToken(randomTokens.get()), actingUserId, clinicId,
                "VERIFY_TOKEN_REGENERATED");
    }

    @Override
    public ConnectionTestResult testConnection(UUID actingUserId, UUID clinicId) {
        requireManage(actingUserId, clinicId);
        ChannelSettings existing = current(clinicId);
        LocalDateTime now = now();

        CheckResult whatsapp = existing.whatsappPhoneNumberId() == null || existing.whatsappAccessToken() == null
                ? new CheckResult(false, "Falta el número o el token de acceso de WhatsApp.")
                : checks.checkWhatsApp(existing.whatsappPhoneNumberId(), existing.whatsappAccessToken());
        CheckResult gemini = existing.geminiApiKey() == null
                ? new CheckResult(false, "Falta la clave de Gemini.")
                : checks.checkGemini(existing.geminiApiKey(), existing.geminiModel());

        save(existing.toBuilder()
                .whatsappVerifiedAt(whatsapp.ok() ? now : null)
                .geminiVerifiedAt(gemini.ok() ? now : null), actingUserId, clinicId, "CONNECTION_TESTED");
        return new ConnectionTestResult(whatsapp.ok(), whatsapp.detail(), gemini.ok(), gemini.detail());
    }

    @Override
    public ChannelSettingsView setEnabled(UUID actingUserId, UUID clinicId, boolean enabled) {
        requireManage(actingUserId, clinicId);
        ChannelSettings existing = current(clinicId);
        if (enabled) {
            List<String> missing = missingToEnable(existing);
            if (!missing.isEmpty()) {
                throw new IllegalStateException("Para activar el asistente falta: " + String.join(", ", missing) + ".");
            }
        }
        return save(existing.toBuilder().enabled(enabled), actingUserId, clinicId, enabled ? "ENABLED" : "DISABLED");
    }

    // ---- utilidades -------------------------------------------------------------------------

    private ChannelSettingsView save(ChannelSettings.Builder updated, UUID actingUserId, UUID clinicId, String action) {
        LocalDateTime now = now();
        ChannelSettings saved = settings.save(updated.updatedBy(actingUserId).updatedAt(now).build());
        audit.record(clinicId, actingUserId, action, now);
        return view(saved, true);
    }

    private ChannelSettings current(UUID clinicId) {
        return settings.findByClinicId(clinicId).orElseGet(() -> ChannelSettings.unconfigured(clinicId));
    }

    private static ChannelSettingsView view(ChannelSettings current, boolean includeVerifyToken) {
        return new ChannelSettingsView(
                current.whatsappPhoneNumberId(),
                current.whatsappBusinessAccountId(),
                hint(current.whatsappAccessToken()),
                hint(current.whatsappAppSecret()),
                hint(current.geminiApiKey()),
                current.geminiModel(),
                current.promptMode(),
                current.customPrompt(),
                AssistantPrompt.DEFAULT,
                current.webhookKey() == null ? null : WEBHOOK_PATH_PREFIX + current.webhookKey(),
                includeVerifyToken ? current.verifyToken() : null,
                current.chatRetentionMonths(),
                current.enabled(),
                current.whatsappVerifiedAt(),
                current.geminiVerifiedAt(),
                missingToEnable(current),
                current.patientTemplateName(),
                current.doctorTemplateName(),
                current.templateLanguage());
    }

    private static List<String> missingToEnable(ChannelSettings current) {
        List<String> missing = new ArrayList<>();
        if (current.whatsappPhoneNumberId() == null) {
            missing.add("el número de WhatsApp");
        }
        if (current.whatsappAccessToken() == null) {
            missing.add("el token de acceso de WhatsApp");
        }
        if (current.whatsappAppSecret() == null) {
            missing.add("el secreto de la app de Meta");
        }
        if (current.geminiApiKey() == null) {
            missing.add("la clave de Gemini");
        }
        if (current.whatsappVerifiedAt() == null) {
            missing.add("probar la conexión de WhatsApp");
        }
        if (current.geminiVerifiedAt() == null) {
            missing.add("probar la conexión de Gemini");
        }
        return List.copyOf(missing);
    }

    private static String hint(String secret) {
        if (secret == null) {
            return null;
        }
        return "••••" + (secret.length() > 4 ? secret.substring(secret.length() - 4) : "");
    }

    private static String requireMetaId(String value, String label) {
        String trimmed = value == null ? "" : value.trim();
        if (!META_ID.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(label + " debe ser el número que muestra Meta (solo dígitos).");
        }
        return trimmed;
    }

    private static String secretOrKeep(String provided, String existing, int maxLength, String label) {
        if (isBlank(provided)) {
            return existing;
        }
        String trimmed = provided.trim();
        if (!NO_WHITESPACE.matcher(trimmed).matches() || trimmed.length() > maxLength) {
            throw new IllegalArgumentException(label + " no es válido: revisa que lo copiaste completo y sin espacios.");
        }
        return trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void requireManage(UUID actingUserId, UUID clinicId) {
        if (!can(actingUserId, clinicId, StaffPermission.MANAGE_INTEGRATIONS)) {
            throw new ClinicAccessDeniedException("Solo quien administra las integraciones puede configurar el asistente.");
        }
    }

    private boolean can(UUID actingUserId, UUID clinicId, StaffPermission permission) {
        return actingUserId != null && permissions.hasPermission(clinicId, actingUserId, permission);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
