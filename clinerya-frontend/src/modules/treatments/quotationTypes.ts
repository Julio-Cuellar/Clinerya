export type QuotationStatus = "DRAFT" | "SENT" | "ACCEPTED" | "REJECTED" | "EXPIRED";

export const QUOTATION_STATUS_LABELS: Record<QuotationStatus, string> = {
  DRAFT: "Borrador",
  SENT: "Enviada",
  ACCEPTED: "Aceptada",
  REJECTED: "Rechazada",
  EXPIRED: "Vencida"
};

export type BadgeTone = "success" | "neutral" | "warning";

/**
 * Tono del badge por estado de cotización. Vive junto a las etiquetas porque antes había tres
 * copias de esta misma decisión repartidas por la app y una de ellas no miraba el estado.
 */
export const QUOTATION_STATUS_BADGES: Record<QuotationStatus, BadgeTone> = {
  DRAFT: "warning",
  SENT: "warning",
  ACCEPTED: "success",
  REJECTED: "neutral",
  EXPIRED: "neutral"
};

export type ItemProgressStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED";

export const ITEM_PROGRESS_STATUS_LABELS: Record<ItemProgressStatus, string> = {
  PENDING: "Pendiente",
  IN_PROGRESS: "En progreso",
  COMPLETED: "Completado"
};

export interface MaterialLineResponse {
  id: string;
  materialId?: string;
  materialName: string;
  estimatedQuantity: number;
  unitCostAtQuote: number;
  estimatedCost: number;
}

export interface MaterialLineRequest {
  materialId?: string;
  materialName?: string;
  estimatedQuantity: number;
  manualUnitCost?: number;
}

export interface QuotationItemResponse {
  id: string;
  catalogItemId?: string;
  description: string;
  toothNumber?: number;
  laborCharge: number;
  materials: MaterialLineResponse[];
  materialsTotal: number;
  discountPercentage?: number;
  progressStatus: ItemProgressStatus;
  subtotal: number;
}

export interface QuotationItemRequest {
  catalogItemId?: string;
  description: string;
  toothNumber?: number;
  laborCharge: number;
  materials: MaterialLineRequest[];
  discountPercentage?: number;
}

export interface QuotationResponse {
  id: string;
  clinicId: string;
  patientId: string;
  createdByUserId: string;
  quotationDate: string;
  status: QuotationStatus;
  notes?: string;
  validUntil?: string;
  items: QuotationItemResponse[];
  grandTotal: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateQuotationRequest {
  clinicId: string;
  quotationDate?: string;
  notes?: string;
  validUntil?: string;
  items: QuotationItemRequest[];
}

export interface UpdateQuotationHeaderRequest {
  clinicId: string;
  notes?: string;
  validUntil?: string;
  quotationDate?: string;
}

export interface ReplaceQuotationItemsRequest {
  clinicId: string;
  items: QuotationItemRequest[];
}

export interface UpdateItemProgressRequest {
  clinicId: string;
  progressStatus: ItemProgressStatus;
}

export interface CreateVisitRequest {
  clinicId: string;
  visitDate?: string;
  doctorId?: string;
  notes?: string;
  items: CreateVisitLineItemRequest[];
}

export interface CreateVisitLineItemRequest {
  quotationItemId?: string;
  materialsUsed: CreateVisitMaterialUsageRequest[];
}

export interface CreateVisitMaterialUsageRequest {
  materialId?: string;
  materialName: string;
  actualQuantity: number;
}

export interface VisitResponse {
  id: string;
  clinicId: string;
  patientId: string;
  quotationId: string;
  visitDate: string;
  doctorId?: string;
  notes?: string;
  items: VisitLineItemResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface VisitLineItemResponse {
  id: string;
  quotationItemId: string;
  materialsUsed: VisitMaterialUsageResponse[];
}

export interface VisitMaterialUsageResponse {
  id: string;
  materialId?: string;
  materialName: string;
  actualQuantity: number;
}
