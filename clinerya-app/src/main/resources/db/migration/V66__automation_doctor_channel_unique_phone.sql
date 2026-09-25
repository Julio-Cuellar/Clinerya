-- Un celular activo identifica a un solo medico de la clinica (asi se reconoce cuando escribe).
-- El servicio ya lo valida; el indice unico lo garantiza aun con dos altas simultaneas.
DROP INDEX IF EXISTS automation.idx_doctor_channels_active_phone;

CREATE UNIQUE INDEX IF NOT EXISTS uq_doctor_channels_active_phone
    ON automation.doctor_channels (clinic_id, phone)
    WHERE active;
