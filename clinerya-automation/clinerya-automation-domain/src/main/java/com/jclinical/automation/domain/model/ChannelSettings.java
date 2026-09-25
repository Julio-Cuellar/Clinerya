package com.jclinical.automation.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Configuracion del asistente de una clinica: su numero de WhatsApp (app propia de Meta), su clave
 * de Gemini y su prompt. Los secretos viajan en claro solo dentro del proceso; se guardan cifrados
 * y nunca se devuelven al navegador.
 */
public record ChannelSettings(
        UUID clinicId,
        String whatsappPhoneNumberId,
        String whatsappBusinessAccountId,
        String whatsappAccessToken,
        String whatsappAppSecret,
        String verifyToken,
        String webhookKey,
        String geminiApiKey,
        String geminiModel,
        PromptMode promptMode,
        String customPrompt,
        int chatRetentionMonths,
        boolean enabled,
        LocalDateTime whatsappVerifiedAt,
        LocalDateTime geminiVerifiedAt,
        UUID updatedBy,
        LocalDateTime updatedAt
) {

    public static final String DEFAULT_GEMINI_MODEL = "gemini-2.5-flash";
    public static final int DEFAULT_CHAT_RETENTION_MONTHS = 12;

    public static ChannelSettings unconfigured(UUID clinicId) {
        return new ChannelSettings(clinicId, null, null, null, null, null, null, null, DEFAULT_GEMINI_MODEL,
                PromptMode.DEFAULT, null, DEFAULT_CHAT_RETENTION_MONTHS, false, null, null, null, null);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private UUID clinicId;
        private String whatsappPhoneNumberId;
        private String whatsappBusinessAccountId;
        private String whatsappAccessToken;
        private String whatsappAppSecret;
        private String verifyToken;
        private String webhookKey;
        private String geminiApiKey;
        private String geminiModel;
        private PromptMode promptMode;
        private String customPrompt;
        private int chatRetentionMonths;
        private boolean enabled;
        private LocalDateTime whatsappVerifiedAt;
        private LocalDateTime geminiVerifiedAt;
        private UUID updatedBy;
        private LocalDateTime updatedAt;

        private Builder(ChannelSettings source) {
            clinicId = source.clinicId;
            whatsappPhoneNumberId = source.whatsappPhoneNumberId;
            whatsappBusinessAccountId = source.whatsappBusinessAccountId;
            whatsappAccessToken = source.whatsappAccessToken;
            whatsappAppSecret = source.whatsappAppSecret;
            verifyToken = source.verifyToken;
            webhookKey = source.webhookKey;
            geminiApiKey = source.geminiApiKey;
            geminiModel = source.geminiModel;
            promptMode = source.promptMode;
            customPrompt = source.customPrompt;
            chatRetentionMonths = source.chatRetentionMonths;
            enabled = source.enabled;
            whatsappVerifiedAt = source.whatsappVerifiedAt;
            geminiVerifiedAt = source.geminiVerifiedAt;
            updatedBy = source.updatedBy;
            updatedAt = source.updatedAt;
        }

        public Builder whatsappPhoneNumberId(String value) { whatsappPhoneNumberId = value; return this; }
        public Builder whatsappBusinessAccountId(String value) { whatsappBusinessAccountId = value; return this; }
        public Builder whatsappAccessToken(String value) { whatsappAccessToken = value; return this; }
        public Builder whatsappAppSecret(String value) { whatsappAppSecret = value; return this; }
        public Builder verifyToken(String value) { verifyToken = value; return this; }
        public Builder webhookKey(String value) { webhookKey = value; return this; }
        public Builder geminiApiKey(String value) { geminiApiKey = value; return this; }
        public Builder geminiModel(String value) { geminiModel = value; return this; }
        public Builder promptMode(PromptMode value) { promptMode = value; return this; }
        public Builder customPrompt(String value) { customPrompt = value; return this; }
        public Builder chatRetentionMonths(int value) { chatRetentionMonths = value; return this; }
        public Builder enabled(boolean value) { enabled = value; return this; }
        public Builder whatsappVerifiedAt(LocalDateTime value) { whatsappVerifiedAt = value; return this; }
        public Builder geminiVerifiedAt(LocalDateTime value) { geminiVerifiedAt = value; return this; }
        public Builder updatedBy(UUID value) { updatedBy = value; return this; }
        public Builder updatedAt(LocalDateTime value) { updatedAt = value; return this; }

        public ChannelSettings build() {
            return new ChannelSettings(clinicId, whatsappPhoneNumberId, whatsappBusinessAccountId, whatsappAccessToken,
                    whatsappAppSecret, verifyToken, webhookKey, geminiApiKey, geminiModel, promptMode, customPrompt,
                    chatRetentionMonths, enabled, whatsappVerifiedAt, geminiVerifiedAt, updatedBy, updatedAt);
        }
    }
}
