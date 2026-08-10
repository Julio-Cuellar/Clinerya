CREATE TABLE IF NOT EXISTS agenda.appointment_quotation_items (
    appointment_id uuid NOT NULL,
    quotation_item_id uuid NOT NULL,
    CONSTRAINT pk_appointment_quotation_items PRIMARY KEY (appointment_id, quotation_item_id),
    CONSTRAINT fk_appointment_quotation_items_appointment
        FOREIGN KEY (appointment_id) REFERENCES agenda.appointments (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_appointment_quotation_items_item
    ON agenda.appointment_quotation_items (quotation_item_id);

INSERT INTO agenda.appointment_quotation_items (appointment_id, quotation_item_id)
SELECT id, quotation_item_id
  FROM agenda.appointments
 WHERE quotation_item_id IS NOT NULL
ON CONFLICT (appointment_id, quotation_item_id) DO NOTHING;
