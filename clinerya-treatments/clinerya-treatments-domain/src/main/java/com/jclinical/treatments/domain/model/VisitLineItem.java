package com.jclinical.treatments.domain.model;

import java.util.List;
import java.util.UUID;

public class VisitLineItem {
    private final UUID id;
    private final UUID quotationItemId;
    private final List<VisitMaterialUsage> materialsUsed;

    public VisitLineItem(UUID id, UUID quotationItemId, List<VisitMaterialUsage> materialsUsed) {
        this.id = id;
        this.quotationItemId = quotationItemId;
        this.materialsUsed = materialsUsed;
    }

    public UUID getId() {
        return id;
    }

    public UUID getQuotationItemId() {
        return quotationItemId;
    }

    public List<VisitMaterialUsage> getMaterialsUsed() {
        return materialsUsed;
    }
}
