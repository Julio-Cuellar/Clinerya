import { useEffect, useState } from "react";
import { ApiClientError, doctorChannelsApi, getFriendlyError } from "@shared/api/api";
import type { DoctorChannelView } from "../types";
import { doctorChannelStatus } from "../logic/assistant";
import { formatPhone } from "../logic/phone";

const DATE_TIME = new Intl.DateTimeFormat("es-MX", { dateStyle: "short", timeStyle: "short" });

/**
 * "Avisos de solicitudes por WhatsApp" en el Perfil del medico. Quien no atiende pacientes no la ve
 * (el backend responde 403). El aviso nunca lleva datos del paciente.
 */
export function MyWhatsAppNoticesCard({ clinicId }: { clinicId: string }) {
  const [channel, setChannel] = useState<DoctorChannelView | null>(null);
  const [hidden, setHidden] = useState(false);
  const [form, setForm] = useState({ phone: "", consent: false, active: false });
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [saving, setSaving] = useState(false);

  const adopt = (next: DoctorChannelView) => {
    setChannel(next);
    setForm({ phone: formatPhone(next.phone), consent: Boolean(next.consentAt), active: next.active });
  };

  useEffect(() => {
    doctorChannelsApi
      .getMine(clinicId)
      .then(adopt)
      .catch((caught) => {
        if (caught instanceof ApiClientError && caught.status === 403) setHidden(true);
        else setError(getFriendlyError(caught));
      });
  }, [clinicId]);

  const save = async () => {
    setSaving(true);
    setError("");
    setNotice("");
    try {
      adopt(await doctorChannelsApi.updateMine(clinicId, form));
      setNotice("Guardado.");
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  if (hidden) return null;
  if (!channel) return <div className="wa-card wa-sub">{error || "Cargando tus avisos de WhatsApp…"}</div>;

  const status = doctorChannelStatus(channel);

  return (
    <div style={{ display: "grid", gridTemplateColumns: "minmax(0, 1.3fr) minmax(0, 1fr)", gap: 16, alignItems: "start" }}>
      <section className="wa-card" aria-labelledby="my-wa-title">
        <div className="wa-card-head">
          <h3 id="my-wa-title">Avisos de solicitudes por WhatsApp</h3>
          <span className={`wa-pill ${status.tone}`}>{status.label}</span>
        </div>
        <p className="wa-help">Te avisamos cuando un paciente pide cita contigo. Las solicitudes se responden aquí en Clinerya, nunca desde WhatsApp.</p>
        {error && <p className="alert error" style={{ marginTop: 12 }}>{error}</p>}
        {notice && <p className="alert success" style={{ marginTop: 12 }}>{notice}</p>}
        <div style={{ display: "grid", gap: 14, marginTop: 14 }}>
          <label className="field">
            <span>Tu celular</span>
            <input value={form.phone} placeholder="55 1234 5678" onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            <span className="wa-sub">10 dígitos si es de México; de otro país, con su lada internacional.</span>
          </label>
          <label className="wa-check">
            <input type="checkbox" checked={form.consent}
              onChange={(e) => setForm({ ...form, consent: e.target.checked, active: e.target.checked && form.active })} />
            <span>
              Acepto recibir en este número avisos de la clínica sobre mis solicitudes de cita. Puedo retirarlo cuando quiera.
              {channel.consentAt && <><br /><span className="wa-sub">Aceptado el {DATE_TIME.format(new Date(channel.consentAt))}</span></>}
            </span>
          </label>
          <label className="wa-check">
            <input type="checkbox" checked={form.active} disabled={!form.consent} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
            <span>Recibir avisos ahora<br /><span className="wa-sub">Desmárcalo para pausarlos sin borrar tu número.</span></span>
          </label>
          <div className="wa-row end" style={{ marginTop: 0 }}>
            <button className="btn primary" type="button" disabled={saving || !form.phone.trim()} onClick={save}>Guardar</button>
          </div>
        </div>
      </section>
      <aside className="wa-card" aria-labelledby="my-wa-preview" style={{ background: "var(--color-surface)" }}>
        <h3 id="my-wa-preview" style={{ margin: 0, fontSize: 14 }}>Así se ve el aviso</h3>
        <div style={{ display: "grid", gap: 8, marginTop: 12 }}>
          <div className="wa-bubble in" style={{ maxWidth: "100%" }}>Tienes una nueva solicitud de cita para el Mar 29/09 10:00: {window.location.origin}/solicitudes-de-cita</div>
          <div className="wa-bubble in" style={{ maxWidth: "100%" }}>Recordatorio: sigue pendiente la solicitud de cita del Mar 29/09 10:00: {window.location.origin}/solicitudes-de-cita</div>
        </div>
        <p className="wa-help" style={{ marginTop: 10 }}>
          No incluye el nombre ni datos del paciente. Si respondes al chat, te recordamos que las solicitudes se atienden en Clinerya. El recordatorio llega una sola vez, a las 4 h.
        </p>
      </aside>
    </div>
  );
}
