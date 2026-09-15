-- Catalogo CIE-10 (subset de uso comun en consulta general/dental en Mexico) y el
-- vinculo diagnostico-nota clinica. Hoy `assessment` es texto libre sin codificar,
-- lo que bloquea reportes epidemiologicos y cualquier informe a Secretaria de Salud.

CREATE TABLE records.icd10_catalog (
    code        VARCHAR(10)  NOT NULL,
    description VARCHAR(500) NOT NULL,
    chapter     VARCHAR(255),
    billable    BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (code)
);

CREATE INDEX idx_icd10_catalog_description ON records.icd10_catalog (LOWER(description));

CREATE TABLE records.clinical_note_diagnoses (
    id                UUID         NOT NULL,
    clinical_note_id  UUID         NOT NULL,
    clinic_id         UUID         NOT NULL,
    icd10_code        VARCHAR(10)  NOT NULL,
    kind              VARCHAR(16)  NOT NULL,
    created_at        TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_note_diagnosis_note FOREIGN KEY (clinical_note_id) REFERENCES records.clinical_notes (id),
    CONSTRAINT fk_note_diagnosis_icd10 FOREIGN KEY (icd10_code) REFERENCES records.icd10_catalog (code)
);

CREATE INDEX idx_note_diagnoses_note ON records.clinical_note_diagnoses (clinical_note_id, clinic_id);
CREATE INDEX idx_note_diagnoses_code ON records.clinical_note_diagnoses (icd10_code);

INSERT INTO records.icd10_catalog (code, description, chapter, billable) VALUES
    ('A09',   'Diarrea y gastroenteritis de presunto origen infeccioso', 'Ciertas enfermedades infecciosas y parasitarias', TRUE),
    ('A90',   'Fiebre del dengue', 'Ciertas enfermedades infecciosas y parasitarias', TRUE),
    ('B34.9', 'Infeccion viral, no especificada', 'Ciertas enfermedades infecciosas y parasitarias', TRUE),
    ('E03.9', 'Hipotiroidismo, no especificado', 'Enfermedades endocrinas, nutricionales y metabolicas', TRUE),
    ('E11.9', 'Diabetes mellitus tipo 2 sin complicaciones', 'Enfermedades endocrinas, nutricionales y metabolicas', TRUE),
    ('E66.9', 'Obesidad, no especificada', 'Enfermedades endocrinas, nutricionales y metabolicas', TRUE),
    ('E78.5', 'Hiperlipidemia, no especificada', 'Enfermedades endocrinas, nutricionales y metabolicas', TRUE),
    ('F32.9', 'Episodio depresivo, no especificado', 'Trastornos mentales y del comportamiento', TRUE),
    ('F41.1', 'Trastorno de ansiedad generalizada', 'Trastornos mentales y del comportamiento', TRUE),
    ('G43.9', 'Migrana, no especificada', 'Enfermedades del sistema nervioso', TRUE),
    ('H10.9', 'Conjuntivitis, no especificada', 'Enfermedades del ojo y sus anexos', TRUE),
    ('H66.9', 'Otitis media, no especificada', 'Enfermedades del oido y de la apofisis mastoides', TRUE),
    ('I10',   'Hipertension esencial (primaria)', 'Enfermedades del sistema circulatorio', TRUE),
    ('I25.9', 'Enfermedad isquemica cronica del corazon, no especificada', 'Enfermedades del sistema circulatorio', TRUE),
    ('J00',   'Rinofaringitis aguda (resfriado comun)', 'Enfermedades del sistema respiratorio', TRUE),
    ('J02.9', 'Faringitis aguda, no especificada', 'Enfermedades del sistema respiratorio', TRUE),
    ('J03.9', 'Amigdalitis aguda, no especificada', 'Enfermedades del sistema respiratorio', TRUE),
    ('J06.9', 'Infeccion aguda de las vias respiratorias superiores, no especificada', 'Enfermedades del sistema respiratorio', TRUE),
    ('J11.1', 'Influenza con otras manifestaciones respiratorias', 'Enfermedades del sistema respiratorio', TRUE),
    ('J18.9', 'Neumonia, no especificada', 'Enfermedades del sistema respiratorio', TRUE),
    ('J20.9', 'Bronquitis aguda, no especificada', 'Enfermedades del sistema respiratorio', TRUE),
    ('J45.9', 'Asma, no especificada', 'Enfermedades del sistema respiratorio', TRUE),
    ('K02.9', 'Caries dental, no especificada', 'Enfermedades del sistema digestivo', TRUE),
    ('K04.0', 'Pulpitis', 'Enfermedades del sistema digestivo', TRUE),
    ('K04.7', 'Absceso periapical sin fistula', 'Enfermedades del sistema digestivo', TRUE),
    ('K05.0', 'Gingivitis aguda', 'Enfermedades del sistema digestivo', TRUE),
    ('K05.3', 'Periodontitis cronica', 'Enfermedades del sistema digestivo', TRUE),
    ('K08.1', 'Perdida de dientes por accidente, extraccion o enfermedad periodontal local', 'Enfermedades del sistema digestivo', TRUE),
    ('K21.9', 'Enfermedad por reflujo gastroesofagico sin esofagitis', 'Enfermedades del sistema digestivo', TRUE),
    ('K29.7', 'Gastritis, no especificada', 'Enfermedades del sistema digestivo', TRUE),
    ('K59.0', 'Estrenimiento', 'Enfermedades del sistema digestivo', TRUE),
    ('L20.9', 'Dermatitis atopica, no especificada', 'Enfermedades de la piel y del tejido subcutaneo', TRUE),
    ('L23.9', 'Dermatitis alergica de contacto, causa no especificada', 'Enfermedades de la piel y del tejido subcutaneo', TRUE),
    ('L30.9', 'Dermatitis, no especificada', 'Enfermedades de la piel y del tejido subcutaneo', TRUE),
    ('M25.5', 'Dolor en articulacion', 'Enfermedades del sistema osteomuscular y del tejido conjuntivo', TRUE),
    ('M26.6', 'Trastornos de la articulacion temporomandibular', 'Enfermedades del sistema osteomuscular y del tejido conjuntivo', TRUE),
    ('M54.5', 'Lumbago no especificado', 'Enfermedades del sistema osteomuscular y del tejido conjuntivo', TRUE),
    ('M79.1', 'Mialgia', 'Enfermedades del sistema osteomuscular y del tejido conjuntivo', TRUE),
    ('N39.0', 'Infeccion de vias urinarias, sitio no especificado', 'Enfermedades del sistema genitourinario', TRUE),
    ('O26.9', 'Complicacion relacionada con el embarazo, no especificada', 'Embarazo, parto y puerperio', TRUE),
    ('R05',   'Tos', 'Sintomas, signos y hallazgos anormales', TRUE),
    ('R06.0', 'Disnea', 'Sintomas, signos y hallazgos anormales', TRUE),
    ('R10.4', 'Dolor abdominal, otro y no especificado', 'Sintomas, signos y hallazgos anormales', TRUE),
    ('R11',   'Nausea y vomito', 'Sintomas, signos y hallazgos anormales', TRUE),
    ('R50.9', 'Fiebre, no especificada', 'Sintomas, signos y hallazgos anormales', TRUE),
    ('R51',   'Cefalea', 'Sintomas, signos y hallazgos anormales', TRUE),
    ('R52',   'Dolor, no clasificado en otra parte', 'Sintomas, signos y hallazgos anormales', TRUE),
    ('S00.9', 'Traumatismo superficial de la cabeza, parte no especificada', 'Traumatismos, envenenamientos y otras consecuencias de causas externas', TRUE),
    ('T14.9', 'Traumatismo, no especificado', 'Traumatismos, envenenamientos y otras consecuencias de causas externas', TRUE),
    ('Z00.0', 'Examen medico general', 'Factores que influyen en el estado de salud y contacto con los servicios de salud', TRUE),
    ('Z01.2', 'Examen dental', 'Factores que influyen en el estado de salud y contacto con los servicios de salud', TRUE),
    ('Z23',   'Necesidad de inmunizacion contra enfermedad bacteriana unica', 'Factores que influyen en el estado de salud y contacto con los servicios de salud', TRUE),
    ('Z34.9', 'Supervision de embarazo normal, no especificado', 'Factores que influyen en el estado de salud y contacto con los servicios de salud', TRUE);
