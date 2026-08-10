export type TemplateFieldType =
  | "text"
  | "textarea"
  | "number"
  | "date"
  | "select"
  | "table"
  | "file"
  | "odontogram"
  | "clinic_header"
  | "signature_patient"
  | "signature_doctor";

export type TemplateKind = "historia_clinica" | "complementario";

export type TemplateFontFamily =
  | "helvetica"
  | "arial"
  | "verdana"
  | "tahoma"
  | "trebuchet"
  | "times"
  | "georgia"
  | "garamond"
  | "palatino"
  | "courier"
  | "consolas"
  | "comic-sans"
  | "impact";

export type TemplatePdfFontFamily = "helvetica" | "times" | "courier";

export type TemplateTextAlign = "left" | "center" | "right";

export interface TemplateElement {
  id: string;
  sectionId?: string;
  label: string;
  type: TemplateFieldType;
  options?: string[];
  columns?: string[];
  x: number;
  y: number;
  width: number;
  height: number;
  fontFamily?: TemplateFontFamily;
  fontSize?: number;
  bold?: boolean;
  align?: TemplateTextAlign;
  color?: string;
  backgroundColor?: string;
}

export const DEFAULT_FONT_FAMILY: TemplateFontFamily = "helvetica";
export const DEFAULT_FONT_SIZE = 12;
export const DEFAULT_TEXT_COLOR = "#1a1a1a";
export const DEFAULT_TEXT_ALIGN: TemplateTextAlign = "center";

export const FONT_FAMILY_OPTIONS: Array<{ value: TemplateFontFamily; label: string; cssStack: string; pdfFamily: TemplatePdfFontFamily }> = [
  { value: "helvetica", label: "Helvetica", cssStack: "Helvetica, Arial, sans-serif", pdfFamily: "helvetica" },
  { value: "arial", label: "Arial", cssStack: "Arial, Helvetica, sans-serif", pdfFamily: "helvetica" },
  { value: "verdana", label: "Verdana", cssStack: "Verdana, Geneva, sans-serif", pdfFamily: "helvetica" },
  { value: "tahoma", label: "Tahoma", cssStack: "Tahoma, Geneva, sans-serif", pdfFamily: "helvetica" },
  { value: "trebuchet", label: "Trebuchet MS", cssStack: "'Trebuchet MS', sans-serif", pdfFamily: "helvetica" },
  { value: "times", label: "Times New Roman", cssStack: "'Times New Roman', Times, serif", pdfFamily: "times" },
  { value: "georgia", label: "Georgia", cssStack: "Georgia, 'Times New Roman', serif", pdfFamily: "times" },
  { value: "garamond", label: "Garamond", cssStack: "Garamond, 'Times New Roman', serif", pdfFamily: "times" },
  { value: "palatino", label: "Palatino", cssStack: "'Palatino Linotype', Palatino, serif", pdfFamily: "times" },
  { value: "courier", label: "Courier", cssStack: "'Courier New', Courier, monospace", pdfFamily: "courier" },
  { value: "consolas", label: "Consolas", cssStack: "Consolas, 'Courier New', monospace", pdfFamily: "courier" },
  { value: "comic-sans", label: "Comic Sans MS", cssStack: "'Comic Sans MS', 'Comic Sans', cursive", pdfFamily: "helvetica" },
  { value: "impact", label: "Impact", cssStack: "Impact, 'Arial Narrow', sans-serif", pdfFamily: "helvetica" }
];

export const FONT_SIZE_OPTIONS = [9, 10, 11, 12, 14, 16, 18, 20, 24];

export function fontCssStack(fontFamily: TemplateFontFamily | undefined): string {
  return FONT_FAMILY_OPTIONS.find((option) => option.value === fontFamily)?.cssStack ?? FONT_FAMILY_OPTIONS[0].cssStack;
}

export function fontPdfFamily(fontFamily: TemplateFontFamily | undefined): TemplatePdfFontFamily {
  return FONT_FAMILY_OPTIONS.find((option) => option.value === fontFamily)?.pdfFamily ?? "helvetica";
}

export interface TemplateCompliance {
  overridden: boolean;
  overriddenAt?: string;
  missingSections?: string[];
}

export interface TemplatePage {
  id: string;
  elements: TemplateElement[];
  canvasHeight: number;
}

export interface TemplateSchema {
  kind: TemplateKind;
  pages: TemplatePage[];
  compliance?: TemplateCompliance;
}

export function genPageId(): string {
  return `page_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
}

export function genFieldId(): string {
  return `field_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
}

export interface HistoryTemplateResponse {
  id: string;
  clinicId: string;
  name: string;
  description?: string;
  schemaJson: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateHistoryTemplateRequest {
  name: string;
  description?: string;
  schemaJson: string;
}

export interface UpdateHistoryTemplateRequest {
  name: string;
  description?: string;
  schemaJson: string;
  active: boolean;
}

export interface MedicalHistoryResponse {
  id: string;
  patientId: string;
  clinicId: string;
  templateId: string;
  answersJson: string;
  createdAt: string;
  updatedAt: string;
}

export interface SaveMedicalHistoryRequest {
  clinicId: string;
  templateId: string;
  answersJson: string;
}

const EMPTY_SCHEMA: TemplateSchema = { kind: "complementario", pages: [{ id: genPageId(), elements: [], canvasHeight: 0 }] };

interface LegacyTemplateSchema {
  kind?: TemplateKind;
  elements?: TemplateElement[];
  canvasHeight?: number;
  pages?: TemplatePage[];
  compliance?: TemplateCompliance;
}

export function parseSchema(schemaJson: string | undefined | null): TemplateSchema {
  if (!schemaJson) return { ...EMPTY_SCHEMA };
  try {
    const parsed = JSON.parse(schemaJson) as LegacyTemplateSchema;
    const kind: TemplateKind = parsed.kind === "historia_clinica" ? "historia_clinica" : "complementario";
    const pages: TemplatePage[] = Array.isArray(parsed.pages) && parsed.pages.length > 0
      ? parsed.pages
      : [{ id: genPageId(), elements: Array.isArray(parsed.elements) ? parsed.elements : [], canvasHeight: parsed.canvasHeight ?? 0 }];
    return { kind, pages, compliance: parsed.compliance };
  } catch {
    return { ...EMPTY_SCHEMA };
  }
}

export function serializeSchema(schema: TemplateSchema): string {
  return JSON.stringify(schema);
}

export function parseAnswers(answersJson: string | undefined | null): Record<string, string> {
  if (!answersJson) return {};
  try {
    const parsed = JSON.parse(answersJson) as Record<string, string>;
    return typeof parsed === "object" && parsed !== null ? parsed : {};
  } catch {
    return {};
  }
}

export function serializeAnswers(answers: Record<string, string>): string {
  return JSON.stringify(answers);
}

export type TemplateTableRow = string[];

function emptyTableRow(columnCount: number): TemplateTableRow {
  return new Array(Math.max(1, columnCount)).fill("");
}

export function parseTableRows(raw: string | undefined, columnCount: number): TemplateTableRow[] {
  if (!raw) return [emptyTableRow(columnCount)];
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed) && parsed.length > 0 && parsed.every((row) => Array.isArray(row))) {
      return parsed as TemplateTableRow[];
    }
  } catch {
    // valor previo no es una tabla serializada; se descarta y se arranca vacío
  }
  return [emptyTableRow(columnCount)];
}

export function serializeTableRows(rows: TemplateTableRow[]): string {
  return JSON.stringify(rows);
}

export interface AttachmentMeta {
  id: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  createdAt: string;
}

export function parseAttachments(raw: string | undefined): AttachmentMeta[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? (parsed as AttachmentMeta[]) : [];
  } catch {
    return [];
  }
}

export function serializeAttachments(attachments: AttachmentMeta[]): string {
  return JSON.stringify(attachments);
}

export type NoteStatus = "DRAFT" | "SIGNED";

export interface VitalSigns {
  temperature?: number | null;
  bloodPressure?: string | null;
  heartRate?: number | null;
  respiratoryRate?: number | null;
  weight?: number | null;
  height?: number | null;
  bmi?: number | null;
  oxygenSaturation?: number | null;
}

export interface ClinicalNoteResponse {
  id: string;
  patientId: string;
  clinicId: string;
  doctorId: string;
  subjective?: string;
  objective?: string;
  vitalSigns?: VitalSigns | null;
  assessment?: string;
  plan?: string;
  status: NoteStatus;
  authoredByExternalUserId?: string | null;
  signedAt?: string | null;
  signedByUserId?: string | null;
  documentHash?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ClinicalNoteFields {
  subjective?: string;
  objective?: string;
  temperature?: number;
  bloodPressure?: string;
  heartRate?: number;
  respiratoryRate?: number;
  weight?: number;
  height?: number;
  oxygenSaturation?: number;
  assessment?: string;
  plan?: string;
}

export interface CreateClinicalNoteRequest extends ClinicalNoteFields {
  clinicId: string;
  doctorId: string;
  status: NoteStatus;
}

export interface UpdateClinicalNoteRequest extends ClinicalNoteFields {
  clinicId: string;
}

export interface RecordAccessLogResponse {
  id: string;
  clinicId: string;
  patientId: string;
  userId: string;
  userName: string;
  resourceType: string;
  resourceId?: string | null;
  actionType: "READ" | "WRITE";
  ipAddress?: string | null;
  userAgent?: string | null;
  createdAt: string;
}

export interface RecordAccessLogPageResponse {
  items: RecordAccessLogResponse[];
  nextCursor: string | null;
  hasMore: boolean;
}

export interface MedicalHistoryVersionResponse {
  id: string;
  medicalHistoryId: string;
  version: number;
  answersJson: string;
  changedByUserId: string;
  changedByUserName: string;
  ipAddress?: string | null;
  userAgent?: string | null;
  createdAt: string;
}

export interface PrivacyConsentResponse {
  id: string;
  patientId: string;
  clinicId: string;
  privacyNoticeText: string;
  documentHash: string;
  signerName: string;
  signatureImage: string;
  signatureImageHash: string;
  ipAddress?: string | null;
  userAgent?: string | null;
  signedAt: string;
  createdAt: string;
}

export interface SignPrivacyConsentRequest {
  clinicId: string;
  privacyNoticeText: string;
  signerName: string;
  signatureImage: string;
}

