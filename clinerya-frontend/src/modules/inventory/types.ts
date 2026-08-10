export type MovementType = "PURCHASE_ENTRY" | "USAGE_EXIT" | "SALE_EXIT" | "ADJUSTMENT_IN" | "ADJUSTMENT_OUT";

export const MOVEMENT_TYPE_LABELS: Record<MovementType, string> = {
  PURCHASE_ENTRY: "Compra",
  USAGE_EXIT: "Uso clínico",
  SALE_EXIT: "Venta directa",
  ADJUSTMENT_IN: "Ajuste entrada",
  ADJUSTMENT_OUT: "Ajuste salida"
};

export interface MaterialResponse {
  id: string;
  clinicId: string;
  name: string;
  category?: string;
  internalCode?: string;
  brand?: string;
  description?: string;
  unitOfMeasure: string;
  presentationName?: string;
  quantityPerPresentation?: number;
  unitCost: number;
  currentStock: number;
  reservedQuantity?: number;
  availableQuantity?: number;
  minimumStock?: number;
  belowMinimumStock: boolean;
  saleEnabled: boolean;
  salePrice?: number;
  tracksBatches: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MaterialReservationResponse {
  id: string;
  appointmentId: string;
  patientId?: string;
  patientName: string;
  treatmentName: string;
  scheduledStart: string;
  materialId: string;
  materialName: string;
  unitOfMeasure: string;
  quantity: number;
  currentStock: number;
  totalReservedQuantity: number;
  availableQuantity: number;
  status: "RESERVED" | "RELEASED";
  createdAt: string;
}

export interface CreateMaterialRequest {
  name: string;
  category?: string;
  internalCode?: string;
  brand?: string;
  description?: string;
  unitOfMeasure: string;
  presentationName?: string;
  quantityPerPresentation?: number;
  unitCost: number;
  minimumStock?: number;
  saleEnabled: boolean;
  salePrice?: number;
  tracksBatches: boolean;
}

export interface UpdateMaterialRequest extends CreateMaterialRequest {
  active: boolean;
}

export interface InventoryMovementResponse {
  id: string;
  clinicId: string;
  materialId: string;
  type: MovementType;
  quantity: number;
  presentationQuantity?: number;
  presentationNameAtMovement?: string;
  quantityPerPresentationAtMovement?: number;
  unitCostAtMovement: number;
  batchId?: string;
  movementDate: string;
  referenceType?: string;
  referenceId?: string;
  notes?: string;
  createdAt: string;
}

export interface RegisterInventoryMovementRequest {
  type: MovementType;
  quantity?: number;
  presentationQuantity?: number;
  movementDate?: string;
  notes?: string;
  lotNumber?: string;
  expirationDate?: string;
  batchId?: string;
}

export interface GeneralLedgerEntryResponse {
  id: string;
  materialId: string;
  materialName: string;
  materialUnitOfMeasure: string;
  type: MovementType;
  quantity: number;
  unitCostAtMovement: number;
  batchId?: string;
  movementDate: string;
  referenceType?: string;
  referenceId?: string;
  notes?: string;
  createdAt: string;
}

export interface BatchResponse {
  id: string;
  materialId: string;
  lotNumber?: string;
  expirationDate?: string;
  initialQuantity: number;
  remainingQuantity: number;
  unitCostAtEntry: number;
  depleted: boolean;
  expired: boolean;
  createdAt: string;
}

export interface SupplierResponse {
  id: string;
  clinicId: string;
  name: string;
  contactName?: string;
  phone?: string;
  email?: string;
  taxId?: string;
  notes?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SaveSupplierRequest {
  name: string;
  contactName?: string;
  phone?: string;
  email?: string;
  taxId?: string;
  notes?: string;
  active?: boolean;
}

export interface SupplierMaterialResponse {
  id: string;
  clinicId: string;
  supplierId: string;
  materialId: string;
  materialName: string;
  unitOfMeasure: string;
  supplierUnitCost: number;
  lastSuppliedAt?: string;
  receiptCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface AddSupplierMaterialRequest {
  materialId: string;
  supplierUnitCost?: number;
}

export type PurchaseOrderStatus = "DRAFT" | "ORDERED" | "PARTIALLY_RECEIVED" | "RECEIVED" | "CANCELLED";

export const PURCHASE_ORDER_STATUS_LABELS: Record<PurchaseOrderStatus, string> = {
  DRAFT: "Borrador",
  ORDERED: "Ordenada",
  PARTIALLY_RECEIVED: "Recepción parcial",
  RECEIVED: "Recibida",
  CANCELLED: "Cancelada"
};

export interface PurchaseOrderLineResponse {
  id: string;
  materialId: string;
  materialName: string;
  unitOfMeasure: string;
  orderedQuantity: number;
  receivedQuantity: number;
  remainingQuantity: number;
  unitCost: number;
  subtotal: number;
}

export interface PurchaseOrderResponse {
  id: string;
  clinicId: string;
  supplierId: string;
  supplierName: string;
  folio: string;
  status: PurchaseOrderStatus;
  orderDate: string;
  expectedDate?: string;
  notes?: string;
  bankAccountId?: string;
  total: number;
  lines: PurchaseOrderLineResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CreatePurchaseOrderRequest {
  supplierId: string;
  orderDate?: string;
  expectedDate?: string;
  notes?: string;
  bankAccountId?: string;
  lines: Array<{
    materialId: string;
    quantity: number;
    unitCost: number;
  }>;
}

export interface PurchaseReceiptResponse {
  id: string;
  clinicId: string;
  purchaseOrderId: string;
  receivedAt: string;
  notes?: string;
  total: number;
  lines: Array<{
    id: string;
    purchaseOrderLineId: string;
    materialId: string;
    materialName: string;
    quantity: number;
    unitCost: number;
    lotNumber?: string;
    expirationDate?: string;
    inventoryMovementId: string;
    subtotal: number;
  }>;
  createdAt: string;
}

export interface ReceivePurchaseOrderRequest {
  receivedAt?: string;
  notes?: string;
  lines: Array<{
    purchaseOrderLineId: string;
    quantity: number;
    unitCost: number;
    lotNumber?: string;
    expirationDate?: string;
  }>;
}
