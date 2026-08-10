package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.records.domain.model.Prescription;
import com.jclinical.records.domain.model.PrescriptionItem;
import com.jclinical.records.domain.ports.in.ManagePrescriptionsUseCase;
import com.jclinical.records.infra.adapters.in.web.dto.IssuePrescriptionRequest;
import com.jclinical.records.infra.adapters.in.web.dto.PrescriptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/prescriptions")
public class PrescriptionController {

    private final ManagePrescriptionsUseCase managePrescriptionsUseCase;

    public PrescriptionController(ManagePrescriptionsUseCase managePrescriptionsUseCase) {
        this.managePrescriptionsUseCase = managePrescriptionsUseCase;
    }

    @PostMapping
    public ResponseEntity<PrescriptionResponse> issuePrescription(
            @PathVariable UUID clinicId,
            @RequestBody IssuePrescriptionRequest request) {

        List<PrescriptionItem> items = request.getItems() == null ? List.of() :
                request.getItems().stream()
                        .map(i -> PrescriptionItem.builder()
                                .medicationName(i.getMedicationName())
                                .dosage(i.getDosage())
                                .frequency(i.getFrequency())
                                .duration(i.getDuration())
                                .instructions(i.getInstructions())
                                .build())
                        .collect(Collectors.toList());

        Prescription prescription = managePrescriptionsUseCase.issuePrescription(
                clinicId,
                request.getPatientId(),
                request.getDoctorId(),
                request.getAppointmentId(),
                request.getNotes(),
                items
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(PrescriptionResponse.fromDomain(prescription));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptionsByPatient(
            @PathVariable UUID clinicId,
            @PathVariable UUID patientId) {
        List<Prescription> prescriptions = managePrescriptionsUseCase.getPrescriptionsByPatient(clinicId, patientId);
        List<PrescriptionResponse> response = prescriptions.stream()
                .map(PrescriptionResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{prescriptionId}")
    public ResponseEntity<PrescriptionResponse> getPrescriptionById(
            @PathVariable UUID clinicId,
            @PathVariable UUID prescriptionId) {
        Prescription prescription = managePrescriptionsUseCase.getPrescriptionsByPatient(clinicId, prescriptionId).stream()
                .filter(p -> p.getId().equals(prescriptionId))
                .findFirst()
                .orElseGet(() -> managePrescriptionsUseCase.getPrescriptionById(clinicId, prescriptionId));
        return ResponseEntity.ok(PrescriptionResponse.fromDomain(prescription));
    }
}
