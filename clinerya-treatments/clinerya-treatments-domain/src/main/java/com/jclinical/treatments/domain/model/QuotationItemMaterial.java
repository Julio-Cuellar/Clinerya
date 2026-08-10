package com.jclinical.treatments.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationItemMaterial {
    private UUID id;
    private UUID materialId;
    private String materialName;
    private BigDecimal estimatedQuantity;
    private BigDecimal unitCostAtQuote;

    public BigDecimal estimatedCost() {
        return estimatedQuantity.multiply(unitCostAtQuote).setScale(2, RoundingMode.HALF_UP);
    }
}
