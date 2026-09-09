package com.jclinical.records.domain.service;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.records.domain.model.Prescription;
import com.jclinical.records.domain.model.PrescriptionItem;
import com.jclinical.records.domain.model.PrescriptionStatus;
import com.jclinical.records.domain.ports.in.ManagePrescriptionsUseCase;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort.AccessDecision;
import com.jclinical.records.domain.ports.out.PatientAccessAuthorizationPort.AccessLevel;
import com.jclinical.records.domain.ports.out.PatientValidatorPort;
import com.jclinical.records.domain.ports.out.PrescriptionRepositoryPort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PrescriptionService implements ManagePrescriptionsUseCase {

    private final PrescriptionRepositoryPort prescriptionRepositoryPort;
    private final PatientValidatorPort patientValidator;
    private final PatientAccessAuthorizationPort accessAuthorizationPort;

    public PrescriptionService(PrescriptionRepositoryPort prescriptionRepositoryPort,
                               PatientValidatorPort patientValidator,
                               PatientAccessAuthorizationPort accessAuthorizationPort) {
        this.prescriptionRepositoryPort = prescriptionRepositoryPort;
        this.patientValidator = patientValidator;
        this.accessAuthorizationPort = accessAuthorizationPort;
    }

    @Override
    public Prescription issuePrescription(
            UUID clinicId,
            UUID patientId,
            UUID doctorId,
            UUID appointmentId,
            String notes,
            List<PrescriptionItem> items,
            UUID requestingUserId
    ) {
        if (clinicId == null || patientId == null) {
            throw new IllegalArgumentException("La clínica y el paciente son obligatorios para emitir una receta.");
        }
        authorize(requestingUserId, patientId, clinicId, true);
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
    public List<Prescription> getPrescriptionsByPatient(UUID clinicId, UUID patientId, UUID requestingUserId) {
        authorize(requestingUserId, patientId, clinicId, false);
        return prescriptionRepositoryPort.findByClinicIdAndPatientId(clinicId, patientId);
    }

    @Override
    public Prescription getPrescriptionById(UUID clinicId, UUID prescriptionId, UUID requestingUserId) {
        Prescription prescription = prescriptionRepositoryPort.findById(clinicId, prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Receta no encontrada."));
        authorize(requestingUserId, prescription.getPatientId(), clinicId, false);
        return prescription;
    }

    private void authorize(UUID requestingUserId, UUID patientId, UUID clinicId, boolean requireWrite) {
        if (!patientValidator.existsByIdAndClinicId(patientId, clinicId)) {
            throw new IllegalArgumentException("El paciente no existe en esta clínica.");
        }
        AccessDecision decision = accessAuthorizationPort.resolveAccess(requestingUserId, clinicId, patientId);
        if (decision.level() == AccessLevel.NONE || (requireWrite && decision.level() != AccessLevel.READ_WRITE)) {
            throw new ClinicAccessDeniedException("No tienes acceso a este expediente.");
        }
    }
}
