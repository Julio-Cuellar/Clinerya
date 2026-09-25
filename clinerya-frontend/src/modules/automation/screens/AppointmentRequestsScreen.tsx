import { useCallback, useEffect, useMemo, useState } from "react";
import { IconBell, IconCheck } from "@tabler/icons-react";
import { appointmentRequestsApi, getFriendlyError, staffApi } from "@shared/api/api";
import type { AppointmentRequestView, InboxChangePush, Slot } from "../types";
import { doctorInboxTopic } from "../realtime/destinations";
import { useRealtimeTopic } from "../realtime/useRealtimeTopic";
import {
  MAX_PROPOSED_OPTIONS,
  groupSlotsByDay,
  receivedAgo,
  slotLabel,
  slotTime,
  timeLeft,
  toggleOption
} from "../logic/requests";
import { LiveBadge } from "../components/LiveBadge";

const CLOCK_TICK_MS = 30_000;
const TOAST_MS = 6_000;
/** Una solicitud se marca como "Nueva" durante su primera hora. */
const NEW_FOR_MS = 60 * 60_000;

const LONG_DATE = new Intl.DateTimeFormat("es-MX", { weekday: "long", day: "numeric", month: "long" });

function requestedSlot(request: AppointmentRequestView): string {
  const day = LONG_DATE.format(new Date(request.start));
  return `${day.charAt(0).toUpperCase()}${day.slice(1)} · ${slotTime(request.start)} – ${slotTime(request.end)}`;
}

/**
 * Bandeja del medico: solicitudes que los pacientes hicieron por WhatsApp. Solo el medico asignado
 * las ve y las responde (aceptar, rechazar o proponer 1 a 3 horarios). Se actualiza en vivo.
 */
export function AppointmentRequestsScreen({ clinicId, userId }: { clinicId?: string; userId: string }) {
  const [requests, setRequests] = useState<AppointmentRequestView[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [doctorStaffId, setDoctorStaffId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [toast, setToast] = useState("");
  const [busy, setBusy] = useState(false);
  const [now, setNow] = useState(() => new Date());
  const [proposing, setProposing] = useState(false);
  const [slots, setSlots] = useState<Slot[]>([]);
  const [chosen, setChosen] = useState<Slot[]>([]);
  const [reason, setReason] = useState("");

  const load = useCallback(async () => {
    if (!clinicId) return;
    try {
      const pending = await appointmentRequestsApi.listPending(clinicId);
      setRequests(pending);
      setSelectedId((current) => (current && pending.some((r) => r.id === current) ? current : pending[0]?.id ?? null));
      setError("");
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  }, [clinicId]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!clinicId) return;
    staffApi
      .list(clinicId)
      .then((staff) => setDoctorStaffId(staff.find((member) => member.userId === userId)?.staffId ?? null))
      .catch(() => setDoctorStaffId(null));
  }, [clinicId, userId]);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), CLOCK_TICK_MS);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!toast) return undefined;
    const timer = window.setTimeout(() => setToast(""), TOAST_MS);
    return () => window.clearTimeout(timer);
  }, [toast]);

  const topic = clinicId && doctorStaffId ? doctorInboxTopic(clinicId, doctorStaffId) : null;
  const liveStatus = useRealtimeTopic<InboxChangePush>(
    topic,
    () => {
      setToast("Llegó una solicitud nueva");
      void load();
    },
    () => void load()
  );

  const selected = useMemo(() => requests.find((request) => request.id === selectedId) ?? null, [requests, selectedId]);

  const resetResponse = () => {
    setProposing(false);
    setSlots([]);
    setChosen([]);
    setReason("");
  };

  const select = (id: string) => {
    setSelectedId(id);
    setNotice("");
    resetResponse();
  };

  const respond = async (action: () => Promise<unknown>, done: string) => {
    setBusy(true);
    setError("");
    try {
      await action();
      setNotice(done);
      resetResponse();
      await load();
    } catch (caught) {
      setError(getFriendlyError(caught));
      await load();
    } finally {
      setBusy(false);
    }
  };

  const openProposal = async () => {
    if (!clinicId || !selected) return;
    setProposing(true);
    setChosen([]);
    try {
      setSlots(await appointmentRequestsApi.proposableSlots(clinicId, selected.id));
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  if (!clinicId) {
    return <section className="wa-page"><p className="wa-empty">Selecciona una clínica para ver sus solicitudes.</p></section>;
  }

  return (
    <section className="wa-page">
      {toast && (
        <div className="wa-toast" role="status">
          <IconBell size={16} aria-hidden="true" /> {toast}
        </div>
      )}
      <div className="wa-page-head">
        <p className="wa-help" style={{ margin: 0 }}>
          Pacientes que pidieron cita contigo por WhatsApp. El horario queda apartado 24 h mientras respondes.
        </p>
        <LiveBadge status={liveStatus} />
      </div>

      {error && <p className="alert error">{error}</p>}
      {notice && <p className="alert success">{notice}</p>}

      {loading ? (
        <p className="wa-empty">Cargando solicitudes…</p>
      ) : requests.length === 0 ? (
        <div className="wa-card wa-empty">
          No tienes solicitudes pendientes. Cuando un paciente pida cita contigo por WhatsApp aparecerá aquí.
        </div>
      ) : (
        <div className="wa-split">
          <div className="wa-list">
            <span className="wa-sub">Pendientes ({requests.length})</span>
            {requests.map((request) => {
              const left = timeLeft(request.expiresAt, now);
              return (
                <button
                  key={request.id}
                  type="button"
                  className={`wa-request${request.id === selectedId ? " selected" : ""}`}
                  onClick={() => select(request.id)}
                  aria-pressed={request.id === selectedId}
                >
                  <div className="wa-request-top">
                    <strong>{request.patientName ?? "Paciente"}</strong>
                    {left.urgent ? (
                      <span className="wa-pill warn">Vence pronto</span>
                    ) : now.getTime() - new Date(request.createdAt).getTime() < NEW_FOR_MS ? (
                      <span className="wa-pill info">Nueva</span>
                    ) : null}
                  </div>
                  <div style={{ fontSize: 13, marginTop: 6 }}>{slotLabel(request.start)}</div>
                  <div className="wa-sub">
                    Recibida {receivedAgo(request.createdAt, now)} · {left.label}
                  </div>
                </button>
              );
            })}
            <p className="wa-help">Si no respondes en 24 h, la solicitud vence, el horario se libera y se avisa al paciente.</p>
          </div>

          {selected && (
            <article className="wa-card" style={{ display: "flex", flexDirection: "column", gap: 16 }} aria-labelledby="request-title">
              <div>
                <h2 id="request-title" style={{ margin: 0, fontSize: 18 }}>{selected.patientName ?? "Paciente"}</h2>
                <p className="wa-help">Pidió cita por WhatsApp</p>
              </div>
              <dl className="wa-kv">
                <dt>Horario pedido</dt>
                <dd>{requestedSlot(selected)}</dd>
                <dt>Médico</dt>
                <dd>{selected.doctorName ?? "—"} (tú)</dd>
                <dt>Plazo</dt>
                <dd>{timeLeft(selected.expiresAt, now).label}</dd>
              </dl>

              <div className="wa-actions">
                <button
                  className="btn primary"
                  type="button"
                  disabled={busy}
                  onClick={() => respond(() => appointmentRequestsApi.accept(clinicId, selected.id), "Cita agendada. Le avisamos al paciente por WhatsApp.")}
                >
                  <IconCheck size={16} aria-hidden="true" /> Aceptar cita
                </button>
                <button className="btn secondary" type="button" disabled={busy} aria-expanded={proposing} onClick={openProposal}>
                  Proponer otros horarios
                </button>
                <button
                  className="btn destructive"
                  type="button"
                  disabled={busy}
                  onClick={() => respond(() => appointmentRequestsApi.reject(clinicId, selected.id, reason), "Solicitud rechazada. Le avisamos al paciente.")}
                >
                  Rechazar
                </button>
              </div>

              {proposing && (
                <div className="wa-propose">
                  <div className="wa-row between">
                    <strong style={{ fontSize: 14 }}>Proponer otros horarios</strong>
                    <span className="wa-sub">Elige de 1 a {MAX_PROPOSED_OPTIONS} · seleccionados {chosen.length}</span>
                  </div>
                  <p className="wa-help">
                    Horarios libres de tu agenda en los próximos 14 días. Se apartan al enviarlos y el paciente tiene 24 h para elegir uno.
                  </p>
                  {slots.length === 0 ? (
                    <p className="wa-sub">No hay horarios libres en los próximos 14 días.</p>
                  ) : (
                    groupSlotsByDay(slots).map((group) => (
                      <div key={group.day}>
                        <div className="wa-day">{group.label}</div>
                        <div className="wa-slots">
                          {group.slots.map((slot) => {
                            const isRequested = slot.start === selected.start;
                            const isChosen = chosen.some((option) => option.start === slot.start);
                            return (
                              <button
                                key={slot.start}
                                type="button"
                                className="wa-slot"
                                aria-pressed={isChosen}
                                disabled={isRequested || (!isChosen && chosen.length >= MAX_PROPOSED_OPTIONS)}
                                title={isRequested ? "Es el horario que pidió el paciente: acéptalo en lugar de proponerlo" : undefined}
                                onClick={() => setChosen((current) => toggleOption(current, slot))}
                              >
                                {slotTime(slot.start)}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    ))
                  )}
                  <div className="wa-row end">
                    <button className="btn ghost" type="button" onClick={resetResponse}>Cancelar</button>
                    <button
                      className="btn primary"
                      type="button"
                      disabled={busy || chosen.length === 0}
                      onClick={() =>
                        respond(
                          () => appointmentRequestsApi.propose(clinicId, selected.id, chosen),
                          "Enviamos las opciones al paciente. Tiene 24 h para elegir una."
                        )
                      }
                    >
                      Enviar {chosen.length === 1 ? "1 opción" : `${chosen.length} opciones`} al paciente
                    </button>
                  </div>
                </div>
              )}

              <label className="field">
                <span>Si rechazas: motivo (opcional, se le envía al paciente)</span>
                <input
                  value={reason}
                  maxLength={500}
                  placeholder="Ej. Ese día no hay consulta; con gusto te atendemos otro día"
                  onChange={(event) => setReason(event.target.value)}
                />
              </label>
            </article>
          )}
        </div>
      )}
    </section>
  );
}
