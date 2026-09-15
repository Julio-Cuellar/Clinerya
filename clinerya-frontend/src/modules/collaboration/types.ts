export type ExternalAccessStatus = "PENDING" | "ACCEPTED" | "REJECTED" | "REVOKED";

export type AccessLevel = "READ_ONLY" | "COMMENT" | "FULL";

export interface ExternalAccessGrantResponse {
  id: string;
  sourceClinicId: string;
  patientId: string;
  patientName?: string;
  invitedByStaffId: string;
  externalUserId: string | null;
  invitedEmail: string;
  accessLevel: AccessLevel;
  status: ExternalAccessStatus;
  createdAt: string;
  respondedAt?: string;
  revokedAt?: string;
}

export interface InviteExternalAccessRequest {
  email: string;
  accessLevel: AccessLevel;
}

export interface TemporaryShareView {
  id: string;
  email: string;
  createdByUserId?: string;
  createdAt: string;
  expiresAt: string;
  lastAccessedAt?: string;
  accessCount: number;
  sections: string[];
}

/** Claves de SharedSection del backend → etiqueta legible. */
export const SHARE_SECTIONS: Record<string, string> = {
  CLINICAL_NOTES: "Notas clínicas",
  MEDICAL_HISTORY: "Historia clínica",
  VITAL_SIGNS: "Signos vitales",
  PRESCRIPTIONS: "Prescripciones",
  STUDIES: "Estudios"
};

/** Secciones ofrecidas al generar un enlace (todas marcadas por defecto). */
export const SHARE_SECTION_OPTIONS: Array<{ key: string; label: string; hint: string }> = [
  { key: "CLINICAL_NOTES", label: "Notas clínicas", hint: "Notas de evolución SOAP" },
  { key: "MEDICAL_HISTORY", label: "Historia clínica", hint: "Formularios y antecedentes" },
  { key: "VITAL_SIGNS", label: "Signos vitales", hint: "Dentro de cada nota" },
  { key: "PRESCRIPTIONS", label: "Prescripciones", hint: "Recetas emitidas" },
  { key: "STUDIES", label: "Estudios", hint: "Rayos X, laboratorios y otros documentos" }
];
