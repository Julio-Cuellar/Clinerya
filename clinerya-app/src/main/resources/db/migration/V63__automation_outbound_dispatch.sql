-- Envio por la API de WhatsApp Cloud (entrega 5.4).
--
-- Plantillas aprobadas por clinica: WhatsApp solo permite escribir fuera de la ventana de 24 h desde
-- el ultimo mensaje del numero con una plantilla aprobada en Meta.
ALTER TABLE automation.clinic_channel_settings
    ADD COLUMN IF NOT EXISTS patient_template_name varchar(512),
    ADD COLUMN IF NOT EXISTS doctor_template_name  varchar(512),
    ADD COLUMN IF NOT EXISTS template_language     varchar(10) NOT NULL DEFAULT 'es_MX';

-- Cola de salida: destinatario (paciente o medico), parametros de plantilla, reintentos con espera
-- creciente, id que devuelve Meta y estado de entrega que llega por el webhook.
ALTER TABLE automation.outbound_messages
    ADD COLUMN IF NOT EXISTS audience            varchar(10) NOT NULL DEFAULT 'PATIENT',
    ADD COLUMN IF NOT EXISTS template_parameters text        NOT NULL DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS attempts            integer     NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS next_attempt_at     timestamp,
    ADD COLUMN IF NOT EXISTS wa_message_id       varchar(200),
    ADD COLUMN IF NOT EXISTS via_template        boolean     NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS last_error          text,
    ADD COLUMN IF NOT EXISTS failed_at           timestamp,
    ADD COLUMN IF NOT EXISTS delivery_status     varchar(20);

ALTER TABLE automation.outbound_messages
    ADD CONSTRAINT ck_outbound_messages_audience CHECK (audience IN ('PATIENT', 'DOCTOR'));

DROP INDEX IF EXISTS automation.idx_outbound_messages_pending;

-- Siguiente mensaje que toca enviar, y "hay uno anterior pendiente para este celular".
CREATE INDEX IF NOT EXISTS idx_outbound_messages_due
    ON automation.outbound_messages (created_at)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_outbound_messages_pending_phone
    ON automation.outbound_messages (clinic_id, phone, created_at)
    WHERE status = 'PENDING';

CREATE UNIQUE INDEX IF NOT EXISTS uq_outbound_messages_wa_id
    ON automation.outbound_messages (wa_message_id)
    WHERE wa_message_id IS NOT NULL;

-- Remitente de cada mensaje recibido: su ultimo mensaje abre la ventana de 24 h.
ALTER TABLE automation.inbound_message_ids
    ADD COLUMN IF NOT EXISTS from_phone varchar(20);

CREATE INDEX IF NOT EXISTS idx_inbound_message_ids_sender
    ON automation.inbound_message_ids (clinic_id, from_phone, received_at);
