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

/**
 * Quién es el médico responsable de la clínica y qué papel tiene el titular de la cuenta. Si el
 * titular es el responsable, su cédula es la del responsable; si no, indica si atiende pacientes
 * (con su propia cédula) o solo gestiona la cuenta.
 */
export interface ResponsibleDoctorRequest {
  ownerIsResponsibleDoctor: boolean;
  responsibleDoctorName: string;
  responsibleDoctorProfessionalLicense: string;
  ownerAttendsPatients?: boolean;
  ownerCedulaProfesional?: string;
}

export interface CreateClinicRequest extends ResponsibleDoctorRequest {
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
}

export interface CompleteClinicSetupRequest extends ResponsibleDoctorRequest {
  name: string;
  email?: string;
  timezone?: string;
  legalName?: string;
  rfc?: string;
  taxRegimeCode?: string;
  addressStreet?: string;
  addressColonia?: string;
  addressMunicipality?: string;
  addressState?: string;
  addressZip?: string;
  phone?: string;
  logoUrl?: string;
  cofeprisPermitNumber?: string;
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
