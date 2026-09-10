package com.jclinical.treatments.domain.service;

import com.jclinical.treatments.domain.model.TreatmentCatalogItem;
import com.jclinical.treatments.domain.model.TreatmentCatalogMaterial;
import com.jclinical.treatments.domain.ports.in.ManageTreatmentCatalogUseCase;
import com.jclinical.treatments.domain.ports.out.InventoryMaterialPort;
import com.jclinical.treatments.domain.ports.out.InventoryMaterialPort.MaterialSnapshot;
import com.jclinical.treatments.domain.ports.out.TreatmentCatalogRepositoryPort;
import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.StaffPermission;
import com.jclinical.core.security.StaffPermissionCheckerPort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TreatmentCatalogService implements ManageTreatmentCatalogUseCase {

    private final TreatmentCatalogRepositoryPort catalogRepository;
    private final InventoryMaterialPort inventoryMaterialPort;
    private final StaffPermissionCheckerPort permissionChecker;

    public TreatmentCatalogService(
            TreatmentCatalogRepositoryPort catalogRepository,
            InventoryMaterialPort inventoryMaterialPort,
            StaffPermissionCheckerPort permissionChecker) {
        this.catalogRepository = catalogRepository;
        this.inventoryMaterialPort = inventoryMaterialPort;
        this.permissionChecker = permissionChecker;
    }

    private void authorize(UUID actingUserId, UUID clinicId, StaffPermission permission) {
        if (actingUserId == null || !permissionChecker.hasPermission(clinicId, actingUserId, permission)) {
            throw new ClinicAccessDeniedException("No tienes permisos para esta operación del catálogo de tratamientos.");
        }
    }

    @Override
    public TreatmentCatalogItem createCatalogItem(UUID clinicId, UUID actingUserId, CreateCatalogItemCommand command) {
        authorize(actingUserId, clinicId, StaffPermission.MANAGE_TREATMENT_CATALOG);
        validate(command.name(), command.defaultPrice());

        TreatmentCatalogItem item = TreatmentCatalogItem.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .name(command.name())
                .category(command.category())
                .description(command.description())
                .defaultPrice(command.defaultPrice())
                .estimatedDurationMinutes(command.estimatedDurationMinutes())
                .materials(toMaterials(command.materials(), clinicId))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return catalogRepository.save(item);
    }

    @Override
    public TreatmentCatalogItem updateCatalogItem(UUID itemId, UUID clinicId, UUID actingUserId, UpdateCatalogItemCommand command) {
        authorize(actingUserId, clinicId, StaffPermission.MANAGE_TREATMENT_CATALOG);
        validate(command.name(), command.defaultPrice());

        TreatmentCatalogItem item = catalogRepository.findByIdAndClinicId(itemId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El servicio no existe en esta clínica."));

        item.setName(command.name());
        item.setCategory(command.category());
        item.setDescription(command.description());
        item.setDefaultPrice(command.defaultPrice());
        item.setEstimatedDurationMinutes(command.estimatedDurationMinutes());
        item.setMaterials(toMaterials(command.materials(), clinicId));
        item.setActive(command.active());
        item.setUpdatedAt(LocalDateTime.now());

        return catalogRepository.save(item);
    }

    @Override
    public void deactivateCatalogItem(UUID itemId, UUID clinicId, UUID actingUserId) {
        authorize(actingUserId, clinicId, StaffPermission.MANAGE_TREATMENT_CATALOG);
        TreatmentCatalogItem item = catalogRepository.findByIdAndClinicId(itemId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El servicio no existe en esta clínica."));
        item.setActive(false);
        item.setUpdatedAt(LocalDateTime.now());
        catalogRepository.save(item);
    }

    @Override
    public Optional<TreatmentCatalogItem> getCatalogItem(UUID itemId, UUID clinicId, UUID actingUserId) {
        authorize(actingUserId, clinicId, StaffPermission.VIEW_TREATMENTS);
        return catalogRepository.findByIdAndClinicId(itemId, clinicId);
    }

    @Override
    public List<TreatmentCatalogItem> getCatalogItemsByClinic(UUID clinicId, UUID actingUserId, boolean includeInactive) {
        authorize(actingUserId, clinicId, StaffPermission.VIEW_TREATMENTS);
        return catalogRepository.findByClinicId(clinicId, includeInactive);
    }

    private List<TreatmentCatalogMaterial> toMaterials(List<CatalogMaterialCommand> commands, UUID clinicId) {
        if (commands == null) {
            return List.of();
        }
        return commands.stream().map(command -> toMaterial(command, clinicId)).toList();
    }

    private TreatmentCatalogMaterial toMaterial(CatalogMaterialCommand command, UUID clinicId) {
        if (command.materialId() == null) {
            throw new IllegalArgumentException("El material del checklist es obligatorio.");
        }
        MaterialSnapshot snapshot = inventoryMaterialPort.findActiveMaterial(command.materialId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("El material no existe o no está activo en esta clínica."));
        if (command.typicalQuantity() != null && command.typicalQuantity().signum() <= 0) {
            throw new IllegalArgumentException("La cantidad típica debe ser mayor a cero si se especifica.");
        }
        return TreatmentCatalogMaterial.builder()
                .id(UUID.randomUUID())
                .materialId(snapshot.materialId())
                .materialName(snapshot.name())
                .typicalQuantity(command.typicalQuantity())
                .build();
    }

    private void validate(String name, BigDecimal defaultPrice) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del servicio es obligatorio.");
        }
        if (defaultPrice == null || defaultPrice.signum() < 0) {
            throw new IllegalArgumentException("El precio debe ser mayor o igual a cero.");
        }
    }
}
