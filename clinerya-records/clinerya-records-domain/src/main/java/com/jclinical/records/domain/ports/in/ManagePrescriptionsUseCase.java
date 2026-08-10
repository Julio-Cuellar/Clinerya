package com.jclinical.records.domain.ports.in;

import com.jclinical.records.domain.model.Prescription;
import com.jclinical.records.domain.model.PrescriptionItem;

import java.util.List;
import java.util.UUID;

public interface ManagePrescriptionsUseCase {
    Prescription issuePrescription(
            UUID clinicId,
            UUID patientId,
            UUID doctorId,
            UUID appointmentId,
            String notes,
            List<PrescriptionItem> items
    );

    List<Prescription> getPrescriptionsByPatient(UUID clinicId, UUID patientId);

    Prescription getPrescriptionById(UUID clinicId, UUID prescriptionId);
}
