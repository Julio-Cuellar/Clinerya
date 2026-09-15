package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.records.domain.model.Prescription;
import com.jclinical.records.domain.model.PrescriptionItem;
import com.jclinical.records.domain.ports.in.ManagePrescriptionsUseCase;
import com.jclinical.records.domain.ports.in.ManageRecordAccessLogUseCase;
import com.jclinical.records.infra.adapters.in.web.dto.IssuePrescriptionRequest;
import com.jclinical.records.infra.adapters.in.web.dto.PrescriptionResponse;
import com.jclinical.users.infra.security.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/prescriptions")
@RequiredArgsConstructor
@Transactional
public class PrescriptionController {

    private final ManagePrescriptionsUseCase managePrescriptionsUseCase;
    private final CurrentUserResolver currentUserResolver;
    private final ManageRecordAccessLogUseCase recordAccessLogUseCase;

    @PostMapping
    public ResponseEntity<PrescriptionResponse> issuePrescription(
            @PathVariable UUID clinicId,
            @RequestBody IssuePrescriptionRequest request,
            HttpServletRequest servletRequest) {

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

        var currentUser = currentUserResolver.getCurrentUser();
        Prescription prescription = managePrescriptionsUseCase.issuePrescription(
                clinicId,
                request.getPatientId(),
                request.getDoctorId(),
                request.getAppointmentId(),
                request.getNotes(),
                items,
                currentUser.getId()
        );

        recordAccessLogUseCase.logAccess(
                clinicId,
                request.getPatientId(),
                currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()),
                "PRESCRIPTION",
                prescription.getId(),
                "WRITE",
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(PrescriptionResponse.fromDomain(prescription));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptionsByPatient(
            @PathVariable UUID clinicId,
            @PathVariable UUID patientId,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        List<Prescription> prescriptions = managePrescriptionsUseCase.getPrescriptionsByPatient(
                clinicId, patientId, currentUser.getId());

        recordAccessLogUseCase.logAccess(
                clinicId,
                patientId,
                currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()),
                "PRESCRIPTION",
                null,
                "READ",
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        List<PrescriptionResponse> response = prescriptions.stream()
                .map(PrescriptionResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{prescriptionId}")
    public ResponseEntity<PrescriptionResponse> getPrescriptionById(
            @PathVariable UUID clinicId,
            @PathVariable UUID prescriptionId,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        Prescription prescription = managePrescriptionsUseCase.getPrescriptionById(
                clinicId, prescriptionId, currentUser.getId());

        recordAccessLogUseCase.logAccess(
                clinicId,
                prescription.getPatientId(),
                currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()),
                "PRESCRIPTION",
                prescription.getId(),
                "READ",
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        return ResponseEntity.ok(PrescriptionResponse.fromDomain(prescription));
    }

    private String displayName(String fullName, String email) {
        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim();
        }
        return email;
    }
}
