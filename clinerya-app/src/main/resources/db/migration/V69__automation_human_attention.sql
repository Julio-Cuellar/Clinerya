-- Atencion humana en Chats (plan agente-conversacional, fase G).

-- Mensajes que escribe el personal desde Chats: direccion STAFF y quien lo escribio.
ALTER TABLE automation.chat_messages DROP CONSTRAINT IF EXISTS ck_chat_messages_direction;
ALTER TABLE automation.chat_messages
    ADD CONSTRAINT ck_chat_messages_direction CHECK (direction IN ('INBOUND', 'OUTBOUND', 'STAFF'));
ALTER TABLE automation.chat_messages ADD COLUMN IF NOT EXISTS author_user_id uuid;

-- Bitacora: quien tomo o regreso cada chat y cuando (user_id null: el agente o el sistema).
CREATE TABLE IF NOT EXISTS automation.chat_attention_log (
    id          uuid PRIMARY KEY,
    clinic_id   uuid         NOT NULL,
    phone       varchar(20)  NOT NULL,
    action      varchar(30)  NOT NULL,
    user_id     uuid,
    at          timestamp    NOT NULL,
    CONSTRAINT ck_chat_attention_log_action
        CHECK (action IN ('REQUESTED_BY_AGENT', 'TAKEN', 'RELEASED', 'AUTO_RELEASED'))
);

CREATE INDEX IF NOT EXISTS idx_chat_attention_log_chat ON automation.chat_attention_log (clinic_id, phone, at DESC);
