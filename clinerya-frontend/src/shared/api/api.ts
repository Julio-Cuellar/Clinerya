import type {
  ApiErrorBody,
  LoginRequest,
  LoginResponse,
  MessageResponse,
  RefreshResponse,
  RegisterRequest,
  UserProfile
} from "@modules/auth/types";
import type {
  ClinicResponse,
  ClinicRoomStaffAssignmentResponse,
  CreateClinicRequest,
  UpdateClinicRequest
} from "@modules/clinics/types";
import type { PatientResponse, RegisterPatientRequest, UpdatePatientRequest } from "@modules/patients/types";
import type {
  AttachmentMeta,
  ClinicalNoteResponse,
  ClinicalNoteAddendumResponse,
  CreateClinicalNoteRequest,
  CreateHistoryTemplateRequest,
  HistoryTemplateResponse,
  MedicalHistoryResponse,
  SaveMedicalHistoryRequest,
  UpdateClinicalNoteRequest,
  UpdateHistoryTemplateRequest,
  MedicalHistoryVersionResponse,
  RecordAccessLogResponse,
  RecordAccessLogPageResponse,
  PrivacyConsentResponse,
  SignPrivacyConsentRequest,
  PatientClinicalSummaryResponse,
  PatientAllergyDto,
  PatientConditionDto,
  PatientMedicationDto,
  AllergyInput,
  ConditionInput,
  MedicationInput
} from "@modules/records/types";
import type { IssuePrescriptionRequest, Prescription } from "@modules/records/prescriptionTypes";
import type {
  CreateTreatmentCatalogItemRequest,
  TreatmentCatalogItemResponse,
  UpdateTreatmentCatalogItemRequest
} from "@modules/treatments/types";
import type {
  CreateQuotationRequest,
  ItemProgressStatus,
  QuotationResponse,
  QuotationStatus,
  ReplaceQuotationItemsRequest,
  UpdateQuotationHeaderRequest,
  CreateVisitRequest,
  VisitResponse
} from "@modules/treatments/quotationTypes";
import type {
  BatchResponse,
  AddSupplierMaterialRequest,
  CreateMaterialRequest,
  GeneralLedgerEntryResponse,
  InventoryMovementResponse,
  MaterialResponse,
  MaterialReservationResponse,
  CreatePurchaseOrderRequest,
  PurchaseOrderResponse,
  PurchaseReceiptResponse,
  ReceivePurchaseOrderRequest,
  RegisterInventoryMovementRequest,
  SaveSupplierRequest,
  SupplierMaterialResponse,
  SupplierResponse,
  UpdateMaterialRequest
} from "@modules/inventory/types";
import type {
  AppointmentResponse,
  AppointmentStatus,
  ClinicRoomResponse,
  CreateAppointmentRequest,
  CreateAppointmentSeriesRequest,
  DayScheduleRequest,
  DayScheduleResponse,
  DoctorResponse,
  RescheduleAppointmentRequest,
  RoomBlockResponse,
  RoomBlockType,
  WaitingListEntryResponse,
  AddToWaitingListRequest,
  WaitingListStatus
} from "@modules/agenda/types";
import type {
  CashExpenseResponse,
  CashSessionResponse,
  CloseCashSessionRequest,
  OpenCashSessionRequest,
  QuotationBalanceResponse,
  PendingAppointmentChargeResponse,
  RegisterCashExpenseRequest,
  RegisterTicketRequest,
  TicketResponse,
  VoidCashExpenseRequest,
  VoidTicketRequest
} from "@modules/cash/types";
import type {
  ExternalAccessGrantResponse,
  InviteExternalAccessRequest,
  TemporaryShareView
} from "@modules/collaboration/types";
import type { NotificationListResponse } from "@modules/notifications/types";
import type {
  BankAccountMovementResponse,
  BankAccountResponse,
  CreditAccountAlertResponse,
  CorrectBankAccountBalanceRequest,
  CreateBankAccountRequest,
  CreateJournalEntryRequest,
  CreateOpeningBalanceSetupRequest,
  DeactivateBankAccountRequest,
  IncomeStatementResponse,
  JournalEntryResponse,
  JournalQueryResponse,
  TrialBalanceResponse,
  WasteReportResponse,
  OpeningBalanceSetupResponse,
  TransferFundsRequest,
  UpdateBankAccountRequest
} from "@modules/accounting/types";
import type {
  AuthorizationUrlResponse,
  CalendarConnectionStatusResponse,
  ExternalCalendarEventResponse
} from "@modules/agenda/integrationsTypes";

const API_BASE_URL = import.meta.env.VITE_API_URL ?? "/api";
const ACCESS_TOKEN_KEY = "clinicloud.access_token";
const USER_KEY = "clinicloud.user";

export class ApiClientError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
  }
}

// El refresh token ya no pasa por aqui: vive solo en una cookie HttpOnly que el
// navegador manda solo, y que este codigo no puede leer ni escribir.
export const sessionStore = {
  getAccessToken: () => localStorage.getItem(ACCESS_TOKEN_KEY),
  getUser: (): UserProfile | null => {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as UserProfile) : null;
  },
  setTokens: (payload: { token: string }) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, payload.token);
  },
  setUser: (user: UserProfile) => localStorage.setItem(USER_KEY, JSON.stringify(user)),
  clear: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }
};

async function request<T>(path: string, options: RequestInit = {}, retry = true): Promise<T> {
  const headers = new Headers(options.headers);
  if (!headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const token = sessionStore.getAccessToken();
  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    // Necesario para que la cookie del refresh token viaje en /auth/refresh y /auth/logout.
    credentials: "include"
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const payload = await response.json().catch(() => null);

  if (!response.ok) {
    // Ya no se puede saber desde JS si hay un refresh token (vive en una cookie
    // HttpOnly): se intenta siempre y, si no hay cookie valida, el backend responde
    // 400 y el catch limpia la sesion.
    if (response.status === 401 && retry && path !== "/v1/auth/refresh") {
      try {
        const refreshed = await authApi.refresh();
        sessionStore.setTokens(refreshed);
        return request<T>(path, options, false);
      } catch {
        sessionStore.clear();
      }
    }
    const apiError = payload as ApiErrorBody | null;
    throw new ApiClientError(apiError?.message ?? "No se pudo completar la solicitud.", response.status);
  }

  return payload as T;
}

export const authApi = {
  register: (body: RegisterRequest) =>
    request<UserProfile>("/v1/users/register", { method: "POST", body: JSON.stringify(body) }),
  verifyEmail: (email: string, token: string) =>
    request<MessageResponse>("/v1/users/verify-email", { method: "POST", body: JSON.stringify({ email, token }) }),
  resendVerification: (email: string) =>
    request<MessageResponse>("/v1/users/resend-verification", {
      method: "POST",
      body: JSON.stringify({ email })
    }),
  requestPasswordReset: (email: string) =>
    request<MessageResponse>("/v1/users/password-reset/request", {
      method: "POST",
      body: JSON.stringify({ email })
    }),
  confirmPasswordReset: (token: string, newPassword: string) =>
    request<MessageResponse>("/v1/users/password-reset/confirm", {
      method: "POST",
      body: JSON.stringify({ token, newPassword })
    }),
  login: (body: LoginRequest) =>
    request<LoginResponse>("/v1/auth/login", { method: "POST", body: JSON.stringify(body) }),
  // El refresh token va en la cookie HttpOnly; el navegador la manda solo.
  refresh: () => request<RefreshResponse>("/v1/auth/refresh", { method: "POST" }, false),
  // El backend lee la misma cookie para invalidar el refresh token y borrarla; si no,
  // sobrevive al logout y sigue emitiendo tokens de acceso durante toda su vigencia.
  logout: () => request<void>("/v1/auth/logout", { method: "POST" }, false),
  me: () => request<UserProfile>("/v1/auth/me"),
  changePassword: (body: any) =>
    request<{ message: string }>("/v1/users/change-password", { method: "POST", body: JSON.stringify(body) }),
  updateTheme: (theme: "LIGHT" | "DARK") =>
    request<{ message: string }>("/v1/users/theme", { method: "PUT", body: JSON.stringify({ theme }) })
};

export const clinicsApi = {
  list: () => request<ClinicResponse[]>("/v1/clinics"),
  get: (clinicId: string) => request<ClinicResponse>(`/v1/clinics/${clinicId}`),
  create: (body: CreateClinicRequest) =>
    request<ClinicResponse>("/v1/clinics", { method: "POST", body: JSON.stringify(body) }),
  update: (clinicId: string, body: UpdateClinicRequest) =>
    request<ClinicResponse>(`/v1/clinics/${clinicId}`, { method: "PUT", body: JSON.stringify(body) })
};

export const patientsApi = {
  listByClinic: (clinicId: string) =>
    request<PatientResponse[]>(`/v1/patients?clinicId=${encodeURIComponent(clinicId)}`),
  get: (patientId: string) => request<PatientResponse>(`/v1/patients/${patientId}`),
  register: (body: RegisterPatientRequest) =>
    request<PatientResponse>("/v1/patients", { method: "POST", body: JSON.stringify(body) }),
  update: (patientId: string, body: UpdatePatientRequest) =>
    request<PatientResponse>(`/v1/patients/${patientId}`, { method: "PUT", body: JSON.stringify(body) }),
  remove: (patientId: string) => request<void>(`/v1/patients/${patientId}`, { method: "DELETE" })
};

export const historyTemplatesApi = {
  list: (clinicId: string, patientId?: string) =>
    request<HistoryTemplateResponse[]>(
      `/v1/clinics/${clinicId}/history-templates${patientId ? `?patientId=${patientId}` : ""}`
    ),
  create: (clinicId: string, body: CreateHistoryTemplateRequest) =>
    request<HistoryTemplateResponse>(`/v1/clinics/${clinicId}/history-templates`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  update: (clinicId: string, templateId: string, body: UpdateHistoryTemplateRequest) =>
    request<HistoryTemplateResponse>(`/v1/clinics/${clinicId}/history-templates/${templateId}`, {
      method: "PUT",
      body: JSON.stringify(body)
    }),
  remove: (clinicId: string, templateId: string) =>
    request<void>(`/v1/clinics/${clinicId}/history-templates/${templateId}`, { method: "DELETE" })
};

export const medicalHistoryApi = {
  listByPatient: (patientId: string, clinicId: string) =>
    request<MedicalHistoryResponse[]>(`/v1/patients/${patientId}/medical-history?clinicId=${encodeURIComponent(clinicId)}`),
  getByTemplate: (patientId: string, templateId: string, clinicId: string) =>
    request<MedicalHistoryResponse>(
      `/v1/patients/${patientId}/medical-history/by-template/${templateId}?clinicId=${encodeURIComponent(clinicId)}`
    ),
  save: (patientId: string, body: SaveMedicalHistoryRequest) =>
    request<MedicalHistoryResponse>(`/v1/patients/${patientId}/medical-history`, {
      method: "PUT",
      body: JSON.stringify(body)
    }),
  listVersions: (patientId: string, templateId: string, clinicId: string) =>
    request<MedicalHistoryVersionResponse[]>(
      `/v1/patients/${patientId}/medical-history/by-template/${templateId}/versions?clinicId=${encodeURIComponent(clinicId)}`
    ),
  getVersion: (patientId: string, templateId: string, versionNumber: number, clinicId: string) =>
    request<MedicalHistoryVersionResponse>(
      `/v1/patients/${patientId}/medical-history/by-template/${templateId}/versions/${versionNumber}?clinicId=${encodeURIComponent(clinicId)}`
    )
};

export const recordAccessLogsApi = {
  listByPatient: (
    patientId: string,
    clinicId: string,
    options: { cursor?: string | null; limit?: number } = {}
  ) => {
    const params = new URLSearchParams({
      clinicId,
      limit: String(options.limit ?? 50)
    });
    if (options.cursor) {
      params.set("cursor", options.cursor);
    }
    return request<RecordAccessLogPageResponse>(`/v1/patients/${patientId}/access-logs?${params.toString()}`);
  }
};

export const privacyConsentApi = {
  get: (patientId: string, clinicId: string) =>
    request<PrivacyConsentResponse>(`/v1/patients/${patientId}/privacy-consent?clinicId=${encodeURIComponent(clinicId)}`),
  save: (patientId: string, body: SignPrivacyConsentRequest) =>
    request<PrivacyConsentResponse>(`/v1/patients/${patientId}/privacy-consent`, {
      method: "POST",
      body: JSON.stringify(body)
    })
};

export const clinicalNotesApi = {
  listByPatient: (patientId: string, clinicId: string) =>
    request<ClinicalNoteResponse[]>(`/v1/patients/${patientId}/clinical-notes?clinicId=${encodeURIComponent(clinicId)}`),
  create: (patientId: string, body: CreateClinicalNoteRequest) =>
    request<ClinicalNoteResponse>(`/v1/patients/${patientId}/clinical-notes`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  update: (patientId: string, noteId: string, body: UpdateClinicalNoteRequest) =>
    request<ClinicalNoteResponse>(`/v1/patients/${patientId}/clinical-notes/${noteId}`, {
      method: "PUT",
      body: JSON.stringify(body)
    }),
  sign: (patientId: string, noteId: string, clinicId: string) =>
    request<ClinicalNoteResponse>(
      `/v1/patients/${patientId}/clinical-notes/${noteId}/sign?clinicId=${encodeURIComponent(clinicId)}`,
      { method: "PATCH" }
    ),
  listAddenda: (patientId: string, noteId: string, clinicId: string) =>
    request<ClinicalNoteAddendumResponse[]>(
      `/v1/patients/${patientId}/clinical-notes/${noteId}/addenda?clinicId=${encodeURIComponent(clinicId)}`
    ),
  addAddendum: (patientId: string, noteId: string, body: { clinicId: string; content: string }) =>
    request<ClinicalNoteAddendumResponse>(`/v1/patients/${patientId}/clinical-notes/${noteId}/addenda`, {
      method: "POST",
      body: JSON.stringify(body)
    })
};

export const prescriptionsApi = {
  listByPatient: (clinicId: string, patientId: string) =>
    request<Prescription[]>(`/v1/clinics/${clinicId}/prescriptions/patient/${patientId}`),
  issue: (clinicId: string, body: IssuePrescriptionRequest) =>
    request<Prescription>(`/v1/clinics/${clinicId}/prescriptions`, {
      method: "POST",
      body: JSON.stringify(body)
    })
};

const summaryBase = (patientId: string, clinicId: string) =>
  `/v1/patients/${patientId}/clinical-summary?clinicId=${encodeURIComponent(clinicId)}`;
const summarySub = (patientId: string, clinicId: string, path: string) =>
  `/v1/patients/${patientId}/clinical-summary/${path}?clinicId=${encodeURIComponent(clinicId)}`;

export const clinicalSummaryApi = {
  get: (patientId: string, clinicId: string) =>
    request<PatientClinicalSummaryResponse>(summaryBase(patientId, clinicId)),

  setReview: (
    patientId: string,
    clinicId: string,
    kind: "allergies" | "conditions" | "medications",
    noneReported: boolean
  ) =>
    request<void>(summarySub(patientId, clinicId, `${kind}/review`), {
      method: "PUT",
      body: JSON.stringify({ noneReported })
    }),

  addAllergy: (patientId: string, clinicId: string, body: AllergyInput) =>
    request<PatientAllergyDto>(summarySub(patientId, clinicId, "allergies"), {
      method: "POST",
      body: JSON.stringify(body)
    }),
  updateAllergy: (patientId: string, allergyId: string, clinicId: string, body: AllergyInput) =>
    request<PatientAllergyDto>(summarySub(patientId, clinicId, `allergies/${allergyId}`), {
      method: "PUT",
      body: JSON.stringify(body)
    }),
  removeAllergy: (patientId: string, allergyId: string, clinicId: string) =>
    request<void>(summarySub(patientId, clinicId, `allergies/${allergyId}`), { method: "DELETE" }),

  addCondition: (patientId: string, clinicId: string, body: ConditionInput) =>
    request<PatientConditionDto>(summarySub(patientId, clinicId, "conditions"), {
      method: "POST",
      body: JSON.stringify(body)
    }),
  updateCondition: (patientId: string, conditionId: string, clinicId: string, body: ConditionInput) =>
    request<PatientConditionDto>(summarySub(patientId, clinicId, `conditions/${conditionId}`), {
      method: "PUT",
      body: JSON.stringify(body)
    }),
  removeCondition: (patientId: string, conditionId: string, clinicId: string) =>
    request<void>(summarySub(patientId, clinicId, `conditions/${conditionId}`), { method: "DELETE" }),

  addMedication: (patientId: string, clinicId: string, body: MedicationInput) =>
    request<PatientMedicationDto>(summarySub(patientId, clinicId, "medications"), {
      method: "POST",
      body: JSON.stringify(body)
    }),
  updateMedication: (patientId: string, medicationId: string, clinicId: string, body: MedicationInput) =>
    request<PatientMedicationDto>(summarySub(patientId, clinicId, `medications/${medicationId}`), {
      method: "PUT",
      body: JSON.stringify(body)
    }),
  removeMedication: (patientId: string, medicationId: string, clinicId: string) =>
    request<void>(summarySub(patientId, clinicId, `medications/${medicationId}`), { method: "DELETE" })
};

export const treatmentCatalogApi = {
  list: (clinicId: string, includeInactive = false) =>
    request<TreatmentCatalogItemResponse[]>(
      `/v1/clinics/${clinicId}/treatment-catalog?includeInactive=${includeInactive}`
    ),
  get: (clinicId: string, itemId: string) =>
    request<TreatmentCatalogItemResponse>(`/v1/clinics/${clinicId}/treatment-catalog/${itemId}`),
  create: (clinicId: string, body: CreateTreatmentCatalogItemRequest) =>
    request<TreatmentCatalogItemResponse>(`/v1/clinics/${clinicId}/treatment-catalog`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  update: (clinicId: string, itemId: string, body: UpdateTreatmentCatalogItemRequest) =>
    request<TreatmentCatalogItemResponse>(`/v1/clinics/${clinicId}/treatment-catalog/${itemId}`, {
      method: "PUT",
      body: JSON.stringify(body)
    }),
  remove: (clinicId: string, itemId: string) =>
    request<void>(`/v1/clinics/${clinicId}/treatment-catalog/${itemId}`, { method: "DELETE" })
};

export const quotationsApi = {
  listByPatient: (patientId: string, clinicId: string) =>
    request<QuotationResponse[]>(`/v1/patients/${patientId}/quotations?clinicId=${encodeURIComponent(clinicId)}`),
  get: (patientId: string, quotationId: string, clinicId: string) =>
    request<QuotationResponse>(
      `/v1/patients/${patientId}/quotations/${quotationId}?clinicId=${encodeURIComponent(clinicId)}`
    ),
  create: (patientId: string, body: CreateQuotationRequest) =>
    request<QuotationResponse>(`/v1/patients/${patientId}/quotations`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  updateHeader: (patientId: string, quotationId: string, body: UpdateQuotationHeaderRequest) =>
    request<QuotationResponse>(`/v1/patients/${patientId}/quotations/${quotationId}`, {
      method: "PUT",
      body: JSON.stringify(body)
    }),
  replaceItems: (patientId: string, quotationId: string, body: ReplaceQuotationItemsRequest) =>
    request<QuotationResponse>(`/v1/patients/${patientId}/quotations/${quotationId}/items`, {
      method: "PUT",
      body: JSON.stringify(body)
    }),
  transitionStatus: (patientId: string, quotationId: string, clinicId: string, targetStatus: QuotationStatus) =>
    request<QuotationResponse>(`/v1/patients/${patientId}/quotations/${quotationId}/status`, {
      method: "PATCH",
      body: JSON.stringify({ clinicId, targetStatus })
    }),
  updateItemProgress: (
    patientId: string,
    quotationId: string,
    itemId: string,
    clinicId: string,
    progressStatus: ItemProgressStatus
  ) =>
    request<QuotationResponse>(`/v1/patients/${patientId}/quotations/${quotationId}/items/${itemId}/progress`, {
      method: "PATCH",
      body: JSON.stringify({ clinicId, progressStatus })
    }),
};

export const visitsApi = {
  listByQuotation: (patientId: string, quotationId: string, clinicId: string) =>
    request<VisitResponse[]>(
      `/v1/patients/${patientId}/quotations/${quotationId}/visits?clinicId=${encodeURIComponent(clinicId)}`
    ),
  get: (patientId: string, quotationId: string, visitId: string, clinicId: string) =>
    request<VisitResponse>(
      `/v1/patients/${patientId}/quotations/${quotationId}/visits/${visitId}?clinicId=${encodeURIComponent(clinicId)}`
    ),
  create: (patientId: string, quotationId: string, body: CreateVisitRequest) =>
    request<VisitResponse>(`/v1/patients/${patientId}/quotations/${quotationId}/visits`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  createGeneral: (patientId: string, body: CreateVisitRequest) =>
    request<VisitResponse>(`/v1/patients/${patientId}/visits`, {
      method: "POST",
      body: JSON.stringify(body)
    })
};

export const materialsApi = {
  list: (clinicId: string, includeInactive = false) =>
    request<MaterialResponse[]>(`/v1/clinics/${clinicId}/materials?includeInactive=${includeInactive}`),
  get: (clinicId: string, materialId: string) =>
    request<MaterialResponse>(`/v1/clinics/${clinicId}/materials/${materialId}`),
  create: (clinicId: string, body: CreateMaterialRequest) =>
    request<MaterialResponse>(`/v1/clinics/${clinicId}/materials`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  update: (clinicId: string, materialId: string, body: UpdateMaterialRequest) =>
    request<MaterialResponse>(`/v1/clinics/${clinicId}/materials/${materialId}`, {
      method: "PUT",
      body: JSON.stringify(body)
    }),
  remove: (clinicId: string, materialId: string) =>
    request<void>(`/v1/clinics/${clinicId}/materials/${materialId}`, { method: "DELETE" })
};

export const inventoryReservationsApi = {
  listActive: (clinicId: string) =>
    request<MaterialReservationResponse[]>(`/v1/clinics/${clinicId}/material-reservations`)
};

export const inventoryMovementsApi = {
  list: (clinicId: string, materialId: string, page = 0, size = 25) =>
    request<InventoryMovementResponse[]>(
      `/v1/clinics/${clinicId}/materials/${materialId}/movements?page=${page}&size=${size}`
    ),
  register: (clinicId: string, materialId: string, body: RegisterInventoryMovementRequest) =>
    request<InventoryMovementResponse>(`/v1/clinics/${clinicId}/materials/${materialId}/movements`, {
      method: "POST",
      body: JSON.stringify(body)
    })
};

export const inventoryBatchesApi = {
  list: (clinicId: string, materialId: string) =>
    request<BatchResponse[]>(`/v1/clinics/${clinicId}/materials/${materialId}/batches`),
  regularizeExpired: (clinicId: string, asOfDate?: string) =>
    request<InventoryMovementResponse[]>(
      `/v1/clinics/${clinicId}/inventory-batches/regularize-expired${asOfDate ? `?asOfDate=${asOfDate}` : ""}`,
      { method: "POST" }
    )
};

export const inventoryLedgerApi = {
  list: (clinicId: string, page = 0, size = 100) =>
    request<GeneralLedgerEntryResponse[]>(
      `/v1/clinics/${clinicId}/inventory-movements?page=${page}&size=${size}`
    )
};

export const inventorySuppliersApi = {
  list: (clinicId: string) => request<SupplierResponse[]>(`/v1/clinics/${clinicId}/suppliers`),
  create: (clinicId: string, body: SaveSupplierRequest) =>
    request<SupplierResponse>(`/v1/clinics/${clinicId}/suppliers`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  update: (clinicId: string, supplierId: string, body: SaveSupplierRequest) =>
    request<SupplierResponse>(`/v1/clinics/${clinicId}/suppliers/${supplierId}`, {
      method: "PUT",
      body: JSON.stringify(body)
    }),
  listMaterials: (clinicId: string, supplierId: string) =>
    request<SupplierMaterialResponse[]>(`/v1/clinics/${clinicId}/suppliers/${supplierId}/materials`),
  addMaterial: (clinicId: string, supplierId: string, body: AddSupplierMaterialRequest) =>
    request<SupplierMaterialResponse>(`/v1/clinics/${clinicId}/suppliers/${supplierId}/materials`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  removeMaterial: (clinicId: string, supplierId: string, materialId: string) =>
    request<void>(`/v1/clinics/${clinicId}/suppliers/${supplierId}/materials/${materialId}`, {
      method: "DELETE"
    })
};

export const purchaseOrdersApi = {
  list: (clinicId: string) => request<PurchaseOrderResponse[]>(`/v1/clinics/${clinicId}/purchase-orders`),
  get: (clinicId: string, orderId: string) =>
    request<PurchaseOrderResponse>(`/v1/clinics/${clinicId}/purchase-orders/${orderId}`),
  create: (clinicId: string, body: CreatePurchaseOrderRequest) =>
    request<PurchaseOrderResponse>(`/v1/clinics/${clinicId}/purchase-orders`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  markOrdered: (clinicId: string, orderId: string) =>
    request<PurchaseOrderResponse>(`/v1/clinics/${clinicId}/purchase-orders/${orderId}/ordered`, {
      method: "PATCH"
    }),
  cancel: (clinicId: string, orderId: string) =>
    request<PurchaseOrderResponse>(`/v1/clinics/${clinicId}/purchase-orders/${orderId}/cancel`, {
      method: "PATCH"
    }),
  listReceipts: (clinicId: string, orderId: string) =>
    request<PurchaseReceiptResponse[]>(`/v1/clinics/${clinicId}/purchase-orders/${orderId}/receipts`),
  receive: (clinicId: string, orderId: string, body: ReceivePurchaseOrderRequest) =>
    request<PurchaseReceiptResponse>(`/v1/clinics/${clinicId}/purchase-orders/${orderId}/receipts`, {
      method: "POST",
      body: JSON.stringify(body)
    })
};

export const agendaApi = {
  listByRange: (clinicId: string, from: string, to: string) =>
    request<AppointmentResponse[]>(
      `/v1/clinics/${clinicId}/appointments?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`
    ),
  get: (clinicId: string, appointmentId: string) =>
    request<AppointmentResponse>(`/v1/clinics/${clinicId}/appointments/${appointmentId}`),
  listByQuotation: (clinicId: string, quotationId: string) =>
    request<AppointmentResponse[]>(`/v1/clinics/${clinicId}/appointments/by-quotation/${quotationId}`),
  listByPatient: (clinicId: string, patientId: string) =>
    request<AppointmentResponse[]>(`/v1/clinics/${clinicId}/appointments/by-patient/${patientId}`),
  create: (clinicId: string, body: CreateAppointmentRequest) =>
    request<AppointmentResponse>(`/v1/clinics/${clinicId}/appointments`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  createSeries: (clinicId: string, body: CreateAppointmentSeriesRequest) =>
    request<AppointmentResponse[]>(`/v1/clinics/${clinicId}/appointments/series`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  reschedule: (clinicId: string, appointmentId: string, body: RescheduleAppointmentRequest) =>
    request<AppointmentResponse>(`/v1/clinics/${clinicId}/appointments/${appointmentId}/reschedule`, {
      method: "PATCH",
      body: JSON.stringify(body)
    }),
  transitionStatus: (clinicId: string, appointmentId: string, status: AppointmentStatus, cancellationReason?: string) =>
    request<AppointmentResponse>(`/v1/clinics/${clinicId}/appointments/${appointmentId}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status, cancellationReason })
    }),
  listDoctors: (clinicId: string) => request<DoctorResponse[]>(`/v1/clinics/${clinicId}/doctors`),
  listWithoutPatient: (clinicId: string) =>
    request<AppointmentResponse[]>(`/v1/clinics/${clinicId}/appointments/without-patient`),
  assignPatient: (clinicId: string, appointmentId: string, patientId?: string) =>
    request<AppointmentResponse>(`/v1/clinics/${clinicId}/appointments/${appointmentId}/assign-patient`, {
      method: "PATCH",
      body: JSON.stringify({ patientId })
    }),
  deleteAppointment: (clinicId: string, appointmentId: string) =>
    request<void>(`/v1/clinics/${clinicId}/appointments/${appointmentId}`, {
      method: "DELETE"
    }),
  listRoomBlocks: (clinicId: string, from: string, to: string) =>
    request<RoomBlockResponse[]>(
      `/v1/clinics/${clinicId}/room-blocks?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`
    ),
  createRoomBlock: (
    clinicId: string,
    body: { roomId: string; startsAt: string; endsAt: string; type?: RoomBlockType; reason?: string }
  ) =>
    request<RoomBlockResponse>(`/v1/clinics/${clinicId}/room-blocks`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  deactivateRoomBlock: (clinicId: string, blockId: string) =>
    request<void>(`/v1/clinics/${clinicId}/room-blocks/${blockId}/deactivate`, {
      method: "PATCH"
    })
};

export const clinicRoomsApi = {
  getRooms: (clinicId: string, activeOnly = false) =>
    request<ClinicRoomResponse[]>(`/v1/clinics/${clinicId}/rooms?activeOnly=${activeOnly}`),
  createRoom: (clinicId: string, body: { name: string; code?: string; colorHex?: string; description?: string }) =>
    request<ClinicRoomResponse>(`/v1/clinics/${clinicId}/rooms`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  updateRoom: (clinicId: string, roomId: string, body: { name?: string; code?: string; colorHex?: string; description?: string }) =>
    request<ClinicRoomResponse>(`/v1/clinics/${clinicId}/rooms/${roomId}`, {
      method: "PUT",
      body: JSON.stringify(body)
    }),
  deactivateRoom: (clinicId: string, roomId: string) =>
    request<void>(`/v1/clinics/${clinicId}/rooms/${roomId}/deactivate`, {
      method: "PATCH"
    }),
  activateRoom: (clinicId: string, roomId: string) =>
    request<void>(`/v1/clinics/${clinicId}/rooms/${roomId}/activate`, {
      method: "PATCH"
    }),
  listAssignedStaff: (clinicId: string, roomId: string) =>
    request<ClinicRoomStaffAssignmentResponse[]>(`/v1/clinics/${clinicId}/rooms/${roomId}/staff`),
  assignStaff: (clinicId: string, roomId: string, staffId: string) =>
    request<ClinicRoomStaffAssignmentResponse>(`/v1/clinics/${clinicId}/rooms/${roomId}/staff/${staffId}`, {
      method: "PUT"
    }),
  unassignStaff: (clinicId: string, roomId: string, staffId: string) =>
    request<void>(`/v1/clinics/${clinicId}/rooms/${roomId}/staff/${staffId}`, {
      method: "DELETE"
    })
};

export const waitingListApi = {
  getWaitingList: (clinicId: string, waitingOnly = true) =>
    request<WaitingListEntryResponse[]>(`/clinics/${clinicId}/waiting-list?waitingOnly=${waitingOnly}`),
  addToWaitingList: (clinicId: string, body: AddToWaitingListRequest) =>
    request<WaitingListEntryResponse>(`/clinics/${clinicId}/waiting-list`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  updateStatus: (clinicId: string, entryId: string, status: WaitingListStatus) =>
    request<WaitingListEntryResponse>(`/clinics/${clinicId}/waiting-list/${entryId}/status?status=${status}`, {
      method: "PATCH"
    }),
  deleteEntry: (clinicId: string, entryId: string) =>
    request<void>(`/clinics/${clinicId}/waiting-list/${entryId}`, {
      method: "DELETE"
    })
};

export const clinicScheduleApi = {
  get: (clinicId: string) => request<DayScheduleResponse[]>(`/v1/clinics/${clinicId}/schedule`),
  update: (clinicId: string, body: DayScheduleRequest[]) =>
    request<DayScheduleResponse[]>(`/v1/clinics/${clinicId}/schedule`, {
      method: "PUT",
      body: JSON.stringify(body)
    })
};

export const cashSessionsApi = {
  getCurrent: (clinicId: string) => request<CashSessionResponse | null>(`/v1/clinics/${clinicId}/cash-sessions/current`),
  open: (clinicId: string, body: OpenCashSessionRequest) =>
    request<CashSessionResponse>(`/v1/clinics/${clinicId}/cash-sessions`, { method: "POST", body: JSON.stringify(body) }),
  close: (clinicId: string, body: CloseCashSessionRequest) =>
    request<CashSessionResponse>(`/v1/clinics/${clinicId}/cash-sessions/close`, { method: "POST", body: JSON.stringify(body) }),
  list: (clinicId: string) => request<CashSessionResponse[]>(`/v1/clinics/${clinicId}/cash-sessions`),
  get: (clinicId: string, sessionId: string) => request<CashSessionResponse>(`/v1/clinics/${clinicId}/cash-sessions/${sessionId}`)
};

export const ticketsApi = {
  register: (clinicId: string, body: RegisterTicketRequest) =>
    request<TicketResponse>(`/v1/clinics/${clinicId}/tickets`, { method: "POST", body: JSON.stringify(body) }),
  get: (clinicId: string, ticketId: string) => request<TicketResponse>(`/v1/clinics/${clinicId}/tickets/${ticketId}`),
  listBySession: (clinicId: string, cashSessionId: string) =>
    request<TicketResponse[]>(`/v1/clinics/${clinicId}/tickets/by-session/${cashSessionId}`),
  listByRange: (clinicId: string, from: string, to: string) =>
    request<TicketResponse[]>(`/v1/clinics/${clinicId}/tickets?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`),
  listByPatient: (clinicId: string, patientId: string) =>
    request<TicketResponse[]>(`/v1/clinics/${clinicId}/tickets/by-patient/${patientId}`),
  void: (clinicId: string, ticketId: string, body: VoidTicketRequest) =>
    request<TicketResponse>(`/v1/clinics/${clinicId}/tickets/${ticketId}/void`, { method: "POST", body: JSON.stringify(body) }),
  getQuotationBalance: (clinicId: string, quotationId: string, patientId: string) =>
    request<QuotationBalanceResponse>(
      `/v1/clinics/${clinicId}/tickets/quotation-balance/${quotationId}?patientId=${encodeURIComponent(patientId)}`
    ),
  listPendingAppointmentCharges: (clinicId: string) =>
    request<PendingAppointmentChargeResponse[]>(`/v1/clinics/${clinicId}/tickets/pending-appointment-charges`)
};

export const cashExpensesApi = {
  register: (clinicId: string, body: RegisterCashExpenseRequest) =>
    request<CashExpenseResponse>(`/v1/clinics/${clinicId}/cash-expenses`, { method: "POST", body: JSON.stringify(body) }),
  get: (clinicId: string, expenseId: string) => request<CashExpenseResponse>(`/v1/clinics/${clinicId}/cash-expenses/${expenseId}`),
  listBySession: (clinicId: string, cashSessionId: string) =>
    request<CashExpenseResponse[]>(`/v1/clinics/${clinicId}/cash-expenses/by-session/${cashSessionId}`),
  void: (clinicId: string, expenseId: string, body: VoidCashExpenseRequest) =>
    request<CashExpenseResponse>(`/v1/clinics/${clinicId}/cash-expenses/${expenseId}/void`, { method: "POST", body: JSON.stringify(body) })
};

export const attachmentsApi = {
  list: (patientId: string, clinicId: string, elementId: string) =>
    request<AttachmentMeta[]>(
      `/v1/patients/${patientId}/attachments?clinicId=${encodeURIComponent(clinicId)}&elementId=${encodeURIComponent(elementId)}`
    ),
  upload: async (patientId: string, clinicId: string, elementId: string, file: File): Promise<AttachmentMeta> => {
    const formData = new FormData();
    formData.append("file", file);
    const headers = new Headers();
    const token = sessionStore.getAccessToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
    const response = await fetch(
      `${API_BASE_URL}/v1/patients/${patientId}/attachments?clinicId=${encodeURIComponent(clinicId)}&elementId=${encodeURIComponent(elementId)}`,
      { method: "POST", headers, body: formData }
    );
    const payload = await response.json().catch(() => null);
    if (!response.ok) {
      const apiError = payload as ApiErrorBody | null;
      throw new ApiClientError(apiError?.message ?? "No se pudo subir el archivo.", response.status);
    }
    return payload as AttachmentMeta;
  },
  remove: (patientId: string, attachmentId: string) =>
    request<void>(`/v1/patients/${patientId}/attachments/${attachmentId}`, { method: "DELETE" }),
  downloadBlob: async (patientId: string, attachmentId: string): Promise<Blob> => {
    const headers = new Headers();
    const token = sessionStore.getAccessToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
    const response = await fetch(`${API_BASE_URL}/v1/patients/${patientId}/attachments/${attachmentId}/content`, { headers });
    if (!response.ok) throw new ApiClientError("No se pudo descargar el archivo.", response.status);
    return response.blob();
  }
};

export const collaborationApi = {
  invite: (clinicId: string, patientId: string, body: InviteExternalAccessRequest) =>
    request<ExternalAccessGrantResponse>(`/v1/clinics/${clinicId}/patients/${patientId}/external-access`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  listByClinic: (clinicId: string) =>
    request<ExternalAccessGrantResponse[]>(`/v1/clinics/${clinicId}/external-access`),
  revoke: (clinicId: string, grantId: string) =>
    request<void>(`/v1/clinics/${clinicId}/external-access/${grantId}`, { method: "DELETE" }),
  listMine: () => request<ExternalAccessGrantResponse[]>("/v1/me/external-access"),
  accept: (grantId: string) =>
    request<ExternalAccessGrantResponse>(`/v1/me/external-access/${grantId}/accept`, { method: "POST" }),
  reject: (grantId: string) =>
    request<ExternalAccessGrantResponse>(`/v1/me/external-access/${grantId}/reject`, { method: "POST" }),
  createTemporaryShare: (
    clinicId: string,
    patientId: string,
    body: { email: string; daysValid: number; sections?: string[] }
  ) =>
    request<{ token: string; expiresAt: string }>(`/v1/clinics/${clinicId}/patients/${patientId}/temporary-shares`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  getSharedRecord: (token: string) =>
    request<any>(`/v1/public/shared-history`, {
      method: "POST",
      body: JSON.stringify({ token })
    }),
  requestShareVerification: (token: string) =>
    request<void>(`/v1/public/shared-history/verify/request`, {
      method: "POST",
      body: JSON.stringify({ token })
    }),
  confirmShareVerification: (token: string, code: string) =>
    request<void>(`/v1/public/shared-history/verify/confirm`, {
      method: "POST",
      body: JSON.stringify({ token, code })
    }),
  getSharedStudyContent: async (token: string, attachmentId: string): Promise<Blob> => {
    const response = await fetch(`${API_BASE_URL}/v1/public/shared-history/studies/${attachmentId}/content`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token })
    });
    if (!response.ok) throw new ApiClientError("No se pudo descargar el estudio.", response.status);
    return response.blob();
  },
  listTemporaryShares: (clinicId: string, patientId: string) =>
    request<TemporaryShareView[]>(`/v1/clinics/${clinicId}/patients/${patientId}/temporary-shares`),
  revokeTemporaryShare: (clinicId: string, patientId: string, shareId: string) =>
    request<void>(`/v1/clinics/${clinicId}/patients/${patientId}/temporary-shares/${shareId}`, {
      method: "DELETE"
    })
};

export const staffApi = {
  list: (clinicId: string, role?: string) =>
    request<ClinicStaffResponse[]>(`/v1/clinics/${clinicId}/staff${role ? `?role=${role}` : ""}`),
  add: (clinicId: string, body: AddStaffRequest) =>
    request<ClinicStaffResponse>(`/v1/clinics/${clinicId}/staff`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  invite: (clinicId: string, body: AddStaffRequest) =>
    request<StaffInvitationResponse>(`/v1/clinics/${clinicId}/staff/invitations`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  setInvitationCompensation: (clinicId: string, invitationId: string, body: StaffCompensationRequest | null) =>
    request<void>(`/v1/clinics/${clinicId}/staff/onboarding/invitations/${invitationId}/compensation`, {
      method: "PUT",
      body: JSON.stringify(body ?? {})
    }),
  listInvitations: (clinicId: string) =>
    request<StaffInvitationResponse[]>(`/v1/clinics/${clinicId}/staff/invitations`),
  registerStaff: (body: RegisterStaffInvitationRequest) =>
    request<void>(`/v1/users/register-staff`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  update: (clinicId: string, staffId: string, body: { role: string }) =>
    request<ClinicStaffResponse>(`/v1/clinics/${clinicId}/staff/${staffId}`, {
      method: "PUT",
      body: JSON.stringify(body)
    }),
  getPermissions: (clinicId: string, staffId: string) =>
    request<StaffPermissionSummary>(`/v1/clinics/${clinicId}/staff/${staffId}/permissions`),
  updatePermissions: (clinicId: string, staffId: string, permissions: StaffPermissionChange[]) =>
    request<StaffPermissionSummary>(`/v1/clinics/${clinicId}/staff/${staffId}/permissions`, {
      method: "PUT",
      body: JSON.stringify({ permissions })
    }),
  remove: (clinicId: string, staffId: string) =>
    request<void>(`/v1/clinics/${clinicId}/staff/${staffId}`, {
      method: "DELETE"
    }),
  clockIn: (clinicId: string, body: ClockInRequest) =>
    request<StaffAttendanceResponse>(`/v1/clinics/${clinicId}/staff/operations/attendance/clock-in`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  clockOut: (clinicId: string, attendanceId: string, body: ClockOutRequest) =>
    request<StaffAttendanceResponse>(`/v1/clinics/${clinicId}/staff/operations/attendance/${attendanceId}/clock-out`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  listAttendance: (clinicId: string, params: { staffId?: string; from?: string; to?: string } = {}) => {
    const query = new URLSearchParams();
    if (params.staffId) query.set("staffId", params.staffId);
    if (params.from) query.set("from", params.from);
    if (params.to) query.set("to", params.to);
    return request<StaffAttendanceResponse[]>(
      `/v1/clinics/${clinicId}/staff/operations/attendance${query.size ? `?${query.toString()}` : ""}`
    );
  },
  recordActivity: (clinicId: string, body: StaffActivityRequest) =>
    request<StaffActivityResponse>(`/v1/clinics/${clinicId}/staff/operations/activities`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  listActivities: (clinicId: string, params: { staffId?: string; type?: string; from?: string; to?: string } = {}) => {
    const query = new URLSearchParams();
    if (params.staffId) query.set("staffId", params.staffId);
    if (params.type) query.set("type", params.type);
    if (params.from) query.set("from", params.from);
    if (params.to) query.set("to", params.to);
    return request<StaffActivityResponse[]>(
      `/v1/clinics/${clinicId}/staff/operations/activities${query.size ? `?${query.toString()}` : ""}`
    );
  },
  createPayrollPeriod: (clinicId: string, body: PayrollPeriodRequest) =>
    request<StaffPayrollPeriodResponse>(`/v1/clinics/${clinicId}/staff/operations/payroll/periods`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  listPayrollPeriods: (clinicId: string) =>
    request<StaffPayrollPeriodResponse[]>(`/v1/clinics/${clinicId}/staff/operations/payroll/periods`),
  upsertPayrollLine: (clinicId: string, periodId: string, staffId: string, body: PayrollLineRequest) =>
    request<StaffPayrollLineResponse>(`/v1/clinics/${clinicId}/staff/operations/payroll/periods/${periodId}/lines/${staffId}`, {
      method: "PUT",
      body: JSON.stringify(body)
    }),
  deletePayrollLine: (clinicId: string, periodId: string, staffId: string) =>
    request<StaffPayrollPeriodResponse>(`/v1/clinics/${clinicId}/staff/operations/payroll/periods/${periodId}/lines/${staffId}`, {
      method: "DELETE"
    }),
  listPayrollLines: (clinicId: string, periodId: string) =>
    request<StaffPayrollLineResponse[]>(`/v1/clinics/${clinicId}/staff/operations/payroll/periods/${periodId}/lines`),
  generatePayrollLines: (clinicId: string, periodId: string, source: PayrollLineSource) =>
    request<StaffPayrollLineResponse[]>(
      `/v1/clinics/${clinicId}/staff/operations/payroll/periods/${periodId}/lines/generate?source=${source}`,
      { method: "POST" }
    ),
  previewPeriodCommissions: (clinicId: string, periodId: string) =>
    request<CommissionPreviewEntry[]>(
      `/v1/clinics/${clinicId}/staff/operations/payroll/periods/${periodId}/commission-preview`
    ),
  applyPeriodCommissions: (clinicId: string, periodId: string) =>
    request<StaffPayrollLineResponse[]>(
      `/v1/clinics/${clinicId}/staff/operations/payroll/periods/${periodId}/lines/apply-commissions`,
      { method: "POST" }
    ),
  closePayrollPeriod: (clinicId: string, periodId: string) =>
    request<StaffPayrollPeriodResponse>(`/v1/clinics/${clinicId}/staff/operations/payroll/periods/${periodId}/close`, {
      method: "POST"
    }),
  payPayrollPeriod: (clinicId: string, periodId: string, body: PayrollPaymentRequest) =>
    request<StaffPayrollPeriodResponse>(`/v1/clinics/${clinicId}/staff/operations/payroll/periods/${periodId}/pay`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  listStaffCompensation: (clinicId: string) =>
    request<StaffCompensationResponse[]>(`/v1/clinics/${clinicId}/staff/compensation`),
  getStaffCompensation: (clinicId: string, staffId: string) =>
    request<StaffCompensationResponse>(`/v1/clinics/${clinicId}/staff/${staffId}/compensation`),
  updateStaffCompensation: (clinicId: string, staffId: string, body: StaffCompensationRequest) =>
    request<StaffCompensationResponse>(`/v1/clinics/${clinicId}/staff/${staffId}/compensation`, {
      method: "PUT",
      body: JSON.stringify(body)
    })
};

export interface ClinicStaffResponse {
  staffId: string;
  clinicId: string;
  userId: string;
  role: string;
  fullName: string;
}

export type StaffPermission =
  | "VIEW_DASHBOARD"
  | "VIEW_DASHBOARD_METRICS"
  | "VIEW_OPERATIONAL_ALERTS"
  | "MANAGE_AGENDA"
  | "VIEW_AGENDA"
  | "CREATE_APPOINTMENTS"
  | "EDIT_APPOINTMENTS"
  | "CANCEL_APPOINTMENTS"
  | "MANAGE_WAITING_LIST"
  | "MANAGE_SCHEDULES"
  | "MANAGE_ROOMS"
  | "VIEW_ROOMS"
  | "CREATE_ROOMS"
  | "EDIT_ROOMS"
  | "ASSIGN_ROOM_STAFF"
  | "VIEW_PATIENT_CARE"
  | "MANAGE_PATIENT_CARE"
  | "CREATE_VISITS"
  | "EDIT_VISITS"
  | "MANAGE_PRESCRIPTIONS"
  | "MANAGE_PATIENTS"
  | "VIEW_PATIENTS"
  | "CREATE_PATIENTS"
  | "EDIT_PATIENTS"
  | "EXPORT_PATIENTS"
  | "VIEW_MEDICAL_RECORDS"
  | "EDIT_MEDICAL_RECORDS"
  | "CREATE_CLINICAL_NOTES"
  | "EDIT_CLINICAL_NOTES"
  | "VIEW_RECORD_DOCUMENTS"
  | "MANAGE_RECORD_TEMPLATES"
  | "MANAGE_TREATMENTS"
  | "VIEW_TREATMENTS"
  | "MANAGE_TREATMENT_CATALOG"
  | "MANAGE_QUOTATIONS"
  | "MANAGE_PROCEDURES"
  | "REGISTER_TREATMENT_SALES"
  | "MANAGE_CASH"
  | "VIEW_CASH"
  | "CREATE_CHARGES"
  | "MANAGE_REFUNDS"
  | "MANAGE_CASH_CUTS"
  | "MANAGE_EXPENSES"
  | "MANAGE_INVENTORY"
  | "VIEW_INVENTORY"
  | "MANAGE_MATERIALS"
  | "MANAGE_PURCHASES"
  | "MANAGE_INVENTORY_MOVEMENTS"
  | "MANAGE_INVENTORY_LOTS"
  | "VIEW_ACCOUNTING"
  | "MANAGE_ACCOUNTING"
  | "VIEW_JOURNAL_ENTRIES"
  | "CREATE_JOURNAL_ENTRIES"
  | "VIEW_FINANCIAL_REPORTS"
  | "MANAGE_BANK_ACCOUNTS"
  | "MANAGE_STAFF"
  | "VIEW_STAFF"
  | "MANAGE_STAFF_PERMISSIONS"
  | "MANAGE_ATTENDANCE"
  | "MANAGE_STAFF_ACTIVITY"
  | "MANAGE_CLINIC"
  | "VIEW_CLINIC_SETTINGS"
  | "MANAGE_INTEGRATIONS"
  | "MANAGE_CLINIC_BACKUPS"
  | "MANAGE_PAYROLL"
  | "VIEW_PAYROLL";

export type StaffPermissionOverrideState = "INHERIT" | "GRANTED" | "REVOKED";

export interface StaffPermissionItem {
  permission: StaffPermission;
  overrideState: StaffPermissionOverrideState;
  enabled: boolean;
}

export interface StaffPermissionSummary {
  staffId: string;
  role: string;
  permissions: StaffPermissionItem[];
}

export interface StaffPermissionChange {
  permission: StaffPermission;
  state: StaffPermissionOverrideState;
}

export interface AddStaffRequest {
  email: string;
  role: string;
}

export interface StaffInvitationResponse {
  invitationId: string;
  clinicId: string;
  email: string;
  role: string;
  token: string;
  used: boolean;
  expiresAt: string;
}

export interface RegisterStaffInvitationRequest {
  token: string;
  fullName: string;
  password: string;
}

export type StaffActivityType = "TREATMENT" | "PROCEDURE" | "SALE" | "ADMINISTRATIVE" | "NOTE";
export type StaffAttendanceStatus = "OPEN" | "CLOSED";
export type StaffPayrollPeriodStatus = "DRAFT" | "CLOSED";
export type StaffPayrollPaymentStatus = "UNPAID" | "PAID";

export interface StaffAttendanceResponse {
  id: string;
  clinicId: string;
  staffId: string;
  workDate: string;
  clockInAt: string;
  clockOutAt: string | null;
  status: StaffAttendanceStatus;
  notes?: string | null;
}

export interface ClockInRequest {
  staffId: string;
  clockInAt?: string;
  notes?: string;
}

export interface ClockOutRequest {
  clockOutAt?: string;
  notes?: string;
}

export interface StaffActivityRequest {
  staffId: string;
  type: StaffActivityType;
  referenceType?: string;
  referenceId?: string;
  description?: string;
  amount?: number;
  occurredAt?: string;
}

export interface StaffActivityResponse {
  id: string;
  clinicId: string;
  staffId: string;
  type: StaffActivityType;
  referenceType?: string | null;
  referenceId?: string | null;
  description?: string | null;
  amount: number;
  occurredAt: string;
}

export interface PayrollPeriodRequest {
  name: string;
  periodStart: string;
  periodEnd: string;
}

export interface StaffPayrollPeriodResponse {
  id: string;
  clinicId: string;
  name: string;
  periodStart: string;
  periodEnd: string;
  status: StaffPayrollPeriodStatus;
  paymentStatus: StaffPayrollPaymentStatus;
  grossAmount: number;
  netAmount: number;
  closedAt: string | null;
  paidAt: string | null;
  paymentAccountId: string | null;
  paymentJournalEntryId: string | null;
}

export interface PayrollPaymentRequest {
  bankAccountId: string;
  paymentDate?: string;
}

export interface PayrollLineRequest {
  baseSalary?: number;
  commissionAmount?: number;
  bonusAmount?: number;
  deductionAmount?: number;
  notes?: string;
}

export interface StaffPayrollLineResponse {
  id: string;
  clinicId: string;
  payrollPeriodId: string;
  staffId: string;
  baseSalary: number;
  commissionAmount: number;
  bonusAmount: number;
  deductionAmount: number;
  grossAmount: number;
  netAmount: number;
  notes?: string | null;
}

export type StaffPayFrequency = "WEEKLY" | "BIWEEKLY" | "MONTHLY";
export type StaffPaymentMethod = "BANK_TRANSFER" | "CASH";
export type PayrollLineSource = "BASE_COMPENSATION" | "PREVIOUS_PERIOD";

export interface CommissionPreviewEntry {
  staffId: string;
  activityTotal: number;
  currentCommission: number;
}

export interface StaffCompensationRequest {
  baseSalary: number;
  payFrequency: StaffPayFrequency;
  paymentMethod: StaffPaymentMethod;
  paymentAccountClabe?: string | null;
  rfc?: string | null;
  curp?: string | null;
  nss?: string | null;
}

export interface StaffCompensationResponse {
  staffId: string;
  clinicId: string;
  baseSalary: number;
  payFrequency: StaffPayFrequency;
  paymentMethod: StaffPaymentMethod;
  paymentAccountClabe?: string | null;
  rfc?: string | null;
  curp?: string | null;
  nss?: string | null;
}

export const accountingApi = {
  listJournalEntries: (clinicId: string) =>
    request<JournalEntryResponse[]>(`/v1/clinics/${clinicId}/accounting/journal-entries`),
  queryJournalEntries: (clinicId: string, params: { from?: string; to?: string; search?: string; sourceEventType?: string; page?: number; size?: number }) => {
    const query = new URLSearchParams();
    if (params.from) query.set("from", params.from);
    if (params.to) query.set("to", params.to);
    if (params.search?.trim()) query.set("search", params.search.trim());
    if (params.sourceEventType && params.sourceEventType !== "ALL") query.set("sourceEventType", params.sourceEventType);
    if (params.page !== undefined) query.set("page", String(params.page));
    if (params.size !== undefined) query.set("size", String(params.size));
    const suffix = query.toString() ? `?${query.toString()}` : "";
    return request<JournalQueryResponse>(`/v1/clinics/${clinicId}/accounting/journal-entries/query${suffix}`);
  },
  createJournalEntry: (clinicId: string, body: CreateJournalEntryRequest) =>
    request<JournalEntryResponse>(`/v1/clinics/${clinicId}/accounting/journal-entries`, {
      method: "POST",
      body: JSON.stringify(body)
      }),
  getIncomeStatement: (clinicId: string, from: string, to: string, compare = true) =>
    request<IncomeStatementResponse>(
      `/v1/clinics/${clinicId}/accounting/reports/income-statement?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&compare=${compare}`
    ),
  getOpeningBalances: (clinicId: string) =>
    request<OpeningBalanceSetupResponse | null>(`/v1/clinics/${clinicId}/accounting/opening-balances`),
  createOpeningBalances: (clinicId: string, body: CreateOpeningBalanceSetupRequest) =>
    request<OpeningBalanceSetupResponse>(`/v1/clinics/${clinicId}/accounting/opening-balances`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  listBankAccounts: (clinicId: string) =>
    request<BankAccountResponse[]>(`/v1/clinics/${clinicId}/accounting/bank-accounts`),
  listCreditAlerts: (clinicId: string, withinDays = 7) =>
    request<CreditAccountAlertResponse[]>(
      `/v1/clinics/${clinicId}/accounting/credit-alerts?withinDays=${withinDays}`
    ),
  createBankAccount: (clinicId: string, body: CreateBankAccountRequest) =>
    request<BankAccountResponse>(`/v1/clinics/${clinicId}/accounting/bank-accounts`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  updateBankAccount: (clinicId: string, bankAccountId: string, body: UpdateBankAccountRequest) =>
    request<BankAccountResponse>(`/v1/clinics/${clinicId}/accounting/bank-accounts/${bankAccountId}`, {
      method: "PUT",
      body: JSON.stringify(body)
    }),
  correctBankAccountBalance: (clinicId: string, bankAccountId: string, body: CorrectBankAccountBalanceRequest) =>
    request<JournalEntryResponse>(`/v1/clinics/${clinicId}/accounting/bank-accounts/${bankAccountId}/corrections`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  deactivateBankAccount: (clinicId: string, bankAccountId: string, body: DeactivateBankAccountRequest) =>
    request<BankAccountResponse>(`/v1/clinics/${clinicId}/accounting/bank-accounts/${bankAccountId}/deactivate`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  listBankAccountMovements: (clinicId: string, bankAccountId: string) =>
    request<BankAccountMovementResponse[]>(
      `/v1/clinics/${clinicId}/accounting/bank-accounts/${bankAccountId}/movements`
    ),
  getTrialBalance: (clinicId: string, from: string, to: string) =>
    request<TrialBalanceResponse>(
      `/v1/clinics/${clinicId}/accounting/reports/trial-balance?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`
    ),
  getWasteReport: (clinicId: string, from: string, to: string) =>
    request<WasteReportResponse>(
      `/v1/clinics/${clinicId}/accounting/reports/waste?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`
    ),
  transferFunds: (clinicId: string, body: TransferFundsRequest) =>
    request<JournalEntryResponse>(`/v1/clinics/${clinicId}/accounting/account-transfers`, {
      method: "POST",
      body: JSON.stringify(body)
    })
};

export const notificationsApi = {
  list: (clinicId: string, unreadOnly = false, limit = 50) =>
    request<NotificationListResponse>(
      `/v1/clinics/${clinicId}/notifications?unreadOnly=${unreadOnly}&limit=${limit}`
    ),
  markRead: (clinicId: string, notificationId: string) =>
    request<void>(`/v1/clinics/${clinicId}/notifications/${notificationId}/read`, { method: "POST" }),
  markAllRead: (clinicId: string) =>
    request<void>(`/v1/clinics/${clinicId}/notifications/read-all`, { method: "POST" })
};

export const integrationsApi = {
  getGoogleCalendarStatus: (clinicId: string, staffId: string) =>
    request<{ connected: boolean; email?: string; importPastEvents: boolean }>(
      `/v1/clinics/${clinicId}/staff/${staffId}/google-calendar/status`
    ),
  updateGoogleCalendarPreferences: (clinicId: string, staffId: string, body: { importPastEvents: boolean }) =>
    request<void>(`/v1/clinics/${clinicId}/staff/${staffId}/google-calendar/preferences`, {
      method: "PATCH",
      body: JSON.stringify(body)
    }),
  getGoogleCalendarAuthorizationUrl: (clinicId: string, staffId: string, importPastEvents?: boolean) =>
    request<AuthorizationUrlResponse>(
      `/v1/clinics/${clinicId}/staff/${staffId}/google-calendar/connect?importPastEvents=${!!importPastEvents}`
    ),


  disconnectGoogleCalendar: (clinicId: string, staffId: string) =>
    request<void>(`/v1/clinics/${clinicId}/staff/${staffId}/google-calendar`, {
      method: "DELETE"
    }),
  listExternalEvents: (clinicId: string, from: string, to: string) =>
    request<ExternalCalendarEventResponse[]>(
      `/v1/clinics/${clinicId}/external-calendar-events?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`
    ),
  linkExternalEvent: (clinicId: string, eventId: string, body: { patientId?: string; reason: string }) =>
    request<void>(`/v1/clinics/${clinicId}/external-calendar-events/${eventId}/link`, {
      method: "POST",
      body: JSON.stringify(body)
    }),

  dismissExternalEvent: (clinicId: string, eventId: string) =>
    request<void>(`/v1/clinics/${clinicId}/external-calendar-events/${eventId}/dismiss`, {
      method: "POST"
    })
};

export const systemConfigsApi = {
  getConfig: (key: string) =>
    request<{ key: string; value: string; description?: string }>(`/v1/system-configs/${key}`),
  updateConfig: (key: string, value: string, description?: string) =>
    request<{ key: string; value: string; description?: string }>(`/v1/system-configs/${key}`, {
      method: "PUT",
      body: JSON.stringify({ value, description })
    }),
  downloadBackup: async (): Promise<Blob> => {
    const headers = new Headers();
    const token = sessionStore.getAccessToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
    const response = await fetch(`${API_BASE_URL}/v1/system-configs/backups/trigger`, { method: "POST", headers });
    if (!response.ok) {
      const payload = await response.json().catch(() => null);
      throw new ApiClientError(payload?.message ?? "No se pudo descargar el respaldo.", response.status);
    }
    return response.blob();
  }
};

export function getFriendlyError(error: unknown) {
  if (error instanceof ApiClientError) {
    if (error.message.includes("DataIntegrityViolationException") && error.message.toLowerCase().includes("phone")) {
      return "El teléfono es obligatorio.";
    }
    return error.message;
  }
  return "Ocurrió un error inesperado.";
}
