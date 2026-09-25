-- Ids de mensajes de WhatsApp ya recibidos por el webhook (entrega 5.3). Meta reintenta la entrega
-- si no recibe 200 a tiempo; la llave primaria garantiza que cada mensaje se procese una sola vez.
CREATE TABLE IF NOT EXISTS automation.inbound_message_ids (
    wa_message_id  varchar(200) PRIMARY KEY,
    clinic_id      uuid         NOT NULL,
    received_at    timestamp    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_inbound_message_ids_received
    ON automation.inbound_message_ids (received_at);
