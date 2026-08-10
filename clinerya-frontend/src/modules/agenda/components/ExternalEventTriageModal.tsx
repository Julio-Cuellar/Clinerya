import { FormEvent, useState } from "react";
import { IconBrandGoogle, IconX } from "@tabler/icons-react";
import { integrationsApi, getFriendlyError } from "@shared/api/api";
import type { PatientResponse } from "@modules/patients/types";
import type { ExternalCalendarEventResponse } from "@modules/agenda/integrationsTypes";

interface ExternalEventTriageModalProps {
  clinicId: string;
  event: ExternalCalendarEventResponse;
  patients: PatientResponse[];
  onClose: () => void;
  onSaved: () => void;
}

export function ExternalEventTriageModal({
  clinicId,
  event,
  patients,
  onClose,
  onSaved
}: ExternalEventTriageModalProps) {
  const [patientId, setPatientId] = useState("");
  const [reason, setReason] = useState(event.summary ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const start = new Date(event.startTime);
  const end = new Date(event.endTime);
  const formattedTime = `${start.toLocaleDateString("es-MX", {
    weekday: "long",
    day: "numeric",
    month: "long"
  })} de ${start.toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit", hour12: false })} a ${end.toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit", hour12: false })}`;

  const handleLink = async (formEvent: FormEvent<HTMLFormElement>) => {
    formEvent.preventDefault();
    setError("");
    setSaving(true);
    try {
      await integrationsApi.linkExternalEvent(clinicId, event.id, {
        patientId: patientId || undefined,
        reason: reason.trim()
      });
      onSaved();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  const handleDismiss = async () => {
    setError("");
    if (!confirm("¿Estás seguro de que deseas descartar este evento externo? No se mostrará en la agenda.")) {
      return;
    }
    setSaving(true);
    try {
      await integrationsApi.dismissExternalEvent(clinicId, event.id);
      onSaved();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="panel-heading">
          <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
            <IconBrandGoogle size={20} style={{ color: "#4285F4" }} />
            <h2>Revisar Evento Externo</h2>
          </div>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>
        <form className="profile-form" onSubmit={handleLink}>
          <div style={{ marginBottom: "16px", padding: "12px", background: "var(--background-alt, #f5f5f7)", borderRadius: "8px" }}>
            <h4 style={{ margin: "0 0 4px 0", fontSize: "14px", fontWeight: "600" }}>
              {event.summary || "(Sin título)"}
            </h4>
            <p style={{ margin: 0, fontSize: "12px", color: "var(--text-muted, #86868b)" }}>
              {formattedTime}
            </p>
            {event.description && (
              <p style={{ margin: "8px 0 0 0", fontSize: "12px", color: "var(--text-muted, #86868b)", fontStyle: "italic" }}>
                {event.description}
              </p>
            )}
          </div>

          <label className="field">
            <span>Paciente (Opcional)</span>
            <select value={patientId} onChange={(e) => setPatientId(e.target.value)}>
              <option value="">Paciente nuevo (Sin registrar / Primera vez)</option>
              {patients.map((patient) => (
                <option key={patient.id} value={patient.id}>
                  {patient.firstName} {patient.lastNamePaterno} {patient.lastNameMaterno ?? ""}
                </option>
              ))}
            </select>
          </label>

          <label className="field field-full">
            <span>Motivo de la Cita</span>
            <input
              type="text"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Revisión, consulta, limpieza..."
              required
            />
          </label>

          {error && <p className="alert error">{error}</p>}

          <div className="form-actions" style={{ display: "flex", justifyContent: "space-between", marginTop: "24px" }}>
            <button
              className="btn danger ghost"
              type="button"
              disabled={saving}
              onClick={handleDismiss}
            >
              Descartar
            </button>
            <button className="btn primary" type="submit" disabled={saving}>
              {saving ? "Agendando..." : "Agendar Cita"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
