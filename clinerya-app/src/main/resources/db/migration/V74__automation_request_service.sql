-- Plan v2, S3: la accion pendiente y la solicitud al medico llevan el servicio del catalogo, para que la
-- cita que se agende al aprobarla copie su nombre, tipo y precio.
ALTER TABLE automation.agent_pending_actions ADD COLUMN IF NOT EXISTS service_id uuid;
ALTER TABLE automation.appointment_requests ADD COLUMN IF NOT EXISTS service_id uuid;
