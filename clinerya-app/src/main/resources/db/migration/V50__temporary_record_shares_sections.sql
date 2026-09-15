-- Al generar un enlace compartido se elige qué secciones del expediente expone.
-- Se guarda como CSV de claves de SharedSection; vacío/desconocido se trata como
-- "todas" en el dominio. Los enlaces vigentes fueron borrados por V48.
ALTER TABLE records.temporary_record_shares
    ADD COLUMN shared_sections varchar(255) NOT NULL
        DEFAULT 'CLINICAL_NOTES,MEDICAL_HISTORY,STUDIES,VITAL_SIGNS,PRESCRIPTIONS';
