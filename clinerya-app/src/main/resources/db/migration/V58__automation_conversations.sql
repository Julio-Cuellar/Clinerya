-- Conversaciones de WhatsApp de la automatizacion de citas (clinerya-automation). Cada fila es la
-- conversacion de un celular con una clinica y su estado en la maquina de estados.
--
-- offered_options guarda, en JSON, las opciones que se le ofrecieron al paciente en el ultimo paso:
-- es la lista blanca contra la que se valida su siguiente mensaje (un boton o texto que no
-- corresponda a ninguna no mueve la conversacion).
--
-- El indice unico parcial impide dos conversaciones activas del mismo celular en la misma clinica,
-- aun si llegan dos mensajes a la vez.
CREATE SCHEMA IF NOT EXISTS automation;

CREATE TABLE IF NOT EXISTS automation.conversations (
    id                 uuid PRIMARY KEY,
    clinic_id          uuid         NOT NULL,
    phone              varchar(20)  NOT NULL,
    state              varchar(30)  NOT NULL,
    patient_id         uuid,
    patient_name       varchar(200),
    doctor_staff_id    uuid,
    doctor_name        varchar(200),
    request_id         uuid,
    offered_options    text         NOT NULL DEFAULT '[]',
    unrecognized_count integer      NOT NULL DEFAULT 0,
    created_at         timestamp    NOT NULL,
    last_activity_at   timestamp    NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_conversations_active_phone
    ON automation.conversations (clinic_id, phone)
    WHERE state NOT IN ('CERRADA', 'EXPIRADA');
