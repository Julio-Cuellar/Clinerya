package com.jclinical.records.domain.service;

import com.jclinical.records.domain.model.Prescription;
import com.jclinical.records.domain.model.PrescriptionItem;
import com.jclinical.records.domain.model.PrescriptionStatus;
import com.jclinical.records.domain.ports.in.ManagePrescriptionsUseCase;
import com.jclinical.records.domain.ports.out.PrescriptionRepositoryPort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PrescriptionService implements ManagePrescriptionsUseCase {

    private final PrescriptionRepositoryPort prescriptionRepositoryPort;

    public PrescriptionService(PrescriptionRepositoryPort prescriptionRepositoryPort) {
        this.prescriptionRepositoryPort = prescriptionRepositoryPort;
    }

    @Override
    public Prescription issuePrescription(
            UUID clinicId,
            UUID patientId,
            UUID doctorId,
            UUID appointmentId,
            String notes,
            List<PrescriptionItem> items
    ) {
        if (clinicId == null || patientId == null) {
            throw new IllegalArgumentException("La clínica y el paciente son obligatorios para emitir una receta.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("La receta debe incluir al menos un medicamento.");
        }

        UUID prescriptionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        List<PrescriptionItem> preparedItems = new ArrayList<>();
        for (PrescriptionItem item : items) {
            preparedItems.add(PrescriptionItem.builder()
                    .id(UUID.randomUUID())
                    .prescriptionId(prescriptionId)
                    .medicationName(item.getMedicationName())
                    .dosage(item.getDosage())
                    .frequency(item.getFrequency())
                    .duration(item.getDuration())
                    .instructions(item.getInstructions())
                    .build());
        }

        Prescription prescription = Prescription.builder()
                .id(prescriptionId)
                .clinicId(clinicId)
                .patientId(patientId)
                .doctorId(doctorId)
                .appointmentId(appointmentId)
                .notes(notes)
                .status(PrescriptionStatus.ISSUED)
                .items(preparedItems)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return prescriptionRepositoryPort.save(prescription);
    }

    @Override
    public List<Prescription> getPrescriptionsByPatient(UUID clinicId, UUID patientId) {
        return prescriptionRepositoryPort.findByClinicIdAndPatientId(clinicId, patientId);
    }

    @Override
    public Prescription getPrescriptionById(UUID clinicId, UUID prescriptionId) {
        return prescriptionRepositoryPort.findById(clinicId, prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Receta no encontrada."));
    }
}
