package com.jclinical.inventory.infra.config;

import com.jclinical.inventory.domain.model.InventoryBatch;
import com.jclinical.inventory.domain.model.InventoryMovement;
import com.jclinical.inventory.domain.ports.in.ManageInventoryMovementUseCase;
import com.jclinical.inventory.domain.service.InventoryMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class TransactionalInventoryMovementUseCase implements ManageInventoryMovementUseCase {

    private final InventoryMovementService movementService;

    @Override
    @Transactional
    public InventoryMovement registerPurchaseEntry(UUID actingUserId, UUID clinicId, UUID materialId, RegisterMovementCommand command) {
        return movementService.registerPurchaseEntry(actingUserId, clinicId, materialId, command);
    }

    @Override
    @Transactional
    public InventoryMovement registerAdjustment(UUID actingUserId, UUID clinicId, UUID materialId, RegisterMovementCommand command) {
        return movementService.registerAdjustment(actingUserId, clinicId, materialId, command);
    }

    @Override
    @Transactional
    public InventoryMovement registerSaleExit(UUID actingUserId, UUID clinicId, UUID materialId, RegisterMovementCommand command) {
        return movementService.registerSaleExit(actingUserId, clinicId, materialId, command);
    }

    @Override
    @Transactional
    public InventoryMovement registerUsageExit(UUID clinicId, UUID materialId, RegisterUsageExitCommand command) {
        return movementService.registerUsageExit(clinicId, materialId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovement> listMovementsByMaterial(UUID actingUserId, UUID clinicId, UUID materialId, int page, int size) {
        return movementService.listMovementsByMaterial(actingUserId, clinicId, materialId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovement> listMovementsByClinic(UUID actingUserId, UUID clinicId, int page, int size) {
        return movementService.listMovementsByClinic(actingUserId, clinicId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryBatch> listBatchesByMaterial(UUID actingUserId, UUID clinicId, UUID materialId) {
        return movementService.listBatchesByMaterial(actingUserId, clinicId, materialId);
    }

    @Override
    @Transactional
    public List<InventoryMovement> registerExpiredBatchWastes(UUID actingUserId, UUID clinicId, LocalDate asOfDate) {
        return movementService.registerExpiredBatchWastes(actingUserId, clinicId, asOfDate);
    }
}
