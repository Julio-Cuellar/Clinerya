-- Ficha del contacto en Chats: el nombre que cada numero puso en su perfil de WhatsApp (Meta lo manda
-- con cada mensaje). Es un dato personal: va cifrado (FieldCipher).
CREATE TABLE IF NOT EXISTS automation.chat_contacts (
    clinic_id     uuid         NOT NULL,
    phone         varchar(20)  NOT NULL,
    profile_name  text,
    updated_at    timestamp    NOT NULL,
    PRIMARY KEY (clinic_id, phone)
);
