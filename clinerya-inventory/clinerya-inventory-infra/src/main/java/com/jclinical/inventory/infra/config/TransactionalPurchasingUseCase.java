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
    public Supplier createSupplier(UUID clinicId, UUID actingUserId, CreateSupplierCommand command) {
        return service.createSupplier(clinicId, actingUserId, command);
    }

    @Override
    @Transactional
    public Supplier updateSupplier(UUID clinicId, UUID supplierId, UUID actingUserId, UpdateSupplierCommand command) {
        return service.updateSupplier(clinicId, supplierId, actingUserId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Supplier> listSuppliers(UUID clinicId, UUID actingUserId) {
        return service.listSuppliers(clinicId, actingUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierMaterial> listSupplierMaterials(UUID clinicId, UUID supplierId, UUID actingUserId) {
        return service.listSupplierMaterials(clinicId, supplierId, actingUserId);
    }

    @Override
    @Transactional
    public SupplierMaterial addSupplierMaterial(UUID clinicId, UUID supplierId, UUID actingUserId, AddSupplierMaterialCommand command) {
        return service.addSupplierMaterial(clinicId, supplierId, actingUserId, command);
    }

    @Override
    @Transactional
    public void removeSupplierMaterial(UUID clinicId, UUID supplierId, UUID materialId, UUID actingUserId) {
        service.removeSupplierMaterial(clinicId, supplierId, materialId, actingUserId);
    }

    @Override
    @Transactional
    public PurchaseOrder createPurchaseOrder(UUID clinicId, UUID actingUserId, CreatePurchaseOrderCommand command) {
        return service.createPurchaseOrder(clinicId, actingUserId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrder getPurchaseOrder(UUID clinicId, UUID orderId, UUID actingUserId) {
        return service.getPurchaseOrder(clinicId, orderId, actingUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrder> listPurchaseOrders(UUID clinicId, UUID actingUserId) {
        return service.listPurchaseOrders(clinicId, actingUserId);
    }

    @Override
    @Transactional
    public PurchaseOrder markPurchaseOrderOrdered(UUID clinicId, UUID orderId, UUID actingUserId) {
        return service.markPurchaseOrderOrdered(clinicId, orderId, actingUserId);
    }

    @Override
    @Transactional
    public PurchaseOrder cancelPurchaseOrder(UUID clinicId, UUID orderId, UUID actingUserId) {
        return service.cancelPurchaseOrder(clinicId, orderId, actingUserId);
    }

    @Override
    @Transactional
    public PurchaseReceipt receivePurchaseOrder(UUID clinicId, UUID orderId, UUID actingUserId, ReceivePurchaseOrderCommand command) {
        return service.receivePurchaseOrder(clinicId, orderId, actingUserId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseReceipt> listReceipts(UUID clinicId, UUID orderId, UUID actingUserId) {
        return service.listReceipts(clinicId, orderId, actingUserId);
    }
}
