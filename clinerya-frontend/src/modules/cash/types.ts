export type PaymentMethod = "CASH" | "TRANSFER" | "CARD" | "CHECK";

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: "Efectivo",
  TRANSFER: "Transferencia",
  CARD: "Tarjeta",
  CHECK: "Cheque"
};

export type CashSessionStatus = "OPEN" | "CLOSED";
export type TicketStatus = "ACTIVE" | "VOIDED";

export const TICKET_STATUS_LABELS: Record<TicketStatus, string> = {
  ACTIVE: "Activo",
  VOIDED: "Anulado"
};

export interface CashSessionResponse {
  id: string;
  clinicId: string;
  openedByStaffId: string;
  openedAt: string;
  openingAmount: number;
  closedByStaffId: string | null;
  closedAt: string | null;
  countedCashAmount: number | null;
  expectedCashAmount: number | null;
  cashDifference: number | null;
  status: CashSessionStatus;
}

export interface OpenCashSessionRequest {
  openedByStaffId: string;
  openingAmount: number;
}

export interface CloseCashSessionRequest {
  closedByStaffId: string;
  countedCashAmount: number;
}

export interface PaymentLineRequest {
  method: PaymentMethod;
  amount: number;
  reference?: string;
  bankAccountId?: string;
}

export interface PaymentLineResponse {
  id: string;
  method: PaymentMethod;
  amount: number;
  reference: string | null;
  bankAccountId: string | null;
}

export interface RegisterTicketRequest {
  patientId: string;
  quotationId?: string;
  concept?: string;
  createdByStaffId: string;
  paymentLines: PaymentLineRequest[];
  discountAmount?: number;
  discountAuthorizedByStaffId?: string;
  discountReason?: string;
}

export interface TicketResponse {
  id: string;
  clinicId: string;
  cashSessionId: string;
  patientId: string;
  quotationId: string | null;
  folio: number;
  totalAmount: number;
  concept: string | null;
  createdByStaffId: string;
  createdAt: string;
  status: TicketStatus;
  voidedByStaffId: string | null;
  voidedAt: string | null;
  voidReason: string | null;
  discountAmount: number | null;
  discountAuthorizedByStaffId: string | null;
  discountReason: string | null;
  paymentLines: PaymentLineResponse[];
}

export interface VoidTicketRequest {
  staffId: string;
  reason: string;
}

export interface QuotationBalanceResponse {
  quotationId: string;
  grandTotal: number;
  paidAmount: number;
  remainingBalance: number;
}

export interface PendingAppointmentChargeResponse {
  appointmentId: string;
  patientId: string;
  doctorStaffId: string;
  quotationId: string;
  quotationItemId: string;
  concept: string;
  amount: number;
  completedAt: string;
}

export interface RegisterCashExpenseRequest {
  concept: string;
  amount: number;
  createdByStaffId: string;
}

export interface CashExpenseResponse {
  id: string;
  clinicId: string;
  cashSessionId: string;
  concept: string;
  amount: number;
  createdByStaffId: string;
  createdAt: string;
  status: TicketStatus;
  voidedByStaffId: string | null;
  voidedAt: string | null;
  voidReason: string | null;
}

export interface VoidCashExpenseRequest {
  staffId: string;
  reason: string;
}
