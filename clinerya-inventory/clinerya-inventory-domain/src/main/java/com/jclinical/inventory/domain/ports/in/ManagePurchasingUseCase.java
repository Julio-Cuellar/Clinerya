package com.jclinical.inventory.domain.ports.in;

import com.jclinical.inventory.domain.model.PurchaseOrder;
import com.jclinical.inventory.domain.model.PurchaseReceipt;
import com.jclinical.inventory.domain.model.Supplier;
import com.jclinical.inventory.domain.model.SupplierMaterial;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ManagePurchasingUseCase {

    Supplier createSupplier(UUID actingUserId, UUID clinicId, CreateSupplierCommand command);

    Supplier updateSupplier(UUID actingUserId, UUID clinicId, UUID supplierId, UpdateSupplierCommand command);

    List<Supplier> listSuppliers(UUID actingUserId, UUID clinicId);

    List<SupplierMaterial> listSupplierMaterials(UUID actingUserId, UUID clinicId, UUID supplierId);

    SupplierMaterial addSupplierMaterial(UUID actingUserId, UUID clinicId, UUID supplierId, AddSupplierMaterialCommand command);

    void removeSupplierMaterial(UUID actingUserId, UUID clinicId, UUID supplierId, UUID materialId);

    PurchaseOrder createPurchaseOrder(UUID actingUserId, UUID clinicId, CreatePurchaseOrderCommand command);

    PurchaseOrder getPurchaseOrder(UUID actingUserId, UUID clinicId, UUID orderId);

    List<PurchaseOrder> listPurchaseOrders(UUID actingUserId, UUID clinicId);

    PurchaseOrder markPurchaseOrderOrdered(UUID actingUserId, UUID clinicId, UUID orderId);

    PurchaseOrder cancelPurchaseOrder(UUID actingUserId, UUID clinicId, UUID orderId);

    PurchaseReceipt receivePurchaseOrder(UUID actingUserId, UUID clinicId, UUID orderId, ReceivePurchaseOrderCommand command);

    List<PurchaseReceipt> listReceipts(UUID actingUserId, UUID clinicId, UUID orderId);

    record CreateSupplierCommand(
            String name,
            String contactName,
            String phone,
            String email,
            String taxId,
            String notes
    ) {}

    record UpdateSupplierCommand(
            String name,
            String contactName,
            String phone,
            String email,
            String taxId,
            String notes,
            boolean active
    ) {}

    record AddSupplierMaterialCommand(
            UUID materialId,
            BigDecimal supplierUnitCost
    ) {}

    record CreatePurchaseOrderCommand(
            UUID supplierId,
            LocalDate orderDate,
            LocalDate expectedDate,
            String notes,
            UUID bankAccountId,
            List<CreatePurchaseOrderLineCommand> lines
    ) {}

    record CreatePurchaseOrderLineCommand(
            UUID materialId,
            BigDecimal quantity,
            BigDecimal unitCost
    ) {}

    record ReceivePurchaseOrderCommand(
            LocalDateTime receivedAt,
            String notes,
            List<ReceivePurchaseOrderLineCommand> lines
    ) {}

    record ReceivePurchaseOrderLineCommand(
            UUID purchaseOrderLineId,
            BigDecimal quantity,
            BigDecimal unitCost,
            String lotNumber,
            LocalDate expirationDate
    ) {}
}
