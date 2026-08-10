package com.jclinical.treatments.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public class VisitMaterialUsage {
    private final UUID id;
    private final UUID materialId;
    private final String materialName;
    private final BigDecimal actualQuantity;

    public VisitMaterialUsage(UUID id, UUID materialId, String materialName, BigDecimal actualQuantity) {
        this.id = id;
        this.materialId = materialId;
        this.materialName = materialName;
        this.actualQuantity = actualQuantity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public BigDecimal getActualQuantity() {
        return actualQuantity;
    }
}
