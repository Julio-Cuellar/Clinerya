CREATE TABLE IF NOT EXISTS core.prescriptions (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    doctor_id uuid,
    appointment_id uuid,
    notes TEXT,
    status varchar(32) NOT NULL DEFAULT 'ISSUED',
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS core.prescription_items (
    id uuid NOT NULL,
    prescription_id uuid NOT NULL,
    medication_name varchar(255) NOT NULL,
    dosage varchar(128),
    frequency varchar(128),
    duration varchar(128),
    instructions TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_prescription_items_prescription FOREIGN KEY (prescription_id) REFERENCES core.prescriptions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_prescriptions_clinic_patient ON core.prescriptions(clinic_id, patient_id);
