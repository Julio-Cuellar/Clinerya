package com.jclinical.treatments.domain.ports.in;

import com.jclinical.treatments.domain.model.PricingType;
import com.jclinical.treatments.domain.model.TreatmentCatalogItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageTreatmentCatalogUseCase {

    TreatmentCatalogItem createCatalogItem(UUID actingUserId, UUID clinicId, CreateCatalogItemCommand command);

    TreatmentCatalogItem updateCatalogItem(UUID actingUserId, UUID itemId, UUID clinicId, UpdateCatalogItemCommand command);

    void deactivateCatalogItem(UUID actingUserId, UUID itemId, UUID clinicId);

    Optional<TreatmentCatalogItem> getCatalogItem(UUID actingUserId, UUID itemId, UUID clinicId);

    List<TreatmentCatalogItem> getCatalogItemsByClinic(UUID actingUserId, UUID clinicId, boolean includeInactive);

    /**
     * Siembra el catálogo sugerido para la especialidad de la clínica.
     *
     * <p>Es idempotente: sólo crea los servicios cuyo nombre todavía no existe en la clínica, así
     * que correrla dos veces no duplica nada. El onboarding es salteable y reanudable, de modo que
     * esto va a pasar.
     */
    SeedResult seedCatalogForSpecialty(UUID actingUserId, UUID clinicId);

    /** Qué hizo la siembra: cuántos servicios creó y cuántos ya existían. */
    record SeedResult(int created, int skipped, List<TreatmentCatalogItem> items) {}

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
        List<CatalogMaterialCommand> materials,
        PricingType pricingType,
        boolean availableInAssistant
    ) {
        /** Precio fijo y fuera del asistente, como eran todos los servicios antes de estas reglas. */
        public CreateCatalogItemCommand(String name, String category, String description, BigDecimal defaultPrice,
                                        Integer estimatedDurationMinutes, List<CatalogMaterialCommand> materials) {
            this(name, category, description, defaultPrice, estimatedDurationMinutes, materials, PricingType.FIXED, false);
        }
    }

    record UpdateCatalogItemCommand(
        String name,
        String category,
        String description,
        BigDecimal defaultPrice,
        Integer estimatedDurationMinutes,
        List<CatalogMaterialCommand> materials,
        boolean active,
        PricingType pricingType,
        boolean availableInAssistant
    ) {
        public UpdateCatalogItemCommand(String name, String category, String description, BigDecimal defaultPrice,
                                        Integer estimatedDurationMinutes, List<CatalogMaterialCommand> materials,
                                        boolean active) {
            this(name, category, description, defaultPrice, estimatedDurationMinutes, materials, active, PricingType.FIXED,
                    false);
        }
    }
}
