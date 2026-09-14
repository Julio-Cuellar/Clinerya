-- El seed manual de V52 incluia "M26.6" para "trastornos de la articulacion
-- temporomandibular", pero ese es el codigo de ICD-10-CM (EEUU); el catalogo
-- oficial de la CIE-10 mexicana (DGIS, CAT_DIAGNOSTICOS) usa K07.6 para lo
-- mismo -- ya cargado por Icd10CatalogLoader junto con el resto del catalogo
-- oficial. Este codigo nunca se expuso en produccion, asi que es seguro
-- borrarlo directamente (el DELETE sobre clinical_note_diagnoses es solo
-- defensivo, no deberia haber filas).
DELETE FROM records.clinical_note_diagnoses WHERE icd10_code = 'M26.6';
DELETE FROM records.icd10_catalog WHERE code = 'M26.6';
