package com.jclinical.automation.domain.ports.in;

import com.jclinical.automation.domain.model.PromptMode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Configuracion del asistente de WhatsApp de una clinica (Ajustes > Clinica e Integraciones).
 * Cambiarla exige MANAGE_INTEGRATIONS; verla, VIEW_CLINIC_SETTINGS. Los secretos nunca se devuelven.
 */
public interface ManageChannelSettingsUseCase {

    ChannelSettingsView getSettings(UUID actingUserId, UUID clinicId);

    /** Un secreto en blanco conserva el que ya estaba guardado. */
    ChannelSettingsView updateWhatsApp(UUID actingUserId, UUID clinicId, WhatsAppCredentials credentials);

    /** Una clave en blanco conserva la que ya estaba guardada; un modelo en blanco usa el predeterminado. */
    ChannelSettingsView updateGemini(UUID actingUserId, UUID clinicId, GeminiCredentials credentials);

    ChannelSettingsView updateAssistant(UUID actingUserId, UUID clinicId, AssistantPreferences preferences);

    ChannelSettingsView removeSecret(UUID actingUserId, UUID clinicId, SecretKind kind);

    /** Plantillas aprobadas en Meta para escribir fuera de la ventana de 24 h (en blanco: sin plantilla). */
    ChannelSettingsView updateTemplates(UUID actingUserId, UUID clinicId, TemplateSettings templates);

    ChannelSettingsView regenerateVerifyToken(UUID actingUserId, UUID clinicId);

    ConnectionTestResult testConnection(UUID actingUserId, UUID clinicId);

    /** Activar exige WhatsApp y Gemini configurados y probados (sin clave de Gemini no hay asistente). */
    ChannelSettingsView setEnabled(UUID actingUserId, UUID clinicId, boolean enabled);

    record WhatsAppCredentials(String phoneNumberId, String businessAccountId, String accessToken, String appSecret) {}

    record GeminiCredentials(String apiKey, String model) {}

    record AssistantPreferences(PromptMode promptMode, String customPrompt, int chatRetentionMonths) {}

    record TemplateSettings(String patientTemplateName, String doctorTemplateName, String languageCode) {}

    enum SecretKind {
        WHATSAPP_ACCESS_TOKEN,
        WHATSAPP_APP_SECRET,
        GEMINI_API_KEY
    }

    record ConnectionTestResult(boolean whatsappOk, String whatsappDetail, boolean geminiOk, String geminiDetail) {}

    /**
     * Lo que ve la pantalla. Los secretos aparecen solo como pista ("••••1234"); el token de
     * verificacion solo se incluye para quien administra integraciones (debe copiarlo en Meta).
     */
    record ChannelSettingsView(
            String whatsappPhoneNumberId,
            String whatsappBusinessAccountId,
            String accessTokenHint,
            String appSecretHint,
            String geminiApiKeyHint,
            String geminiModel,
            PromptMode promptMode,
            String customPrompt,
            String defaultPrompt,
            String webhookPath,
            String verifyToken,
            int chatRetentionMonths,
            boolean enabled,
            LocalDateTime whatsappVerifiedAt,
            LocalDateTime geminiVerifiedAt,
            List<String> missingToEnable,
            String patientTemplateName,
            String doctorTemplateName,
            String templateLanguage
    ) {}
}
