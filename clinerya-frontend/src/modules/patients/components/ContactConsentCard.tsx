import { useEffect, useState } from "react";
import { IconCheck, IconX } from "@tabler/icons-react";
import { getFriendlyError, patientsApi } from "@shared/api/api";
import type { ConsentSource, ContactConsentTextResponse, PatientResponse } from "@modules/patients/types";

const SOURCE_LABELS: Record<ConsentSource, string> = {
  CLINIC_REGISTRATION: "alta en clínica",
  CLINIC_UPDATE: "ficha del paciente",
  WHATSAPP_CHAT: "chat de WhatsApp"
};

function formatDateTime(value?: string) {
  if (!value) return "";
  return new Date(value).toLocaleString("es-MX", { dateStyle: "short", timeStyle: "short" });
}

/**
 * Estado del consentimiento en la ficha del paciente, con las acciones para registrarlo o revocarlo.
 * {@code recordedByName} lo resuelve quien usa la tarjeta; si no se conoce se muestra un generico.
 */
export function ContactConsentCard({
  patient,
  recordedByName,
  onUpdated
}: {
  patient: PatientResponse;
  recordedByName?: string;
  onUpdated: (patient: PatientResponse) => void;
}) {
  const consent = patient.contactConsent;
  const [dialog, setDialog] = useState<"grant" | "revoke" | null>(null);
  const asked = Boolean(consent?.recordedAt);
  const granted = Boolean(consent?.granted);

  return (
    <div className="consent-card">
      <strong>Contacto automático</strong>
      <div className="consent-status">
        <span className={`consent-dot ${granted ? "granted" : "denied"}`} aria-hidden="true" />
        <span>
          {!asked
            ? "Sin preguntar"
            : `${granted ? "Autorizado" : "No autorizado"} · ${formatDateTime(consent?.recordedAt)}${
                consent?.source ? ` · ${SOURCE_LABELS[consent.source]}` : ""
              }`}
        </span>
      </div>
      {asked && <small className="consent-meta">Registrado por: {recordedByName ?? "Personal de la clínica"}</small>}
      {!asked && (
        <small className="consent-meta">Pídale la autorización en su próxima visita para enviarle recordatorios.</small>
      )}
      <div className="form-actions">
        {granted ? (
          <button className="btn secondary" type="button" onClick={() => setDialog("revoke")}>
            Revocar
          </button>
        ) : (
          <button className="btn primary" type="button" onClick={() => setDialog("grant")}>
            Registrar autorización
          </button>
        )}
      </div>

      {dialog && (
        <ContactConsentDialog
          mode={dialog}
          patient={patient}
          onClose={() => setDialog(null)}
          onSaved={(updated) => {
            setDialog(null);
            onUpdated(updated);
          }}
        />
      )}
    </div>
  );
}

function ContactConsentDialog({
  mode,
  patient,
  onClose,
  onSaved
}: {
  mode: "grant" | "revoke";
  patient: PatientResponse;
  onClose: () => void;
  onSaved: (patient: PatientResponse) => void;
}) {
  const [consentText, setConsentText] = useState<ContactConsentTextResponse | null>(null);
  const [accepted, setAccepted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (mode !== "grant") return;
    patientsApi
      .contactConsentText()
      .then(setConsentText)
      .catch((caught) => setError(getFriendlyError(caught)));
  }, [mode]);

  const save = async () => {
    setLoading(true);
    setError("");
    try {
      const updated = await patientsApi.recordContactConsent(
        patient.id,
        mode === "grant" ? { granted: true, textVersion: consentText?.version } : { granted: false }
      );
      onSaved(updated);
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
          <h2>{mode === "grant" ? "Registrar autorización" : "Revocar autorización"}</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>

        {mode === "grant" ? (
          consentText && (
            <div className="consent-box">
              <p className="consent-instruction">Léale al paciente:</p>
              <blockquote className="consent-text">“{consentText.text}”</blockquote>
              <label className="checkbox-field">
                <input type="checkbox" checked={accepted} onChange={(event) => setAccepted(event.target.checked)} />
                <span>El paciente autoriza el contacto por WhatsApp y correo</span>
              </label>
              <small className="consent-meta">Texto v{consentText.version} · quedará registrado quién lo marcó</small>
            </div>
          )
        ) : (
          <p>El paciente dejará de recibir recordatorios y avisos automáticos.</p>
        )}

        {error && <p className="alert error">{error}</p>}

        <div className="form-actions">
          <button className="btn secondary" type="button" onClick={onClose} disabled={loading}>
            Cancelar
          </button>
          <button
            className="btn primary"
            type="button"
            onClick={save}
            disabled={loading || (mode === "grant" && (!consentText || !accepted))}
          >
            <IconCheck size={18} aria-hidden="true" />
            {loading ? "Guardando" : mode === "grant" ? "Registrar" : "Revocar"}
          </button>
        </div>
      </div>
    </div>
  );
}
