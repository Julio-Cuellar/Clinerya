import { FormEvent, useState } from "react";
import { IconBuildingHospital, IconX } from "@tabler/icons-react";
import { clinicsApi, getFriendlyError } from "@shared/api/api";
import type { ClinicResponse, CreateClinicRequest, UpdateClinicRequest } from "@modules/clinics/types";
import { ClinicFields } from "@modules/clinics/components/ClinicFields";
import { ResponsibleDoctorFields, readResponsibleDoctor } from "@modules/clinics/components/ResponsibleDoctorFields";

export function ClinicModal({
  mode,
  clinic,
  accountHolderName,
  onClose,
  onSaved
}: {
  mode: "create" | "edit";
  clinic?: ClinicResponse;
  accountHolderName: string;
  onClose: () => void;
  onSaved: (clinic: ClinicResponse) => void;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    const form = new FormData(event.currentTarget);
    const value = (name: string) => String(form.get(name) ?? "").trim();

    try {
      if (mode === "create") {
        const body: CreateClinicRequest = {
          name: value("name"),
          email: value("email"),
          timezone: value("timezone") || "America/Mexico_City",
          legalName: value("legalName") || undefined,
          rfc: value("rfc") || undefined,
          taxRegimeCode: value("taxRegimeCode") || undefined,
          addressStreet: value("addressStreet") || undefined,
          addressColonia: value("addressColonia") || undefined,
          addressMunicipality: value("addressMunicipality") || undefined,
          addressState: value("addressState") || undefined,
          addressZip: value("addressZip") || undefined,
          phone: value("phone") || undefined,
          cofeprisPermitNumber: value("cofeprisPermitNumber") || undefined,
          ...readResponsibleDoctor(form)
        };
        const created = await clinicsApi.create(body);
        onSaved(created);
      } else if (clinic) {
        const body: UpdateClinicRequest = {
          name: value("name"),
          email: value("email") || undefined,
          timezone: value("timezone") || undefined,
          legalName: value("legalName") || undefined,
          rfc: value("rfc") || undefined,
          taxRegimeCode: value("taxRegimeCode") || undefined,
          addressStreet: value("addressStreet") || undefined,
          addressColonia: value("addressColonia") || undefined,
          addressMunicipality: value("addressMunicipality") || undefined,
          addressState: value("addressState") || undefined,
          addressZip: value("addressZip") || undefined,
          phone: value("phone") || undefined,
          logoUrl: value("logoUrl") || undefined,
          cofeprisPermitNumber: value("cofeprisPermitNumber") || undefined,
          responsibleDoctorName: value("responsibleDoctorName") || undefined,
          responsibleDoctorProfessionalLicense: value("responsibleDoctorProfessionalLicense") || undefined
        };
        const updated = await clinicsApi.update(clinic.id, body);
        onSaved(updated);
      }
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>{mode === "create" ? "Nueva clínica" : `Completar datos: ${clinic?.name}`}</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>
        <form className="profile-form" onSubmit={submit}>
          <ClinicFields clinic={clinic} requireEmail={mode === "create"} withoutResponsibleDoctor={mode === "create"} />
          {mode === "create" && <ResponsibleDoctorFields accountHolderName={accountHolderName} />}

          {error && <p className="alert error">{error}</p>}
          <div className="form-actions">
            <button className="btn primary" disabled={loading} type="submit">
              <IconBuildingHospital size={20} aria-hidden="true" />
              {loading ? "Guardando" : mode === "create" ? "Crear clínica" : "Guardar datos"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
