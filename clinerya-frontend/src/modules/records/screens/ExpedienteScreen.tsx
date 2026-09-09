import { useEffect, useMemo, useState } from "react";
import {
  IconArrowLeft,
  IconCalendarEvent,
  IconCash,
  IconClipboardList,
  IconFileText,
  IconFolderOpen,
  IconHistory,
  IconPill,
  IconSearch
} from "@tabler/icons-react";
import { APPOINTMENT_STATUS_LABELS, type AppointmentResponse } from "@modules/agenda/types";
import { PAYMENT_METHOD_LABELS, type TicketResponse } from "@modules/cash/types";
import { serializeAttachments, type AttachmentMeta, type ClinicalNoteResponse } from "@modules/records/types";
import type { Prescription } from "@modules/records/prescriptionTypes";
import { ITEM_PROGRESS_STATUS_LABELS, QUOTATION_STATUS_LABELS, type QuotationResponse } from "@modules/treatments/quotationTypes";
import type { PatientResponse } from "@modules/patients/types";
import { genderLabel } from "@modules/patients/constants/patientOptions";
import { getAge } from "@shared/utils/getAge";
import {
  agendaApi,
  attachmentsApi,
  clinicalNotesApi,
  getFriendlyError,
  prescriptionsApi,
  quotationsApi,
  ticketsApi
} from "@shared/api/api";
import { FileFieldEditor } from "@shared/ui/FileFieldEditor";
import { PatientHistoryPanel } from "@modules/records/components/PatientHistoryPanel";
import { PatientClinicalHeader } from "@modules/records/components/PatientClinicalHeader";
import { PrescriptionSection } from "@modules/records/components/PrescriptionSection";
import { TemplatesPanel } from "@modules/records/components/TemplatesPanel";

type MainTab = "expedientes" | "plantillas";
type RecordTab = "cronologia" | "historia" | "tratamientos" | "citas" | "recetas" | "pagos" | "estudios";

const STUDIES_ELEMENT_ID = "patient_studies";
const currencyFormatter = new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" });

function patientName(patient: PatientResponse) {
  return [patient.firstName, patient.lastNamePaterno, patient.lastNameMaterno].filter(Boolean).join(" ");
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("es-MX", {
    dateStyle: "medium",
    timeStyle: "short"
  });
}

function quoteStatusBadge(status: QuotationResponse["status"]) {
  if (status === "ACCEPTED") return "success";
  if (status === "REJECTED" || status === "EXPIRED") return "neutral";
  return "warning";
}

function appointmentStatusClass(status: AppointmentResponse["status"]) {
  if (status === "COMPLETED") return "success";
  if (status === "CANCELLED" || status === "NO_SHOW") return "neutral";
  if (status === "CONFIRMED") return "success";
  return "warning";
}

function PatientSelector({
  patients,
  hasClinic,
  search,
  setSearch,
  onSelect
}: {
  patients: PatientResponse[];
  hasClinic: boolean;
  search: string;
  setSearch: (value: string) => void;
  onSelect: (patient: PatientResponse) => void;
}) {
  const filteredPatients = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return patients;
    return patients.filter((patient) =>
      [patient.firstName, patient.lastNamePaterno, patient.lastNameMaterno, patient.curp, patient.phone, patient.email]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()
        .includes(term)
    );
  }, [patients, search]);

  return (
    <article className="panel full expediente-patient-panel">
      <div className="panel-heading">
        <div>
          <h2>Expedientes</h2>
          <span className="panel-subtitle">Selecciona un paciente para consultar su expediente completo.</span>
        </div>
        <span className="badge neutral">{patients.length}</span>
      </div>

      {!hasClinic && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Completa los datos de tu clinica</strong>
            <span>Necesitas una clinica activa para consultar expedientes.</span>
          </div>
        </div>
      )}

      {hasClinic && patients.length > 0 && (
        <div className="table-toolbar">
          <label className="search-field expediente-search">
            <IconSearch size={16} aria-hidden="true" />
            <input
              type="search"
              placeholder="Buscar por nombre, CURP, telefono o correo"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </label>
        </div>
      )}

      {hasClinic && patients.length === 0 && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Sin pacientes registrados</strong>
            <span>Registra pacientes para comenzar a crear expedientes.</span>
          </div>
        </div>
      )}

      {hasClinic && patients.length > 0 && (
        <div className="table-wrapper">
          <table className="data-table expediente-patient-table">
            <thead>
              <tr>
                <th>Paciente</th>
                <th>Edad</th>
                <th>Genero</th>
                <th>Contacto</th>
                <th>CURP</th>
              </tr>
            </thead>
            <tbody>
              {filteredPatients.map((patient) => {
                const age = getAge(patient.dateOfBirth);
                return (
                  <tr key={patient.id} onClick={() => onSelect(patient)}>
                    <td>
                      <strong>{patientName(patient)}</strong>
                      <span className="table-subtext">{patient.occupation || "Sin ocupacion registrada"}</span>
                    </td>
                    <td>{age !== undefined ? `${age} anos` : "Sin registrar"}</td>
                    <td>{genderLabel(patient.gender) ?? "Sin registrar"}</td>
                    <td>{patient.phone || patient.email || "Sin contacto"}</td>
                    <td>{patient.curp || "Sin registrar"}</td>
                  </tr>
                );
              })}
              {filteredPatients.length === 0 && (
                <tr>
                  <td colSpan={5}>Sin resultados para "{search}"</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </article>
  );
}

type TimelineKind = "cita" | "nota" | "receta" | "pago" | "estudio";

interface TimelineEvent {
  id: string;
  kind: TimelineKind;
  date: string;
  title: string;
  detail: string;
  badge?: { label: string; tone: string };
  target: RecordTab;
}

const TIMELINE_KIND_LABELS: Record<TimelineKind, string> = {
  cita: "Cita",
  nota: "Nota clinica",
  receta: "Receta",
  pago: "Pago",
  estudio: "Estudio"
};

function noteSummary(note: ClinicalNoteResponse) {
  const text = note.assessment || note.plan || note.subjective || note.objective || "";
  return text.length > 120 ? `${text.slice(0, 117)}...` : text || "Sin contenido";
}

function CronologiaTab({
  clinicId,
  patient,
  onNavigate
}: {
  clinicId: string;
  patient: PatientResponse;
  onNavigate: (tab: RecordTab) => void;
}) {
  const [events, setEvents] = useState<TimelineEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState<TimelineKind | "todos">("todos");

  useEffect(() => {
    setLoading(true);
    setError("");
    Promise.allSettled([
      agendaApi.listByPatient(clinicId, patient.id),
      clinicalNotesApi.listByPatient(patient.id, clinicId),
      prescriptionsApi.listByPatient(clinicId, patient.id),
      ticketsApi.listByPatient(clinicId, patient.id),
      attachmentsApi.list(patient.id, clinicId, STUDIES_ELEMENT_ID)
    ])
      .then(([citas, notas, recetas, pagos, estudios]) => {
        const merged: TimelineEvent[] = [];

        if (citas.status === "fulfilled") {
          for (const cita of citas.value as AppointmentResponse[]) {
            merged.push({
              id: `cita-${cita.id}`,
              kind: "cita",
              date: cita.scheduledStart,
              title: cita.reason || "Consulta",
              detail: cita.notes || "Sin notas",
              badge: {
                label: APPOINTMENT_STATUS_LABELS[cita.status],
                tone: appointmentStatusClass(cita.status)
              },
              target: "citas"
            });
          }
        }

        if (notas.status === "fulfilled") {
          for (const nota of notas.value as ClinicalNoteResponse[]) {
            merged.push({
              id: `nota-${nota.id}`,
              kind: "nota",
              date: nota.createdAt,
              title: "Nota clinica",
              detail: noteSummary(nota),
              badge:
                nota.status === "SIGNED"
                  ? { label: "Firmada", tone: "success" }
                  : { label: "Borrador", tone: "warning" },
              target: "historia"
            });
          }
        }

        if (recetas.status === "fulfilled") {
          for (const receta of recetas.value as Prescription[]) {
            const meds = receta.items.map((item) => item.medicationName).filter(Boolean);
            merged.push({
              id: `receta-${receta.id}`,
              kind: "receta",
              date: receta.createdAt,
              title: meds.length > 0 ? meds.join(", ") : "Receta",
              detail: receta.notes || `${receta.items.length} medicamento(s)`,
              target: "recetas"
            });
          }
        }

        if (pagos.status === "fulfilled") {
          for (const ticket of pagos.value as TicketResponse[]) {
            merged.push({
              id: `pago-${ticket.id}`,
              kind: "pago",
              date: ticket.createdAt,
              title: `Pago #${ticket.folio}`,
              detail: ticket.concept || "Sin concepto",
              badge: {
                label: currencyFormatter.format(ticket.totalAmount),
                tone: ticket.status === "ACTIVE" ? "success" : "neutral"
              },
              target: "pagos"
            });
          }
        }

        if (estudios.status === "fulfilled") {
          for (const study of estudios.value as AttachmentMeta[]) {
            merged.push({
              id: `estudio-${study.id}`,
              kind: "estudio",
              date: study.createdAt,
              title: study.originalFilename,
              detail: "Documento de estudio",
              target: "estudios"
            });
          }
        }

        merged.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
        setEvents(merged);

        if (
          citas.status === "rejected" &&
          notas.status === "rejected" &&
          recetas.status === "rejected" &&
          pagos.status === "rejected" &&
          estudios.status === "rejected"
        ) {
          setError("No se pudo cargar la cronologia del paciente.");
        }
      })
      .finally(() => setLoading(false));
  }, [clinicId, patient.id]);

  const visibleEvents = filter === "todos" ? events : events.filter((event) => event.kind === filter);

  return (
    <article className="panel full">
      <div className="panel-heading">
        <div>
          <h2>Cronologia</h2>
          <span className="panel-subtitle">Citas, notas, recetas, pagos y estudios ordenados por fecha.</span>
        </div>
        <span className="badge neutral">{visibleEvents.length}</span>
      </div>

      <div className="table-toolbar">
        <label className="field">
          <span>Filtrar por tipo</span>
          <select value={filter} onChange={(event) => setFilter(event.target.value as TimelineKind | "todos")}>
            <option value="todos">Todos los eventos</option>
            <option value="cita">Citas</option>
            <option value="nota">Notas clinicas</option>
            <option value="receta">Recetas</option>
            <option value="pago">Pagos</option>
            <option value="estudio">Estudios</option>
          </select>
        </label>
      </div>

      {loading && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Cargando cronologia...</strong>
          </div>
        </div>
      )}
      {error && <p className="alert error">{error}</p>}

      {!loading && !error && visibleEvents.length === 0 && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Sin eventos registrados</strong>
            <span>A medida que se agenden citas, se firmen notas o se emitan recetas apareceran aqui.</span>
          </div>
        </div>
      )}

      {!loading && visibleEvents.length > 0 && (
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Tipo</th>
                <th>Detalle</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {visibleEvents.map((event) => (
                <tr key={event.id} onClick={() => onNavigate(event.target)}>
                  <td>{formatDateTime(event.date)}</td>
                  <td>
                    <span className="badge neutral">{TIMELINE_KIND_LABELS[event.kind]}</span>
                  </td>
                  <td>
                    <strong>{event.title}</strong>
                    <span className="table-subtext">{event.detail}</span>
                  </td>
                  <td>{event.badge && <span className={`badge ${event.badge.tone}`}>{event.badge.label}</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </article>
  );
}

function TreatmentsTab({ clinicId, patient }: { clinicId: string; patient: PatientResponse }) {
  const [quotations, setQuotations] = useState<QuotationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    setLoading(true);
    setError("");
    quotationsApi
      .listByPatient(patient.id, clinicId)
      .then(setQuotations)
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  }, [clinicId, patient.id]);

  const treatmentRows = quotations.flatMap((quotation) => quotation.items.map((item) => ({ quotation, item })));

  return (
    <article className="panel full">
      <div className="panel-heading">
        <div>
          <h2>Tratamientos</h2>
          <span className="panel-subtitle">Procedimientos vinculados a cotizaciones del paciente.</span>
        </div>
        <span className="badge neutral">{treatmentRows.length}</span>
      </div>

      {loading && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Cargando tratamientos...</strong>
          </div>
        </div>
      )}
      {error && <p className="alert error">{error}</p>}

      {!loading && treatmentRows.length === 0 && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Sin tratamientos registrados</strong>
            <span>Cuando existan cotizaciones o planes de tratamiento, apareceran aqui.</span>
          </div>
        </div>
      )}

      {!loading && treatmentRows.length > 0 && (
        <div className="table-wrapper">
          <table className="data-table no-row-click">
            <thead>
              <tr>
                <th>Tratamiento</th>
                <th>Diente</th>
                <th>Cotizacion</th>
                <th>Avance</th>
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              {treatmentRows.map(({ quotation, item }) => (
                <tr key={item.id}>
                  <td>
                    <strong>{item.description}</strong>
                    <span className="table-subtext">{quotation.notes || "Sin notas"}</span>
                  </td>
                  <td>{item.toothNumber ?? "General"}</td>
                  <td>
                    <span className={`badge ${quoteStatusBadge(quotation.status)}`}>
                      {QUOTATION_STATUS_LABELS[quotation.status]}
                    </span>
                    <span className="table-subtext">{quotation.quotationDate}</span>
                  </td>
                  <td>{ITEM_PROGRESS_STATUS_LABELS[item.progressStatus]}</td>
                  <td>{currencyFormatter.format(item.subtotal)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </article>
  );
}

function AppointmentsTab({ clinicId, patient }: { clinicId: string; patient: PatientResponse }) {
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    setLoading(true);
    setError("");
    agendaApi
      .listByPatient(clinicId, patient.id)
      .then((list) =>
        setAppointments(
          [...list].sort(
            (a, b) => new Date(b.scheduledStart).getTime() - new Date(a.scheduledStart).getTime()
          )
        )
      )
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  }, [clinicId, patient.id]);

  return (
    <article className="panel full">
      <div className="panel-heading">
        <div>
          <h2>Historial de citas</h2>
          <span className="panel-subtitle">Citas registradas para este paciente.</span>
        </div>
        <span className="badge neutral">{appointments.length}</span>
      </div>

      {loading && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Cargando citas...</strong>
          </div>
        </div>
      )}
      {error && <p className="alert error">{error}</p>}

      {!loading && appointments.length === 0 && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Sin citas registradas</strong>
            <span>Las citas agendadas y concluidas apareceran en esta seccion.</span>
          </div>
        </div>
      )}

      {!loading && appointments.length > 0 && (
        <div className="table-wrapper">
          <table className="data-table no-row-click">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Motivo</th>
                <th>Estado</th>
                <th>Notas</th>
              </tr>
            </thead>
            <tbody>
              {appointments.map((appointment) => (
                <tr key={appointment.id}>
                  <td>
                    <strong>{formatDateTime(appointment.scheduledStart)}</strong>
                    <span className="table-subtext">Fin: {formatDateTime(appointment.scheduledEnd)}</span>
                  </td>
                  <td>{appointment.reason || "Consulta"}</td>
                  <td>
                    <span className={`badge ${appointmentStatusClass(appointment.status)}`}>
                      {APPOINTMENT_STATUS_LABELS[appointment.status]}
                    </span>
                  </td>
                  <td>{appointment.notes || "Sin notas"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </article>
  );
}

function PaymentsTab({ clinicId, patient }: { clinicId: string; patient: PatientResponse }) {
  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    setLoading(true);
    setError("");
    ticketsApi
      .listByPatient(clinicId, patient.id)
      .then((list) => setTickets([...list].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())))
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  }, [clinicId, patient.id]);

  const activeTotal = tickets
    .filter((ticket) => ticket.status === "ACTIVE")
    .reduce((sum, ticket) => sum + ticket.totalAmount, 0);

  return (
    <article className="panel full">
      <div className="panel-heading">
        <div>
          <h2>Pagos</h2>
          <span className="panel-subtitle">Tickets y pagos registrados para el paciente.</span>
        </div>
        <span className="badge success">{currencyFormatter.format(activeTotal)}</span>
      </div>

      {loading && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Cargando pagos...</strong>
          </div>
        </div>
      )}
      {error && <p className="alert error">{error}</p>}

      {!loading && tickets.length === 0 && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Sin pagos registrados</strong>
            <span>Los pagos hechos en caja apareceran aqui.</span>
          </div>
        </div>
      )}

      {!loading && tickets.length > 0 && (
        <div className="table-wrapper">
          <table className="data-table no-row-click">
            <thead>
              <tr>
                <th>Folio</th>
                <th>Fecha</th>
                <th>Concepto</th>
                <th>Forma de pago</th>
                <th>Monto</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map((ticket) => (
                <tr key={ticket.id}>
                  <td>#{ticket.folio}</td>
                  <td>{formatDateTime(ticket.createdAt)}</td>
                  <td>{ticket.concept || "Sin concepto"}</td>
                  <td>
                    {ticket.paymentLines.length > 0
                      ? ticket.paymentLines.map((line) => PAYMENT_METHOD_LABELS[line.method]).join(" + ")
                      : "Sin registrar"}
                  </td>
                  <td>{currencyFormatter.format(ticket.totalAmount)}</td>
                  <td>
                    <span className={`badge ${ticket.status === "ACTIVE" ? "success" : "neutral"}`}>
                      {ticket.status === "ACTIVE" ? "Activo" : "Anulado"}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </article>
  );
}

function StudiesTab({ clinicId, patient }: { clinicId: string; patient: PatientResponse }) {
  const [value, setValue] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    setLoading(true);
    setError("");
    attachmentsApi
      .list(patient.id, clinicId, STUDIES_ELEMENT_ID)
      .then((list) => setValue(serializeAttachments(list)))
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  }, [clinicId, patient.id]);

  return (
    <article className="panel full">
      <div className="panel-heading">
        <div>
          <h2>Estudios</h2>
          <span className="panel-subtitle">Rayos X, laboratorios, ultrasonidos, resonancias y otros documentos de apoyo.</span>
        </div>
      </div>

      {loading && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Cargando estudios...</strong>
          </div>
        </div>
      )}
      {error && <p className="alert error">{error}</p>}
      {!loading && (
        <FileFieldEditor
          patientId={patient.id}
          clinicId={clinicId}
          elementId={STUDIES_ELEMENT_ID}
          value={value}
          onChange={setValue}
        />
      )}
    </article>
  );
}

function PatientRecord({
  clinicId,
  patient,
  onBack
}: {
  clinicId: string;
  patient: PatientResponse;
  onBack: () => void;
}) {
  const [tab, setTab] = useState<RecordTab>("cronologia");
  const age = getAge(patient.dateOfBirth);

  const tabs: Array<{ key: RecordTab; label: string; Icon: typeof IconFileText }> = [
    { key: "cronologia", label: "Cronologia", Icon: IconHistory },
    { key: "historia", label: "Historia clinica", Icon: IconFileText },
    { key: "tratamientos", label: "Tratamientos", Icon: IconClipboardList },
    { key: "citas", label: "Citas", Icon: IconCalendarEvent },
    { key: "recetas", label: "Recetas", Icon: IconPill },
    { key: "pagos", label: "Pagos", Icon: IconCash },
    { key: "estudios", label: "Estudios", Icon: IconFolderOpen }
  ];

  return (
    <>
      <article className="panel full expediente-record-header">
        <div className="panel-heading">
          <div>
            <h2>{patientName(patient)}</h2>
            <span className="panel-subtitle">
              {age !== undefined ? `${age} anos` : "Edad sin registrar"} - {patient.phone || patient.email || "Sin contacto"} - {patient.curp || "CURP sin registrar"}
            </span>
          </div>
          <button className="btn secondary" type="button" onClick={onBack}>
            <IconArrowLeft size={16} aria-hidden="true" />
            Volver a pacientes
          </button>
        </div>
        <div className="expediente-record-tabs" role="tablist" aria-label="Secciones del expediente">
          {tabs.map(({ key, label, Icon }) => (
            <button
              key={key}
              className={tab === key ? "active" : ""}
              type="button"
              onClick={() => setTab(key)}
              role="tab"
              aria-selected={tab === key}
            >
              <Icon size={16} aria-hidden="true" />
              {label}
            </button>
          ))}
        </div>
      </article>

      <PatientClinicalHeader clinicId={clinicId} patientId={patient.id} />

      {tab === "cronologia" && (
        <CronologiaTab clinicId={clinicId} patient={patient} onNavigate={setTab} />
      )}
      {tab === "historia" && (
        <PatientHistoryPanel
          clinicId={clinicId}
          patient={patient}
          onChangePatient={onBack}
          showClinicalHeader={false}
        />
      )}
      {tab === "tratamientos" && <TreatmentsTab clinicId={clinicId} patient={patient} />}
      {tab === "citas" && <AppointmentsTab clinicId={clinicId} patient={patient} />}
      {tab === "recetas" && (
        <div style={{ gridColumn: "1 / -1" }}>
          <PrescriptionSection clinicId={clinicId} patient={patient} />
        </div>
      )}
      {tab === "pagos" && <PaymentsTab clinicId={clinicId} patient={patient} />}
      {tab === "estudios" && <StudiesTab clinicId={clinicId} patient={patient} />}
    </>
  );
}

export function ExpedienteScreen({
  clinicId,
  hasClinic,
  patients
}: {
  clinicId?: string;
  hasClinic: boolean;
  patients: PatientResponse[];
}) {
  const [mainTab, setMainTab] = useState<MainTab>("expedientes");
  const [selectedPatient, setSelectedPatient] = useState<PatientResponse | null>(null);
  const [search, setSearch] = useState("");

  return (
    <section className="dashboard-grid expediente-screen">
      <div className="tab-switch">
        <button
          className={mainTab === "expedientes" ? "active" : ""}
          type="button"
          onClick={() => setMainTab("expedientes")}
        >
          Expedientes
        </button>
        <button
          className={mainTab === "plantillas" ? "active" : ""}
          type="button"
          onClick={() => setMainTab("plantillas")}
        >
          Plantillas
        </button>
      </div>

      {mainTab === "plantillas" && <TemplatesPanel clinicId={clinicId} hasClinic={hasClinic} />}

      {mainTab === "expedientes" && selectedPatient && clinicId && (
        <PatientRecord clinicId={clinicId} patient={selectedPatient} onBack={() => setSelectedPatient(null)} />
      )}

      {mainTab === "expedientes" && (!selectedPatient || !clinicId) && (
        <PatientSelector
          patients={patients}
          hasClinic={hasClinic}
          search={search}
          setSearch={setSearch}
          onSelect={setSelectedPatient}
        />
      )}
    </section>
  );
}
