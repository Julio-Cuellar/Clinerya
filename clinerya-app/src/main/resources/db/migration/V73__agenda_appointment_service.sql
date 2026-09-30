-- Plan v2, S2: la cita puede llevar un servicio del catalogo. Nombre, tipo de precio y precio se copian
-- al agendar: si el catalogo cambia despues, la cita (y el ingreso estimado) no cambian. Precio solo si es fijo.
ALTER TABLE agenda.appointments
    ADD COLUMN IF NOT EXISTS service_id uuid,
    ADD COLUMN IF NOT EXISTS service_name varchar(200),
    ADD COLUMN IF NOT EXISTS service_pricing varchar(30),
    ADD COLUMN IF NOT EXISTS service_price numeric(12, 2);

ALTER TABLE agenda.appointments DROP CONSTRAINT IF EXISTS ck_appointments_service_pricing;
ALTER TABLE agenda.appointments
    ADD CONSTRAINT ck_appointments_service_pricing
        CHECK (service_pricing IS NULL OR service_pricing IN ('FIXED', 'VARIES_BY_PATIENT'));
