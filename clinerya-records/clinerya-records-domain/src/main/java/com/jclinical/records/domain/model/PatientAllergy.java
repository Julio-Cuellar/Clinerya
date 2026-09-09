package com.jclinical.records.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientAllergy {
    private UUID id;
    private UUID clinicId;
    private UUID patientId;
    private String substance;
    private String reaction;
    private AllergySeverity severity;
    private AllergyCategory category;
    private ClinicalDataSource source;
    private UUID notedByUserId;
    private String notedByUserName;
    private LocalDateTime notedAt;
}
