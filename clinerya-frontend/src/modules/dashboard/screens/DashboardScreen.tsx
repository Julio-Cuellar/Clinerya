import {
  IconAlertTriangle,
  IconArrowRight,
  IconBuildingHospital,
  IconCalendar,
  IconCash,
  IconCheck,
  IconClock,
  IconPlus,
  IconReceipt,
  IconRefresh,
  IconSettings,
  IconStethoscope,
  IconUserPlus
} from "@tabler/icons-react";
import { useEffect, useMemo, useState } from "react";
import { modules, type ModuleKey } from "@app/constants/modules";
import { accountingApi, agendaApi, cashSessionsApi, ticketsApi } from "@shared/api/api";
import type { AppointmentResponse, DoctorResponse } from "@modules/agenda/types";
import type { CashSessionResponse, PendingAppointmentChargeResponse } from "@modules/cash/types";
import type { ClinicResponse } from "@modules/clinics/types";
import type { PatientResponse } from "@modules/patients/types";

const STATUS_LABELS: Record<string, string> = {
  SCHEDULED: "Programada",
  CONFIRMED: "Confirmada",
  COMPLETED: "Completada",
  CANCELLED: "Cancelada",
  NO_SHOW: "No asistió"
};

const moduleSummary: Record<Exclude<ModuleKey, "dashboard">, string> = {
  agenda: "Agenda diaria, doctores y citas por confirmar.",
  solicitudes: "Citas que los pacientes pidieron por WhatsApp.",
  chats: "Conversaciones del asistente de WhatsApp.",
  consultorios: "Espacios de atención, estado y disponibilidad de la clínica.",
  atencion: "Atención médica, notas SOAP, recetas y consumos.",
  pacientes: "Directorio clínico y datos administrativos.",
  expediente: "Historias clínicas, plantillas y documentos.",
  tratamientos: "Catálogo, cotizaciones y planes de tratamiento.",
  caja: "Turnos, cobros, gastos y tickets.",
  inventario: "Materiales, lotes, entradas y salidas.",
  contabilidad: "Saldos iniciales, diario, bancos y reportes.",
  personal: "Accesos externos y colaboración por expediente.",
  configuracion: "Propiedades de la clinica, consultorios e integraciones."
};

const currencyFormatter = new Intl.NumberFormat("es-MX", {
  style: "currency",
  currency: "MXN",
  maximumFractionDigits: 0
});

function clinicIsComplete(clinic?: ClinicResponse) {
  return Boolean(clinic?.legalName && clinic.rfc && clinic.addressStreet);
}

function patientName(patients: PatientResponse[], patientId?: string): string {
  if (!patientId) return "Paciente no asignado";
  const found = patients.find((patient) => patient.id === patientId);
  if (!found) return "Paciente registrado";
  return `${found.firstName} ${found.lastNamePaterno} ${found.lastNameMaterno ?? ""}`.trim();
}

function doctorLabel(doctors: DoctorResponse[], staffId?: string): string {
  if (!staffId) return "Sin doctor asignado";
  return doctors.find((doctor) => doctor.staffId === staffId)?.fullName ?? "Doctor de guardia";
}

function formatTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--:--";
  return date.toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit", hour12: false });
}

function appointmentStatusClass(appointment: AppointmentResponse): string {
  if (appointment.status === "COMPLETED") return "status-completed";
  if (appointment.status === "CONFIRMED") return "status-confirmed";
  if (appointment.status === "CANCELLED" || appointment.status === "NO_SHOW") return "status-cancelled";
  return "status-scheduled";
}

export function DashboardScreen({
  clinicId,
  clinic,
  hasClinic,
  patients,
  patientsLoading,
  pendingCajaCount,
  allowedModules,
  onNavigateModule,
  onCreatePatient,
  onEditClinic,
  onOpenConfiguration
}: {
  clinicId?: string;
  clinic?: ClinicResponse;
  hasClinic: boolean;
  patients: PatientResponse[];
  patientsLoading: boolean;
  pendingCajaCount: number;
  allowedModules: Set<ModuleKey>;
  onNavigateModule: (moduleKey: ModuleKey) => void;
  onCreatePatient: () => void;
  onEditClinic: () => void;
  onOpenConfiguration: () => void;
}) {
  const completeClinic = clinicIsComplete(clinic);
  const [accountingReady, setAccountingReady] = useState(false);
  const [accountingLoading, setAccountingLoading] = useState(false);
  const [todayAppointments, setTodayAppointments] = useState<AppointmentResponse[]>([]);
  const [doctors, setDoctors] = useState<DoctorResponse[]>([]);
  const [currentSession, setCurrentSession] = useState<CashSessionResponse | null>(null);
  const [pendingCharges, setPendingCharges] = useState<PendingAppointmentChargeResponse[]>([]);
  const [dashboardLoading, setDashboardLoading] = useState(false);
  const [appointmentsError, setAppointmentsError] = useState(false);
  const [cashError, setCashError] = useState(false);
  const [chargesError, setChargesError] = useState(false);
  const [lastUpdatedAt, setLastUpdatedAt] = useState<Date | null>(null);
  const [refreshVersion, setRefreshVersion] = useState(0);

  const visibleModules = modules.filter((item) => item.key !== "dashboard" && allowedModules.has(item.key));

  useEffect(() => {
    if (!clinicId || !allowedModules.has("contabilidad")) {
      setAccountingReady(false);
      return;
    }
    setAccountingLoading(true);
    accountingApi
      .getOpeningBalances(clinicId)
      .then((setup) => setAccountingReady(Boolean(setup)))
      .catch(() => setAccountingReady(false))
      .finally(() => setAccountingLoading(false));
  }, [allowedModules, clinicId, refreshVersion]);

  useEffect(() => {
    if (!clinicId) return;
    let active = true;
    const now = new Date();
    const from = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0).toISOString();
    const to = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59).toISOString();
    const requests: Promise<void>[] = [];

    setDashboardLoading(true);

    if (allowedModules.has("agenda")) {
      setAppointmentsError(false);
      requests.push(
        agendaApi
          .listByRange(clinicId, from, to)
          .then((appointments) => {
            if (active) setTodayAppointments(appointments);
          })
          .catch(() => {
            if (active) setAppointmentsError(true);
          })
      );
      requests.push(
        agendaApi
          .listDoctors(clinicId)
          .then((items) => {
            if (active) setDoctors(items);
          })
          .catch(() => undefined)
      );
    } else {
      setTodayAppointments([]);
    }

    if (allowedModules.has("caja")) {
      setCashError(false);
      setChargesError(false);
      requests.push(
        cashSessionsApi
          .getCurrent(clinicId)
          .then((session) => {
            if (active) setCurrentSession(session);
          })
          .catch(() => {
            if (active) setCashError(true);
          })
      );
      requests.push(
        ticketsApi
          .listPendingAppointmentCharges(clinicId)
          .then((charges) => {
            if (active) setPendingCharges(charges);
          })
          .catch(() => {
            if (active) setChargesError(true);
          })
      );
    } else {
      setCurrentSession(null);
      setPendingCharges([]);
    }

    Promise.allSettled(requests).then(() => {
      if (!active) return;
      setDashboardLoading(false);
      setLastUpdatedAt(new Date());
    });

    return () => {
      active = false;
    };
  }, [allowedModules, clinicId, refreshVersion]);

  const confirmedCount = todayAppointments.filter((appointment) => appointment.status === "CONFIRMED").length;
  const completedCount = todayAppointments.filter((appointment) => appointment.status === "COMPLETED").length;
  const scheduledCount = todayAppointments.filter((appointment) => appointment.status === "SCHEDULED").length;
  const activeAppointmentCount = todayAppointments.filter(
    (appointment) => appointment.status !== "CANCELLED" && appointment.status !== "NO_SHOW"
  ).length;
  const pendingChargeCount = chargesError ? pendingCajaCount : pendingCharges.length;
  const pendingChargeAmount = pendingCharges.reduce((total, charge) => total + Number(charge.amount || 0), 0);

  const visibleAppointments = useMemo(() => {
    const sorted = [...todayAppointments].sort(
      (left, right) => new Date(left.scheduledStart).getTime() - new Date(right.scheduledStart).getTime()
    );
    if (sorted.length <= 7) return sorted;
    const firstPendingIndex = sorted.findIndex(
      (appointment) => appointment.status === "SCHEDULED" || appointment.status === "CONFIRMED"
    );
    return sorted.slice(Math.max(0, firstPendingIndex - 1), Math.max(0, firstPendingIndex - 1) + 7);
  }, [todayAppointments]);

  const priorities = [
    ...(allowedModules.has("caja") && !cashError && !currentSession && activeAppointmentCount > 0
      ? [
          {
            id: "cash-closed",
            severity: "critical",
            icon: IconAlertTriangle,
            title: "Caja sin abrir",
            detail: `Hay ${activeAppointmentCount} cita(s) activas hoy y todavía no se pueden registrar cobros.`,
            actionLabel: "Abrir caja",
            action: () => onNavigateModule("caja")
          }
        ]
      : []),
    ...(allowedModules.has("caja") && pendingChargeCount > 0
      ? [
          {
            id: "pending-charges",
            severity: "warning",
            icon: IconReceipt,
            title: `${pendingChargeCount} cobro(s) pendiente(s)`,
            detail: chargesError
              ? "No fue posible consultar el importe total."
              : `${currencyFormatter.format(pendingChargeAmount)} por consultas terminadas.`,
            actionLabel: "Revisar cobros",
            action: () => onNavigateModule("caja")
          }
        ]
      : []),
    ...(allowedModules.has("agenda") && !appointmentsError && scheduledCount > 0
      ? [
          {
            id: "unconfirmed",
            severity: "info",
            icon: IconClock,
            title: `${scheduledCount} cita(s) sin confirmar`,
            detail: "Revisa la asistencia de los pacientes programados para hoy.",
            actionLabel: "Revisar agenda",
            action: () => onNavigateModule("agenda")
          }
        ]
      : [])
  ];

  const nextActions = [
    ...(!completeClinic
      ? [
          {
            label: "Completar datos de clínica",
            detail: clinic?.name ?? "Sin clínica activa",
            action: onEditClinic
          }
        ]
      : []),
    ...(allowedModules.has("pacientes") && !patientsLoading && patients.length === 0
      ? [
          {
            label: "Registrar primer paciente",
            detail: "Prepara el directorio para crear citas y expedientes.",
            action: onCreatePatient
          }
        ]
      : []),
    ...(allowedModules.has("contabilidad") && !accountingLoading && !accountingReady
      ? [
          {
            label: "Configurar saldos iniciales",
            detail: "Habilita la operación contable del consultorio.",
            action: () => onNavigateModule("contabilidad")
          }
        ]
      : []),
    {
      label: "Revisar integraciones",
      detail: "Google Calendar y conexiones operativas.",
      action: onOpenConfiguration
    }
  ];

  if (!hasClinic) {
    return (
      <section className="dashboard-home">
        <article className="panel full dashboard-empty-state">
          <IconBuildingHospital size={26} aria-hidden="true" />
          <div>
            <h2>Sin clínica activa</h2>
            <p>Completa o crea una clínica para ver el resumen operativo.</p>
          </div>
        </article>
      </section>
    );
  }

  const todayDateFormatted = new Date().toLocaleDateString("es-MX", {
    weekday: "long",
    day: "numeric",
    month: "long"
  });

  return (
    <section className="dashboard-home" aria-label="Resumen operativo">
      <header className="dashboard-command-bar">
        <div>
          <span className="dashboard-date">Hoy, {todayDateFormatted}</span>
          <small>
            {lastUpdatedAt ? `Actualizado a las ${formatTime(lastUpdatedAt.toISOString())}` : "Preparando resumen operativo"}
          </small>
        </div>
        <div className="dashboard-quick-actions">
          <button
            className="icon-btn"
            type="button"
            title="Actualizar dashboard"
            aria-label="Actualizar dashboard"
            disabled={dashboardLoading}
            onClick={() => setRefreshVersion((version) => version + 1)}
          >
            <IconRefresh className={dashboardLoading ? "is-spinning" : ""} size={18} />
          </button>
          {allowedModules.has("agenda") && (
            <button className="btn primary" type="button" onClick={() => onNavigateModule("agenda")}>
              <IconCalendar size={18} /> Nueva cita
            </button>
          )}
          {allowedModules.has("pacientes") && (
            <button className="btn secondary" type="button" onClick={onCreatePatient}>
              <IconUserPlus size={18} /> Nuevo paciente
            </button>
          )}
          {allowedModules.has("caja") && (
            <button className="btn secondary" type="button" onClick={() => onNavigateModule("caja")}>
              <IconCash size={18} /> Ir a caja
            </button>
          )}
        </div>
      </header>

      <div className="dashboard-overview-grid">
        <button
          className="dashboard-metric metric-clinic"
          type="button"
          disabled={!allowedModules.has("agenda")}
          onClick={() => onNavigateModule("agenda")}
        >
          <span className="dashboard-metric-icon"><IconCalendar size={20} aria-hidden="true" /></span>
          <span className="dashboard-metric-copy">
            <span>Citas de hoy</span>
            <strong>{appointmentsError ? "—" : dashboardLoading && !lastUpdatedAt ? "…" : todayAppointments.length}</strong>
            <small>{appointmentsError ? "No se pudo cargar" : `${completedCount} completadas · ${confirmedCount} confirmadas`}</small>
          </span>
        </button>

        <button
          className="dashboard-metric metric-modules"
          type="button"
          disabled={!allowedModules.has("agenda")}
          onClick={() => onNavigateModule("agenda")}
        >
          <span className="dashboard-metric-icon"><IconClock size={20} aria-hidden="true" /></span>
          <span className="dashboard-metric-copy">
            <span>Sin confirmar</span>
            <strong>{appointmentsError ? "—" : scheduledCount}</strong>
            <small>{scheduledCount > 0 ? "Requieren seguimiento hoy" : "Agenda al día"}</small>
          </span>
        </button>

        <button
          className="dashboard-metric metric-patients"
          type="button"
          disabled={!allowedModules.has("caja")}
          onClick={() => onNavigateModule("caja")}
        >
          <span className="dashboard-metric-icon"><IconReceipt size={20} aria-hidden="true" /></span>
          <span className="dashboard-metric-copy">
            <span>Cobros pendientes</span>
            <strong>{chargesError && pendingCajaCount === 0 ? "—" : pendingChargeCount}</strong>
            <small>{chargesError ? "Importe no disponible" : `${currencyFormatter.format(pendingChargeAmount)} por cobrar`}</small>
          </span>
        </button>

        <button
          className="dashboard-metric metric-caja"
          type="button"
          disabled={!allowedModules.has("caja")}
          onClick={() => onNavigateModule("caja")}
        >
          <span className="dashboard-metric-icon"><IconCash size={20} aria-hidden="true" /></span>
          <span className="dashboard-metric-copy">
            <span>Turno de caja</span>
            <strong>{cashError ? "—" : currentSession ? "Abierto" : "Cerrado"}</strong>
            <small>{cashError ? "No se pudo consultar" : currentSession ? `Desde las ${formatTime(currentSession.openedAt)}` : "Requiere apertura"}</small>
          </span>
        </button>
      </div>

      <div className="dashboard-home-grid">
        <section className="dashboard-module-section" aria-labelledby="dashboard-agenda-title">
          <div className="dashboard-section-heading">
            <div>
              <h2 id="dashboard-agenda-title">Agenda de hoy</h2>
              <p>Próximas consultas y estado de la jornada.</p>
            </div>
            {allowedModules.has("agenda") && (
              <button className="btn ghost" type="button" onClick={() => onNavigateModule("agenda")}>
                Ver agenda <IconArrowRight size={16} />
              </button>
            )}
          </div>

          <div className="panel dashboard-agenda-panel">
            {dashboardLoading && !lastUpdatedAt ? (
              <div className="dashboard-loading-state" aria-live="polite">Cargando citas del día…</div>
            ) : appointmentsError ? (
              <div className="dashboard-error-state" role="alert">
                <IconAlertTriangle size={24} aria-hidden="true" />
                <strong>No se pudo cargar la agenda</strong>
                <p>Los datos anteriores se conservan, pero podrían estar desactualizados.</p>
                <button className="btn secondary" type="button" onClick={() => setRefreshVersion((version) => version + 1)}>
                  <IconRefresh size={16} /> Reintentar
                </button>
              </div>
            ) : visibleAppointments.length === 0 ? (
              <div className="dashboard-empty-agenda">
                <IconCalendar size={30} aria-hidden="true" />
                <strong>No hay citas agendadas para hoy</strong>
                <p>La jornada está libre por ahora.</p>
                {allowedModules.has("agenda") && (
                  <button className="btn primary" type="button" onClick={() => onNavigateModule("agenda")}>
                    <IconPlus size={16} /> Programar cita
                  </button>
                )}
              </div>
            ) : (
              <div className="agenda-list">
                {visibleAppointments.map((appointment) => {
                  const statusClass = appointmentStatusClass(appointment);
                  const canOpenCash = appointment.status === "COMPLETED" && allowedModules.has("caja");
                  const canOpenRecord = appointment.status !== "COMPLETED" && allowedModules.has("expediente");
                  return (
                    <div key={appointment.id} className={`agenda-list-row dashboard-agenda-row ${statusClass}`}>
                      <div className="agenda-list-time">
                        <IconClock size={14} aria-hidden="true" />
                        {formatTime(appointment.scheduledStart)}–{formatTime(appointment.scheduledEnd)}
                      </div>
                      <div className="agenda-list-info">
                        <strong>{patientName(patients, appointment.patientId)}</strong>
                        <span>
                          {appointment.reason || "Consulta médica"} · <IconStethoscope size={12} aria-hidden="true" /> {doctorLabel(doctors, appointment.doctorStaffId)}
                        </span>
                      </div>
                      <span className={`agenda-status-badge ${statusClass}`}>
                        {STATUS_LABELS[appointment.status] ?? appointment.status}
                      </span>
                      {(canOpenCash || canOpenRecord) && (
                        <div className="dashboard-row-actions">
                          <button
                            className={`btn ${canOpenCash ? "primary" : "secondary"} dashboard-compact-button`}
                            type="button"
                            onClick={() => onNavigateModule(canOpenCash ? "caja" : "expediente")}
                          >
                            {canOpenCash ? "Ir a caja" : "Expediente"}
                          </button>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </section>

        <aside className="dashboard-next-panel" aria-labelledby="dashboard-priorities-title">
          <div className="dashboard-section-heading">
            <div>
              <h2 id="dashboard-priorities-title">Requiere atención</h2>
              <p>Pendientes ordenados por impacto operativo.</p>
            </div>
            {priorities.length > 0 && <span className="dashboard-priority-count">{priorities.length}</span>}
          </div>

          <div className="dashboard-priority-list">
            {priorities.map((priority) => {
              const PriorityIcon = priority.icon;
              return (
                <article className={`dashboard-priority is-${priority.severity}`} key={priority.id}>
                  <div className="dashboard-priority-title">
                    <PriorityIcon size={18} aria-hidden="true" />
                    <strong>{priority.title}</strong>
                  </div>
                  <p>{priority.detail}</p>
                  <button className="btn ghost dashboard-compact-button" type="button" onClick={priority.action}>
                    {priority.actionLabel} <IconArrowRight size={15} />
                  </button>
                </article>
              );
            })}
            {priorities.length === 0 && (
              <div className="dashboard-all-clear">
                <IconCheck size={20} aria-hidden="true" />
                <div>
                  <strong>Operación al día</strong>
                  <p>No hay pendientes urgentes con la información disponible.</p>
                </div>
              </div>
            )}
          </div>

          <div className="dashboard-section-heading dashboard-secondary-heading">
            <div>
              <h2 id="dashboard-actions-title">Preparación</h2>
              <p>Configuración que mejora la operación.</p>
            </div>
          </div>
          <div className="dashboard-action-list">
            {nextActions.slice(0, 3).map((action) => (
              <button className="dashboard-action-row" key={action.label} type="button" onClick={action.action}>
                <span className="dashboard-action-status"><IconSettings size={16} aria-hidden="true" /></span>
                <span><strong>{action.label}</strong><small>{action.detail}</small></span>
                <IconArrowRight size={16} aria-hidden="true" />
              </button>
            ))}
          </div>
        </aside>
      </div>

      <section className="dashboard-module-directory" aria-labelledby="dashboard-modules-title">
        <div className="dashboard-section-heading">
          <div>
            <h2 id="dashboard-modules-title">Módulos</h2>
            <p>Accesos secundarios a las áreas disponibles para tu rol.</p>
          </div>
        </div>
        <div className="dashboard-module-grid">
          {visibleModules.map((item) => {
            const ModuleIcon = item.icon;
            return (
              <button className="dashboard-module-card" key={item.key} type="button" onClick={() => onNavigateModule(item.key)}>
                <span className="dashboard-module-icon"><ModuleIcon size={22} aria-hidden="true" /></span>
                <span><strong>{item.label}</strong><small>{moduleSummary[item.key as Exclude<ModuleKey, "dashboard">]}</small></span>
                <IconArrowRight size={16} aria-hidden="true" />
              </button>
            );
          })}
        </div>
      </section>
    </section>
  );
}
