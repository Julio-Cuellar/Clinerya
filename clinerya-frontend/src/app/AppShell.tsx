import { useEffect, useMemo, useState } from "react";
import { IconLogout, IconMenu2, IconMoon, IconPlus, IconShieldLock, IconSun } from "@tabler/icons-react";
import { NotificationCenter } from "@modules/notifications/components/NotificationCenter";
import { ClinicModal } from "@modules/clinics/components/ClinicModal";
import { ClinicOnboardingBanner } from "@modules/clinics/components/ClinicOnboardingBanner";
import { ClinicOnboardingScreen } from "@modules/clinics/screens/ClinicOnboardingScreen";
import {
  dismissOnboarding,
  getClinicOnboardingState,
  isOnboardingDismissed
} from "@modules/clinics/lib/clinicOnboarding";
import { treatmentCatalogApi } from "@shared/api/api";
import { ModuleWelcomeScreen } from "@shared/layout/ModuleWelcomeScreen";
import { PatientModal } from "@modules/patients/components/PatientModal";
import { PatientsPanel } from "@modules/patients/components/PatientsPanel";
import { Sidebar, getAllowedModulesForUser } from "@shared/layout/Sidebar";
import { AgendaScreen } from "@modules/agenda/screens/AgendaScreen";
import { CajaScreen } from "@modules/cash/screens/CajaScreen";
import { DashboardScreen } from "@modules/dashboard/screens/DashboardScreen";
import { ExpedienteScreen } from "@modules/records/screens/ExpedienteScreen";
import { InventarioScreen } from "@modules/inventory/screens/InventarioScreen";
import { PersonalScreen } from "@modules/staff/screens/PersonalScreen";
import { ConsultoriosScreen } from "@modules/clinics/screens/ConsultoriosScreen";
import { ContabilidadScreen } from "@modules/accounting/screens/ContabilidadScreen";
import { TratamientosScreen } from "@modules/treatments/screens/TratamientosScreen";
import { PatientCareScreen } from "@modules/treatments/screens/PatientCareScreen";
import { SettingsScreen } from "@modules/settings/screens/SettingsScreen";
import { AppointmentRequestsScreen } from "@modules/automation/screens/AppointmentRequestsScreen";
import { WhatsAppChatsScreen } from "@modules/automation/screens/WhatsAppChatsScreen";
import { mobileRestricted, moduleCopy, modules, type ModuleKey } from "@app/constants/modules";
import { authApi, clinicsApi, getFriendlyError, patientsApi, sessionStore, ticketsApi } from "@shared/api/api";
import type { UserProfile } from "@modules/auth/types";
import type { ClinicResponse } from "@modules/clinics/types";
import type { PatientResponse } from "@modules/patients/types";
import type { AppointmentResponse } from "@modules/agenda/types";
import type { Theme } from "@modules/settings/themeTypes";
import { getInitials } from "@shared/utils/getInitials";

const modulePathByKey: Record<ModuleKey, string> = {
  dashboard: "/dashboard",
  agenda: "/agenda",
  // El aviso de WhatsApp al medico enlaza a esta ruta (app.automation.doctor-inbox-path).
  solicitudes: "/solicitudes-de-cita",
  chats: "/chats-whatsapp",
  consultorios: "/consultorios",
  atencion: "/atencion",
  pacientes: "/pacientes",
  expediente: "/expediente",
  tratamientos: "/tratamientos",
  caja: "/caja",
  inventario: "/inventario",
  contabilidad: "/contabilidad/reportes-generales",
  personal: "/personal",
  configuracion: "/configuracion"
};

const getModuleWelcomeStorageKey = (userId: string, clinicId: string, moduleKey: ModuleKey) =>
  `clinicloud.module-welcome.${userId}.${clinicId}.${moduleKey}`;

function moduleFromPath(): ModuleKey {
  const firstSegment = window.location.pathname.split("/").filter(Boolean)[0];
  if (!firstSegment) {
    return "dashboard";
  }
  const matched = modules.find(
    (item) => item.key === firstSegment || modulePathByKey[item.key].split("/").filter(Boolean)[0] === firstSegment
  );
  return matched?.key ?? "dashboard";
}

export function AppShell({
  user,
  setUser,
  theme,
  setTheme,
  colorPalette,
  setColorPalette
}: {
  user: UserProfile;
  setUser: (user: UserProfile | null) => void;
  theme: Theme;
  setTheme: (theme: Theme) => void;
  colorPalette: string;
  setColorPalette: (color: string) => void;
}) {
  const [active, setActive] = useState<ModuleKey>(() => moduleFromPath());
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem("clinicloud.sidebar") === "collapsed");
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [clinics, setClinics] = useState<ClinicResponse[]>([]);
  const [clinicModal, setClinicModal] = useState<{ mode: "create" | "edit"; clinic?: ClinicResponse } | null>(null);
  const [patients, setPatients] = useState<PatientResponse[]>([]);
  const [pendingCajaCount, setPendingCajaCount] = useState(0);
  const [carePatient, setCarePatient] = useState<PatientResponse | null>(null);
  const [careAppointment, setCareAppointment] = useState<AppointmentResponse | undefined>(undefined);
  const [catalogItemCount, setCatalogItemCount] = useState<number | undefined>(undefined);
  const [onboardingOpen, setOnboardingOpen] = useState(false);
  const [onboardingDismissed, setOnboardingDismissed] = useState(false);

  const handleStartCare = (patient: PatientResponse, appointment?: AppointmentResponse) => {
    setCarePatient(patient);
    setCareAppointment(appointment);
    setActive("atencion");
  };

  const [patientsLoading, setPatientsLoading] = useState(false);
  const [patientModalOpen, setPatientModalOpen] = useState(false);
  const [, setWelcomeRefresh] = useState(0);
  const copy = moduleCopy[active];
  const activeModule = modules.find((item) => item.key === active);
  const ActiveModuleIcon = activeModule?.icon ?? IconPlus;
  const comingSoonModules = new Set<ModuleKey>([]);
  const initials = useMemo(() => getInitials(user.fullName), [user.fullName]);
  const activeClinicId = clinics[0]?.id ?? user.clinics?.[0]?.id;
  const activeClinic = clinics[0]?.name ?? user.clinics?.[0]?.name;
  const activeWelcomeStorageKey = activeClinicId ? getModuleWelcomeStorageKey(user.id, activeClinicId, active) : null;
  const activeClinicRecord = clinics[0];
  const onboardingState = getClinicOnboardingState(activeClinicRecord, catalogItemCount);
  const shouldShowModuleWelcome =
    active !== "dashboard" && active !== "contabilidad" && active !== "configuracion" && active !== "consultorios" && activeWelcomeStorageKey !== null && !localStorage.getItem(activeWelcomeStorageKey);

  useEffect(() => {
    if (!activeClinicId) {
      setPendingCajaCount(0);
      return;
    }
    if (active !== "dashboard" && active !== "caja") return;
    ticketsApi
      .listPendingAppointmentCharges(activeClinicId)
      .then((charges) => setPendingCajaCount(charges.length))
      .catch(() => setPendingCajaCount(0));
  }, [active, activeClinicId]);

  const loadClinics = () => {
    clinicsApi
      .list()
      .then(setClinics)
      .catch((caught) => setError(getFriendlyError(caught)));
  };

  useEffect(() => {
    loadClinics();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const googleCalendarStatus = params.get("googleCalendar");
    if (googleCalendarStatus) {
      navigateModule("configuracion");
      if (googleCalendarStatus === "connected") {
        setStatus("Google Calendar conectado correctamente.");
      } else {
        setError("No se pudo conectar Google Calendar. Intenta de nuevo.");
      }
      params.delete("googleCalendar");
      const newSearch = params.toString();
      window.history.replaceState({}, "", window.location.pathname + (newSearch ? `?${newSearch}` : ""));
    }
  }, []);

  useEffect(() => {
    const handlePopState = () => {
      setActive(moduleFromPath());
      setDrawerOpen(false);
    };
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  useEffect(() => {
    if (
      (active !== "dashboard" && active !== "pacientes" && active !== "expediente" && active !== "tratamientos" && active !== "agenda" && active !== "caja") ||
      !activeClinicId
    )
      return;
    setPatientsLoading(true);
    patientsApi
      .listByClinic(activeClinicId)
      .then(setPatients)
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setPatientsLoading(false));
  }, [active, activeClinicId]);

  const logout = async () => {
    try {
      await authApi.logout();
    } finally {
      sessionStore.clear();
      setUser(null);
    }
  };

  const navigateModule = (key: ModuleKey) => {
    setActive(key);
    setDrawerOpen(false);
    const targetPath = modulePathByKey[key];
    if (window.location.pathname !== targetPath) {
      window.history.pushState({ module: key }, "", targetPath);
      window.dispatchEvent(new PopStateEvent("popstate"));
    }
  };

  // Rol y permisos por clínica los decide el backend (p. ej. si el titular atiende pacientes);
  // suponerlos aquí dejaba ver módulos a los que después respondía 403.
  const refreshProfile = async () => {
    try {
      const refreshed = await authApi.me();
      sessionStore.setUser(refreshed);
      setUser(refreshed);
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  const handleClinicSaved = async (saved: ClinicResponse) => {
    setClinics((prev) => {
      const exists = prev.some((clinic) => clinic.id === saved.id);
      return exists ? prev.map((clinic) => (clinic.id === saved.id ? saved : clinic)) : [...prev, saved];
    });
    setClinicModal(null);
    setStatus(clinicModal?.mode === "create" ? "Clínica creada correctamente." : "Datos de la clínica actualizados.");
    await refreshProfile();
  };

  useEffect(() => {
    if (!activeClinicId) {
      setCatalogItemCount(undefined);
      return;
    }
    setOnboardingDismissed(isOnboardingDismissed(user.id, activeClinicId));
    treatmentCatalogApi
      .list(activeClinicId)
      .then((items) => setCatalogItemCount(items.length))
      .catch(() => setCatalogItemCount(undefined));
  }, [activeClinicId, user.id]);

  // Navegar a otro modulo sale del asistente: dejarlo montado haria que apretar "Agenda" siguiera
  // mostrando el alta guiada.
  useEffect(() => {
    setOnboardingOpen(false);
  }, [active]);

  const handleDismissOnboarding = () => {
    if (activeClinicId) {
      dismissOnboarding(user.id, activeClinicId);
    }
    setOnboardingDismissed(true);
  };

  // Ajustes edita su propia copia de la clinica; sin esto el resto de la app (vocabulario del
  // modulo de tratamientos, por ejemplo) se quedaria con los datos viejos hasta recargar.
  const handleClinicUpdated = (updated: ClinicResponse) => {
    setClinics((prev) => prev.map((clinic) => (clinic.id === updated.id ? updated : clinic)));
  };

  const completeModuleWelcome = () => {
    if (activeWelcomeStorageKey) {
      localStorage.setItem(activeWelcomeStorageKey, "seen");
    }
    setWelcomeRefresh((current) => current + 1);
  };

  const activeClinicSummary = user.clinics?.find((c) => c.id === activeClinicId);
  const userRole = activeClinicSummary?.role ?? "DOCTOR";
  const userPermissions = activeClinicSummary?.permissions;
  const allowedModules = getAllowedModulesForUser(userRole, userPermissions);
  // Sin permisos cargados (sesión vieja) se deja pasar, igual que getAllowedModulesForUser: el
  // backend responde 403 de todos modos. Un administrador que no atiende pacientes no los tiene.
  const hasPermission = (permission: string) => !userPermissions || userPermissions.includes(permission);
  const canAttendPatients = hasPermission("VIEW_PATIENT_CARE");
  const canSeeMedicalRecords = hasPermission("VIEW_MEDICAL_RECORDS");
  // El alta guiada escribe datos de la clinica, su especialidad y el catalogo. Quien no puede
  // hacer nada de eso no deberia ver el aviso: lo unico que conseguiria es una fila de 403.
  const canConfigureClinic =
    activeClinicRecord?.ownerUserId === user.id || userRole === "CLINIC_ADMIN" || userRole === "ADMIN";
  const showOnboardingBanner =
    onboardingState.pending &&
    canConfigureClinic &&
    !onboardingOpen &&
    !onboardingDismissed &&
    Boolean(activeClinicRecord);

  useEffect(() => {
    if (!allowedModules.has(active)) {
      const firstAllowed = modules.find((m) => allowedModules.has(m.key))?.key ?? "agenda";
      navigateModule(firstAllowed);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userRole, userPermissions, active]);

  const nav = (
    <Sidebar
      active={active}
      setActive={navigateModule}
      collapsed={collapsed}
      setCollapsed={(next) => {
        setCollapsed(next);
        localStorage.setItem("clinicloud.sidebar", next ? "collapsed" : "expanded");
      }}
      user={user}
      initials={initials}
      activeClinic={activeClinic}
      onLogout={logout}
      onConfigurationClick={() => navigateModule("configuracion")}
      cajaNotificationCount={pendingCajaCount}
      userRole={userRole}
      userPermissions={userPermissions}
    />
  );

  return (
    <div className={`app-shell ${collapsed ? "is-collapsed" : ""}`}>
      <div className="desktop-sidebar">{nav}</div>
      {drawerOpen && (
        <div className="sidebar-overlay" onClick={() => setDrawerOpen(false)}>
          {nav}
        </div>
      )}

      <header className="topbar">
        <button className="icon-btn mobile-only" type="button" aria-label="Abrir navegación" onClick={() => setDrawerOpen(true)}>
          <IconMenu2 size={20} />
        </button>
        <div>
          <h1>{modules.find((item) => item.key === active)?.label}</h1>
          <p>{copy.subtitle}</p>
        </div>
        <div className="topbar-actions">
          <NotificationCenter clinicId={activeClinicId} />
          <button className="icon-btn" type="button" aria-label="Cambiar tema" onClick={() => setTheme(theme === "light" ? "dark" : "light")}>
            {theme === "light" ? <IconMoon size={18} /> : <IconSun size={18} />}
          </button>
          {copy.secondary && <button className="btn secondary" type="button">{copy.secondary}</button>}
          {copy.primary && (
            <button
              className="btn primary"
              type="button"
              disabled={active === "pacientes" && !activeClinicId}
              onClick={active === "pacientes" ? () => setPatientModalOpen(true) : undefined}
            >
              {copy.primary}
            </button>
          )}
        </div>
      </header>
      <main className="content-area">
        {mobileRestricted.has(active) && (
          <div className="mobile-restriction">
            Esta función está optimizada para pantallas más grandes. Por favor, usa una tablet o computadora.
          </div>
        )}
        {showOnboardingBanner && (
          <ClinicOnboardingBanner
            state={onboardingState}
            onContinue={() => setOnboardingOpen(true)}
            onDismiss={handleDismissOnboarding}
          />
        )}
        {onboardingOpen && activeClinicRecord ? (
          <ClinicOnboardingScreen
            clinic={activeClinicRecord}
            catalogItemCount={catalogItemCount}
            onClinicUpdated={handleClinicUpdated}
            onCatalogSeeded={setCatalogItemCount}
            onClose={() => setOnboardingOpen(false)}
          />
        ) : (
        <div key={active} className="fade-in-slide">
          {active === "dashboard" ? (
            <DashboardScreen
              clinicId={activeClinicId}
              clinic={clinics[0]}
              hasClinic={Boolean(activeClinicId)}
              patients={patients}
              patientsLoading={patientsLoading}
              pendingCajaCount={pendingCajaCount}
              allowedModules={allowedModules}
              onNavigateModule={navigateModule}
              onCreatePatient={() => setPatientModalOpen(true)}
              onEditClinic={() => setClinicModal(clinics[0] ? { mode: "edit", clinic: clinics[0] } : { mode: "create" })}
              onOpenConfiguration={() => navigateModule("configuracion")}
            />
          ) : shouldShowModuleWelcome ? (
            <ModuleWelcomeScreen moduleKey={active} onStart={completeModuleWelcome} />
          ) : active === "agenda" ? (
            <AgendaScreen
              clinicId={activeClinicId}
              hasClinic={Boolean(activeClinicId)}
              patients={patients}
              onStartCare={canAttendPatients ? handleStartCare : undefined}
            />
          ) : active === "solicitudes" ? (
            <AppointmentRequestsScreen clinicId={activeClinicId} userId={user.id} />
          ) : active === "chats" ? (
            <WhatsAppChatsScreen
              clinicId={activeClinicId}
              canSeeAccessLog={hasPermission("MANAGE_CLINIC")}
              onOpenPatients={() => navigateModule("pacientes")}
              onRegisterPatient={() => {
                navigateModule("pacientes");
                setPatientModalOpen(true);
              }}
            />
          ) : active === "consultorios" ? (
            <ConsultoriosScreen clinicId={activeClinicId} hasClinic={Boolean(activeClinicId)} />
          ) : active === "atencion" && canAttendPatients ? (
            carePatient && activeClinicId ? (
              <PatientCareScreen
                clinicId={activeClinicId}
                patient={carePatient}
                appointment={careAppointment}
                onBack={() => setCarePatient(null)}
              />
            ) : (
              <div className="panel full" style={{ padding: "20px" }}>
                <h2>Atención al Paciente</h2>
                <p style={{ color: "#64748b", fontSize: "14px", marginBottom: "16px" }}>
                  Selecciona un paciente para iniciar su sesión de consulta o inicia la atención desde una cita agendada en la Agenda.
                </p>
                <div className="table-wrapper">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Paciente</th>
                        <th>CURP</th>
                        <th>Teléfono</th>
                        <th>Acción</th>
                      </tr>
                    </thead>
                    <tbody>
                      {patients.map((pat) => (
                        <tr key={pat.id} onClick={() => handleStartCare(pat)}>
                          <td>
                            <strong>{[pat.firstName, pat.lastNamePaterno, pat.lastNameMaterno].filter(Boolean).join(" ")}</strong>
                          </td>
                          <td>{pat.curp || "Sin registrar"}</td>
                          <td>{pat.phone || "Sin registrar"}</td>
                          <td>
                            <button type="button" className="button primary" style={{ fontSize: "12px", padding: "4px 10px" }}>
                              Iniciar Consulta
                            </button>
                          </td>
                        </tr>
                      ))}
                      {patients.length === 0 && (
                        <tr>
                          <td colSpan={4}>No hay pacientes registrados en esta clínica.</td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )
          ) : active === "pacientes" ? (
            <PatientsPanel
              clinicId={activeClinicId}
              patients={patients}
              loading={patientsLoading}
              hasClinic={Boolean(activeClinicId)}
              onPatientUpdated={(updated) =>
                setPatients((prev) => prev.map((patient) => (patient.id === updated.id ? updated : patient)))
              }
              onPatientDeleted={(patientId) =>
                setPatients((prev) => prev.filter((patient) => patient.id !== patientId))
              }
            />
          ) : active === "expediente" ? (
            <ExpedienteScreen
              clinicId={activeClinicId}
              clinic={clinics[0]}
              hasClinic={Boolean(activeClinicId)}
              patients={patients}
            />
          ) : active === "tratamientos" ? (
            <TratamientosScreen
              clinicId={activeClinicId}
              clinic={clinics[0]}
              hasClinic={Boolean(activeClinicId)}
              patients={patients}
              canSeeQuotations={canSeeMedicalRecords}
            />
          ) : active === "caja" ? (
            <CajaScreen
              clinicId={activeClinicId}
              hasClinic={Boolean(activeClinicId)}
              patients={patients}
            />
        ) : active === "inventario" ? (
          <InventarioScreen clinicId={activeClinicId} hasClinic={Boolean(activeClinicId)} />
        ) : active === "contabilidad" ? (
          <ContabilidadScreen clinicId={activeClinicId} hasClinic={Boolean(activeClinicId)} />
        ) : active === "personal" ? (
          <PersonalScreen
            userId={user.id}
            clinicId={activeClinicId}
            hasClinic={Boolean(activeClinicId)}
            onAccessChanged={refreshProfile}
          />
        ) : active === "configuracion" ? (
          <SettingsScreen
            clinicId={activeClinicId}
            onClinicUpdated={handleClinicUpdated}
            userId={user.id}
            user={user}
            theme={theme}
            setTheme={setTheme}
            colorPalette={colorPalette}
            setColorPalette={setColorPalette}
          />
        ) : comingSoonModules.has(active) ? (
          <section className="panel full coming-soon-panel">
            <div className="coming-soon-card">
              <div className="coming-soon-icon">
                <ActiveModuleIcon size={30} strokeWidth={1.8} aria-hidden="true" />
              </div>
              <h2>Disponible próximamente</h2>
              <p>{copy.subtitle}</p>
            </div>
          </section>
        ) : (
          <section className="dashboard-grid">
            <article className="panel">
              <div className="panel-heading">
                <h2>Perfil y sesión</h2>
                <span className="badge success">{user.active ? "Activo" : "Inactivo"}</span>
              </div>
              <div className="clinic-list">
                <div className="clinic-row">
                  <strong>{user.fullName}</strong>
                  <span>{user.email}</span>
                </div>
                <div className="clinic-row">
                  <strong>{user.phone || "Sin teléfono registrado"}</strong>
                  <span>{user.emailVerified ? "Correo verificado" : "Correo sin verificar"}</span>
                </div>
              </div>
            </article>

            <article className="panel wide">
              <div className="panel-heading">
                <h2>Clínicas</h2>
                <div className="topbar-actions">
                  <span className="badge neutral">{clinics.length}</span>
                  <button className="btn secondary" type="button" onClick={() => setClinicModal({ mode: "create" })}>
                    <IconPlus size={16} />
                    Nueva clínica
                  </button>
                </div>
              </div>
              <div className="clinic-list">
                {clinics.length === 0 && (
                  <div className="clinic-row">
                    <strong>Sin clínicas registradas</strong>
                    <span>Crea tu primera clínica para comenzar</span>
                  </div>
                )}
                {clinics.map((clinic) => {
                  const complete = Boolean(clinic.legalName && clinic.rfc && clinic.addressStreet);
                  return (
                    <div className="clinic-row clinic-row-actionable" key={clinic.id}>
                      <div>
                        <strong>{clinic.name}</strong>
                        <span>{clinic.email || "Sin correo de contacto"}</span>
                      </div>
                      <div className="clinic-row-actions">
                        <span className={`badge ${complete ? "success" : "warning"}`}>{complete ? "Completa" : "Incompleta"}</span>
                        <button className="btn ghost" type="button" onClick={() => setClinicModal({ mode: "edit", clinic })}>
                          Completar datos
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </article>

            <article className="panel">
              <div className="panel-heading">
                <h2>Sesión</h2>
                <IconShieldLock size={20} aria-hidden="true" />
              </div>
              <div className="clinic-list">
                <button className="btn destructive" type="button" onClick={logout}>
                  <IconLogout size={18} />
                  Cerrar sesión
                </button>
              </div>
            </article>
          </section>
        )}
        </div>
        )}

        {status && <p className="alert success">{status}</p>}
        {error && <p className="alert error">{error}</p>}
      </main>

      {clinicModal && (
        <ClinicModal
          mode={clinicModal.mode}
          clinic={clinicModal.clinic}
          accountHolderName={user.fullName}
          onClose={() => setClinicModal(null)}
          onSaved={handleClinicSaved}
        />
      )}

      {patientModalOpen && activeClinicId && (
        <PatientModal
          mode="create"
          clinicId={activeClinicId}
          onClose={() => setPatientModalOpen(false)}
          onSaved={(patient) => {
            setPatients((prev) => [...prev, patient]);
            setPatientModalOpen(false);
            setStatus("Paciente registrado correctamente.");
          }}
        />
      )}

      {carePatient && activeClinicId && canAttendPatients && (
        <PatientCareScreen
          clinicId={activeClinicId}
          patient={carePatient}
          appointment={careAppointment}
          onBack={() => {
            setCarePatient(null);
            setCareAppointment(undefined);
          }}
          onPatientCreated={(newPatient) => {
            setPatients((prev) => [...prev.filter((p) => p.id !== newPatient.id), newPatient]);
            setCarePatient(newPatient);
          }}
        />
      )}

    </div>
  );
}
