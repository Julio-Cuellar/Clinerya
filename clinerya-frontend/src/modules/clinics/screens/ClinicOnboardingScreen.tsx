import { FormEvent, useEffect, useState } from "react";
import {
  IconCheck,
  IconInfoCircle,
  IconMailForward,
  IconStethoscope,
  IconWand
} from "@tabler/icons-react";
import { ClinicFields } from "@modules/clinics/components/ClinicFields";
import { ClinicSpecialtyPicker } from "@modules/clinics/components/ClinicSpecialtyPicker";
import {
  getClinicOnboardingState,
  type OnboardingStepId
} from "@modules/clinics/lib/clinicOnboarding";
import type { ClinicResponse, ClinicSpecialty, UpdateClinicRequest } from "@modules/clinics/types";
import { buildRecommendedTemplate } from "@modules/records/lib/recommendedTemplates";
import { parseSchema } from "@modules/records/types";
import {
  clinicsApi,
  getFriendlyError,
  historyTemplatesApi,
  staffApi,
  treatmentCatalogApi
} from "@shared/api/api";
import { getClinicProfileFor } from "@shared/utils/clinicProfile";

const STEPS: { id: OnboardingStepId; label: string }[] = [
  { id: 1, label: "Datos de la clínica" },
  { id: 2, label: "Especialidad" },
  { id: 3, label: "Catálogo y plantilla" },
  { id: 4, label: "Invita a tu equipo" }
];

export function ClinicOnboardingScreen({
  clinic,
  catalogItemCount,
  onClinicUpdated,
  onCatalogSeeded,
  onClose
}: {
  clinic: ClinicResponse;
  catalogItemCount?: number;
  onClinicUpdated: (clinic: ClinicResponse) => void;
  onCatalogSeeded: (count: number) => void;
  onClose: () => void;
}) {
  const state = getClinicOnboardingState(clinic, catalogItemCount);
  const [step, setStep] = useState<OnboardingStepId>(state.resumeStep);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const profile = getClinicProfileFor(clinic);
  const currentSpecialty = clinic.specialty ?? "SIN_CONFIGURAR";
  const [specialty, setSpecialty] = useState<ClinicSpecialty>(currentSpecialty);

  const [seedCatalog, setSeedCatalog] = useState(true);
  const [seedTemplate, setSeedTemplate] = useState(true);
  const [catalogResult, setCatalogResult] = useState("");
  const [templateResult, setTemplateResult] = useState("");
  // La siembra del catálogo es idempotente del lado del servidor; la de plantillas no, así que
  // hay que saber si ya existe una historia clínica antes de ofrecer crearla.
  const [hasHistoryTemplate, setHasHistoryTemplate] = useState<boolean | undefined>(undefined);

  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState("DOCTOR");
  const [inviteError, setInviteError] = useState("");
  const [invited, setInvited] = useState<string[]>([]);

  // Si el paso pendiente cambia porque se guardó algo, el asistente sigue al usuario.
  useEffect(() => {
    setSpecialty(clinic.specialty ?? "SIN_CONFIGURAR");
  }, [clinic.specialty]);

  useEffect(() => {
    historyTemplatesApi
      .list(clinic.id)
      .then((templates) => {
        const exists = templates.some(
          (template) => template.active && parseSchema(template.schemaJson).kind === "historia_clinica"
        );
        setHasHistoryTemplate(exists);
        if (exists) setSeedTemplate(false);
      })
      .catch(() => setHasHistoryTemplate(undefined));
  }, [clinic.id]);

  const saveClinicData = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setBusy(true);
    setError("");
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
      onClinicUpdated(await clinicsApi.update(clinic.id, body));
      setStep(2);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy(false);
    }
  };

  const saveSpecialty = async () => {
    if (specialty === "SIN_CONFIGURAR") {
      setError("Elige una especialidad para continuar.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      onClinicUpdated(await clinicsApi.updateSpecialty(clinic.id, { specialty }));
      setStep(3);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy(false);
    }
  };

  const runSeeds = async () => {
    setBusy(true);
    setError("");
    setCatalogResult("");
    setTemplateResult("");
    try {
      if (seedCatalog && profile.hasSuggestedCatalog) {
        const result = await treatmentCatalogApi.seed(clinic.id);
        onCatalogSeeded(result.created + result.skipped);
        setCatalogResult(
          result.created > 0
            ? `${result.created} servicios creados.`
            : "El catálogo ya estaba armado, no se duplicó nada."
        );
      }
      // Nunca se crea una segunda historia clínica: correr el asistente dos veces dejaría al
      // médico con plantillas duplicadas y sin forma obvia de saber cuál usar.
      if (seedTemplate && !hasHistoryTemplate) {
        await historyTemplatesApi.create(
          clinic.id,
          buildRecommendedTemplate(profile.recommendedHistoryTemplate)
        );
        setHasHistoryTemplate(true);
        setTemplateResult("Plantilla de historia clínica creada.");
      }
      setStep(4);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy(false);
    }
  };

  const sendInvite = async () => {
    const email = inviteEmail.trim();
    if (!email) {
      setInviteError("Escribe un correo para enviar la invitación.");
      return;
    }
    setBusy(true);
    setInviteError("");
    try {
      await staffApi.invite(clinic.id, { email, role: inviteRole });
      setInvited((prev) => [...prev, email]);
      setInviteEmail("");
    } catch (caught) {
      setInviteError(getFriendlyError(caught));
    } finally {
      setBusy(false);
    }
  };

  const stepDone = (id: OnboardingStepId) => {
    if (id === 2) return state.specialtyDone;
    if (id === 3) return state.specialtyDone && state.catalogDone;
    return false;
  };

  return (
    <section className="onboarding-shell">
      <header className="onboarding-header">
        <div className="onboarding-badge" aria-hidden="true">
          <IconStethoscope size={16} />
        </div>
        <div className="onboarding-title">
          <strong>Configura tu clínica</strong>
          <span>
            {clinic.name} · paso {step} de {STEPS.length}
          </span>
        </div>
        <button className="btn" type="button" onClick={onClose}>
          Continuar después
        </button>
      </header>

      <div className="onboarding-body">
        <nav className="onboarding-rail" aria-label="Pasos de configuración">
          {STEPS.map((item) => {
            const done = stepDone(item.id);
            const isCurrent = item.id === step;
            return (
              <button
                key={item.id}
                type="button"
                className={`onboarding-step${isCurrent ? " current" : ""}${done ? " done" : ""}`}
                aria-current={isCurrent ? "step" : undefined}
                onClick={() => setStep(item.id)}
              >
                <span className="onboarding-step-dot">
                  {done ? <IconCheck size={12} aria-hidden="true" /> : item.id}
                </span>
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        <div className="onboarding-panel">
          {step === 1 && (
            <>
              <h3>Datos de la clínica</h3>
              <p className="onboarding-lead">
                Esto es lo que aparece en tus cotizaciones y documentos impresos. Puedes completarlo
                después desde Configuración.
              </p>
              <form className="profile-form" onSubmit={saveClinicData}>
                <ClinicFields clinic={clinic} requireEmail />
                {error && <p className="alert error">{error}</p>}
                <div className="form-actions">
                  <button className="btn primary" type="submit" disabled={busy}>
                    {busy ? "Guardando..." : "Guardar y continuar"}
                  </button>
                  <button className="btn" type="button" onClick={() => setStep(2)}>
                    Omitir por ahora
                  </button>
                </div>
              </form>
            </>
          )}

          {step === 2 && (
            <>
              <h3>¿A qué se dedica esta clínica?</h3>
              <p className="onboarding-lead">
                Ajusta el vocabulario del módulo de tratamientos, qué campos se capturan y qué
                plantilla de historia clínica se recomienda. Puedes cambiarlo cuando quieras.
              </p>
              <ClinicSpecialtyPicker selected={specialty} onSelect={setSpecialty} />
              {error && <p className="alert error">{error}</p>}
              <div className="form-actions">
                <button className="btn primary" type="button" disabled={busy} onClick={saveSpecialty}>
                  {busy ? "Guardando..." : "Guardar y continuar"}
                </button>
                <button className="btn" type="button" onClick={() => setStep(1)}>
                  Atrás
                </button>
              </div>
            </>
          )}

          {step === 3 && (
            <>
              <h3>Esto es lo que vamos a crear</h3>
              <p className="onboarding-lead">
                Todo es editable después. Desmarca lo que prefieras armar tú.
              </p>

              {!state.specialtyDone ? (
                <p className="alert warning">
                  Elige primero una especialidad para saber qué sugerirte.
                </p>
              ) : (
                <>
                  {profile.hasSuggestedCatalog && (
                    <label className="onboarding-check">
                      <input
                        type="checkbox"
                        checked={seedCatalog}
                        onChange={(event) => setSeedCatalog(event.target.checked)}
                      />
                      <span>
                        <strong>Catálogo de {profile.label.toLowerCase()}</strong>
                        <em>Servicios con precio de referencia, sin materiales enganchados.</em>
                      </span>
                    </label>
                  )}

                  <label className="onboarding-check">
                    <input
                      type="checkbox"
                      checked={seedTemplate && !hasHistoryTemplate}
                      disabled={hasHistoryTemplate === true}
                      onChange={(event) => setSeedTemplate(event.target.checked)}
                    />
                    <span>
                      <strong>
                        {profile.recommendedHistoryTemplate === "NOM_013"
                          ? "Historia clínica odontológica (NOM-013)"
                          : "Historia clínica general (NOM-004)"}
                      </strong>
                      <em>
                        {hasHistoryTemplate
                          ? "Esta clínica ya tiene una historia clínica; no se crea otra."
                          : "Plantilla base, editable a criterio de la clínica."}
                      </em>
                    </span>
                  </label>

                  <p className="alert info onboarding-note">
                    <IconInfoCircle size={16} aria-hidden="true" />
                    Los precios del catálogo son de referencia. Ajústalos antes de cotizar.
                  </p>

                  {catalogResult && <p className="alert success">{catalogResult}</p>}
                  {templateResult && <p className="alert success">{templateResult}</p>}
                  {error && <p className="alert error">{error}</p>}

                  <div className="form-actions">
                    <button className="btn primary" type="button" disabled={busy} onClick={runSeeds}>
                      <IconWand size={16} aria-hidden="true" />
                      {busy ? "Creando..." : "Crear y continuar"}
                    </button>
                    <button className="btn" type="button" onClick={() => setStep(4)}>
                      Omitir por ahora
                    </button>
                  </div>
                </>
              )}
            </>
          )}

          {step === 4 && (
            <>
              <h3>Invita a tu equipo</h3>
              <p className="onboarding-lead">
                Cada persona recibe un enlace para crear su cuenta con el rol que le asignes. Si
                trabajas solo, puedes terminar aquí.
              </p>

              <div className="onboarding-invite">
                <label className="field">
                  <span>Correo</span>
                  <input
                    type="email"
                    value={inviteEmail}
                    placeholder="nombre@clinica.com"
                    onChange={(event) => {
                      setInviteEmail(event.target.value);
                      setInviteError("");
                    }}
                  />
                </label>
                <label className="field">
                  <span>Rol</span>
                  <select value={inviteRole} onChange={(event) => setInviteRole(event.target.value)}>
                    <option value="DOCTOR">Doctor / Especialista</option>
                    <option value="RECEPTIONIST">Recepcionista</option>
                    <option value="ASSISTANT">Asistente médico</option>
                    <option value="CLINIC_ADMIN">Administrador de clínica</option>
                  </select>
                </label>
                <button className="btn" type="button" disabled={busy} onClick={sendInvite}>
                  <IconMailForward size={16} aria-hidden="true" />
                  {busy ? "Enviando..." : "Invitar"}
                </button>
              </div>

              {inviteError && <p className="alert error">{inviteError}</p>}
              {invited.length > 0 && (
                <p className="alert success">
                  Invitaciones enviadas: {invited.join(", ")}
                </p>
              )}

              <div className="form-actions">
                <button className="btn primary" type="button" onClick={onClose}>
                  <IconCheck size={16} aria-hidden="true" />
                  Terminar
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </section>
  );
}
