package com.jclinical.inventory.infra.config;

import com.jclinical.inventory.domain.model.PurchaseOrder;
import com.jclinical.inventory.domain.model.PurchaseReceipt;
import com.jclinical.inventory.domain.model.Supplier;
import com.jclinical.inventory.domain.model.SupplierMaterial;
import com.jclinical.inventory.domain.ports.in.ManagePurchasingUseCase;
import com.jclinical.inventory.domain.service.PurchasingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class TransactionalPurchasingUseCase implements ManagePurchasingUseCase {
    private final PurchasingService service;

    @Override
    @Transactional
    public Supplier createSupplier(UUID actingUserId, UUID clinicId, CreateSupplierCommand command) {
        return service.createSupplier(actingUserId, clinicId, command);
    }

    @Override
    @Transactional
    public Supplier updateSupplier(UUID actingUserId, UUID clinicId, UUID supplierId, UpdateSupplierCommand command) {
        return service.updateSupplier(actingUserId, clinicId, supplierId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Supplier> listSuppliers(UUID actingUserId, UUID clinicId) {
        return service.listSuppliers(actingUserId, clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierMaterial> listSupplierMaterials(UUID actingUserId, UUID clinicId, UUID supplierId) {
        return service.listSupplierMaterials(actingUserId, clinicId, supplierId);
    }

    @Override
    @Transactional
    public SupplierMaterial addSupplierMaterial(UUID actingUserId, UUID clinicId, UUID supplierId, AddSupplierMaterialCommand command) {
        return service.addSupplierMaterial(actingUserId, clinicId, supplierId, command);
    }

    @Override
    @Transactional
    public void removeSupplierMaterial(UUID actingUserId, UUID clinicId, UUID supplierId, UUID materialId) {
        service.removeSupplierMaterial(actingUserId, clinicId, supplierId, materialId);
    }

    @Override
    @Transactional
    public PurchaseOrder createPurchaseOrder(UUID actingUserId, UUID clinicId, CreatePurchaseOrderCommand command) {
        return service.createPurchaseOrder(actingUserId, clinicId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrder getPurchaseOrder(UUID actingUserId, UUID clinicId, UUID orderId) {
        return service.getPurchaseOrder(actingUserId, clinicId, orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrder> listPurchaseOrders(UUID actingUserId, UUID clinicId) {
        return service.listPurchaseOrders(actingUserId, clinicId);
    }

    @Override
    @Transactional
    public PurchaseOrder markPurchaseOrderOrdered(UUID actingUserId, UUID clinicId, UUID orderId) {
        return service.markPurchaseOrderOrdered(actingUserId, clinicId, orderId);
    }

    @Override
    @Transactional
    public PurchaseOrder cancelPurchaseOrder(UUID actingUserId, UUID clinicId, UUID orderId) {
        return service.cancelPurchaseOrder(actingUserId, clinicId, orderId);
    }

    @Override
    @Transactional
    public PurchaseReceipt receivePurchaseOrder(UUID actingUserId, UUID clinicId, UUID orderId, ReceivePurchaseOrderCommand command) {
        return service.receivePurchaseOrder(actingUserId, clinicId, orderId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseReceipt> listReceipts(UUID actingUserId, UUID clinicId, UUID orderId) {
        return service.listReceipts(actingUserId, clinicId, orderId);
    }
}
