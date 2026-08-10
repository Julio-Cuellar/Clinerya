package com.jclinical.treatments.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationItem {
    private UUID id;
    private UUID catalogItemId;
    private String description;
    private Integer toothNumber;
    private BigDecimal laborCharge;
    @Builder.Default
    private List<QuotationItemMaterial> materials = new ArrayList<>();
    private BigDecimal discountPercentage;
    @Builder.Default
    private ItemProgressStatus progressStatus = ItemProgressStatus.PENDING;

    public BigDecimal materialsTotal() {
        return materials.stream()
                .map(QuotationItemMaterial::estimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal subtotal() {
        BigDecimal discount = discountPercentage != null ? discountPercentage : BigDecimal.ZERO;
        BigDecimal factor = BigDecimal.ONE.subtract(discount.divide(BigDecimal.valueOf(100)));
        return laborCharge
                .add(materialsTotal())
                .multiply(factor)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
