import { useEffect, useRef, useState } from "react";
import { IconCheck, IconClock, IconEye, IconPlus, IconTrash, IconUserPlus, IconX, IconPencil, IconShieldLock } from "@tabler/icons-react";
import {
  collaborationApi,
  getFriendlyError,
  staffApi,
  type ClinicStaffResponse,
  type StaffActivityResponse,
  type StaffActivityType,
  type StaffAttendanceResponse,
  type StaffInvitationResponse,
  type StaffPermission,
  type StaffPermissionChange,
  type StaffPermissionOverrideState,
  type StaffPermissionSummary
} from "@shared/api/api";

import type { AccessLevel, ExternalAccessGrantResponse, ExternalAccessStatus } from "@modules/collaboration/types";
import type { PatientResponse } from "@modules/patients/types";
import { PatientHistoryPanel } from "@modules/records/components/PatientHistoryPanel";
import { PayrollPanel } from "@modules/staff/components/payroll/PayrollPanel";
import { StaffDetailScreen } from "@modules/staff/screens/StaffDetailScreen";


const staffRoleLabels: Record<string, string> = {
  ADMIN: "Administrador de sistema",
  DOCTOR: "Doctor / Especialista",
  RECEPTIONIST: "Recepcionista",
  ASSISTANT: "Asistente médico",
  CLINIC_ADMIN: "Administrador de clínica",
  ACCOUNTANT: "Contador",
  CLEANING: "Personal de limpieza"
};

const statusLabel: Record<ExternalAccessStatus, string> = {
  PENDING: "Pendiente",
  ACCEPTED: "Activo",
  REJECTED: "Rechazado",
  REVOKED: "Revocado"
};

const statusBadge: Record<ExternalAccessStatus, string> = {
  PENDING: "warning",
  ACCEPTED: "success",
  REJECTED: "neutral",
  REVOKED: "neutral"
};

const accessLevelLabel: Record<AccessLevel, string> = {
  READ_ONLY: "Solo lectura",
  COMMENT: "Lectura + comentar",
  FULL: "Lectura + notas e historia clínica"
};

const activityTypeLabels: Record<StaffActivityType, string> = {
  TREATMENT: "Tratamiento",
  PROCEDURE: "Procedimiento",
  SALE: "Venta",
  ADMINISTRATIVE: "Administrativo",
  NOTE: "Nota"
};

const staffPermissionLabels: Partial<Record<StaffPermission, { label: string; description: string }>> = {
  VIEW_DASHBOARD: { label: "Ver dashboard", description: "Resumen operativo de la clinica" },
  MANAGE_AGENDA: { label: "Gestionar agenda", description: "Crear, modificar y cancelar citas" },
  MANAGE_PATIENTS: { label: "Gestionar pacientes", description: "Registrar y actualizar pacientes" },
  VIEW_MEDICAL_RECORDS: { label: "Consultar expedientes", description: "Ver informacion clinica" },
  EDIT_MEDICAL_RECORDS: { label: "Editar expedientes", description: "Crear y modificar notas clinicas" },
  MANAGE_TREATMENTS: { label: "Gestionar tratamientos", description: "Tratamientos, cotizaciones y procedimientos" },
  MANAGE_CASH: { label: "Gestionar caja", description: "Cobros, cortes y gastos" },
  MANAGE_INVENTORY: { label: "Gestionar inventario", description: "Materiales, compras y movimientos" },
  VIEW_ACCOUNTING: { label: "Consultar contabilidad", description: "Reportes y saldos contables" },
  MANAGE_STAFF: { label: "Gestionar personal", description: "Altas, roles, accesos y nomina" },
  MANAGE_CLINIC: { label: "Gestionar clinica", description: "Propiedades e integraciones" },
  MANAGE_ROOMS: { label: "Gestionar consultorios", description: "Crear espacios y asignar medicos" },
  MANAGE_PAYROLL: { label: "Gestionar nomina", description: "Periodos y pagos del personal" }
};

const permissionStateLabels: Record<StaffPermissionOverrideState, string> = {
  INHERIT: "Heredar del rol",
  GRANTED: "Permitido",
  REVOKED: "Revocado"
};

type PersonalTab = "directory" | "attendance" | "activity" | "payroll";

function staffIdFromPath() {
  const [section, staffId] = window.location.pathname.split("/").filter(Boolean);
  return section === "personal" ? staffId : undefined;
}

function staffName(staff: ClinicStaffResponse[], staffId: string) {
  return staff.find((member) => member.staffId === staffId)?.fullName ?? "Personal no encontrado";
}

function formatMoney(value: number | null | undefined) {
  return new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" }).format(value ?? 0);
}

function formatDateTime(value?: string | null) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("es-MX", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

function stubPatientFromGrant(grant: ExternalAccessGrantResponse): PatientResponse {
  const [firstName, ...rest] = (grant.patientName || "Paciente").split(" ");
  return {
    id: grant.patientId,
    clinicId: grant.sourceClinicId,
    firstName: firstName || "Paciente",
    lastNamePaterno: rest.join(" "),
    createdAt: grant.createdAt,
    updatedAt: grant.createdAt
  };
}

export function PersonalScreen({ userId, clinicId, hasClinic }: { userId: string; clinicId?: string; hasClinic: boolean }) {
  const [granted, setGranted] = useState<ExternalAccessGrantResponse[]>([]);
  const [received, setReceived] = useState<ExternalAccessGrantResponse[]>([]);
  const [staff, setStaff] = useState<ClinicStaffResponse[]>([]);
  const [invitations, setInvitations] = useState<StaffInvitationResponse[]>([]);
  const [myPermissions, setMyPermissions] = useState<StaffPermissionSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [status, setStatus] = useState("");
  const [viewingGrant, setViewingGrant] = useState<ExternalAccessGrantResponse | null>(null);
  const [showAddStaffModal, setShowAddStaffModal] = useState(false);
  const [editingStaff, setEditingStaff] = useState<ClinicStaffResponse | null>(null);
  const [selectedStaffId, setSelectedStaffId] = useState<string | undefined>(() => staffIdFromPath());
  const [activeTab, setActiveTab] = useState<PersonalTab>("directory");
  const [operationsLoading, setOperationsLoading] = useState(false);
  const [attendance, setAttendance] = useState<StaffAttendanceResponse[]>([]);
  const [activities, setActivities] = useState<StaffActivityResponse[]>([]);
  const [attendanceStaffId, setAttendanceStaffId] = useState("");
  const [activityStaffId, setActivityStaffId] = useState("");
  const operationsLoadVersion = useRef(0);

  const load = () => {
    if (!clinicId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError("");
    Promise.all([
      collaborationApi.listByClinic(clinicId),
      collaborationApi.listMine(),
      staffApi.list(clinicId),
      staffApi.listInvitations(clinicId)
    ])
      .then(([grantedList, receivedList, staffList, invitationList]) => {
        setGranted(grantedList);
        setReceived(receivedList);
        setStaff(staffList);
        setInvitations(invitationList);
      })
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  };

  const handleRemoveStaff = async (staffId: string) => {
    if (!clinicId) return;
    if (!window.confirm("¿Estás seguro de que deseas eliminar a este miembro del personal? No podrá ingresar a la clínica.")) {
      return;
    }
    setLoading(true);
    setError("");
    try {
      await staffApi.remove(clinicId, staffId);
      setStatus("Miembro del personal eliminado exitosamente.");
      load();
    } catch (caught) {
      setError(getFriendlyError(caught));
      setLoading(false);
    }
  };



  const loadOperations = async () => {
    if (!clinicId) return;
    const loadVersion = ++operationsLoadVersion.current;
    const isLatestLoad = () => loadVersion === operationsLoadVersion.current;
    setOperationsLoading(true);
    setError("");
    try {
      const [attendanceResult, activityResult] = await Promise.allSettled([
        staffApi.listAttendance(clinicId),
        staffApi.listActivities(clinicId)
      ]);
      if (!isLatestLoad()) return;
      if (attendanceResult.status === "fulfilled") {
        setAttendance(attendanceResult.value);
      }
      if (activityResult.status === "fulfilled") {
        setActivities(activityResult.value);
      }
      const failure = [attendanceResult, activityResult].find((result) => result.status === "rejected");
      if (failure && failure.status === "rejected") {
        throw failure.reason;
      }
    } catch (caught) {
      if (isLatestLoad()) {
        setError(getFriendlyError(caught));
      }
    } finally {
      if (isLatestLoad()) {
        setOperationsLoading(false);
      }
    }
  };

  const handleClockIn = async () => {
    if (!clinicId || !attendanceStaffId) return;
    try {
      await staffApi.clockIn(clinicId, { staffId: attendanceStaffId });
      setStatus("Entrada registrada correctamente.");
      loadOperations();
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  const handleClockOut = async () => {
    if (!clinicId || !attendanceStaffId) return;
    const openEntry = attendance.find((entry) => entry.staffId === attendanceStaffId && entry.status === "OPEN");
    if (!openEntry) {
      setError("Este empleado no tiene una entrada abierta.");
      return;
    }
    try {
      await staffApi.clockOut(clinicId, openEntry.id, {});
      setStatus("Salida registrada correctamente.");
      loadOperations();
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  const submitActivity = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!clinicId) return;
    const form = new FormData(event.currentTarget);
    try {
      await staffApi.recordActivity(clinicId, {
        staffId: String(form.get("staffId") ?? ""),
        type: String(form.get("type") ?? "TREATMENT") as StaffActivityType,
        referenceType: String(form.get("referenceType") ?? "").trim() || undefined,
        description: String(form.get("description") ?? "").trim(),
        amount: Number(form.get("amount") ?? 0)
      });
      event.currentTarget.reset();
      setStatus("Actividad registrada correctamente.");
      loadOperations();
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId]);

  useEffect(() => {
    if (staff.length > 0) {
      const currentUserStaff = staff.find((member) => member.userId === userId);
      setAttendanceStaffId(currentUserStaff?.staffId ?? "");
      setActivityStaffId((current) => current || staff[0].staffId);
    }
  }, [staff, userId]);

  useEffect(() => {
    const currentUserStaff = staff.find((member) => member.userId === userId);
    if (!clinicId || !currentUserStaff) {
      setMyPermissions(null);
      return;
    }
    staffApi
      .getPermissions(clinicId, currentUserStaff.staffId)
      .then(setMyPermissions)
      .catch(() => setMyPermissions(null));
  }, [clinicId, staff, userId]);

  const hasMyPermission = (permission: StaffPermission) =>
    myPermissions?.permissions.some((item) => item.permission === permission && item.enabled) ?? false;
  const canManageStaff = hasMyPermission("MANAGE_STAFF");
  const canManageStaffPermissions = hasMyPermission("MANAGE_STAFF_PERMISSIONS");
  const canManagePayroll = hasMyPermission("MANAGE_PAYROLL");

  useEffect(() => {
    if (!canManagePayroll && activeTab === "payroll") {
      setActiveTab("directory");
    }
  }, [canManagePayroll, activeTab]);

  useEffect(() => {
    if (clinicId && (activeTab === "attendance" || activeTab === "activity")) {
      loadOperations();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId, activeTab]);

  useEffect(() => {
    const handlePopState = () => setSelectedStaffId(staffIdFromPath());
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  const openStaffDetail = (staffId: string) => {
    setSelectedStaffId(staffId);
    window.history.pushState({ module: "personal", staffId }, "", `/personal/${staffId}`);
  };

  const closeStaffDetail = () => {
    setSelectedStaffId(undefined);
    window.history.pushState({ module: "personal" }, "", "/personal");
  };

  const revoke = async (grant: ExternalAccessGrantResponse) => {
    if (!clinicId) return;
    try {
      await collaborationApi.revoke(clinicId, grant.id);
      setStatus("Acceso revocado correctamente.");
      load();
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  const respond = async (grant: ExternalAccessGrantResponse, accept: boolean) => {
    try {
      await (accept ? collaborationApi.accept(grant.id) : collaborationApi.reject(grant.id));
      setStatus(accept ? "Invitación aceptada." : "Invitación rechazada.");
      load();
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  if (hasClinic && clinicId && selectedStaffId) {
    return (
      <StaffDetailScreen
        clinicId={clinicId}
        staff={staff}
        staffId={selectedStaffId}
        staffLoading={loading}
        onBack={closeStaffDetail}
      />
    );
  }

  return (
    <section className="dashboard-grid">
      {!hasClinic && (
        <article className="panel full">
          <div className="clinic-list">
            <div className="clinic-row">
              <strong>Completa los datos de tu clínica</strong>
              <span>Necesitas una clínica activa para gestionar accesos externos</span>
            </div>
          </div>
        </article>
      )}

      {hasClinic && (
        <>
          <div className="tab-switch">
            <button className={activeTab === "directory" ? "active" : ""} type="button" onClick={() => setActiveTab("directory")}>
              Directorio
            </button>
            <button className={activeTab === "attendance" ? "active" : ""} type="button" onClick={() => setActiveTab("attendance")}>
              Asistencia
            </button>
            <button className={activeTab === "activity" ? "active" : ""} type="button" onClick={() => setActiveTab("activity")}>
              Actividad
            </button>
            {canManagePayroll && (
              <button className={activeTab === "payroll" ? "active" : ""} type="button" onClick={() => setActiveTab("payroll")}>
                Nomina
              </button>
            )}
          </div>

          {activeTab === "directory" && (
          <>
          <article className="panel full">
            <div className="panel-heading">
              <div>
                <h2>Personal de mi clínica</h2>
                <span className="panel-subtitle">Administra los profesionales y personal que laboran internamente en tu clínica</span>
              </div>
              <div className="clinic-row-actions">
                <span className="badge neutral">{staff.length}</span>
                {canManageStaff && (
                  <button
                    className="btn primary"
                    type="button"
                    onClick={() => setShowAddStaffModal(true)}
                  >
                    <IconPlus size={16} aria-hidden="true" />
                    Agregar personal
                  </button>
                )}
              </div>
            </div>
            <div className="clinic-list">
              {loading && (
                <div className="clinic-row">
                  <strong>Cargando...</strong>
                </div>
              )}
              {!loading && staff.length === 0 && (
                <div className="clinic-row">
                  <strong>Sin personal registrado</strong>
                  <span>Agrega miembros del personal usando su correo electrónico</span>
                </div>
              )}
              {staff.map((member) => (
                <div
                  className="clinic-row staff-selectable-row"
                  key={member.staffId}
                  role="button"
                  tabIndex={0}
                  onClick={() => openStaffDetail(member.staffId)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter" || event.key === " ") {
                      event.preventDefault();
                      openStaffDetail(member.staffId);
                    }
                  }}
                >
                  <div>
                    <strong>{member.fullName}</strong>
                    <span>{staffRoleLabels[member.role] || member.role}</span>
                  </div>
                  <div className="clinic-row-actions">
                    <span className="badge success">Activo</span>
                    {(canManageStaff || canManageStaffPermissions) && (
                      <button
                        className="icon-btn"
                        type="button"
                        title="Editar rol"
                        onClick={(event) => {
                          event.stopPropagation();
                          setEditingStaff(member);
                        }}
                      >
                        <IconPencil size={16} aria-hidden="true" />
                      </button>
                    )}
                    {canManageStaff && (
                      <button
                        className="icon-btn destructive"
                        type="button"
                        title="Eliminar del personal"
                        onClick={(event) => {
                          event.stopPropagation();
                          handleRemoveStaff(member.staffId);
                        }}
                      >
                        <IconTrash size={16} aria-hidden="true" />
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </article>

          <article className="panel full">
            <div className="panel-heading">
              <div>
                <h2>Invitaciones de personal pendientes</h2>
                <span className="panel-subtitle">Invitaciones enviadas que aún no han completado su registro</span>
              </div>
              <span className="badge neutral">{invitations.length}</span>
            </div>
            <div className="clinic-list">
              {loading && (
                <div className="clinic-row">
                  <strong>Cargando...</strong>
                </div>
              )}
              {!loading && invitations.length === 0 && (
                <div className="clinic-row">
                  <strong>Sin invitaciones pendientes</strong>
                  <span>Las invitaciones que envíes aparecerán aquí hasta que se registren.</span>
                </div>
              )}
              {invitations.map((inv) => (
                <div className="clinic-row" key={inv.invitationId}>
                  <div>
                    <strong>{inv.email}</strong>
                    <span>Rol: {staffRoleLabels[inv.role] || inv.role}</span>
                  </div>
                  <div className="clinic-row-actions">
                    <span className="badge warning">Pendiente</span>
                  </div>
                </div>
              ))}
            </div>
          </article>


          <article className="panel full">
            <div className="panel-heading">
              <h2>Accesos que he otorgado</h2>
              <span className="badge neutral">{granted.length}</span>
            </div>
            <div className="clinic-list">
              {loading && (
                <div className="clinic-row">
                  <strong>Cargando...</strong>
                </div>
              )}
              {!loading && granted.length === 0 && (
                <div className="clinic-row">
                  <strong>Sin accesos otorgados</strong>
                  <span>Comparte el expediente de un paciente con un especialista externo desde el módulo Expediente</span>
                </div>
              )}
              {granted.map((grant) => (
                <div className="clinic-row" key={grant.id}>
                  <div>
                    <strong>{grant.patientName || grant.patientId}</strong>
                    <span>
                      {grant.invitedEmail} · {accessLevelLabel[grant.accessLevel]}
                      {!grant.externalUserId && " · esperando a que se registre"}
                    </span>
                  </div>
                  <div className="clinic-row-actions">
                    <span className={`badge ${statusBadge[grant.status]}`}>{statusLabel[grant.status]}</span>
                    {(grant.status === "PENDING" || grant.status === "ACCEPTED") && (
                      <button className="btn ghost" type="button" onClick={() => revoke(grant)}>
                        <IconTrash size={16} aria-hidden="true" />
                        Revocar
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </article>

          <article className="panel full">
            <div className="panel-heading">
              <h2>Accesos que tengo en otras clínicas</h2>
              <span className="badge neutral">{received.length}</span>
            </div>
            <div className="clinic-list">
              {loading && (
                <div className="clinic-row">
                  <strong>Cargando...</strong>
                </div>
              )}
              {!loading && received.length === 0 && (
                <div className="clinic-row">
                  <strong>Sin invitaciones recibidas</strong>
                  <span>Aquí aparecerán los pacientes que otras clínicas compartan contigo</span>
                </div>
              )}
              {received.map((grant) => (
                <div className="clinic-row" key={grant.id}>
                  <div>
                    <strong>{grant.patientName || grant.patientId}</strong>
                    <span>{accessLevelLabel[grant.accessLevel]}</span>
                  </div>
                  <div className="clinic-row-actions">
                    <span className={`badge ${statusBadge[grant.status]}`}>{statusLabel[grant.status]}</span>
                    {grant.status === "PENDING" && (
                      <>
                        <button className="btn secondary" type="button" onClick={() => respond(grant, true)}>
                          <IconCheck size={16} aria-hidden="true" />
                          Aceptar
                        </button>
                        <button className="btn ghost" type="button" onClick={() => respond(grant, false)}>
                          <IconX size={16} aria-hidden="true" />
                          Rechazar
                        </button>
                      </>
                    )}
                    {grant.status === "ACCEPTED" && (
                      <button className="btn secondary" type="button" onClick={() => setViewingGrant(grant)}>
                        <IconEye size={16} aria-hidden="true" />
                        Ver expediente
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </article>
          </>
          )}

          {activeTab === "attendance" && (
            <StaffAttendancePanel
              staff={staff}
              attendance={attendance}
              loading={operationsLoading}
              currentStaff={staff.find((member) => member.userId === userId)}
              selectedStaffId={attendanceStaffId}
              onClockIn={handleClockIn}
              onClockOut={handleClockOut}
            />
          )}

          {activeTab === "activity" && (
            <StaffActivityPanel
              staff={staff}
              activities={activities}
              loading={operationsLoading}
              selectedStaffId={activityStaffId}
              onSelectedStaffIdChange={setActivityStaffId}
              onSubmit={submitActivity}
            />
          )}

          {activeTab === "payroll" && canManagePayroll && (
            <PayrollPanel clinicId={clinicId} staff={staff} />
          )}
        </>
      )}


      {status && <p className="alert success">{status}</p>}
      {error && <p className="alert error">{error}</p>}

      {viewingGrant && (
        <div className="modal-overlay" onClick={() => setViewingGrant(null)}>
          <div className="modal-card modal-card-wide" onClick={(event) => event.stopPropagation()}>
            <PatientHistoryPanel
              clinicId={viewingGrant.sourceClinicId}
              patient={stubPatientFromGrant(viewingGrant)}
              onChangePatient={() => setViewingGrant(null)}
              allowSharing={false}
              historyReadOnly={viewingGrant.accessLevel !== "FULL"}
              canWriteNotes={viewingGrant.accessLevel !== "READ_ONLY"}
              defaultDoctorId={viewingGrant.invitedByStaffId}
            />
          </div>
        </div>
      )}

      {showAddStaffModal && clinicId && (
        <AddStaffModal
          clinicId={clinicId}
          onClose={() => setShowAddStaffModal(false)}
          onSaved={() => {
            setShowAddStaffModal(false);
            setStatus("Miembro del personal agregado exitosamente.");
            load();
          }}
        />
      )}

      {editingStaff && clinicId && (
        <EditStaffModal
          clinicId={clinicId}
          staff={editingStaff}
          onClose={() => setEditingStaff(null)}
          onSaved={() => {
            setEditingStaff(null);
            setStatus("Rol de miembro del personal modificado exitosamente.");
            load();
          }}
        />
      )}
    </section>
  );
}

function StaffAttendancePanel({
  staff,
  attendance,
  loading,
  currentStaff,
  selectedStaffId,
  onClockIn,
  onClockOut
}: {
  staff: ClinicStaffResponse[];
  attendance: StaffAttendanceResponse[];
  loading: boolean;
  currentStaff?: ClinicStaffResponse;
  selectedStaffId: string;
  onClockIn: () => void;
  onClockOut: () => void;
}) {
  const selectedOpenEntry = attendance.find((entry) => entry.staffId === selectedStaffId && entry.status === "OPEN");

  return (
    <>
      <article className="panel full">
        <div className="panel-heading">
          <div>
            <h2>Asistencia del personal</h2>
            <span className="panel-subtitle">Registra entradas, salidas e historial operativo por empleado</span>
          </div>
          <span className="badge neutral">{attendance.length}</span>
        </div>
        <div className="profile-form">
          <div className="field staff-attendance-current-user">
            <span>Empleado</span>
            {currentStaff ? (
              <strong>{currentStaff.fullName}</strong>
            ) : (
              <span className="description">Tu usuario no tiene un registro activo en esta clinica.</span>
            )}
            <small>Solo puedes registrar tu propia entrada y salida.</small>
          </div>
          <div className="form-actions">
            <button className="btn primary" type="button" disabled={!currentStaff || !selectedStaffId || Boolean(selectedOpenEntry)} onClick={onClockIn}>
              <IconClock size={16} aria-hidden="true" />
              Entrada
            </button>
            <button className="btn secondary" type="button" disabled={!currentStaff || !selectedOpenEntry} onClick={onClockOut}>
              <IconCheck size={16} aria-hidden="true" />
              Salida
            </button>
          </div>
        </div>
      </article>

      <article className="panel full">
        <div className="table-wrapper">
          <table className="data-table no-row-click">
            <thead>
              <tr>
                <th>Empleado</th>
                <th>Fecha</th>
                <th>Entrada</th>
                <th>Salida</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan={5}>Cargando asistencia...</td>
                </tr>
              )}
              {!loading && attendance.length === 0 && (
                <tr>
                  <td colSpan={5}>
                    <div className="empty-table-state">
                      <strong>Sin registros de asistencia</strong>
                      <span>Las entradas y salidas apareceran aqui.</span>
                    </div>
                  </td>
                </tr>
              )}
              {attendance.map((entry) => (
                <tr key={entry.id}>
                  <td>{staffName(staff, entry.staffId)}</td>
                  <td>{entry.workDate}</td>
                  <td>{formatDateTime(entry.clockInAt)}</td>
                  <td>{formatDateTime(entry.clockOutAt)}</td>
                  <td><span className={`badge ${entry.status === "OPEN" ? "warning" : "success"}`}>{entry.status === "OPEN" ? "Abierta" : "Cerrada"}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </article>
    </>
  );
}

function StaffActivityPanel({
  staff,
  activities,
  loading,
  selectedStaffId,
  onSelectedStaffIdChange,
  onSubmit
}: {
  staff: ClinicStaffResponse[];
  activities: StaffActivityResponse[];
  loading: boolean;
  selectedStaffId: string;
  onSelectedStaffIdChange: (staffId: string) => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
}) {
  return (
    <>
      <article className="panel full">
        <div className="panel-heading">
          <div>
            <h2>Actividad y productividad</h2>
            <span className="panel-subtitle">Registra tratamientos, procedimientos, ventas u otras acciones operativas</span>
          </div>
        </div>
        <form className="profile-form" onSubmit={onSubmit}>
          <label className="field">
            <span>Empleado</span>
            <select name="staffId" value={selectedStaffId} onChange={(event) => onSelectedStaffIdChange(event.target.value)}>
              {staff.map((member) => (
                <option key={member.staffId} value={member.staffId}>{member.fullName}</option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Tipo</span>
            <select name="type" defaultValue="TREATMENT">
              {Object.entries(activityTypeLabels).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Monto</span>
            <input name="amount" type="number" min="0" step="0.01" placeholder="0.00" />
          </label>
          <label className="field">
            <span>Referencia</span>
            <input name="referenceType" placeholder="ticket, tratamiento, visita" />
          </label>
          <label className="field field-full">
            <span>Descripcion</span>
            <textarea name="description" rows={3} placeholder="Detalle operativo de la actividad" />
          </label>
          <div className="form-actions">
            <button className="btn primary" type="submit" disabled={!selectedStaffId}>
              <IconPlus size={16} aria-hidden="true" />
              Registrar actividad
            </button>
          </div>
        </form>
      </article>

      <article className="panel full">
        <div className="table-wrapper">
          <table className="data-table no-row-click">
            <thead>
              <tr>
                <th>Empleado</th>
                <th>Tipo</th>
                <th>Descripcion</th>
                <th>Monto</th>
                <th>Fecha</th>
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr><td colSpan={5}>Cargando actividades...</td></tr>
              )}
              {!loading && activities.length === 0 && (
                <tr>
                  <td colSpan={5}>
                    <div className="empty-table-state">
                      <strong>Sin actividades registradas</strong>
                      <span>La productividad del personal se mostrara aqui.</span>
                    </div>
                  </td>
                </tr>
              )}
              {activities.map((activity) => (
                <tr key={activity.id}>
                  <td>{staffName(staff, activity.staffId)}</td>
                  <td>{activityTypeLabels[activity.type]}</td>
                  <td>{activity.description || activity.referenceType || "-"}</td>
                  <td>{formatMoney(activity.amount)}</td>
                  <td>{formatDateTime(activity.occurredAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </article>
    </>
  );
}

function AddStaffModal({
  clinicId,
  onClose,
  onSaved
}: {
  clinicId: string;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    const form = new FormData(event.currentTarget);
    const email = String(form.get("email") ?? "").trim();
    const role = String(form.get("role") ?? "DOCTOR");

    try {
      await staffApi.invite(clinicId, { email, role });
      onSaved();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Invitar personal de la clínica</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>
        <form className="profile-form" onSubmit={submit}>
          <p className="field-full" style={{ fontSize: "13px", color: "var(--color-text-2)", marginBottom: "15px" }}>
            Envía una invitación a un miembro del personal de tu clínica.
            El correo ingresado no debe estar registrado previamente en la plataforma.
            Se generará un enlace para que el invitado complete su registro con sus datos.
          </p>

          <label className="field field-full">
            <span>Correo del usuario</span>
            <input name="email" type="email" required placeholder="usuario@ejemplo.com" />
          </label>

          <label className="field field-full">
            <span>Rol</span>
            <select name="role" defaultValue="DOCTOR">
              <option value="DOCTOR">Doctor / Especialista</option>
              <option value="RECEPTIONIST">Recepcionista</option>
              <option value="ASSISTANT">Asistente médico</option>
              <option value="ADMIN">Administrador de sistema</option>
              <option value="CLINIC_ADMIN">Administrador de clínica</option>
              <option value="ACCOUNTANT">Contador</option>
              <option value="CLEANING">Personal de limpieza</option>
            </select>
          </label>

          {error && <p className="alert error">{error}</p>}
          <div className="form-actions" style={{ marginTop: "20px" }}>
            <button className="btn primary" disabled={loading} type="submit">
              <IconUserPlus size={18} aria-hidden="true" style={{ marginRight: "6px" }} />
              {loading ? "Enviando..." : "Enviar invitación"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function EditStaffModal({
  clinicId,
  staff,
  onClose,
  onSaved
}: {
  clinicId: string;
  staff: ClinicStaffResponse;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [permissionSummary, setPermissionSummary] = useState<StaffPermissionSummary | null>(null);
  const [permissionsLoading, setPermissionsLoading] = useState(true);
  const isSuperAdmin = staff.role === "ADMIN" || permissionSummary?.role === "ADMIN";

  useEffect(() => {
    setPermissionsLoading(true);
    staffApi
      .getPermissions(clinicId, staff.staffId)
      .then(setPermissionSummary)
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setPermissionsLoading(false));
  }, [clinicId, staff.staffId]);

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    const form = new FormData(event.currentTarget);
    const role = isSuperAdmin ? "ADMIN" : String(form.get("role") ?? "DOCTOR");

    try {
      await staffApi.update(clinicId, staff.staffId, { role });
      if (permissionSummary && !isSuperAdmin) {
        const permissions: StaffPermissionChange[] = permissionSummary.permissions.map((item) => ({
          permission: item.permission,
          state: item.overrideState
        }));
        await staffApi.updatePermissions(clinicId, staff.staffId, permissions);
      }
      onSaved();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Modificar rol de personal</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>
        <form className="profile-form" onSubmit={submit}>
          <div className="clinic-row" style={{ marginBottom: "15px", borderBottom: "none" }}>
            <div>
              <strong>{staff.fullName}</strong>
              <span style={{ fontSize: "12px", color: "var(--color-text-2)" }}>Modificando permisos y rol dentro de la clínica</span>
            </div>
          </div>

          <label className="field field-full">
            <span>Nuevo Rol</span>
            <select name="role" defaultValue={staff.role} disabled={Boolean(isSuperAdmin)}>
              <option value="DOCTOR">Doctor / Especialista</option>
              <option value="RECEPTIONIST">Recepcionista</option>
              <option value="ASSISTANT">Asistente médico</option>
              <option value="ADMIN">Administrador de sistema</option>
              <option value="CLINIC_ADMIN">Administrador de clínica</option>
              <option value="ACCOUNTANT">Contador</option>
              <option value="CLEANING">Personal de limpieza</option>
            </select>
          </label>

          <div className="staff-permissions-panel">
            <div className="panel-heading">
              <div>
                <h3>Accesos individuales</h3>
                <p className="description">El rol define la base. Puedes permitir o revocar acciones concretas.</p>
              </div>
            </div>
            {isSuperAdmin && (
              <div className="staff-super-admin-notice compact">
                <IconShieldLock size={16} aria-hidden="true" />
                <span>El Administrador es el superadministrador y sus permisos no se pueden modificar.</span>
              </div>
            )}
            {permissionsLoading && <p className="description">Cargando accesos...</p>}
            {!permissionsLoading && permissionSummary && (
              <div className="staff-permissions-list">
                {permissionSummary.permissions.map((item) => {
                  const copy = staffPermissionLabels[item.permission] ?? {
                    label: item.permission,
                    description: "Permiso operativo del modulo"
                  };
                  return (
                    <div className="staff-permission-row" key={item.permission}>
                      <div>
                        <strong>{copy.label}</strong>
                        <span>{copy.description}</span>
                      </div>
                      <select
                        value={item.overrideState}
                        disabled={Boolean(isSuperAdmin)}
                        onChange={(event) => {
                          const state = event.target.value as StaffPermissionOverrideState;
                          setPermissionSummary((current) => current ? {
                            ...current,
                            permissions: current.permissions.map((permission) => permission.permission === item.permission
                              ? { ...permission, overrideState: state, enabled: state === "GRANTED" || (state === "INHERIT" && permission.enabled) }
                              : permission)
                          } : current);
                        }}
                      >
                        {(Object.keys(permissionStateLabels) as StaffPermissionOverrideState[]).map((state) => (
                          <option value={state} key={state}>{permissionStateLabels[state]}</option>
                        ))}
                      </select>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {error && <p className="alert error">{error}</p>}
          <div className="form-actions" style={{ marginTop: "20px" }}>
            <button className="btn primary" disabled={loading || Boolean(isSuperAdmin)} type="submit">
              {loading ? "Guardando..." : "Guardar cambios"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
