package com.jclinical.treatments.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentCatalogItem {
    private UUID id;
    private UUID clinicId;
    private String name;
    private String category;
    private String description;
    private BigDecimal defaultPrice;
    private Integer estimatedDurationMinutes;
    @Builder.Default
    private List<TreatmentCatalogMaterial> materials = new ArrayList<>();
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
