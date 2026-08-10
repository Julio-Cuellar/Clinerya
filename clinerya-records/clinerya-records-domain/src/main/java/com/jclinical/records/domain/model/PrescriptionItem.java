package com.jclinical.records.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItem {
    private UUID id;
    private UUID prescriptionId;
    private String medicationName;
    private String dosage;
    private String frequency;
    private String duration;
    private String instructions;
}
