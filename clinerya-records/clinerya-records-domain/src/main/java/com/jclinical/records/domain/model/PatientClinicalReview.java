package com.jclinical.records.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Estado de revisión de un tipo de dato clínico del paciente.
 * {@code noneReported} = el clínico preguntó y no hay nada que registrar
 * (distinto de "sin registrar" porque nadie preguntó).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientClinicalReview {
    private UUID id;
    private UUID clinicId;
    private UUID patientId;
    private ClinicalReviewKind kind;
    private boolean noneReported;
    private UUID reviewedByUserId;
    private String reviewedByUserName;
    private LocalDateTime reviewedAt;
}
