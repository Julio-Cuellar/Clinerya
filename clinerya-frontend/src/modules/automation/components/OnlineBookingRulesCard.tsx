import { useEffect, useState } from "react";
import { ApiClientError, getFriendlyError, onlineBookingSettingsApi } from "@shared/api/api";
import type { OnlineBookingSettings } from "../types";

// Valores que acepta el backend: cupo de 10 a 240 min en multiplos de 5; anticipacion de 0 min a 7 dias.
const SLOT_OPTIONS = [15, 20, 30, 45, 60, 90];
const LEAD_OPTIONS = [0, 30, 60, 120, 240, 480, 1440, 2880, 10080];

function leadLabel(minutes: number): string {
  if (minutes === 0) return "Sin mínimo";
  if (minutes < 60) return `${minutes} minutos`;
  if (minutes < 1440) return minutes === 60 ? "1 hora" : `${minutes / 60} horas`;
  const days = minutes / 1440;
  return days === 1 ? "1 día" : `${days} días`;
}

function withCurrent(options: number[], current: number): number[] {
  return options.includes(current) ? options : [...options, current].sort((a, b) => a - b);
}

/** Reglas de horarios que el asistente puede ofrecer (pendiente de la entrega 3). */
export function OnlineBookingRulesCard({ clinicId, canManage }: { clinicId: string; canManage: boolean }) {
  const [settings, setSettings] = useState<OnlineBookingSettings | null>(null);
  const [hidden, setHidden] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    onlineBookingSettingsApi
      .get(clinicId)
      .then(setSettings)
      .catch((caught) => {
        if (caught instanceof ApiClientError && caught.status === 403) setHidden(true);
        else setError(getFriendlyError(caught));
      });
  }, [clinicId]);

  const save = async (action: () => Promise<OnlineBookingSettings>) => {
    setSaving(true);
    setError("");
    setNotice("");
    try {
      setSettings(await action());
      setNotice("Guardado.");
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  if (hidden) return null;
  if (!settings) return <div className="wa-card wa-sub">{error || "Cargando reglas de citas en línea…"}</div>;

  const disabled = !canManage || saving;

  return (
    <section className="wa-card" aria-labelledby="booking-rules-title">
      <h3 id="booking-rules-title" style={{ margin: 0, fontSize: 16 }}>Citas por WhatsApp: reglas de horarios</h3>
      <p className="wa-help">Qué horarios puede ofrecer el asistente. Se calculan con la agenda real de cada médico; los cambios se guardan al elegir.</p>
      {error && <p className="alert error" style={{ marginTop: 12 }}>{error}</p>}
      {notice && <p className="alert success" style={{ marginTop: 12 }}>{notice}</p>}

      <label className="field" style={{ marginTop: 14, maxWidth: 280 }}>
        <span>Duración de cada cita ofrecida (toda la clínica)</span>
        <select value={settings.slotMinutes} disabled={disabled}
          onChange={(e) => save(() => onlineBookingSettingsApi.updateSlotMinutes(clinicId, Number(e.target.value)))}>
          {withCurrent(SLOT_OPTIONS, settings.slotMinutes).map((minutes) => <option key={minutes} value={minutes}>{minutes} minutos</option>)}
        </select>
      </label>

      <h4 style={{ margin: "20px 0 0", fontSize: 14 }}>Anticipación mínima por médico</h4>
      <p className="wa-help">El asistente no ofrece horarios que empiecen antes de este plazo.</p>
      <table className="wa-table">
        <thead><tr><th scope="col">Médico</th><th scope="col">Anticipación mínima</th></tr></thead>
        <tbody>
          {settings.doctors.map((doctor) => (
            <tr key={doctor.doctorStaffId}>
              <td style={{ fontWeight: 600 }}>{doctor.doctorName}</td>
              <td>
                <select className="field" aria-label={`Anticipación de ${doctor.doctorName}`} value={doctor.minLeadMinutes} disabled={disabled}
                  style={{ height: 36, border: "1px solid var(--color-border)", borderRadius: 3, padding: "0 12px", background: "var(--color-card)", color: "var(--color-text-1)" }}
                  onChange={(e) => save(() => onlineBookingSettingsApi.updateDoctorLeadMinutes(clinicId, doctor.doctorStaffId, Number(e.target.value)))}>
                  {withCurrent(LEAD_OPTIONS, doctor.minLeadMinutes).map((minutes) => <option key={minutes} value={minutes}>{leadLabel(minutes)}</option>)}
                </select>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
