import { useEffect, useMemo, useState } from "react";
import { IconArrowLeft, IconClock, IconReceipt, IconShieldLock, IconUser } from "@tabler/icons-react";
import {
  getFriendlyError,
  staffApi,
  type ClinicStaffResponse,
  type StaffActivityResponse,
  type StaffAttendanceResponse,
  type StaffPermissionSummary,
  type StaffPayrollLineResponse,
  type StaffPayrollPeriodResponse
} from "@shared/api/api";
import { StaffPermissionsScreen } from "@modules/staff/screens/StaffPermissionsScreen";

const staffRoleLabels: Record<string, string> = {
  ADMIN: "Administrador de sistema",
  DOCTOR: "Doctor / Especialista",
  RECEPTIONIST: "Recepcionista",
  ASSISTANT: "Asistente medico",
  CLINIC_ADMIN: "Administrador de clinica",
  ACCOUNTANT: "Contador",
  CLEANING: "Personal de limpieza"
};

const activityTypeLabels: Record<string, string> = {
  TREATMENT: "Tratamiento",
  PROCEDURE: "Procedimiento",
  SALE: "Venta",
  ADMINISTRATIVE: "Administrativo",
  NOTE: "Nota"
};

const attendanceStatusLabels: Record<string, string> = {
  OPEN: "Jornada abierta",
  CLOSED: "Jornada cerrada"
};

function formatMoney(value: number | null | undefined) {
  return new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" }).format(value ?? 0);
}

function formatDate(value?: string | null) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("es-MX", { dateStyle: "medium" }).format(new Date(value));
}

function formatDateTime(value?: string | null) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("es-MX", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

function sortByDate<T>(items: T[], getDate: (item: T) => string | null | undefined) {
  return [...items].sort((a, b) => (getDate(b) || "").localeCompare(getDate(a) || ""));
}

function isPermissionsPath(staffId: string) {
  const [section, pathStaffId, page] = window.location.pathname.split("/").filter(Boolean);
  return section === "personal" && pathStaffId === staffId && page === "permisos";
}

export function StaffDetailScreen({
  clinicId,
  staff,
  staffId,
  staffLoading,
  onBack
}: {
  clinicId: string;
  staff: ClinicStaffResponse[];
  staffId: string;
  staffLoading: boolean;
  onBack: () => void;
}) {
  const employee = staff.find((member) => member.staffId === staffId);
  const [permissions, setPermissions] = useState<StaffPermissionSummary | null>(null);
  const [attendance, setAttendance] = useState<StaffAttendanceResponse[]>([]);
  const [activities, setActivities] = useState<StaffActivityResponse[]>([]);
  const [payrollPeriods, setPayrollPeriods] = useState<StaffPayrollPeriodResponse[]>([]);
  const [payrollLines, setPayrollLines] = useState<StaffPayrollLineResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [permissionsPage, setPermissionsPage] = useState(() => isPermissionsPath(staffId));

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError("");

    Promise.all([
      staffApi.getPermissions(clinicId, staffId),
      staffApi.listAttendance(clinicId, { staffId }),
      staffApi.listActivities(clinicId, { staffId }),
      staffApi.listPayrollPeriods(clinicId)
    ])
      .then(async ([permissionSummary, attendanceList, activityList, periods]) => {
        const latestPeriod = periods[0];
        const lines = latestPeriod ? await staffApi.listPayrollLines(clinicId, latestPeriod.id) : [];
        if (cancelled) return;
        setPermissions(permissionSummary);
        setAttendance(attendanceList);
        setActivities(activityList);
        setPayrollPeriods(periods);
        setPayrollLines(lines.filter((line) => line.staffId === staffId));
      })
      .catch((caught) => {
        if (!cancelled) setError(getFriendlyError(caught));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [clinicId, staffId]);

  useEffect(() => {
    const handlePopState = () => setPermissionsPage(isPermissionsPath(staffId));
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, [staffId]);

  const recentActivities = useMemo(() => sortByDate(activities, (item) => item.occurredAt).slice(0, 5), [activities]);
  const recentAttendance = useMemo(() => sortByDate(attendance, (item) => item.clockInAt).slice(0, 5), [attendance]);
  const enabledPermissions = permissions?.permissions.filter((item) => item.enabled) ?? [];
  const latestPayrollPeriod = payrollPeriods[0];
  const latestPayrollLine = payrollLines[0];

  const openPermissions = () => {
    setPermissionsPage(true);
    window.history.pushState({ module: "personal", staffId, page: "permisos" }, "", `/personal/${staffId}/permisos`);
  };

  const closePermissions = () => {
    setPermissionsPage(false);
    window.history.pushState({ module: "personal", staffId }, "", `/personal/${staffId}`);
  };

  if (staffLoading && !employee) {
    return (
      <section className="staff-detail-page">
        <div className="staff-detail-header">
          <button className="icon-btn" type="button" aria-label="Volver al directorio" onClick={onBack}>
            <IconArrowLeft size={18} aria-hidden="true" />
          </button>
          <div>
            <p className="eyebrow">Personal</p>
            <h2>Cargando empleado...</h2>
          </div>
        </div>
      </section>
    );
  }

  if (!employee) {
    return (
      <section className="staff-detail-page">
        <div className="staff-detail-header">
          <button className="icon-btn" type="button" aria-label="Volver al directorio" onClick={onBack}>
            <IconArrowLeft size={18} aria-hidden="true" />
          </button>
          <div>
            <p className="eyebrow">Personal</p>
            <h2>Empleado no encontrado</h2>
            <p className="description">El registro seleccionado ya no pertenece a esta clinica.</p>
          </div>
        </div>
      </section>
    );
  }

  if (permissionsPage) {
    return (
      <StaffPermissionsScreen
        clinicId={clinicId}
        employee={employee}
        staffLoading={staffLoading}
        onBack={closePermissions}
      />
    );
  }

  return (
    <section className="staff-detail-page">
      <div className="staff-detail-header">
        <button className="icon-btn" type="button" aria-label="Volver al directorio" onClick={onBack}>
          <IconArrowLeft size={18} aria-hidden="true" />
        </button>
        <div>
          <p className="eyebrow">Personal / Detalle del empleado</p>
          <h2>{employee.fullName}</h2>
          <p className="description">{staffRoleLabels[employee.role] || employee.role} · Activo</p>
        </div>
        <span className="badge success">Activo</span>
      </div>

      {error && <p className="alert error">{error}</p>}

      <div className="staff-detail-grid">
        <article className="panel staff-detail-card">
          <div className="panel-heading">
            <div>
              <h3>Perfil laboral</h3>
              <span className="panel-subtitle">Identificadores y pertenencia del empleado</span>
            </div>
            <IconUser size={20} aria-hidden="true" />
          </div>
          <dl className="staff-detail-definition-list">
            <div><dt>Nombre</dt><dd>{employee.fullName}</dd></div>
            <div><dt>Rol actual</dt><dd>{staffRoleLabels[employee.role] || employee.role}</dd></div>
            <div><dt>Usuario</dt><dd>{employee.userId}</dd></div>
            <div><dt>Identificador de personal</dt><dd>{employee.staffId}</dd></div>
          </dl>
        </article>

        <article className="panel staff-detail-card">
          <div className="panel-heading">
            <div>
              <h3>Resumen operativo</h3>
              <span className="panel-subtitle">Actividad registrada para este empleado</span>
            </div>
            <IconClock size={20} aria-hidden="true" />
          </div>
          <div className="staff-detail-stat-grid">
            <div className="staff-detail-stat"><span>Asistencias</span><strong>{attendance.length}</strong></div>
            <div className="staff-detail-stat"><span>Actividades</span><strong>{activities.length}</strong></div>
            <div className="staff-detail-stat"><span>Accesos activos</span><strong>{enabledPermissions.length}</strong></div>
          </div>
          {loading && <p className="description">Cargando información operativa...</p>}
        </article>

        <article
          className="panel staff-detail-card staff-detail-clickable"
          role="button"
          tabIndex={0}
          onClick={openPermissions}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault();
              openPermissions();
            }
          }}
        >
          <div className="panel-heading">
            <div>
              <h3>Accesos efectivos</h3>
              <span className="panel-subtitle">Permisos heredados del rol y ajustes individuales. Selecciona para administrar.</span>
            </div>
            <IconShieldLock size={20} aria-hidden="true" />
          </div>
          {loading && !permissions ? (
            <p className="description">Cargando accesos...</p>
          ) : enabledPermissions.length > 0 ? (
            <div className="staff-permission-badges">
              {enabledPermissions.map((item) => <span className="staff-permission-badge" key={item.permission}>{item.permission.replaceAll("_", " ")}</span>)}
            </div>
          ) : (
            <p className="description">No tiene accesos habilitados.</p>
          )}
        </article>

        <article className="panel staff-detail-card">
          <div className="panel-heading">
            <div>
              <h3>Nomina reciente</h3>
              <span className="panel-subtitle">Ultimo periodo disponible</span>
            </div>
            <IconReceipt size={20} aria-hidden="true" />
          </div>
          {latestPayrollPeriod && latestPayrollLine ? (
            <div className="staff-detail-payroll-summary">
              <strong>{latestPayrollPeriod.name}</strong>
              <span>{formatDate(latestPayrollPeriod.periodStart)} - {formatDate(latestPayrollPeriod.periodEnd)}</span>
              <b>{formatMoney(latestPayrollLine.netAmount)} netos</b>
            </div>
          ) : (
            <p className="description">Aun no hay una linea de nomina para este empleado.</p>
          )}
        </article>

        <article className="panel full staff-detail-card">
          <div className="panel-heading">
            <div>
              <h3>Actividad reciente</h3>
              <span className="panel-subtitle">Tratamientos, procedimientos, ventas y otras acciones registradas</span>
            </div>
          </div>
          {recentActivities.length > 0 ? (
            <div className="staff-detail-list">
              {recentActivities.map((activity) => (
                <div className="staff-detail-list-row" key={activity.id}>
                  <div>
                    <strong>{activityTypeLabels[activity.type] || activity.type}</strong>
                    <span>{activity.description || "Sin descripción"} · {formatDateTime(activity.occurredAt)}</span>
                  </div>
                  <b>{formatMoney(activity.amount)}</b>
                </div>
              ))}
            </div>
          ) : (
            <p className="description">No hay actividad registrada para este empleado.</p>
          )}
        </article>

        <article className="panel full staff-detail-card">
          <div className="panel-heading">
            <div>
              <h3>Asistencia reciente</h3>
              <span className="panel-subtitle">Entradas y salidas registradas</span>
            </div>
          </div>
          {recentAttendance.length > 0 ? (
            <div className="staff-detail-list">
              {recentAttendance.map((entry) => (
                <div className="staff-detail-list-row" key={entry.id}>
                  <div>
                    <strong>{formatDate(entry.workDate)}</strong>
                    <span>{formatDateTime(entry.clockInAt)} - {formatDateTime(entry.clockOutAt)}</span>
                  </div>
                  <span className={`badge ${entry.status === "OPEN" ? "warning" : "success"}`}>{attendanceStatusLabels[entry.status] || entry.status}</span>
                </div>
              ))}
            </div>
          ) : (
            <p className="description">No hay registros de asistencia para este empleado.</p>
          )}
        </article>
      </div>
    </section>
  );
}
