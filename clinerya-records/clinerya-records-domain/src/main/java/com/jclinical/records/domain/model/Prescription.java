package com.jclinical.records.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prescription {
    private UUID id;
    private UUID clinicId;
    private UUID patientId;
    private UUID doctorId;
    private UUID appointmentId;
    private String notes;
    private PrescriptionStatus status;
    private List<PrescriptionItem> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
