-- Historial de WhatsApp por clinica y celular (entrega 5.5, decision D9).
--
-- body y option_labels van cifrados por la aplicacion (FieldCipher): pueden traer datos de salud o
-- nombres. El celular y la fecha quedan en claro para agrupar hilos, paginar y purgar por la
-- retencion de cada clinica (12 meses por defecto). No se particiona: como la retencion es por
-- clinica, la purga borra por clinica y no se pueden soltar particiones completas.
CREATE TABLE IF NOT EXISTS automation.chat_messages (
    id             uuid PRIMARY KEY,
    clinic_id      uuid         NOT NULL,
    phone          varchar(20)  NOT NULL,
    direction      varchar(10)  NOT NULL,
    body           text         NOT NULL,
    option_labels  text         NOT NULL,
    created_at     timestamp    NOT NULL,
    CONSTRAINT ck_chat_messages_direction CHECK (direction IN ('INBOUND', 'OUTBOUND'))
);

-- Un hilo, del mensaje mas reciente hacia atras.
CREATE INDEX IF NOT EXISTS idx_chat_messages_thread
    ON automation.chat_messages (clinic_id, phone, created_at DESC);

-- Purga por clinica y antiguedad.
CREATE INDEX IF NOT EXISTS idx_chat_messages_age
    ON automation.chat_messages (clinic_id, created_at);

-- Auditoria de lecturas: quien leyo que chat y cuando. Solo se agrega.
CREATE TABLE IF NOT EXISTS automation.chat_access_log (
    id           uuid PRIMARY KEY,
    clinic_id    uuid         NOT NULL,
    phone        varchar(20)  NOT NULL,
    user_id      uuid         NOT NULL,
    accessed_at  timestamp    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_chat_access_log_clinic
    ON automation.chat_access_log (clinic_id, accessed_at DESC);
