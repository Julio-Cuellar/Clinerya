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
  defaultPrice: number;
  estimatedDurationMinutes?: number;
  materials: CatalogMaterialResponse[];
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTreatmentCatalogItemRequest {
  name: string;
  category?: string;
  description?: string;
  defaultPrice: number;
  estimatedDurationMinutes?: number;
  materials: CatalogMaterialRequest[];
}

export interface UpdateTreatmentCatalogItemRequest extends CreateTreatmentCatalogItemRequest {
  active: boolean;
}
