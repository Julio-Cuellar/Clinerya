package com.jclinical.records.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientCondition {
    private UUID id;
    private UUID clinicId;
    private UUID patientId;
    private String name;
    private String icd10Code;
    private ConditionStatus status;
    private LocalDate onsetDate;
    private ClinicalDataSource source;
    private UUID notedByUserId;
    private String notedByUserName;
    private LocalDateTime notedAt;
}
