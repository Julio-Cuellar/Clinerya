export interface CatalogMaterialResponse {
  id: string;
  materialId: string;
  materialName: string;
  typicalQuantity?: number;
}

export interface CatalogMaterialRequest {
  materialId: string;
  typicalQuantity?: number;
}

export interface TreatmentCatalogItemResponse {
  id: string;
  clinicId: string;
  name: string;
  category?: string;
  description?: string;
  /** Obligatorio si el precio es fijo; referencia "desde" (puede faltar) si varia por paciente. */
  defaultPrice: number | null;
  estimatedDurationMinutes?: number;
  pricingType: "FIXED" | "VARIES_BY_PATIENT";
  availableInAssistant: boolean;
  materials: CatalogMaterialResponse[];
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SeedCatalogResponse {
  created: number;
  skipped: number;
  items: TreatmentCatalogItemResponse[];
}

export interface CreateTreatmentCatalogItemRequest {
  name: string;
  category?: string;
  description?: string;
  defaultPrice: number | null;
  estimatedDurationMinutes?: number;
  materials: CatalogMaterialRequest[];
  pricingType: "FIXED" | "VARIES_BY_PATIENT";
  availableInAssistant: boolean;
}

export interface UpdateTreatmentCatalogItemRequest extends CreateTreatmentCatalogItemRequest {
  active: boolean;
}
