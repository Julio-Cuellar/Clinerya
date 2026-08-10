ALTER TABLE core.clinics
    ADD COLUMN IF NOT EXISTS cofepris_permit_number varchar(255),
    ADD COLUMN IF NOT EXISTS responsible_doctor_name varchar(255),
    ADD COLUMN IF NOT EXISTS responsible_doctor_professional_license varchar(255);
