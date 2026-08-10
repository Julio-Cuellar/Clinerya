package com.jclinical.treatments.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentCatalogMaterial {
    private UUID id;
    private UUID materialId;
    private String materialName;
    private BigDecimal typicalQuantity;
}
