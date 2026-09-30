-- Reglas del catalogo de servicios (plan v2, S1): tipo de precio y disponibilidad en el asistente.
-- Los servicios existentes conservan su precio como precio fijo y quedan fuera del asistente hasta
-- que tengan descripcion. Un servicio de precio variable puede no tener precio de referencia.
ALTER TABLE treatments.treatment_catalog_items
    ADD COLUMN IF NOT EXISTS pricing_type varchar(30) NOT NULL DEFAULT 'FIXED',
    ADD COLUMN IF NOT EXISTS available_in_assistant boolean NOT NULL DEFAULT false,
    ALTER COLUMN default_price DROP NOT NULL;

ALTER TABLE treatments.treatment_catalog_items DROP CONSTRAINT IF EXISTS ck_catalog_items_pricing_type;
ALTER TABLE treatments.treatment_catalog_items
    ADD CONSTRAINT ck_catalog_items_pricing_type CHECK (pricing_type IN ('FIXED', 'VARIES_BY_PATIENT'));

ALTER TABLE treatments.treatment_catalog_items DROP CONSTRAINT IF EXISTS ck_catalog_items_fixed_has_price;
ALTER TABLE treatments.treatment_catalog_items
    ADD CONSTRAINT ck_catalog_items_fixed_has_price CHECK (pricing_type <> 'FIXED' OR default_price IS NOT NULL);
