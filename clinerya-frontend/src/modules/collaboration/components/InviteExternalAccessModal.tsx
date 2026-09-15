import { FormEvent, useState } from "react";
import { IconSend, IconX } from "@tabler/icons-react";
import { collaborationApi, getFriendlyError } from "@shared/api/api";
import type { AccessLevel, ExternalAccessGrantResponse } from "@modules/collaboration/types";
import { SHARE_SECTION_OPTIONS } from "@modules/collaboration/types";

export function InviteExternalAccessModal({
  clinicId,
  patientId,
  patientLabel,
  onClose,
  onInvited
}: {
  clinicId: string;
  patientId: string;
  patientLabel: string;
  onClose: () => void;
  onInvited: (grant: ExternalAccessGrantResponse) => void;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [generatedLink, setGeneratedLink] = useState("");
  const [tab, setTab] = useState<"registered" | "temporary">("registered");

  const submitRegistered = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    const form = new FormData(event.currentTarget);
    const email = String(form.get("email") ?? "").trim();
    const accessLevel = String(form.get("accessLevel") ?? "READ_ONLY") as AccessLevel;

    try {
      const grant = await collaborationApi.invite(clinicId, patientId, { email, accessLevel });
      const link = `${window.location.origin}/accept-sharing?grantId=${grant.id}`;
      setGeneratedLink(link);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  };

  const submitTemporary = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    const form = new FormData(event.currentTarget);
    const email = String(form.get("email") ?? "").trim();
    const daysValid = Number(form.get("daysValid") ?? "7");
    const sections = form.getAll("sections").map(String);

    if (sections.length === 0) {
      setError("Selecciona al menos una sección para compartir.");
      setLoading(false);
      return;
    }

    try {
      const response = await collaborationApi.createTemporaryShare(clinicId, patientId, { email, daysValid, sections });
      const link = `${window.location.origin}/share-view?token=${response.token}`;
      setGeneratedLink(link);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  };

  if (generatedLink) {
    return (
      <div className="modal-overlay" onClick={onClose}>
        <div className="modal-card" onClick={(event) => event.stopPropagation()}>
          <div className="panel-heading">
            <h2>¡Expediente Compartido!</h2>
            <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
              <IconX size={18} />
            </button>
          </div>
          <div className="profile-form">
            <p className="field-full" style={{ fontSize: "14px", color: "var(--color-text-2)", marginBottom: "15px" }}>
              Se ha compartido el expediente con éxito. Comparte el siguiente enlace con el especialista para que pueda acceder directamente:
            </p>
            <div className="field field-full">
              <input
                type="text"
                readOnly
                value={generatedLink}
                onClick={(e) => (e.target as HTMLInputElement).select()}
                style={{
                  fontFamily: "monospace",
                  background: "var(--color-bg-2)",
                  padding: "10px",
                  borderRadius: "6px",
                  border: "1px solid var(--color-border)",
                  color: "var(--color-text-1)",
                  width: "100%",
                  textAlign: "center"
                }}
              />
            </div>
            <div className="form-actions" style={{ marginTop: "20px" }}>
              <button
                className="btn primary"
                type="button"
                onClick={() => {
                  navigator.clipboard.writeText(generatedLink);
                  alert("¡Enlace copiado al portapapeles!");
                }}
              >
                Copiar enlace
              </button>
              <button className="btn secondary" type="button" onClick={() => {
                onInvited({} as any); // trigger reload
              }}>
                Aceptar y Cerrar
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Compartir con especialista externo</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>

        {/* Tab Header */}
        <div style={{ display: "flex", gap: "10px", marginBottom: "20px", borderBottom: "1px solid var(--color-border)", paddingBottom: "10px" }}>
          <button
            type="button"
            className={`btn ${tab === "registered" ? "primary" : "ghost"}`}
            style={{ padding: "8px 16px", fontSize: "13px", flex: 1 }}
            onClick={() => setTab("registered")}
          >
            Médico Registrado (Colaboración)
          </button>
          <button
            type="button"
            className={`btn ${tab === "temporary" ? "primary" : "ghost"}`}
            style={{ padding: "8px 16px", fontSize: "13px", flex: 1 }}
            onClick={() => setTab("temporary")}
          >
            Médico Invitado (Sin Registro)
          </button>
        </div>

        {tab === "registered" ? (
          <form className="profile-form" onSubmit={submitRegistered}>
            <p className="field-full" style={{ fontSize: "13px", color: "var(--color-text-2)", marginBottom: "15px" }}>
              Invita a un especialista para que pueda ver el expediente de <strong>{patientLabel}</strong> desde su propia plataforma. Si no tiene cuenta, se le pedirá registrarse al abrir el enlace.
            </p>

            <label className="field field-full">
              <span>Correo del especialista</span>
              <input name="email" type="email" required placeholder="especialista@ejemplo.com" />
            </label>

            <label className="field field-full">
              <span>Nivel de acceso</span>
              <select name="accessLevel" defaultValue="READ_ONLY">
                <option value="READ_ONLY">Solo lectura</option>
                <option value="COMMENT">Lectura + comentar (notas clínicas)</option>
                <option value="FULL">Lectura + notas e historia clínica</option>
              </select>
            </label>

            {error && <p className="alert error">{error}</p>}
            <div className="form-actions">
              <button className="btn primary" disabled={loading} type="submit">
                <IconSend size={18} aria-hidden="true" />
                {loading ? "Enviando..." : "Generar enlace"}
              </button>
            </div>
          </form>
        ) : (
          <form className="profile-form" onSubmit={submitTemporary}>
            <p className="field-full" style={{ fontSize: "13px", color: "var(--color-text-2)", marginBottom: "15px" }}>
              Genera un enlace público y seguro para que un especialista externo pueda revisar el expediente de <strong>{patientLabel}</strong> de forma temporal <strong>sin necesidad de registrarse ni tener cuenta</strong> en Clinerya.
            </p>

            <label className="field field-full">
              <span>Correo de referencia (Destinatario)</span>
              <input name="email" type="email" required placeholder="especialista@ejemplo.com" />
            </label>

            <label className="field field-full">
              <span>Tiempo de validez del enlace</span>
              <select name="daysValid" defaultValue="7">
                <option value="1">1 día</option>
                <option value="3">3 días</option>
                <option value="7">7 días (Recomendado)</option>
                <option value="15">15 días</option>
                <option value="30">30 días</option>
              </select>
            </label>

            <div className="field field-full">
              <span>¿Qué incluir en el enlace?</span>
              <div style={{ display: "flex", flexDirection: "column", gap: "8px", marginTop: "6px" }}>
                {SHARE_SECTION_OPTIONS.map((option) => (
                  <label key={option.key} style={{ display: "flex", alignItems: "flex-start", gap: "8px", fontSize: "13px", fontWeight: "normal" }}>
                    <input type="checkbox" name="sections" value={option.key} defaultChecked style={{ marginTop: "2px" }} />
                    <span>
                      <strong>{option.label}</strong>
                      <span style={{ color: "var(--color-text-3)" }}> — {option.hint}</span>
                    </span>
                  </label>
                ))}
              </div>
            </div>

            {error && <p className="alert error">{error}</p>}
            <div className="form-actions">
              <button className="btn primary" disabled={loading} type="submit">
                <IconSend size={18} aria-hidden="true" />
                {loading ? "Generando..." : "Generar enlace temporal"}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
