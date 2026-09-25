-- Configuracion del asistente de WhatsApp por clinica (entrega 5.1). Cada clinica conecta su propia
-- app de Meta y su clave de Gemini. Los secretos (token de acceso, secreto de la app, token de
-- verificacion del webhook y clave de Gemini) se guardan cifrados por la aplicacion (FieldCipher);
-- nunca en claro y nunca se devuelven al navegador.
CREATE TABLE IF NOT EXISTS automation.clinic_channel_settings (
    clinic_id                     uuid PRIMARY KEY,
    whatsapp_phone_number_id      varchar(30),
    whatsapp_business_account_id  varchar(30),
    whatsapp_access_token         text,
    whatsapp_app_secret           text,
    verify_token                  text,
    webhook_key                   varchar(80),
    gemini_api_key                text,
    gemini_model                  varchar(60)  NOT NULL,
    prompt_mode                   varchar(10)  NOT NULL DEFAULT 'DEFAULT',
    custom_prompt                 text,
    chat_retention_months         integer      NOT NULL DEFAULT 12,
    enabled                       boolean      NOT NULL DEFAULT false,
    whatsapp_verified_at          timestamp,
    gemini_verified_at            timestamp,
    updated_by                    uuid,
    updated_at                    timestamp,
    CONSTRAINT ck_channel_settings_prompt_mode CHECK (prompt_mode IN ('DEFAULT', 'CUSTOM')),
    CONSTRAINT ck_channel_settings_retention CHECK (chat_retention_months BETWEEN 1 AND 60),
    CONSTRAINT ck_channel_settings_prompt_length CHECK (custom_prompt IS NULL OR length(custom_prompt) <= 4000)
);

-- Un numero de WhatsApp pertenece a una sola clinica; la llave del webhook identifica a la clinica.
CREATE UNIQUE INDEX IF NOT EXISTS uq_channel_settings_phone_number
    ON automation.clinic_channel_settings (whatsapp_phone_number_id)
    WHERE whatsapp_phone_number_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_channel_settings_webhook_key
    ON automation.clinic_channel_settings (webhook_key)
    WHERE webhook_key IS NOT NULL;

-- Bitacora de cambios a la configuracion: quien y que accion, nunca el valor de un secreto.
CREATE TABLE IF NOT EXISTS automation.channel_settings_audit (
    id           uuid PRIMARY KEY,
    clinic_id    uuid         NOT NULL,
    user_id      uuid,
    action       varchar(60)  NOT NULL,
    occurred_at  timestamp    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_channel_settings_audit_clinic
    ON automation.channel_settings_audit (clinic_id, occurred_at);
