package com.jclinical.treatments.infra.adapters.out.persistence;

import com.jclinical.inventory.domain.ports.in.ManageInventoryMovementUseCase;
import com.jclinical.inventory.domain.ports.in.ManageMaterialUseCase;
import com.jclinical.treatments.domain.ports.out.InventoryMaterialPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TreatmentsInventoryMaterialAdapter implements InventoryMaterialPort {

    private final ManageMaterialUseCase materialUseCase;
    private final ManageInventoryMovementUseCase movementUseCase;

    @Override
    public Optional<MaterialSnapshot> findActiveMaterial(UUID materialId, UUID clinicId) {
        return materialUseCase.getMaterialForSystem(materialId, clinicId)
                .filter(com.jclinical.inventory.domain.model.Material::isActive)
                .map(material -> new MaterialSnapshot(material.getId(), material.getName(), material.getUnitCost()));
    }

    @Override
    public void registerUsage(UUID clinicId, UUID materialId, BigDecimal quantity, String referenceType, UUID referenceId, String notes) {
        ManageInventoryMovementUseCase.RegisterUsageExitCommand command = new ManageInventoryMovementUseCase.RegisterUsageExitCommand(
                quantity,
                LocalDateTime.now(),
                referenceType,
                referenceId,
                notes,
                null
        );
        movementUseCase.registerUsageExit(clinicId, materialId, command);
    }
}
