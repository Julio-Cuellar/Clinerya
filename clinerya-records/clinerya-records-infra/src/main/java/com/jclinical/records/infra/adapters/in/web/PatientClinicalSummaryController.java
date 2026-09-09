package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.records.domain.model.ClinicalReviewKind;
import com.jclinical.records.domain.ports.in.ManagePatientClinicalSummaryUseCase;
import com.jclinical.records.domain.ports.in.ManagePatientClinicalSummaryUseCase.AllergyInput;
import com.jclinical.records.domain.ports.in.ManagePatientClinicalSummaryUseCase.ConditionInput;
import com.jclinical.records.domain.ports.in.ManagePatientClinicalSummaryUseCase.MedicationInput;
import com.jclinical.records.domain.ports.in.ManageRecordAccessLogUseCase;
import com.jclinical.records.infra.adapters.in.web.dto.PatientClinicalSummaryResponse;
import com.jclinical.users.infra.security.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/clinical-summary")
@RequiredArgsConstructor
@Transactional
public class PatientClinicalSummaryController {

    private final ManagePatientClinicalSummaryUseCase summaryUseCase;
    private final CurrentUserResolver currentUserResolver;
    private final ManageRecordAccessLogUseCase recordAccessLogUseCase;

    // ---- Resumen -------------------------------------------------------------

    @GetMapping
    public ResponseEntity<PatientClinicalSummaryResponse> getSummary(
            @PathVariable UUID patientId,
            @RequestParam UUID clinicId,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        var summary = summaryUseCase.getSummary(patientId, clinicId, currentUser.getId());
        log(clinicId, patientId, "CLINICAL_SUMMARY", null, "READ", servletRequest);
        return ResponseEntity.ok(PatientClinicalSummaryResponse.from(summary));
    }

    // ---- Estado de revisión ("sin ... reportados") ---------------------------

    public record ReviewRequest(boolean noneReported) {}

    private ResponseEntity<Void> setReview(UUID patientId, UUID clinicId, ClinicalReviewKind kind,
                                           ReviewRequest request, HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        summaryUseCase.setClinicalReview(patientId, clinicId, currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()), kind, request.noneReported());
        log(clinicId, patientId, "PATIENT_CLINICAL_REVIEW", null, "WRITE", servletRequest);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/allergies/review")
    public ResponseEntity<Void> setAllergyReview(
            @PathVariable UUID patientId, @RequestParam UUID clinicId,
            @RequestBody ReviewRequest request, HttpServletRequest servletRequest) {
        return setReview(patientId, clinicId, ClinicalReviewKind.ALLERGIES, request, servletRequest);
    }

    @PutMapping("/conditions/review")
    public ResponseEntity<Void> setConditionReview(
            @PathVariable UUID patientId, @RequestParam UUID clinicId,
            @RequestBody ReviewRequest request, HttpServletRequest servletRequest) {
        return setReview(patientId, clinicId, ClinicalReviewKind.CONDITIONS, request, servletRequest);
    }

    @PutMapping("/medications/review")
    public ResponseEntity<Void> setMedicationReview(
            @PathVariable UUID patientId, @RequestParam UUID clinicId,
            @RequestBody ReviewRequest request, HttpServletRequest servletRequest) {
        return setReview(patientId, clinicId, ClinicalReviewKind.MEDICATIONS, request, servletRequest);
    }

    // ---- Alergias ----------------------------------------------------------

    @PostMapping("/allergies")
    public ResponseEntity<PatientClinicalSummaryResponse.AllergyDto> addAllergy(
            @PathVariable UUID patientId,
            @RequestParam UUID clinicId,
            @RequestBody AllergyInput input,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        var saved = summaryUseCase.addAllergy(patientId, clinicId, currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()), input);
        log(clinicId, patientId, "PATIENT_ALLERGY", saved.getId(), "WRITE", servletRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(PatientClinicalSummaryResponse.allergy(saved));
    }

    @PutMapping("/allergies/{allergyId}")
    public ResponseEntity<PatientClinicalSummaryResponse.AllergyDto> updateAllergy(
            @PathVariable UUID patientId,
            @PathVariable UUID allergyId,
            @RequestParam UUID clinicId,
            @RequestBody AllergyInput input,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        var saved = summaryUseCase.updateAllergy(allergyId, patientId, clinicId, currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()), input);
        log(clinicId, patientId, "PATIENT_ALLERGY", saved.getId(), "WRITE", servletRequest);
        return ResponseEntity.ok(PatientClinicalSummaryResponse.allergy(saved));
    }

    @DeleteMapping("/allergies/{allergyId}")
    public ResponseEntity<Void> removeAllergy(
            @PathVariable UUID patientId,
            @PathVariable UUID allergyId,
            @RequestParam UUID clinicId,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        summaryUseCase.removeAllergy(allergyId, patientId, clinicId, currentUser.getId());
        log(clinicId, patientId, "PATIENT_ALLERGY", allergyId, "WRITE", servletRequest);
        return ResponseEntity.noContent().build();
    }

    // ---- Padecimientos -------------------------------------------------------

    @PostMapping("/conditions")
    public ResponseEntity<PatientClinicalSummaryResponse.ConditionDto> addCondition(
            @PathVariable UUID patientId,
            @RequestParam UUID clinicId,
            @RequestBody ConditionInput input,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        var saved = summaryUseCase.addCondition(patientId, clinicId, currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()), input);
        log(clinicId, patientId, "PATIENT_CONDITION", saved.getId(), "WRITE", servletRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(PatientClinicalSummaryResponse.condition(saved));
    }

    @PutMapping("/conditions/{conditionId}")
    public ResponseEntity<PatientClinicalSummaryResponse.ConditionDto> updateCondition(
            @PathVariable UUID patientId,
            @PathVariable UUID conditionId,
            @RequestParam UUID clinicId,
            @RequestBody ConditionInput input,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        var saved = summaryUseCase.updateCondition(conditionId, patientId, clinicId, currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()), input);
        log(clinicId, patientId, "PATIENT_CONDITION", saved.getId(), "WRITE", servletRequest);
        return ResponseEntity.ok(PatientClinicalSummaryResponse.condition(saved));
    }

    @DeleteMapping("/conditions/{conditionId}")
    public ResponseEntity<Void> removeCondition(
            @PathVariable UUID patientId,
            @PathVariable UUID conditionId,
            @RequestParam UUID clinicId,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        summaryUseCase.removeCondition(conditionId, patientId, clinicId, currentUser.getId());
        log(clinicId, patientId, "PATIENT_CONDITION", conditionId, "WRITE", servletRequest);
        return ResponseEntity.noContent().build();
    }

    // ---- Medicacion --------------------------------------------------------

    @PostMapping("/medications")
    public ResponseEntity<PatientClinicalSummaryResponse.MedicationDto> addMedication(
            @PathVariable UUID patientId,
            @RequestParam UUID clinicId,
            @RequestBody MedicationInput input,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        var saved = summaryUseCase.addMedication(patientId, clinicId, currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()), input);
        log(clinicId, patientId, "PATIENT_MEDICATION", saved.getId(), "WRITE", servletRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(PatientClinicalSummaryResponse.medication(saved));
    }

    @PutMapping("/medications/{medicationId}")
    public ResponseEntity<PatientClinicalSummaryResponse.MedicationDto> updateMedication(
            @PathVariable UUID patientId,
            @PathVariable UUID medicationId,
            @RequestParam UUID clinicId,
            @RequestBody MedicationInput input,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        var saved = summaryUseCase.updateMedication(medicationId, patientId, clinicId, currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()), input);
        log(clinicId, patientId, "PATIENT_MEDICATION", saved.getId(), "WRITE", servletRequest);
        return ResponseEntity.ok(PatientClinicalSummaryResponse.medication(saved));
    }

    @DeleteMapping("/medications/{medicationId}")
    public ResponseEntity<Void> removeMedication(
            @PathVariable UUID patientId,
            @PathVariable UUID medicationId,
            @RequestParam UUID clinicId,
            HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        summaryUseCase.removeMedication(medicationId, patientId, clinicId, currentUser.getId());
        log(clinicId, patientId, "PATIENT_MEDICATION", medicationId, "WRITE", servletRequest);
        return ResponseEntity.noContent().build();
    }

    // ---- helpers ---------------------------------------------------------------

    private void log(UUID clinicId, UUID patientId, String resourceType, UUID resourceId, String action,
                     HttpServletRequest servletRequest) {
        var currentUser = currentUserResolver.getCurrentUser();
        recordAccessLogUseCase.logAccess(
                clinicId,
                patientId,
                currentUser.getId(),
                displayName(currentUser.getFullName(), currentUser.getEmail()),
                resourceType,
                resourceId,
                action,
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );
    }

    private String displayName(String fullName, String email) {
        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim();
        }
        return email;
    }
}
