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
public class MedicalHistoryTemplate {
    private UUID id;
    private UUID clinicId;
    private String name;
    private String description;
    private String schemaJson;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
