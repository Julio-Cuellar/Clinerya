export type ClinicSpecialty =
  | "SIN_CONFIGURAR"
  | "ODONTOLOGIA"
  | "MEDICINA_GENERAL"
  | "NUTRICION"
  | "FISIOTERAPIA"
  | "OFTALMOLOGIA"
  | "OTRA";

export interface ClinicResponse {
  id: string;
  organizationId?: string;
  ownerUserId: string;
  name: string;
  legalName?: string;
  rfc?: string;
  taxRegimeCode?: string;
  addressStreet?: string;
  addressColonia?: string;
  addressMunicipality?: string;
  addressState?: string;
  addressZip?: string;
  phone?: string;
  email?: string;
  logoUrl?: string;
  timezone?: string;
  specialty?: ClinicSpecialty;
  privacyNoticeUrl?: string;
  cofeprisPermitNumber?: string;
  responsibleDoctorName?: string;
  responsibleDoctorProfessionalLicense?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateClinicSpecialtyRequest {
  specialty: ClinicSpecialty;
}

export interface CreateClinicRequest {
  name: string;
  email: string;
  timezone: string;
  legalName?: string;
  rfc?: string;
  taxRegimeCode?: string;
  addressStreet?: string;
  addressColonia?: string;
  addressMunicipality?: string;
  addressState?: string;
  addressZip?: string;
  phone?: string;
  cofeprisPermitNumber?: string;
  responsibleDoctorName?: string;
  responsibleDoctorProfessionalLicense?: string;
}

export interface UpdateClinicRequest {
  name: string;
  legalName?: string;
  rfc?: string;
  taxRegimeCode?: string;
  addressStreet?: string;
  addressColonia?: string;
  addressMunicipality?: string;
  addressState?: string;
  addressZip?: string;
  phone?: string;
  email?: string;
  logoUrl?: string;
  timezone?: string;
  privacyNoticeUrl?: string;
  cofeprisPermitNumber?: string;
  responsibleDoctorName?: string;
  responsibleDoctorProfessionalLicense?: string;
}

export interface ClinicRoomStaffAssignmentResponse {
  id: string;
  clinicId: string;
  roomId: string;
  staffId: string;
  active: boolean;
  assignedAt: string;
  createdAt: string;
  updatedAt: string;
}
