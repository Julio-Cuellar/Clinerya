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

    Supplier createSupplier(UUID clinicId, UUID actingUserId, CreateSupplierCommand command);

    Supplier updateSupplier(UUID clinicId, UUID supplierId, UUID actingUserId, UpdateSupplierCommand command);

    List<Supplier> listSuppliers(UUID clinicId, UUID actingUserId);

    List<SupplierMaterial> listSupplierMaterials(UUID clinicId, UUID supplierId, UUID actingUserId);

    SupplierMaterial addSupplierMaterial(UUID clinicId, UUID supplierId, UUID actingUserId, AddSupplierMaterialCommand command);

    void removeSupplierMaterial(UUID clinicId, UUID supplierId, UUID materialId, UUID actingUserId);

    PurchaseOrder createPurchaseOrder(UUID clinicId, UUID actingUserId, CreatePurchaseOrderCommand command);

    PurchaseOrder getPurchaseOrder(UUID clinicId, UUID orderId, UUID actingUserId);

    List<PurchaseOrder> listPurchaseOrders(UUID clinicId, UUID actingUserId);

    PurchaseOrder markPurchaseOrderOrdered(UUID clinicId, UUID orderId, UUID actingUserId);

    PurchaseOrder cancelPurchaseOrder(UUID clinicId, UUID orderId, UUID actingUserId);

    PurchaseReceipt receivePurchaseOrder(UUID clinicId, UUID orderId, UUID actingUserId, ReceivePurchaseOrderCommand command);

    List<PurchaseReceipt> listReceipts(UUID clinicId, UUID orderId, UUID actingUserId);

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
