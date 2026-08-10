import { useEffect, useMemo, useState } from "react";
import {
  IconArrowLeft,
  IconCalendarEvent,
  IconCash,
  IconClipboardList,
  IconFileText,
  IconFolderOpen,
  IconSearch
} from "@tabler/icons-react";
import { APPOINTMENT_STATUS_LABELS, type AppointmentResponse } from "@modules/agenda/types";
import { PAYMENT_METHOD_LABELS, type TicketResponse } from "@modules/cash/types";
import { serializeAttachments } from "@modules/records/types";
import { ITEM_PROGRESS_STATUS_LABELS, QUOTATION_STATUS_LABELS, type QuotationResponse } from "@modules/treatments/quotationTypes";
import type { PatientResponse } from "@modules/patients/types";
import { genderLabel } from "@modules/patients/constants/patientOptions";
import { getAge } from "@shared/utils/getAge";
import { agendaApi, attachmentsApi, getFriendlyError, quotationsApi, ticketsApi } from "@shared/api/api";
import { FileFieldEditor } from "@shared/ui/FileFieldEditor";
import { PatientHistoryPanel } from "@modules/records/components/PatientHistoryPanel";
import { TemplatesPanel } from "@modules/records/components/TemplatesPanel";

type MainTab = "expedientes" | "plantillas";
type RecordTab = "historia" | "tratamientos" | "citas" | "pagos" | "estudios";

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
    const from = new Date();
    from.setFullYear(from.getFullYear() - 2);
    const to = new Date();
    to.setFullYear(to.getFullYear() + 2);

    setLoading(true);
    setError("");
    agendaApi
      .listByRange(clinicId, from.toISOString(), to.toISOString())
      .then((list) =>
        setAppointments(
          list
            .filter((appointment) => appointment.patientId === patient.id)
            .sort((a, b) => new Date(b.scheduledStart).getTime() - new Date(a.scheduledStart).getTime())
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
  const [tab, setTab] = useState<RecordTab>("historia");
  const age = getAge(patient.dateOfBirth);

  const tabs: Array<{ key: RecordTab; label: string; Icon: typeof IconFileText }> = [
    { key: "historia", label: "Historia clinica", Icon: IconFileText },
    { key: "tratamientos", label: "Tratamientos", Icon: IconClipboardList },
    { key: "citas", label: "Citas", Icon: IconCalendarEvent },
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

      {tab === "historia" && (
        <PatientHistoryPanel clinicId={clinicId} patient={patient} onChangePatient={onBack} />
      )}
      {tab === "tratamientos" && <TreatmentsTab clinicId={clinicId} patient={patient} />}
      {tab === "citas" && <AppointmentsTab clinicId={clinicId} patient={patient} />}
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
