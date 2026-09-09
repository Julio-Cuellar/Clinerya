package com.jclinical.records.infra.adapters.in.web.dto;

import com.jclinical.records.domain.model.PatientAllergy;
import com.jclinical.records.domain.model.PatientCondition;
import com.jclinical.records.domain.model.PatientMedication;
import com.jclinical.records.domain.ports.in.ManagePatientClinicalSummaryUseCase.ClinicalSummary;
import com.jclinical.records.domain.ports.in.ManagePatientClinicalSummaryUseCase.ReviewStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PatientClinicalSummaryResponse(
        UUID patientId,
        String bloodType,
        List<AllergyDto> allergies,
        List<ConditionDto> conditions,
        List<MedicationDto> activeMedications,
        LocalDateTime lastNoteAt,
        ReviewDto allergiesReview,
        ReviewDto conditionsReview,
        ReviewDto medicationsReview
) {

    public record ReviewDto(boolean noneReported, String reviewedByUserName, LocalDateTime reviewedAt) {
        static ReviewDto from(ReviewStatus status) {
            return new ReviewDto(status.noneReported(), status.reviewedByUserName(), status.reviewedAt());
        }
    }

    public record AllergyDto(
            UUID id,
            String substance,
            String reaction,
            String severity,
            String category,
            String source,
            String notedByUserName,
            LocalDateTime notedAt
    ) {
        static AllergyDto from(PatientAllergy a) {
            return new AllergyDto(a.getId(), a.getSubstance(), a.getReaction(),
                    a.getSeverity() != null ? a.getSeverity().name() : null,
                    a.getCategory() != null ? a.getCategory().name() : null,
                    a.getSource() != null ? a.getSource().name() : null,
                    a.getNotedByUserName(), a.getNotedAt());
        }
    }

    public record ConditionDto(
            UUID id,
            String name,
            String icd10Code,
            String status,
            LocalDate onsetDate,
            String source,
            String notedByUserName,
            LocalDateTime notedAt
    ) {
        static ConditionDto from(PatientCondition c) {
            return new ConditionDto(c.getId(), c.getName(), c.getIcd10Code(),
                    c.getStatus() != null ? c.getStatus().name() : null,
                    c.getOnsetDate(),
                    c.getSource() != null ? c.getSource().name() : null,
                    c.getNotedByUserName(), c.getNotedAt());
        }
    }

    public record MedicationDto(
            UUID id,
            String medicationName,
            String dose,
            String schedule,
            boolean active,
            LocalDate startedOn,
            LocalDate stoppedOn,
            UUID prescriptionId,
            String source,
            String notedByUserName,
            LocalDateTime notedAt
    ) {
        static MedicationDto from(PatientMedication m) {
            return new MedicationDto(m.getId(), m.getMedicationName(), m.getDose(), m.getSchedule(),
                    m.isActive(), m.getStartedOn(), m.getStoppedOn(), m.getPrescriptionId(),
                    m.getSource() != null ? m.getSource().name() : null,
                    m.getNotedByUserName(), m.getNotedAt());
        }
    }

    public static PatientClinicalSummaryResponse from(ClinicalSummary summary) {
        return new PatientClinicalSummaryResponse(
                summary.patientId(),
                summary.bloodType(),
                summary.allergies().stream().map(AllergyDto::from).toList(),
                summary.conditions().stream().map(ConditionDto::from).toList(),
                summary.activeMedications().stream().map(MedicationDto::from).toList(),
                summary.lastNoteAt(),
                ReviewDto.from(summary.allergiesReview()),
                ReviewDto.from(summary.conditionsReview()),
                ReviewDto.from(summary.medicationsReview())
        );
    }

    public static AllergyDto allergy(PatientAllergy a) {
        return AllergyDto.from(a);
    }

    public static ConditionDto condition(PatientCondition c) {
        return ConditionDto.from(c);
    }

    public static MedicationDto medication(PatientMedication m) {
        return MedicationDto.from(m);
    }
}
