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
