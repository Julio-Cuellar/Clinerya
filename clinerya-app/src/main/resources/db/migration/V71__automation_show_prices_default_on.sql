-- Plan de reglas de negocio (R1): compartir precios del catalogo viene encendido. Hasta ahora no habia
-- pantalla para cambiarlo, asi que ninguna clinica lo eligio apagado: se enciende en todas.
ALTER TABLE automation.clinic_channel_settings ALTER COLUMN show_prices SET DEFAULT true;
UPDATE automation.clinic_channel_settings SET show_prices = true;
