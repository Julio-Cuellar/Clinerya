package com.jclinical.treatments.domain.ports.in;

import com.jclinical.treatments.domain.model.TreatmentCatalogItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageTreatmentCatalogUseCase {

    TreatmentCatalogItem createCatalogItem(UUID clinicId, UUID actingUserId, CreateCatalogItemCommand command);

    TreatmentCatalogItem updateCatalogItem(UUID itemId, UUID clinicId, UUID actingUserId, UpdateCatalogItemCommand command);

    void deactivateCatalogItem(UUID itemId, UUID clinicId, UUID actingUserId);

    Optional<TreatmentCatalogItem> getCatalogItem(UUID itemId, UUID clinicId, UUID actingUserId);

    List<TreatmentCatalogItem> getCatalogItemsByClinic(UUID clinicId, UUID actingUserId, boolean includeInactive);

    record CatalogMaterialCommand(
        UUID materialId,
        BigDecimal typicalQuantity
    ) {}

    record CreateCatalogItemCommand(
        String name,
        String category,
        String description,
        BigDecimal defaultPrice,
        Integer estimatedDurationMinutes,
        List<CatalogMaterialCommand> materials
    ) {}

    record UpdateCatalogItemCommand(
        String name,
        String category,
        String description,
        BigDecimal defaultPrice,
        Integer estimatedDurationMinutes,
        List<CatalogMaterialCommand> materials,
        boolean active
    ) {}
}
