import { useEffect, useState } from "react";
import { agendaApi, doctorChannelsApi, getFriendlyError } from "@shared/api/api";
import type { DoctorResponse } from "@modules/agenda/types";
import type { DoctorChannelView } from "../types";
import { doctorChannelStatus } from "../logic/assistant";
import { formatPhone } from "../logic/phone";

interface Row {
  doctor: DoctorResponse;
  channel: DoctorChannelView;
}

/** Celulares de los medicos para avisos. Quien administra integraciones los edita por el medico. */
export function DoctorChannelsSection({ clinicId, canManage }: { clinicId: string; canManage: boolean }) {
  const [rows, setRows] = useState<Row[]>([]);
  const [editing, setEditing] = useState<string | null>(null);
  const [form, setForm] = useState({ phone: "", consent: false, active: false });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let cancelled = false;
    agendaApi
      .listDoctors(clinicId)
      .then((doctors) => Promise.all(doctors.map(async (doctor) => ({ doctor, channel: await doctorChannelsApi.get(clinicId, doctor.staffId) }))))
      .then((loaded) => {
        if (!cancelled) setRows(loaded);
      })
      .catch((caught) => {
        if (!cancelled) setError(getFriendlyError(caught));
      });
    return () => {
      cancelled = true;
    };
  }, [clinicId]);

  const startEdit = (row: Row) => {
    setEditing(row.doctor.staffId);
    setForm({ phone: formatPhone(row.channel.phone), consent: Boolean(row.channel.consentAt), active: row.channel.active });
    setError("");
  };

  const save = async (staffId: string) => {
    setSaving(true);
    setError("");
    try {
      const channel = await doctorChannelsApi.update(clinicId, staffId, form);
      setRows((current) => current.map((row) => (row.doctor.staffId === staffId ? { ...row, channel } : row)));
      setEditing(null);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  if (rows.length === 0 && !error) {
    return <p className="wa-sub" style={{ marginTop: 8 }}>No hay médicos que atiendan pacientes en esta clínica.</p>;
  }

  return (
    <>
      {error && <p className="alert error" style={{ marginTop: 8 }}>{error}</p>}
      <table className="wa-table">
        <thead>
          <tr>
            <th scope="col">Médico</th>
            <th scope="col">Celular</th>
            <th scope="col">Estado</th>
            <th scope="col"><span style={{ position: "absolute", left: -9999 }}>Acciones</span></th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const status = doctorChannelStatus(row.channel);
            const isEditing = editing === row.doctor.staffId;
            return (
              <tr key={row.doctor.staffId}>
                <td style={{ fontWeight: 600 }}>{row.doctor.fullName}</td>
                <td>
                  {isEditing ? (
                    <div style={{ display: "grid", gap: 8 }}>
                      <label className="field">
                        <span>Celular</span>
                        <input value={form.phone} placeholder="55 1234 5678" onChange={(e) => setForm({ ...form, phone: e.target.value })} />
                      </label>
                      <label className="wa-check">
                        <input type="checkbox" checked={form.consent}
                          onChange={(e) => setForm({ ...form, consent: e.target.checked, active: e.target.checked && form.active })} />
                        <span>El médico aceptó recibir avisos en este número</span>
                      </label>
                      <label className="wa-check">
                        <input type="checkbox" checked={form.active} disabled={!form.consent}
                          onChange={(e) => setForm({ ...form, active: e.target.checked })} />
                        <span>Enviarle avisos ahora</span>
                      </label>
                    </div>
                  ) : (
                    row.channel.phone ? formatPhone(row.channel.phone) : <span className="wa-sub">Sin registrar</span>
                  )}
                </td>
                <td><span className={`wa-pill ${status.tone}`}>{status.label}</span></td>
                <td style={{ textAlign: "right" }}>
                  {canManage && (isEditing ? (
                    <div className="wa-row" style={{ justifyContent: "flex-end" }}>
                      <button className="btn ghost" type="button" onClick={() => setEditing(null)}>Cancelar</button>
                      <button className="btn primary" type="button" disabled={saving} onClick={() => save(row.doctor.staffId)}>Guardar</button>
                    </div>
                  ) : (
                    <button className="btn ghost" type="button" onClick={() => startEdit(row)}>
                      {row.channel.phone ? "Editar" : "Agregar celular"}
                    </button>
                  ))}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </>
  );
}
