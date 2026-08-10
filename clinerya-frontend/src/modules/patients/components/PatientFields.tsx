import { bloodTypeOptions, genderOptions, maritalStatusOptions } from "@modules/patients/constants/patientOptions";
import type { PatientResponse } from "@modules/patients/types";
import { Field } from "@shared/ui/Field";

export function PatientFields({ patient }: { patient?: PatientResponse }) {
  const address = patient?.address;
  const emergencyContact = patient?.emergencyContact;

  return (
    <>
      <Field name="firstName" label="Nombre(s)" defaultValue={patient?.firstName} required />
      <Field name="lastNamePaterno" label="Apellido paterno" defaultValue={patient?.lastNamePaterno} required />
      <Field name="lastNameMaterno" label="Apellido materno" defaultValue={patient?.lastNameMaterno} required />
      <Field name="dateOfBirth" label="Fecha de nacimiento" type="date" defaultValue={patient?.dateOfBirth} required />
      <Field name="gender" label="Género" options={genderOptions} defaultValue={patient?.gender} required />
      <Field name="curp" label="CURP" defaultValue={patient?.curp} />
      <Field name="phone" label="Teléfono" defaultValue={patient?.phone} required />
      <Field name="email" label="Correo electrónico" type="email" defaultValue={patient?.email} />
      <Field name="occupation" label="Ocupación" defaultValue={patient?.occupation} />
      <Field name="maritalStatus" label="Estado civil" options={maritalStatusOptions} defaultValue={patient?.maritalStatus} />
      <Field name="nationality" label="Nacionalidad" defaultValue={patient?.nationality ?? "Mexicana"} />
      <Field name="bloodType" label="Tipo de sangre" options={bloodTypeOptions} defaultValue={patient?.bloodType} />

      <Field name="addressStreet" label="Calle y número" defaultValue={address?.street} />
      <Field name="addressOutdoorNumber" label="Número exterior" defaultValue={address?.outdoorNumber} />
      <Field name="addressIndoorNumber" label="Número interior" defaultValue={address?.indoorNumber} />
      <Field name="addressColonia" label="Colonia" defaultValue={address?.colonia} />
      <Field name="addressMunicipality" label="Municipio/Alcaldía" defaultValue={address?.municipality} />
      <Field name="addressState" label="Estado" defaultValue={address?.state} />
      <Field name="addressZipCode" label="Código postal" defaultValue={address?.zipCode} />

      <Field name="emergencyContactFullName" label="Contacto de emergencia" defaultValue={emergencyContact?.fullName} />
      <Field name="emergencyContactRelationship" label="Parentesco" defaultValue={emergencyContact?.relationship} />
      <Field name="emergencyContactPhone" label="Teléfono de emergencia" defaultValue={emergencyContact?.phone} />
    </>
  );
}
