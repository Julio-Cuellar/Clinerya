import { regimenesFiscales } from "@modules/clinics/constants/regimenFiscal";
import type { ClinicResponse } from "@modules/clinics/types";
import { Field } from "@shared/ui/Field";

const regimenFiscalOptions = regimenesFiscales.map((regimen) => ({ value: regimen.code, label: regimen.label }));

// withoutResponsibleDoctor: el alta (pantalla inicial y "Nueva clínica") los pide con
// ResponsibleDoctorFields, que además pregunta el papel del titular.
export function ClinicFields({
  clinic,
  requireEmail,
  withoutResponsibleDoctor
}: {
  clinic?: ClinicResponse;
  requireEmail?: boolean;
  withoutResponsibleDoctor?: boolean;
}) {
  return (
    <>
      <Field name="name" label="Nombre de la clínica" defaultValue={clinic?.name} required />
      <Field name="email" label="Correo de contacto" type="email" defaultValue={clinic?.email} required={requireEmail} />
      <Field name="phone" label="Teléfono" defaultValue={clinic?.phone} />
      <Field name="timezone" label="Zona horaria" defaultValue={clinic?.timezone ?? "America/Mexico_City"} />
      <Field name="legalName" label="Razón social" defaultValue={clinic?.legalName} />
      <Field name="rfc" label="RFC" defaultValue={clinic?.rfc} />
      <Field
        name="taxRegimeCode"
        label="Régimen fiscal"
        defaultValue={clinic?.taxRegimeCode}
        options={regimenFiscalOptions}
      />
      <Field name="addressStreet" label="Calle y número" defaultValue={clinic?.addressStreet} />
      <Field name="addressColonia" label="Colonia" defaultValue={clinic?.addressColonia} />
      <Field name="addressMunicipality" label="Municipio/Alcaldía" defaultValue={clinic?.addressMunicipality} />
      <Field name="addressState" label="Estado" defaultValue={clinic?.addressState} />
      <Field name="addressZip" label="Código postal" defaultValue={clinic?.addressZip} />
      <Field
        name="logoUrl"
        label="URL del logotipo (imagen pública)"
        defaultValue={clinic?.logoUrl}
        placeholder="https://..."
      />
      <Field
        name="cofeprisPermitNumber"
        label="Folio de permiso COFEPRIS"
        defaultValue={clinic?.cofeprisPermitNumber}
        placeholder="Ej. 263300201A0123"
      />
      {!withoutResponsibleDoctor && (
        <>
          <Field
            name="responsibleDoctorName"
            label="Nombre del médico responsable"
            defaultValue={clinic?.responsibleDoctorName}
            placeholder="Ej. Dr. Juan Pérez"
          />
          <Field
            name="responsibleDoctorProfessionalLicense"
            label="Cédula profesional del responsable"
            defaultValue={clinic?.responsibleDoctorProfessionalLicense}
            placeholder="Ej. 12345678"
          />
        </>
      )}
    </>
  );
}
