package com.jclinical.inventory.domain.ports.in;

import com.jclinical.inventory.domain.model.Material;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageMaterialUseCase {

    Material createMaterial(UUID clinicId, UUID actingUserId, CreateMaterialCommand command);

    Material updateMaterial(UUID materialId, UUID clinicId, UUID actingUserId, UpdateMaterialCommand command);

    void deactivateMaterial(UUID materialId, UUID clinicId, UUID actingUserId);

    Optional<Material> getMaterial(UUID materialId, UUID clinicId, UUID actingUserId);

    List<Material> getMaterialsByClinic(UUID clinicId, UUID actingUserId, boolean includeInactive);

    /**
     * Lectura interna para el modulo de tratamientos, que ya valido acceso al expediente
     * del paciente antes de resolver el material del checklist. No exponer desde un
     * controlador.
     */
    Optional<Material> getMaterialForSystem(UUID materialId, UUID clinicId);

    record CreateMaterialCommand(
            String name,
            String category,
            String internalCode,
            String brand,
            String description,
            String unitOfMeasure,
            String presentationName,
            BigDecimal quantityPerPresentation,
            BigDecimal unitCost,
            BigDecimal minimumStock,
            boolean saleEnabled,
            BigDecimal salePrice,
            boolean tracksBatches
    ) {}

    record UpdateMaterialCommand(
            String name,
            String category,
            String internalCode,
            String brand,
            String description,
            String unitOfMeasure,
            String presentationName,
            BigDecimal quantityPerPresentation,
            BigDecimal unitCost,
            BigDecimal minimumStock,
            boolean saleEnabled,
            BigDecimal salePrice,
            boolean tracksBatches,
            boolean active
    ) {}
}
