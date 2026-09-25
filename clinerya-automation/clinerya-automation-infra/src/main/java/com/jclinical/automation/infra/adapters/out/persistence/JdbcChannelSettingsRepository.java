package com.jclinical.automation.infra.adapters.out.persistence;

import com.jclinical.automation.domain.model.ChannelSettings;
import com.jclinical.automation.domain.model.PromptMode;
import com.jclinical.automation.domain.ports.out.ChannelSettingsRepositoryPort;
import com.jclinical.core.security.crypto.FieldCipher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** Configuracion del asistente por clinica. Token, secreto de la app, token de verificacion y clave de Gemini van cifrados. */
public class JdbcChannelSettingsRepository implements ChannelSettingsRepositoryPort {

    private static final String COLUMNS = """
            clinic_id, whatsapp_phone_number_id, whatsapp_business_account_id, whatsapp_access_token,
            whatsapp_app_secret, verify_token, webhook_key, gemini_api_key, gemini_model, prompt_mode,
            custom_prompt, chat_retention_months, enabled, whatsapp_verified_at, gemini_verified_at,
            updated_by, updated_at, patient_template_name, doctor_template_name, template_language
            """;

    private static final String SELECT = "SELECT " + COLUMNS + " FROM automation.clinic_channel_settings WHERE ";

    private static final String UPSERT_SQL = """
            INSERT INTO automation.clinic_channel_settings (%s)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (clinic_id) DO UPDATE SET
                whatsapp_phone_number_id = EXCLUDED.whatsapp_phone_number_id,
                whatsapp_business_account_id = EXCLUDED.whatsapp_business_account_id,
                whatsapp_access_token = EXCLUDED.whatsapp_access_token,
                whatsapp_app_secret = EXCLUDED.whatsapp_app_secret,
                verify_token = EXCLUDED.verify_token,
                webhook_key = EXCLUDED.webhook_key,
                gemini_api_key = EXCLUDED.gemini_api_key,
                gemini_model = EXCLUDED.gemini_model,
                prompt_mode = EXCLUDED.prompt_mode,
                custom_prompt = EXCLUDED.custom_prompt,
                chat_retention_months = EXCLUDED.chat_retention_months,
                enabled = EXCLUDED.enabled,
                whatsapp_verified_at = EXCLUDED.whatsapp_verified_at,
                gemini_verified_at = EXCLUDED.gemini_verified_at,
                updated_by = EXCLUDED.updated_by,
                updated_at = EXCLUDED.updated_at,
                patient_template_name = EXCLUDED.patient_template_name,
                doctor_template_name = EXCLUDED.doctor_template_name,
                template_language = EXCLUDED.template_language
            """.formatted(COLUMNS);

    private final JdbcTemplate jdbcTemplate;
    private final FieldCipher cipher;

    public JdbcChannelSettingsRepository(JdbcTemplate jdbcTemplate, FieldCipher cipher) {
        this.jdbcTemplate = jdbcTemplate;
        this.cipher = cipher;
    }

    @Override
    public Optional<ChannelSettings> findByClinicId(UUID clinicId) {
        return findOne(SELECT + "clinic_id = ?", clinicId);
    }

    @Override
    public Optional<ChannelSettings> findByPhoneNumberId(String phoneNumberId) {
        return findOne(SELECT + "whatsapp_phone_number_id = ?", phoneNumberId);
    }

    @Override
    public Optional<ChannelSettings> findByWebhookKey(String webhookKey) {
        return findOne(SELECT + "webhook_key = ?", webhookKey);
    }

    @Override
    public ChannelSettings save(ChannelSettings settings) {
        jdbcTemplate.update(UPSERT_SQL,
                settings.clinicId(), settings.whatsappPhoneNumberId(), settings.whatsappBusinessAccountId(),
                cipher.encrypt(settings.whatsappAccessToken()), cipher.encrypt(settings.whatsappAppSecret()),
                cipher.encrypt(settings.verifyToken()), settings.webhookKey(), cipher.encrypt(settings.geminiApiKey()),
                settings.geminiModel(), settings.promptMode().name(), settings.customPrompt(),
                settings.chatRetentionMonths(), settings.enabled(), timestamp(settings.whatsappVerifiedAt()),
                timestamp(settings.geminiVerifiedAt()), settings.updatedBy(), timestamp(settings.updatedAt()),
                settings.patientTemplateName(), settings.doctorTemplateName(), settings.templateLanguage());
        return settings;
    }

    private Optional<ChannelSettings> findOne(String sql, Object key) {
        return jdbcTemplate.query(sql, (row, rowNum) -> toSettings(row), key).stream().findFirst();
    }

    private ChannelSettings toSettings(ResultSet row) throws SQLException {
        return new ChannelSettings(
                row.getObject("clinic_id", UUID.class),
                row.getString("whatsapp_phone_number_id"),
                row.getString("whatsapp_business_account_id"),
                cipher.decrypt(row.getString("whatsapp_access_token")),
                cipher.decrypt(row.getString("whatsapp_app_secret")),
                cipher.decrypt(row.getString("verify_token")),
                row.getString("webhook_key"),
                cipher.decrypt(row.getString("gemini_api_key")),
                row.getString("gemini_model"),
                PromptMode.valueOf(row.getString("prompt_mode")),
                row.getString("custom_prompt"),
                row.getInt("chat_retention_months"),
                row.getBoolean("enabled"),
                localDateTime(row.getTimestamp("whatsapp_verified_at")),
                localDateTime(row.getTimestamp("gemini_verified_at")),
                row.getObject("updated_by", UUID.class),
                localDateTime(row.getTimestamp("updated_at")),
                row.getString("patient_template_name"),
                row.getString("doctor_template_name"),
                row.getString("template_language"));
    }

    private static Timestamp timestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static LocalDateTime localDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
