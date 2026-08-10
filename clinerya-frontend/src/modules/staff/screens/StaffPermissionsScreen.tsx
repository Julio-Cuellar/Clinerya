import { useEffect, useMemo, useState } from "react";
import { IconArrowLeft, IconCheck, IconLock, IconShieldLock } from "@tabler/icons-react";
import {
  getFriendlyError,
  staffApi,
  type ClinicStaffResponse,
  type StaffPermission,
  type StaffPermissionOverrideState,
  type StaffPermissionSummary
} from "@shared/api/api";

type PermissionDefinition = {
  permission: StaffPermission;
  label: string;
  description: string;
  module: string;
  moduleDescription: string;
};

const permissionCatalog: PermissionDefinition[] = [
  { permission: "VIEW_DASHBOARD", label: "Ver dashboard", description: "Consultar el resumen general de la clinica.", module: "Dashboard", moduleDescription: "Indicadores, pendientes y alertas principales de la operacion." },
  { permission: "VIEW_DASHBOARD_METRICS", label: "Ver indicadores", description: "Consultar metricas, tendencias y resultados resumidos.", module: "Dashboard", moduleDescription: "Indicadores, pendientes y alertas principales de la operacion." },
  { permission: "VIEW_OPERATIONAL_ALERTS", label: "Ver alertas operativas", description: "Consultar pendientes y situaciones que requieren atencion.", module: "Dashboard", moduleDescription: "Indicadores, pendientes y alertas principales de la operacion." },

  { permission: "MANAGE_AGENDA", label: "Gestionar agenda", description: "Administrar la agenda completa de la clinica.", module: "Agenda", moduleDescription: "Citas, disponibilidad, salas y seguimiento de la jornada." },
  { permission: "VIEW_AGENDA", label: "Consultar agenda", description: "Ver citas, horarios y disponibilidad.", module: "Agenda", moduleDescription: "Citas, disponibilidad, salas y seguimiento de la jornada." },
  { permission: "CREATE_APPOINTMENTS", label: "Crear citas", description: "Registrar nuevas citas para pacientes.", module: "Agenda", moduleDescription: "Citas, disponibilidad, salas y seguimiento de la jornada." },
  { permission: "EDIT_APPOINTMENTS", label: "Editar citas", description: "Modificar fecha, hora, medico o consultorio.", module: "Agenda", moduleDescription: "Citas, disponibilidad, salas y seguimiento de la jornada." },
  { permission: "CANCEL_APPOINTMENTS", label: "Cancelar citas", description: "Cancelar citas y registrar el motivo.", module: "Agenda", moduleDescription: "Citas, disponibilidad, salas y seguimiento de la jornada." },
  { permission: "MANAGE_WAITING_LIST", label: "Gestionar lista de espera", description: "Agregar, priorizar y atender pacientes en espera.", module: "Agenda", moduleDescription: "Citas, disponibilidad, salas y seguimiento de la jornada." },
  { permission: "MANAGE_SCHEDULES", label: "Gestionar horarios", description: "Configurar horarios de atencion y disponibilidad.", module: "Agenda", moduleDescription: "Citas, disponibilidad, salas y seguimiento de la jornada." },

  { permission: "MANAGE_ROOMS", label: "Gestionar consultorios", description: "Administrar consultorios y sus propiedades.", module: "Consultorios", moduleDescription: "Espacios de atencion, asignaciones y disponibilidad." },
  { permission: "VIEW_ROOMS", label: "Consultar consultorios", description: "Ver espacios, estado y disponibilidad.", module: "Consultorios", moduleDescription: "Espacios de atencion, asignaciones y disponibilidad." },
  { permission: "CREATE_ROOMS", label: "Crear consultorios", description: "Registrar nuevos espacios de atencion.", module: "Consultorios", moduleDescription: "Espacios de atencion, asignaciones y disponibilidad." },
  { permission: "EDIT_ROOMS", label: "Editar consultorios", description: "Modificar nombre, capacidad y propiedades.", module: "Consultorios", moduleDescription: "Espacios de atencion, asignaciones y disponibilidad." },
  { permission: "ASSIGN_ROOM_STAFF", label: "Asignar personal a consultorios", description: "Asignar o retirar medicos y personal de un consultorio.", module: "Consultorios", moduleDescription: "Espacios de atencion, asignaciones y disponibilidad." },

  { permission: "VIEW_PATIENT_CARE", label: "Consultar atencion al paciente", description: "Ver la consulta activa y su contexto clinico.", module: "Atencion al paciente", moduleDescription: "Consulta activa, notas, recetas y seguimiento de la atencion." },
  { permission: "MANAGE_PATIENT_CARE", label: "Gestionar atencion al paciente", description: "Administrar el flujo de una consulta activa.", module: "Atencion al paciente", moduleDescription: "Consulta activa, notas, recetas y seguimiento de la atencion." },
  { permission: "CREATE_VISITS", label: "Registrar visitas", description: "Abrir y registrar una nueva visita clinica.", module: "Atencion al paciente", moduleDescription: "Consulta activa, notas, recetas y seguimiento de la atencion." },
  { permission: "EDIT_VISITS", label: "Editar visitas", description: "Actualizar datos y resultados de una visita.", module: "Atencion al paciente", moduleDescription: "Consulta activa, notas, recetas y seguimiento de la atencion." },
  { permission: "MANAGE_PRESCRIPTIONS", label: "Gestionar recetas", description: "Crear y modificar recetas de pacientes.", module: "Atencion al paciente", moduleDescription: "Consulta activa, notas, recetas y seguimiento de la atencion." },

  { permission: "MANAGE_PATIENTS", label: "Gestionar pacientes", description: "Administrar el directorio completo de pacientes.", module: "Pacientes", moduleDescription: "Directorio, datos de contacto y ciclo de vida del paciente." },
  { permission: "VIEW_PATIENTS", label: "Consultar pacientes", description: "Buscar y consultar datos de pacientes.", module: "Pacientes", moduleDescription: "Directorio, datos de contacto y ciclo de vida del paciente." },
  { permission: "CREATE_PATIENTS", label: "Crear pacientes", description: "Registrar nuevos pacientes.", module: "Pacientes", moduleDescription: "Directorio, datos de contacto y ciclo de vida del paciente." },
  { permission: "EDIT_PATIENTS", label: "Editar pacientes", description: "Actualizar datos personales y de contacto.", module: "Pacientes", moduleDescription: "Directorio, datos de contacto y ciclo de vida del paciente." },
  { permission: "EXPORT_PATIENTS", label: "Exportar pacientes", description: "Descargar listados de pacientes autorizados.", module: "Pacientes", moduleDescription: "Directorio, datos de contacto y ciclo de vida del paciente." },

  { permission: "VIEW_MEDICAL_RECORDS", label: "Consultar expedientes", description: "Ver informacion clinica y antecedentes.", module: "Expediente", moduleDescription: "Historias clinicas, notas, documentos y auditoria del paciente." },
  { permission: "EDIT_MEDICAL_RECORDS", label: "Editar expedientes", description: "Modificar informacion clinica del paciente.", module: "Expediente", moduleDescription: "Historias clinicas, notas, documentos y auditoria del paciente." },
  { permission: "CREATE_CLINICAL_NOTES", label: "Crear notas clinicas", description: "Registrar notas y evoluciones clinicas.", module: "Expediente", moduleDescription: "Historias clinicas, notas, documentos y auditoria del paciente." },
  { permission: "EDIT_CLINICAL_NOTES", label: "Editar notas clinicas", description: "Modificar notas clinicas propias o autorizadas.", module: "Expediente", moduleDescription: "Historias clinicas, notas, documentos y auditoria del paciente." },
  { permission: "VIEW_RECORD_DOCUMENTS", label: "Consultar documentos", description: "Ver estudios, archivos y documentos del expediente.", module: "Expediente", moduleDescription: "Historias clinicas, notas, documentos y auditoria del paciente." },
  { permission: "MANAGE_RECORD_TEMPLATES", label: "Gestionar plantillas", description: "Crear y modificar plantillas de historia clinica.", module: "Expediente", moduleDescription: "Historias clinicas, notas, documentos y auditoria del paciente." },

  { permission: "MANAGE_TREATMENTS", label: "Gestionar tratamientos", description: "Administrar tratamientos, cotizaciones y procedimientos.", module: "Tratamientos", moduleDescription: "Catalogo de servicios, cotizaciones, procedimientos y ventas." },
  { permission: "VIEW_TREATMENTS", label: "Consultar tratamientos", description: "Ver catalogo, tratamientos y precios.", module: "Tratamientos", moduleDescription: "Catalogo de servicios, cotizaciones, procedimientos y ventas." },
  { permission: "MANAGE_TREATMENT_CATALOG", label: "Gestionar catalogo", description: "Crear y modificar servicios y tratamientos.", module: "Tratamientos", moduleDescription: "Catalogo de servicios, cotizaciones, procedimientos y ventas." },
  { permission: "MANAGE_QUOTATIONS", label: "Gestionar cotizaciones", description: "Crear, editar y autorizar cotizaciones.", module: "Tratamientos", moduleDescription: "Catalogo de servicios, cotizaciones, procedimientos y ventas." },
  { permission: "MANAGE_PROCEDURES", label: "Gestionar procedimientos", description: "Registrar y actualizar procedimientos realizados.", module: "Tratamientos", moduleDescription: "Catalogo de servicios, cotizaciones, procedimientos y ventas." },
  { permission: "REGISTER_TREATMENT_SALES", label: "Registrar ventas de tratamientos", description: "Convertir tratamientos y procedimientos en ventas.", module: "Tratamientos", moduleDescription: "Catalogo de servicios, cotizaciones, procedimientos y ventas." },

  { permission: "MANAGE_CASH", label: "Gestionar caja", description: "Administrar cobros, turnos y movimientos de caja.", module: "Caja", moduleDescription: "Cobros, turnos, tickets, cortes y gastos diarios." },
  { permission: "VIEW_CASH", label: "Consultar caja", description: "Ver cobros, tickets y movimientos de caja.", module: "Caja", moduleDescription: "Cobros, turnos, tickets, cortes y gastos diarios." },
  { permission: "CREATE_CHARGES", label: "Registrar cobros", description: "Registrar pagos y emitir tickets.", module: "Caja", moduleDescription: "Cobros, turnos, tickets, cortes y gastos diarios." },
  { permission: "MANAGE_REFUNDS", label: "Gestionar reembolsos", description: "Solicitar y autorizar devoluciones.", module: "Caja", moduleDescription: "Cobros, turnos, tickets, cortes y gastos diarios." },
  { permission: "MANAGE_CASH_CUTS", label: "Gestionar cortes", description: "Abrir, cerrar y revisar cortes de caja.", module: "Caja", moduleDescription: "Cobros, turnos, tickets, cortes y gastos diarios." },
  { permission: "MANAGE_EXPENSES", label: "Gestionar gastos", description: "Registrar y controlar gastos de la clinica.", module: "Caja", moduleDescription: "Cobros, turnos, tickets, cortes y gastos diarios." },

  { permission: "MANAGE_INVENTORY", label: "Gestionar inventario", description: "Administrar existencias y movimientos.", module: "Inventario", moduleDescription: "Materiales, existencias, compras, lotes y kardex." },
  { permission: "VIEW_INVENTORY", label: "Consultar inventario", description: "Ver existencias, lotes y kardex.", module: "Inventario", moduleDescription: "Materiales, existencias, compras, lotes y kardex." },
  { permission: "MANAGE_MATERIALS", label: "Gestionar materiales", description: "Crear y modificar materiales e insumos.", module: "Inventario", moduleDescription: "Materiales, existencias, compras, lotes y kardex." },
  { permission: "MANAGE_PURCHASES", label: "Gestionar compras", description: "Administrar proveedores, ordenes y recepciones.", module: "Inventario", moduleDescription: "Materiales, existencias, compras, lotes y kardex." },
  { permission: "MANAGE_INVENTORY_MOVEMENTS", label: "Gestionar movimientos", description: "Registrar entradas, salidas, ajustes y reservas.", module: "Inventario", moduleDescription: "Materiales, existencias, compras, lotes y kardex." },
  { permission: "MANAGE_INVENTORY_LOTS", label: "Gestionar lotes", description: "Administrar lotes, caducidades y trazabilidad.", module: "Inventario", moduleDescription: "Materiales, existencias, compras, lotes y kardex." },

  { permission: "VIEW_ACCOUNTING", label: "Consultar contabilidad", description: "Consultar saldos y movimientos contables.", module: "Contabilidad", moduleDescription: "Movimientos, cuentas, bancos, reportes y rentabilidad." },
  { permission: "MANAGE_ACCOUNTING", label: "Gestionar contabilidad", description: "Administrar configuracion y operaciones contables.", module: "Contabilidad", moduleDescription: "Movimientos, cuentas, bancos, reportes y rentabilidad." },
  { permission: "VIEW_JOURNAL_ENTRIES", label: "Consultar libro diario", description: "Ver asientos y movimientos del libro diario.", module: "Contabilidad", moduleDescription: "Movimientos, cuentas, bancos, reportes y rentabilidad." },
  { permission: "CREATE_JOURNAL_ENTRIES", label: "Crear asientos", description: "Registrar asientos contables manuales.", module: "Contabilidad", moduleDescription: "Movimientos, cuentas, bancos, reportes y rentabilidad." },
  { permission: "VIEW_FINANCIAL_REPORTS", label: "Consultar reportes financieros", description: "Ver estado de resultados y reportes financieros.", module: "Contabilidad", moduleDescription: "Movimientos, cuentas, bancos, reportes y rentabilidad." },
  { permission: "MANAGE_BANK_ACCOUNTS", label: "Gestionar cuentas bancarias", description: "Administrar cuentas bancarias y conciliaciones.", module: "Contabilidad", moduleDescription: "Movimientos, cuentas, bancos, reportes y rentabilidad." },

  { permission: "MANAGE_STAFF", label: "Gestionar personal", description: "Administrar altas, roles y estado del personal.", module: "Personal", moduleDescription: "Directorio, permisos, asistencia, actividad y nomina." },
  { permission: "VIEW_STAFF", label: "Consultar personal", description: "Ver el directorio y los datos laborales.", module: "Personal", moduleDescription: "Directorio, permisos, asistencia, actividad y nomina." },
  { permission: "MANAGE_STAFF_PERMISSIONS", label: "Gestionar permisos", description: "Agregar, heredar o revocar permisos individuales.", module: "Personal", moduleDescription: "Directorio, permisos, asistencia, actividad y nomina." },
  { permission: "MANAGE_ATTENDANCE", label: "Gestionar asistencia", description: "Registrar entradas, salidas y jornadas.", module: "Personal", moduleDescription: "Directorio, permisos, asistencia, actividad y nomina." },
  { permission: "MANAGE_STAFF_ACTIVITY", label: "Gestionar actividad", description: "Registrar tratamientos, ventas y acciones del personal.", module: "Personal", moduleDescription: "Directorio, permisos, asistencia, actividad y nomina." },
  { permission: "MANAGE_PAYROLL", label: "Gestionar nomina", description: "Crear periodos y administrar pagos del personal.", module: "Personal", moduleDescription: "Directorio, permisos, asistencia, actividad y nomina." },
  { permission: "VIEW_PAYROLL", label: "Consultar nomina", description: "Ver periodos, lineas y totales de nomina.", module: "Personal", moduleDescription: "Directorio, permisos, asistencia, actividad y nomina." },

  { permission: "MANAGE_CLINIC", label: "Gestionar clinica", description: "Administrar datos generales y propiedades.", module: "Configuracion", moduleDescription: "Datos generales, integraciones, respaldos y preferencias." },
  { permission: "VIEW_CLINIC_SETTINGS", label: "Consultar configuracion", description: "Ver propiedades y preferencias de la clinica.", module: "Configuracion", moduleDescription: "Datos generales, integraciones, respaldos y preferencias." },
  { permission: "MANAGE_INTEGRATIONS", label: "Gestionar integraciones", description: "Conectar y configurar plataformas externas.", module: "Configuracion", moduleDescription: "Datos generales, integraciones, respaldos y preferencias." },
  { permission: "MANAGE_CLINIC_BACKUPS", label: "Gestionar respaldos", description: "Crear y administrar respaldos del sistema.", module: "Configuracion", moduleDescription: "Datos generales, integraciones, respaldos y preferencias." }
];

const permissionStateLabels: Record<StaffPermissionOverrideState, string> = {
  INHERIT: "Heredar del rol",
  GRANTED: "Permitido",
  REVOKED: "Revocado"
};

const moduleOrder = [...new Set(permissionCatalog.map((item) => item.module))];

export function StaffPermissionsScreen({
  clinicId,
  employee,
  staffLoading,
  onBack
}: {
  clinicId: string;
  employee?: ClinicStaffResponse;
  staffLoading: boolean;
  onBack: () => void;
}) {
  const [summary, setSummary] = useState<StaffPermissionSummary | null>(null);
  const [selectedModule, setSelectedModule] = useState(moduleOrder[0]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [status, setStatus] = useState("");
  const employeeStaffId = employee?.staffId;

  useEffect(() => {
    if (!employeeStaffId) return;
    let cancelled = false;
    setLoading(true);
    setSummary(null);
    setError("");
    setStatus("");
    staffApi
      .getPermissions(clinicId, employeeStaffId)
      .then((nextSummary) => {
        if (!cancelled) setSummary(nextSummary);
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
  }, [clinicId, employeeStaffId]);

  const modules = useMemo(
    () => moduleOrder.map((module) => ({
      module,
      definition: permissionCatalog.find((item) => item.module === module),
      permissions: permissionCatalog.filter((item) => item.module === module)
    })),
    []
  );
  const currentModule = modules.find((module) => module.module === selectedModule) ?? modules[0];
  const isSuperAdmin = summary?.role === "ADMIN" || employee?.role === "ADMIN";
  const summaryByPermission = new Map(summary?.permissions.map((item) => [item.permission, item]) ?? []);
  const currentPermissions = currentModule?.permissions ?? [];
  const activePermissions = currentPermissions.filter((item) => summaryByPermission.get(item.permission)?.enabled);
  const availablePermissions = currentPermissions.filter((item) => !summaryByPermission.get(item.permission)?.enabled);
  const activeCount = currentModule?.permissions.filter((item) => summaryByPermission.get(item.permission)?.enabled).length ?? 0;

  const updatePermissionState = (permission: StaffPermission, state: StaffPermissionOverrideState) => {
    setSummary((current) => current ? {
      ...current,
      permissions: current.permissions.map((item) => item.permission === permission
        ? { ...item, overrideState: state, enabled: state === "GRANTED" ? true : state === "REVOKED" ? false : item.enabled }
        : item)
    } : current);
  };

  const save = async () => {
    if (!summary || !employee || isSuperAdmin) return;
    setSaving(true);
    setError("");
    setStatus("");
    try {
      const saved = await staffApi.updatePermissions(
        clinicId,
        employee.staffId,
        summary.permissions.map((item) => ({ permission: item.permission, state: item.overrideState }))
      );
      setSummary(saved);
      setStatus("Permisos actualizados correctamente.");
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  const renderPermissionRow = (definition: PermissionDefinition) => {
    const item = summaryByPermission.get(definition.permission);
    if (!item) return null;
    return (
      <div className="staff-permission-detail-row" key={definition.permission}>
        <div>
          <strong>{definition.label}</strong>
          <span>{definition.description}</span>
          <small>{definition.permission}</small>
        </div>
        <div className="staff-permission-control">
          {item.enabled ? (
            <span className="badge success"><IconCheck size={13} aria-hidden="true" /> Activo</span>
          ) : (
            <span className="badge neutral">Disponible</span>
          )}
          <select
            value={item.overrideState}
            disabled={loading || saving || Boolean(isSuperAdmin)}
            aria-label={`Estado de ${definition.label}`}
            onChange={(event) => updatePermissionState(definition.permission, event.target.value as StaffPermissionOverrideState)}
          >
            {(Object.keys(permissionStateLabels) as StaffPermissionOverrideState[]).map((state) => (
              <option value={state} key={state}>{permissionStateLabels[state]}</option>
            ))}
          </select>
        </div>
      </div>
    );
  };

  if (staffLoading && !employee) {
    return <section className="staff-detail-page"><p className="description">Cargando empleado...</p></section>;
  }

  if (!employee) {
    return (
      <section className="staff-detail-page">
        <button className="btn secondary staff-back-button" type="button" onClick={onBack}>
          <IconArrowLeft size={16} aria-hidden="true" /> Volver al detalle
        </button>
        <p className="alert error">No se encontro el empleado seleccionado.</p>
      </section>
    );
  }

  return (
    <section className="staff-permissions-page">
      <div className="staff-detail-header">
        <button className="icon-btn" type="button" aria-label="Volver al detalle del empleado" onClick={onBack}>
          <IconArrowLeft size={18} aria-hidden="true" />
        </button>
        <div>
          <p className="eyebrow">Personal / {employee.fullName} / Accesos</p>
          <h2>Permisos del empleado</h2>
          <p className="description">Explora los permisos por modulo y define los accesos disponibles para este empleado.</p>
        </div>
      </div>

      {isSuperAdmin && (
        <div className="staff-super-admin-notice">
          <IconLock size={18} aria-hidden="true" />
          <div>
            <strong>Superadministrador protegido</strong>
            <span>El usuario Administrador tiene acceso total al sistema y sus permisos no se pueden agregar, modificar ni revocar.</span>
          </div>
        </div>
      )}

      {error && <p className="alert error">{error}</p>}
      {status && <p className="alert success">{status}</p>}

      <div className="staff-permission-workspace">
        <nav className="staff-permission-module-nav" aria-label="Modulos de permisos">
          <p className="staff-permission-nav-title">Modulos</p>
          {modules.map((module) => {
            const moduleActiveCount = module.permissions.filter((item) => summaryByPermission.get(item.permission)?.enabled).length;
            return (
              <button
                className={selectedModule === module.module ? "active" : ""}
                type="button"
                key={module.module}
                onClick={() => setSelectedModule(module.module)}
              >
                <span>{module.module}</span>
                <small>{moduleActiveCount}/{module.permissions.length}</small>
              </button>
            );
          })}
        </nav>

        <div className="staff-permission-module-content">
          <div className="staff-permission-module-heading">
            <div>
              <p className="eyebrow">Modulo seleccionado</p>
              <h3>{currentModule?.module}</h3>
              <p className="description">{currentModule?.definition?.moduleDescription}</p>
            </div>
            <span className="badge neutral">{activeCount} activos · {availablePermissions.length} disponibles</span>
          </div>

          {loading && <p className="description">Cargando permisos...</p>}
          {!loading && (
            <>
              <section className="staff-permission-section">
                <div className="staff-permission-section-heading">
                  <div>
                    <h4>Permisos que tiene</h4>
                    <span>Accesos efectivos por el rol o por una autorizacion individual.</span>
                  </div>
                  <span className="badge success">{activePermissions.length}</span>
                </div>
                <div className="staff-permission-detail-list">
                  {activePermissions.length > 0 ? activePermissions.map(renderPermissionRow) : <p className="description">No tiene permisos activos en este modulo.</p>}
                </div>
              </section>

              <section className="staff-permission-section">
                <div className="staff-permission-section-heading">
                  <div>
                    <h4>Permisos disponibles</h4>
                    <span>Acciones existentes que puedes habilitar para este empleado.</span>
                  </div>
                  <span className="badge neutral">{availablePermissions.length}</span>
                </div>
                <div className="staff-permission-detail-list">
                  {availablePermissions.length > 0 ? availablePermissions.map(renderPermissionRow) : <p className="description">Todos los permisos de este modulo estan activos.</p>}
                </div>
              </section>
            </>
          )}
        </div>
      </div>

      <div className="form-actions staff-permission-actions">
        <button className="btn secondary" type="button" onClick={onBack}>
          <IconArrowLeft size={16} aria-hidden="true" /> Volver al detalle
        </button>
        <button className="btn primary" type="button" disabled={loading || saving || Boolean(isSuperAdmin)} onClick={save}>
          {saving ? "Guardando..." : "Guardar permisos"}
        </button>
      </div>
    </section>
  );
}
