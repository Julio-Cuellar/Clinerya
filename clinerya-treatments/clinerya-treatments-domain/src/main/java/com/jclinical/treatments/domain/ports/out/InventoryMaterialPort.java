package com.jclinical.treatments.domain.ports.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface InventoryMaterialPort {

    Optional<MaterialSnapshot> findActiveMaterial(UUID materialId, UUID clinicId);

    void registerUsage(UUID clinicId, UUID materialId, BigDecimal quantity, String referenceType, UUID referenceId, String notes);

    record MaterialSnapshot(
            UUID materialId,
            String name,
            BigDecimal unitCost
    ) {}
}
