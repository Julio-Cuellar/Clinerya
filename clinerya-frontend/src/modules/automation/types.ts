/**
 * Contratos del modulo de automatizacion (asistente de WhatsApp). Las fechas llegan como LocalDateTime
 * del backend, sin zona: "2026-09-29T10:00:00", hora local de la clinica.
 */

export type PromptMode = "DEFAULT" | "CUSTOM";

export interface ChannelSettingsView {
  whatsappPhoneNumberId: string | null;
  whatsappBusinessAccountId: string | null;
  accessTokenHint: string | null;
  appSecretHint: string | null;
  geminiApiKeyHint: string | null;
  geminiModel: string;
  promptMode: PromptMode;
  customPrompt: string | null;
  defaultPrompt: string;
  webhookPath: string;
  /** Solo llega a quien administra integraciones. */
  verifyToken: string | null;
  chatRetentionMonths: number;
  enabled: boolean;
  whatsappVerifiedAt: string | null;
  geminiVerifiedAt: string | null;
  missingToEnable: string[];
  patientTemplateName: string | null;
  doctorTemplateName: string | null;
  templateLanguage: string;
}

export type SecretKind = "WHATSAPP_ACCESS_TOKEN" | "WHATSAPP_APP_SECRET" | "GEMINI_API_KEY";

export interface ConnectionTestResult {
  whatsappOk: boolean;
  whatsappDetail: string;
  geminiOk: boolean;
  geminiDetail: string;
}

export interface DoctorChannelView {
  staffId: string;
  phone: string | null;
  active: boolean;
  consentAt: string | null;
  updatedAt: string | null;
}

export interface DoctorChannelUpdate {
  phone: string;
  consent: boolean;
  active: boolean;
}

export interface Slot {
  start: string;
  end: string;
}

export interface AppointmentRequestView {
  id: string;
  patientId: string;
  patientName: string | null;
  doctorStaffId: string;
  doctorName: string | null;
  start: string;
  end: string;
  status: string;
  createdAt: string;
  expiresAt: string;
  appointmentId: string | null;
  rejectionReason: string | null;
  proposedOptions: Slot[];
}

export interface ChatSummary {
  phone: string;
  patientNames: string[];
  lastMessageAt: string;
  messageCount: number;
  /** Lo atiende una persona de la clinica (el agente calla). */
  humanAttention?: boolean;
  /** Quien lo tomo; null: lo pidio el agente. */
  attentionUserId?: string | null;
  attentionSince?: string | null;
}

/** Quien atiende un chat y hasta cuando WhatsApp permite escribirle texto libre. */
export interface ChatAttention {
  human: boolean;
  since: string | null;
  byUserId: string | null;
  replyUntil: string | null;
}

/** Un chat en pantalla: lo que manda el backend mas si tuvo actividad desde que se vio. */
export interface ChatListItem extends ChatSummary {
  unread: boolean;
}

export interface ChatMessage {
  id: string;
  phone: string;
  direction: "INBOUND" | "OUTBOUND" | "STAFF";
  text: string;
  optionLabels: string[];
  at: string;
  /** Quien del personal lo escribio (solo STAFF). */
  authorUserId?: string | null;
}

export interface ChatAccess {
  id: string;
  phone: string;
  userId: string;
  accessedAt: string;
}

/** Aviso en tiempo real de actividad en un chat: nunca trae el texto. */
export interface ChatActivityPush {
  phone: string;
  at: string;
}

export interface InboxChangePush {
  requestId: string;
  kind: "NEW";
}

export interface DoctorLeadTime {
  doctorStaffId: string;
  doctorName: string;
  minLeadMinutes: number;
}

export interface OnlineBookingSettings {
  slotMinutes: number;
  doctors: DoctorLeadTime[];
}
