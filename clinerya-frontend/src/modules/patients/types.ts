export type Gender = "MALE" | "FEMALE" | "OTHER";

export type MaritalStatus = "SINGLE" | "MARRIED" | "DIVORCED" | "WIDOWED" | "COHABITATION";

export type BloodType =
  | "O_POSITIVE"
  | "O_NEGATIVE"
  | "A_POSITIVE"
  | "A_NEGATIVE"
  | "B_POSITIVE"
  | "B_NEGATIVE"
  | "AB_POSITIVE"
  | "AB_NEGATIVE";

export interface Address {
  street?: string;
  outdoorNumber?: string;
  indoorNumber?: string;
  colonia?: string;
  municipality?: string;
  state?: string;
  zipCode?: string;
}

export interface EmergencyContact {
  fullName?: string;
  relationship?: string;
  phone?: string;
}

export type ConsentSource = "CLINIC_REGISTRATION" | "CLINIC_UPDATE" | "WHATSAPP_CHAT";

/** Decision del paciente sobre mensajes automaticos. recordedAt vacio = nunca se le pregunto. */
export interface ContactConsentResponse {
  granted: boolean;
  textVersion?: string;
  source?: ConsentSource;
  recordedByUserId?: string;
  recordedAt?: string;
}

export interface ContactConsentRequest {
  granted: boolean;
  textVersion?: string;
}

export interface ContactConsentTextResponse {
  version: string;
  text: string;
}

export interface PatientResponse {
  id: string;
  clinicId: string;
  firstName: string;
  lastNamePaterno: string;
  lastNameMaterno?: string;
  curp?: string;
  dateOfBirth?: string;
  gender?: Gender;
  phone?: string;
  email?: string;
  occupation?: string;
  maritalStatus?: MaritalStatus;
  nationality?: string;
  bloodType?: BloodType;
  address?: Address;
  emergencyContact?: EmergencyContact;
  contactConsent?: ContactConsentResponse;
  createdAt: string;
  updatedAt: string;
}

export interface RegisterPatientRequest {
  clinicId: string;
  firstName: string;
  lastNamePaterno: string;
  lastNameMaterno?: string;
  curp?: string;
  dateOfBirth?: string;
  gender?: Gender;
  phone?: string;
  email?: string;
  occupation?: string;
  maritalStatus?: MaritalStatus;
  nationality?: string;
  bloodType?: BloodType;
  address?: Address;
  emergencyContact?: EmergencyContact;
  contactConsent?: ContactConsentRequest;
}

export interface UpdatePatientRequest {
  firstName: string;
  lastNamePaterno: string;
  lastNameMaterno?: string;
  curp?: string;
  dateOfBirth?: string;
  gender?: Gender;
  phone?: string;
  email?: string;
  occupation?: string;
  maritalStatus?: MaritalStatus;
  nationality?: string;
  bloodType?: BloodType;
  address?: Address;
  emergencyContact?: EmergencyContact;
}
