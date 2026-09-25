import { FormEvent, useEffect, useState } from "react";
import { IconBuildingHospital } from "@tabler/icons-react";
import { ClinicFields } from "@modules/clinics/components/ClinicFields";
import { ResponsibleDoctorFields, readResponsibleDoctor } from "@modules/clinics/components/ResponsibleDoctorFields";
import { authApi, clinicsApi, getFriendlyError, sessionStore } from "@shared/api/api";
import type { UserProfile } from "@modules/auth/types";
import type { ClinicResponse, CompleteClinicSetupRequest } from "@modules/clinics/types";

export function ClinicSetupScreen({ user, onDone }: { user: UserProfile; onDone: (user: UserProfile) => void }) {
  const clinicId = user.clinics[0]?.id;
  const [clinic, setClinic] = useState<ClinicResponse | undefined>(undefined);
  const [loadingClinic, setLoadingClinic] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!clinicId) {
      setLoadingClinic(false);
      return;
    }
    clinicsApi
      .get(clinicId)
      .then(setClinic)
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoadingClinic(false));
  }, [clinicId]);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!clinicId) return;
    setLoading(true);
    setError("");
    const form = new FormData(event.currentTarget);
    const value = (name: string) => String(form.get(name) ?? "").trim();

    try {
      const body: CompleteClinicSetupRequest = {
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
        ...readResponsibleDoctor(form)
      };
      await clinicsApi.completeSetup(clinicId, body);
      // El papel del titular cambia sus permisos (atiende pacientes o no): hay que volver a pedirlos.
      const refreshed = await authApi.me();
      sessionStore.setUser(refreshed);
      onDone(refreshed);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-layout">
      <section className="auth-panel" aria-labelledby="clinic-setup-title">
        <div className="brand-row">
          <span className="brand-dot" />
          <span className="brand-word">Clinerya</span>
        </div>
        <div className="auth-heading">
          <h1 id="clinic-setup-title">Completa los datos de tu clínica</h1>
          <p>Esta información se usará en tus documentos y comprobantes.</p>
        </div>

        {loadingClinic ? (
          <p>Cargando datos de la clínica...</p>
        ) : (
          <form className="profile-form" onSubmit={submit}>
            <ClinicFields clinic={clinic} requireEmail withoutResponsibleDoctor />
            <ResponsibleDoctorFields accountHolderName={user.fullName} clinic={clinic} />
            {error && <p className="alert error">{error}</p>}
            <div className="form-actions">
              <button className="btn primary" disabled={loading} type="submit">
                <IconBuildingHospital size={20} aria-hidden="true" />
                {loading ? "Guardando" : "Guardar y continuar"}
              </button>
              <button className="btn ghost" type="button" onClick={() => onDone(user)}>
                Completar más tarde
              </button>
            </div>
          </form>
        )}
      </section>
    </main>
  );
}
