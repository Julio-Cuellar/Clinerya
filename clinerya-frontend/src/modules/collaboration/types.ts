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
