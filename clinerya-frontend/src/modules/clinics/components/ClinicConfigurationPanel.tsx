import { FormEvent, useEffect, useState } from "react";
import { IconDeviceFloppy } from "@tabler/icons-react";
import { ClinicFields } from "@modules/clinics/components/ClinicFields";
import type { ClinicResponse, UpdateClinicRequest } from "@modules/clinics/types";
import { clinicsApi, getFriendlyError } from "@shared/api/api";

export function ClinicConfigurationPanel({ clinicId }: { clinicId?: string }) {
  const [clinic, setClinic] = useState<ClinicResponse>();
  const [loading, setLoading] = useState(true);
  const [savingClinic, setSavingClinic] = useState(false);
  const [error, setError] = useState("");
  const [clinicSuccess, setClinicSuccess] = useState("");

  useEffect(() => {
    if (!clinicId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    clinicsApi.get(clinicId)
      .then((clinicData) => setClinic(clinicData))
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  }, [clinicId]);

  const saveClinic = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!clinicId) return;

    setSavingClinic(true);
    setError("");
    setClinicSuccess("");
    const form = new FormData(event.currentTarget);
    const value = (name: string) => String(form.get(name) ?? "").trim();
    const body: UpdateClinicRequest = {
      name: value("name"),
      email: value("email") || undefined,
      phone: value("phone") || undefined,
      timezone: value("timezone") || undefined,
      legalName: value("legalName") || undefined,
      rfc: value("rfc") || undefined,
      taxRegimeCode: value("taxRegimeCode") || undefined,
      addressStreet: value("addressStreet") || undefined,
      addressColonia: value("addressColonia") || undefined,
      addressMunicipality: value("addressMunicipality") || undefined,
      addressState: value("addressState") || undefined,
      addressZip: value("addressZip") || undefined,
      logoUrl: value("logoUrl") || undefined,
      privacyNoticeUrl: value("privacyNoticeUrl") || undefined,
      cofeprisPermitNumber: value("cofeprisPermitNumber") || undefined,
      responsibleDoctorName: value("responsibleDoctorName") || undefined,
      responsibleDoctorProfessionalLicense: value("responsibleDoctorProfessionalLicense") || undefined
    };

    try {
      const updated = await clinicsApi.update(clinicId, body);
      setClinic(updated);
      setClinicSuccess("Datos de la clinica guardados.");
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSavingClinic(false);
    }
  };


  if (!clinicId) return <p className="alert error">No hay una clinica activa para configurar.</p>;
  if (loading) return <p>Cargando propiedades de la clinica...</p>;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
      <div className="panel" style={{ padding: "16px", background: "var(--color-card)" }}>
        <div className="panel-heading">
          <div>
            <h3>Datos y propiedades de la clinica</h3>
            <p className="description" style={{ fontSize: "12px", color: "var(--color-text-3)", marginTop: "4px" }}>
              Informacion operativa, fiscal y de contacto de la clinica activa.
            </p>
          </div>
        </div>
        <form className="profile-form" onSubmit={saveClinic}>
          <ClinicFields clinic={clinic} requireEmail />
          {error && <p className="alert error">{error}</p>}
          {clinicSuccess && <p className="alert success">{clinicSuccess}</p>}
          <div className="form-actions">
            <button className="btn primary" type="submit" disabled={savingClinic}>
              <IconDeviceFloppy size={18} aria-hidden="true" />
              {savingClinic ? "Guardando..." : "Guardar datos de la clinica"}
            </button>
          </div>
        </form>
      </div>

    </div>
  );
}
