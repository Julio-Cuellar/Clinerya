-- Perfil de especialidad por clínica. Controla presentación (vocabulario del módulo de
-- tratamientos, visibilidad del localizador dental, catálogo sugerido y plantilla de historia
-- clínica recomendada); ningún modelo de negocio cambia de forma según este valor.
--
-- Las clínicas existentes se migran a ODONTOLOGIA, no a un valor genérico: hoy todos los
-- usuarios en producción son odontólogos, y dejarlas sin perfil les quitaría el campo de diente
-- de la interfaz hasta que alguien entrara a configurar la clínica.
--
-- Deliberadamente SIN DEFAULT: con un default toda clínica nueva nacería con una especialidad
-- implícita y el paso de selección del onboarding dejaría de significar algo. Las clínicas
-- nuevas nacen en SIN_CONFIGURAR, que la aplicación escribe de forma explícita.
ALTER TABLE clinics.clinics
    ADD COLUMN IF NOT EXISTS specialty varchar(40);

UPDATE clinics.clinics
SET specialty = 'ODONTOLOGIA'
WHERE specialty IS NULL;

ALTER TABLE clinics.clinics
    ALTER COLUMN specialty SET NOT NULL;
