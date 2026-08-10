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
public class MedicalHistory {
    private UUID id;
    private UUID patientId;
    private UUID clinicId;
    private UUID templateId;
    private String answersJson;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
