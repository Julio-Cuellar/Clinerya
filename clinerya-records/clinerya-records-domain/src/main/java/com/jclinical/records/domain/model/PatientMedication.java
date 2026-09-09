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
public class PatientMedication {
    private UUID id;
    private UUID clinicId;
    private UUID patientId;
    private String medicationName;
    private String dose;
    private String schedule;
    private boolean active;
    private LocalDate startedOn;
    private LocalDate stoppedOn;
    private UUID prescriptionId;
    private ClinicalDataSource source;
    private UUID notedByUserId;
    private String notedByUserName;
    private LocalDateTime notedAt;
}
