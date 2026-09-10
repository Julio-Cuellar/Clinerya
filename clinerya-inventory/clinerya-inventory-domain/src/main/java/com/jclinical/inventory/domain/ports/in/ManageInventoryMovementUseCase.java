package com.jclinical.inventory.domain.ports.in;

import com.jclinical.inventory.domain.model.InventoryBatch;
import com.jclinical.inventory.domain.model.InventoryMovement;
import com.jclinical.inventory.domain.model.MovementType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ManageInventoryMovementUseCase {

    InventoryMovement registerPurchaseEntry(UUID actingUserId, UUID clinicId, UUID materialId, RegisterMovementCommand command);

    InventoryMovement registerAdjustment(UUID actingUserId, UUID clinicId, UUID materialId, RegisterMovementCommand command);

    InventoryMovement registerSaleExit(UUID actingUserId, UUID clinicId, UUID materialId, RegisterMovementCommand command);

    /** Sin control de permiso: paso interno del consumo de materiales en una visita clinica. */
    InventoryMovement registerUsageExit(UUID clinicId, UUID materialId, RegisterUsageExitCommand command);

    List<InventoryMovement> listMovementsByMaterial(UUID actingUserId, UUID clinicId, UUID materialId, int page, int size);

    List<InventoryMovement> listMovementsByClinic(UUID actingUserId, UUID clinicId, int page, int size);

    List<InventoryBatch> listBatchesByMaterial(UUID actingUserId, UUID clinicId, UUID materialId);

    List<InventoryMovement> registerExpiredBatchWastes(UUID actingUserId, UUID clinicId, LocalDate asOfDate);

    record RegisterMovementCommand(
            MovementType type,
            BigDecimal quantity,
            BigDecimal presentationQuantity,
            LocalDateTime movementDate,
            String notes,
            String lotNumber,
            LocalDate expirationDate,
            UUID batchId
    ) {}

    record RegisterUsageExitCommand(
            BigDecimal quantity,
            LocalDateTime movementDate,
            String referenceType,
            UUID referenceId,
            String notes,
            UUID batchId
    ) {}
}
