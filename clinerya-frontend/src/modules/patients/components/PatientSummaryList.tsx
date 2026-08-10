import { bloodTypeLabel, genderLabel, maritalStatusLabel } from "@modules/patients/constants/patientOptions";
import type { Address, BloodType, EmergencyContact, Gender, MaritalStatus } from "@modules/patients/types";
import { getAge } from "@shared/utils/getAge";

export interface PatientSummaryData {
  dateOfBirth?: string;
  gender?: Gender;
  curp?: string;
  phone?: string;
  email?: string;
  occupation?: string;
  maritalStatus?: MaritalStatus;
  nationality?: string;
  bloodType?: BloodType;
  address?: Address;
  emergencyContact?: EmergencyContact;
}

export function PatientSummaryList({ patient }: { patient: PatientSummaryData }) {
  const age = getAge(patient.dateOfBirth);
  const address = patient.address;
  const emergencyContact = patient.emergencyContact;
  const addressLine = [address?.street, address?.outdoorNumber, address?.colonia, address?.municipality, address?.state, address?.zipCode]
    .filter(Boolean)
    .join(", ");

  return (
    <div className="clinic-list">
      <div className="clinic-row">
        <strong>Fecha de nacimiento</strong>
        <span>{patient.dateOfBirth || "Sin registrar"}{age !== undefined ? ` (${age} años)` : ""}</span>
      </div>
      <div className="clinic-row">
        <strong>Género</strong>
        <span>{genderLabel(patient.gender) ?? "Sin registrar"}</span>
      </div>
      <div className="clinic-row">
        <strong>CURP</strong>
        <span>{patient.curp || "Sin registrar"}</span>
      </div>
      <div className="clinic-row">
        <strong>Contacto</strong>
        <span>{patient.phone || "Sin teléfono"} · {patient.email || "Sin correo"}</span>
      </div>
      <div className="clinic-row">
        <strong>Ocupación</strong>
        <span>{patient.occupation || "Sin registrar"}</span>
      </div>
      <div className="clinic-row">
        <strong>Estado civil</strong>
        <span>{maritalStatusLabel(patient.maritalStatus) ?? "Sin registrar"}</span>
      </div>
      <div className="clinic-row">
        <strong>Nacionalidad</strong>
        <span>{patient.nationality || "Sin registrar"}</span>
      </div>
      <div className="clinic-row">
        <strong>Tipo de sangre</strong>
        <span>{bloodTypeLabel(patient.bloodType) ?? "Sin registrar"}</span>
      </div>
      <div className="clinic-row">
        <strong>Dirección</strong>
        <span>{addressLine || "Sin registrar"}</span>
      </div>
      <div className="clinic-row">
        <strong>Contacto de emergencia</strong>
        <span>
          {emergencyContact?.fullName
            ? `${emergencyContact.fullName} (${emergencyContact.relationship || "sin parentesco"}) · ${emergencyContact.phone || "sin teléfono"}`
            : "Sin registrar"}
        </span>
      </div>
    </div>
  );
}
