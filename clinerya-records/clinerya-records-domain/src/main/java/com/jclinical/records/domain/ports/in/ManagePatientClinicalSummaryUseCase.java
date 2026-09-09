package com.jclinical.records.domain.ports.in;

import com.jclinical.records.domain.model.AllergyCategory;
import com.jclinical.records.domain.model.AllergySeverity;
import com.jclinical.records.domain.model.ClinicalReviewKind;
import com.jclinical.records.domain.model.ConditionStatus;
import com.jclinical.records.domain.model.PatientAllergy;
import com.jclinical.records.domain.model.PatientCondition;
import com.jclinical.records.domain.model.PatientMedication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resumen clínico consultable del paciente: tipo de sangre + alergias,
 * padecimientos y medicación activa como datos tipados (no texto libre).
 * Todo escribe/lee bajo el mismo modelo de acceso que el resto del expediente.
 */
public interface ManagePatientClinicalSummaryUseCase {

    ClinicalSummary getSummary(UUID patientId, UUID clinicId, UUID requestingUserId);

    /**
     * Marca (o desmarca) el estado "sin ... reportados / preguntadas y negadas"
     * para un tipo de dato clínico del paciente.
     */
    void setClinicalReview(UUID patientId, UUID clinicId, UUID requestingUserId, String requestingUserName,
                           ClinicalReviewKind kind, boolean noneReported);

    PatientAllergy addAllergy(UUID patientId, UUID clinicId, UUID requestingUserId, String requestingUserName, AllergyInput input);

    PatientAllergy updateAllergy(UUID allergyId, UUID patientId, UUID clinicId, UUID requestingUserId, String requestingUserName, AllergyInput input);

    void removeAllergy(UUID allergyId, UUID patientId, UUID clinicId, UUID requestingUserId);

    PatientCondition addCondition(UUID patientId, UUID clinicId, UUID requestingUserId, String requestingUserName, ConditionInput input);

    PatientCondition updateCondition(UUID conditionId, UUID patientId, UUID clinicId, UUID requestingUserId, String requestingUserName, ConditionInput input);

    void removeCondition(UUID conditionId, UUID patientId, UUID clinicId, UUID requestingUserId);

    PatientMedication addMedication(UUID patientId, UUID clinicId, UUID requestingUserId, String requestingUserName, MedicationInput input);

    PatientMedication updateMedication(UUID medicationId, UUID patientId, UUID clinicId, UUID requestingUserId, String requestingUserName, MedicationInput input);

    void removeMedication(UUID medicationId, UUID patientId, UUID clinicId, UUID requestingUserId);

    record ClinicalSummary(
            UUID patientId,
            String bloodType,
            List<PatientAllergy> allergies,
            List<PatientCondition> conditions,
            List<PatientMedication> activeMedications,
            LocalDateTime lastNoteAt,
            ReviewStatus allergiesReview,
            ReviewStatus conditionsReview,
            ReviewStatus medicationsReview
    ) {}

    /** Estado de revisión de un tipo de dato clínico. */
    record ReviewStatus(
            boolean noneReported,
            String reviewedByUserName,
            LocalDateTime reviewedAt
    ) {
        public static ReviewStatus empty() {
            return new ReviewStatus(false, null, null);
        }
    }

    record AllergyInput(
            String substance,
            String reaction,
            AllergySeverity severity,
            AllergyCategory category
    ) {}

    record ConditionInput(
            String name,
            String icd10Code,
            ConditionStatus status,
            LocalDate onsetDate
    ) {}

    record MedicationInput(
            String medicationName,
            String dose,
            String schedule,
            boolean active,
            LocalDate startedOn,
            LocalDate stoppedOn,
            UUID prescriptionId
    ) {}
}
