import { useEffect, useState } from "react";
import {
  IconBrandGoogle,
  IconBuildingHospital,
  IconLock,
  IconPalette,
  IconUser,
  IconX,
  IconSun,
  IconMoon,
  IconDatabase
} from "@tabler/icons-react";
import { getFriendlyError, integrationsApi, staffApi, authApi, systemConfigsApi } from "@shared/api/api";
import { ClinicConfigurationPanel } from "@modules/clinics/components/ClinicConfigurationPanel";
import type { UserProfile } from "@modules/auth/types";
import type { Theme } from "@modules/settings/themeTypes";

type TabId = "profile" | "clinic" | "security" | "appearance" | "system";

interface PaletteOption {
  id: string;
  name: string;
  color: string;
}

const PALETTES: PaletteOption[] = [
  { id: "default", name: "Menta Clásica", color: "#376D6D" },
  { id: "blue", name: "Azul Real", color: "#2563EB" },
  { id: "ocean", name: "Azul Océano", color: "#1CA1E3" },
  { id: "turquoise", name: "Turquesa", color: "#0D9488" },
  { id: "green", name: "Verde Bosque", color: "#16A34A" },
  { id: "orange", name: "Naranja Atardecer", color: "#EA580C" },
  { id: "purple", name: "Púrpura Orquídea", color: "#7C3AED" },
  { id: "rose", name: "Rosa Carmesí", color: "#E11D48" }
];

export function SettingsScreen({
  clinicId,
  userId,
  user,
  theme,
  setTheme,
  colorPalette,
  setColorPalette
}: {
  clinicId?: string;
  userId: string;
  user: UserProfile;
  theme: Theme;
  setTheme: (theme: Theme) => void;
  colorPalette: string;
  setColorPalette: (color: string) => void;
}) {
  const [activeTab, setActiveTab] = useState<TabId>("profile");

  // Google Calendar Integration states
  const [calendarLoading, setCalendarLoading] = useState(true);
  const [calendarConnecting, setCalendarConnecting] = useState(false);
  const [calendarError, setCalendarError] = useState("");
  const [staffId, setStaffId] = useState<string | null>(null);
  const [connected, setConnected] = useState(false);
  const [email, setEmail] = useState<string | undefined>(undefined);
  const [importPastEvents, setImportPastEvents] = useState(false);
  const [showConfirmSyncModal, setShowConfirmSyncModal] = useState(false);

  // Security (Change Password) states
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [securityLoading, setSecurityLoading] = useState(false);
  const [securitySuccess, setSecuritySuccess] = useState("");
  const [securityError, setSecurityError] = useState("");

  // Appearance states
  const [themeSaving, setThemeSaving] = useState(false);

  // System Config states
  const [retentionDays, setRetentionDays] = useState("365");
  const [systemLoading, setSystemLoading] = useState(false);
  const [systemSuccess, setSystemSuccess] = useState("");
  const [systemError, setSystemError] = useState("");
  const [systemDataLoading, setSystemDataLoading] = useState(false);

  // Load Google Calendar info (Clinic & Integrations)
  useEffect(() => {
    if (!clinicId) {
      setCalendarLoading(false);
      return;
    }
    setCalendarLoading(true);
    setCalendarError("");
    staffApi
      .list(clinicId)
      .then((staffList) => {
        const own = staffList.find((s) => s.userId === userId);
        if (!own) {
          setCalendarError("No se encontró tu perfil de personal en esta clínica.");
          return;
        }
        setStaffId(own.staffId);
        return integrationsApi.getGoogleCalendarStatus(clinicId, own.staffId);
      })
      .then((status) => {
        if (status) {
          setConnected(status.connected);
          setEmail(status.email);
          setImportPastEvents(status.importPastEvents);
        }
      })
      .catch((caught) => setCalendarError(getFriendlyError(caught)))
      .finally(() => setCalendarLoading(false));
  }, [clinicId, userId]);

  const handleConnect = () => {
    setShowConfirmSyncModal(true);
  };

  const handleConnectFlow = async (importPast: boolean) => {
    if (!clinicId || !staffId) return;
    setCalendarConnecting(true);
    setCalendarError("");
    setShowConfirmSyncModal(false);
    try {
      const { authorizationUrl } = await integrationsApi.getGoogleCalendarAuthorizationUrl(clinicId, staffId, importPast);
      window.location.href = authorizationUrl;
    } catch (caught) {
      setCalendarError(getFriendlyError(caught));
      setCalendarConnecting(false);
    }
  };

  const handleDisconnect = async () => {
    if (!clinicId || !staffId) return;
    setCalendarConnecting(true);
    setCalendarError("");
    try {
      await integrationsApi.disconnectGoogleCalendar(clinicId, staffId);
      setConnected(false);
      setEmail(undefined);
    } catch (caught) {
      setCalendarError(getFriendlyError(caught));
    } finally {
      setCalendarConnecting(false);
    }
  };

  const handleTogglePastEvents = async (checked: boolean) => {
    if (!clinicId || !staffId) return;
    setCalendarConnecting(true);
    setCalendarError("");
    try {
      await integrationsApi.updateGoogleCalendarPreferences(clinicId, staffId, { importPastEvents: checked });
      setImportPastEvents(checked);
    } catch (caught) {
      setCalendarError(getFriendlyError(caught));
    } finally {
      setCalendarConnecting(false);
    }
  };

  // Load System Config on tab change
  useEffect(() => {
    if (activeTab === "system") {
      setSystemDataLoading(true);
      setSystemError("");
      systemConfigsApi
        .getConfig("backup_retention_days")
        .then((res) => {
          if (res) {
            setRetentionDays(res.value);
          }
        })
        .catch((err) => setSystemError(getFriendlyError(err)))
        .finally(() => setSystemDataLoading(false));
    }
  }, [activeTab]);

  // Save System Config Action
  const handleSaveSystemConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    setSystemError("");
    setSystemSuccess("");
    setSystemLoading(true);
    try {
      await systemConfigsApi.updateConfig("backup_retention_days", retentionDays, "Días de retención para backups locales");
      setSystemSuccess("Configuración de respaldos guardada exitosamente.");
    } catch (err) {
      setSystemError(getFriendlyError(err));
    } finally {
      setSystemLoading(false);
    }
  };

  // Generate Backup Action
  const handleGenerateBackup = async () => {
    setSystemError("");
    setSystemSuccess("");
    setSystemLoading(true);
    try {
      const blob = await systemConfigsApi.downloadBackup();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      const dateStr = new Date().toISOString().slice(0, 19).replace(/T|:/g, "_");
      a.download = `backup_manual_${dateStr}.sql.gz`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
      setSystemSuccess("Respaldo generado y descargado exitosamente.");
    } catch (err) {
      setSystemError(getFriendlyError(err));
    } finally {
      setSystemLoading(false);
    }
  };

  // Change Password Action
  const handleChangePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSecurityError("");
    setSecuritySuccess("");

    if (!currentPassword || !newPassword || !confirmPassword) {
      setSecurityError("Todos los campos son obligatorios.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setSecurityError("Las contraseñas nuevas no coinciden.");
      return;
    }

    if (newPassword.length < 8) {
      setSecurityError("La nueva contraseña debe tener al menos 8 caracteres.");
      return;
    }

    setSecurityLoading(true);
    try {
      await authApi.changePassword({ currentPassword, newPassword });
      setSecuritySuccess("Contraseña cambiada exitosamente.");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (caught) {
      setSecurityError(getFriendlyError(caught));
    } finally {
      setSecurityLoading(false);
    }
  };

  // Change theme and persist in DB
  const handleThemeChange = async (newTheme: Theme) => {
    setTheme(newTheme);
    setThemeSaving(true);
    try {
      const dbTheme = newTheme === "light" ? "LIGHT" : "DARK";
      await authApi.updateTheme(dbTheme);
    } catch (err) {
      console.error("No se pudo guardar la preferencia de tema en la base de datos", err);
    } finally {
      setThemeSaving(false);
    }
  };

  return (
    <section className="settings-page">
      <div className="settings-page-header">
        <div className="panel-heading">
          <h2>Configuración General</h2>
        </div>
        <p>Administra tu perfil, la clínica activa, integraciones y preferencias del sistema.</p>
      </div>

      <div className="settings-layout">
          <aside className="settings-sidebar">
            <button
              className={`settings-tab-btn ${activeTab === "profile" ? "active" : ""}`}
              type="button"
              onClick={() => setActiveTab("profile")}
            >
              <IconUser size={18} aria-hidden="true" />
              Perfil
            </button>
            <button
              className={`settings-tab-btn ${activeTab === "clinic" ? "active" : ""}`}
              type="button"
              onClick={() => setActiveTab("clinic")}
            >
              <IconBuildingHospital size={18} aria-hidden="true" />
              Clínica e Integraciones
            </button>
            <button
              className={`settings-tab-btn ${activeTab === "security" ? "active" : ""}`}
              type="button"
              onClick={() => setActiveTab("security")}
            >
              <IconLock size={18} aria-hidden="true" />
              Seguridad
            </button>
            <button
              className={`settings-tab-btn ${activeTab === "appearance" ? "active" : ""}`}
              type="button"
              onClick={() => setActiveTab("appearance")}
            >
              <IconPalette size={18} aria-hidden="true" />
              Apariencia
            </button>
            <button
              className={`settings-tab-btn ${activeTab === "system" ? "active" : ""}`}
              type="button"
              onClick={() => setActiveTab("system")}
            >
              <IconDatabase size={18} aria-hidden="true" />
              Respaldos del Sistema
            </button>
          </aside>

          <main className="settings-content">
            {activeTab === "profile" && (
              <div className="settings-section">
                <div>
                  <h3>Información del Usuario</h3>
                  <p className="description" style={{ fontSize: "12px", color: "var(--color-text-3)", marginTop: "4px" }}>
                    Datos administrativos de tu cuenta de acceso.
                  </p>
                </div>

                <div className="settings-info-grid">
                  <div className="settings-info-item">
                    <label>Nombre Completo</label>
                    <span>{user.fullName}</span>
                  </div>
                  <div className="settings-info-item">
                    <label>Correo Electrónico</label>
                    <span>{user.email}</span>
                  </div>
                  <div className="settings-info-item">
                    <label>Teléfono</label>
                    <span>{user.phone || "No registrado"}</span>
                  </div>
                  <div className="settings-info-item">
                    <label>Estado de Cuenta</label>
                    <span style={{ color: "var(--color-success)" }}>{user.active ? "Activo" : "Inactivo"}</span>
                  </div>
                </div>
              </div>
            )}

            {activeTab === "clinic" && (
              <div className="settings-section">
                <div>
                  <h3>Clínica e Integraciones</h3>
                  <p className="description" style={{ fontSize: "12px", color: "var(--color-text-3)", marginTop: "4px" }}>
                    Configuración de clínicas vinculadas y conexiones con plataformas externas.
                  </p>
                </div>

                <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                  <div className="settings-info-item" style={{ background: "var(--color-surface)" }}>
                    <label>Clínica Activa</label>
                    <span>{user.clinics?.[0]?.name ?? "Sin clínica activa"}</span>
                  </div>

                  <ClinicConfigurationPanel clinicId={clinicId} />

                  <div className="panel" style={{ padding: "16px", background: "var(--color-card)" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <div>
                        <strong style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px" }}>
                          <IconBrandGoogle size={18} style={{ color: "#4285F4" }} />
                          Google Calendar
                        </strong>
                        <p style={{ fontSize: "12px", color: "var(--color-text-2)", marginTop: "4px" }}>
                          {calendarLoading
                            ? "Cargando..."
                            : connected
                              ? `Sincronizado con la cuenta: ${email}`
                              : "Vincula tu agenda médica de la clínica con tu cuenta de Google Calendar."}
                        </p>
                      </div>
                      <div>
                        {!calendarLoading && !calendarError && (
                          connected ? (
                            <button
                              className="btn destructive"
                              type="button"
                              disabled={calendarConnecting}
                              onClick={handleDisconnect}
                            >
                              Desconectar
                            </button>
                          ) : (
                            <button
                              className="btn primary"
                              type="button"
                              disabled={calendarConnecting || !staffId}
                              onClick={handleConnect}
                            >
                              Conectar
                            </button>
                          )
                        )}
                      </div>
                    </div>

                    {!calendarLoading && connected && (
                      <div style={{ marginTop: "16px", paddingTop: "12px", borderTop: "1px solid var(--color-border)" }}>
                        <label style={{ display: "flex", alignItems: "center", gap: "8px", cursor: "pointer", fontSize: "13px" }}>
                          <input
                            type="checkbox"
                            checked={importPastEvents}
                            onChange={(e) => handleTogglePastEvents(e.target.checked)}
                            disabled={calendarConnecting}
                          />
                          <span>Importar citas históricas/pasadas desde Google Calendar</span>
                        </label>
                      </div>
                    )}

                    {calendarError && <p className="alert error" style={{ marginTop: "12px" }}>{calendarError}</p>}
                  </div>
                </div>
              </div>
            )}

            {activeTab === "security" && (
              <div className="settings-section">
                <div>
                  <h3>Seguridad y Contraseña</h3>
                  <p className="description" style={{ fontSize: "12px", color: "var(--color-text-3)", marginTop: "4px" }}>
                    Actualiza tu contraseña periódicamente para mantener tu cuenta segura.
                  </p>
                </div>

                <form onSubmit={handleChangePasswordSubmit} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                  <div className="field" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                    <label style={{ fontSize: "12px", fontWeight: "600" }}>Contraseña Actual</label>
                    <input
                      type="password"
                      value={currentPassword}
                      onChange={(e) => setCurrentPassword(e.target.value)}
                      required
                    />
                  </div>

                  <div className="field" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                    <label style={{ fontSize: "12px", fontWeight: "600" }}>Nueva Contraseña</label>
                    <input
                      type="password"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      required
                    />
                    <small style={{ color: "var(--color-text-3)", fontSize: "11px" }}>Mínimo 8 caracteres.</small>
                  </div>

                  <div className="field" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                    <label style={{ fontSize: "12px", fontWeight: "600" }}>Confirmar Nueva Contraseña</label>
                    <input
                      type="password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      required
                    />
                  </div>

                  {securitySuccess && <p className="alert success">{securitySuccess}</p>}
                  {securityError && <p className="alert error">{securityError}</p>}

                  <button
                    className="btn primary"
                    type="submit"
                    style={{ alignSelf: "flex-start", marginTop: "8px" }}
                    disabled={securityLoading}
                  >
                    {securityLoading ? "Actualizando..." : "Actualizar Contraseña"}
                  </button>
                </form>
              </div>
            )}

            {activeTab === "appearance" && (
              <div className="settings-section">
                <div>
                  <h3>Tema y Apariencia</h3>
                  <p className="description" style={{ fontSize: "12px", color: "var(--color-text-3)", marginTop: "4px" }}>
                    Personaliza los colores y el modo visual de tu aplicación de forma interactiva.
                  </p>
                </div>

                <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
                  <div>
                    <h4 style={{ fontSize: "13px", fontWeight: "600", marginBottom: "12px" }}>Tema Base</h4>
                    <div className="theme-select-row">
                      <button
                        type="button"
                        className={`theme-card ${theme === "light" ? "active" : ""}`}
                        onClick={() => handleThemeChange("light")}
                        disabled={themeSaving}
                      >
                        <IconSun size={18} style={{ color: "#D97706" }} />
                        <span>Claro</span>
                      </button>
                      <button
                        type="button"
                        className={`theme-card ${theme === "dark" ? "active" : ""}`}
                        onClick={() => handleThemeChange("dark")}
                        disabled={themeSaving}
                      >
                        <IconMoon size={18} style={{ color: "#3B82F6" }} />
                        <span>Oscuro</span>
                      </button>
                    </div>
                  </div>

                  <div>
                    <h4 style={{ fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>Paleta de Colores</h4>
                    <p style={{ fontSize: "11px", color: "var(--color-text-3)", marginBottom: "12px" }}>
                      Modifica el color de marca de los botones, enlaces e indicadores.
                    </p>
                    <div className="palette-grid">
                      {PALETTES.map((p) => (
                        <button
                          key={p.id}
                          type="button"
                          className={`palette-btn ${colorPalette === p.id ? "active" : ""}`}
                          onClick={() => setColorPalette(p.id)}
                        >
                          <span className="palette-dot" style={{ backgroundColor: p.color }} />
                          <span className="palette-label">{p.name}</span>
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            )}

            {activeTab === "system" && (
              <div className="settings-section">
                <div>
                  <h3>Respaldos del Sistema</h3>
                  <p className="description" style={{ fontSize: "12px", color: "var(--color-text-3)", marginTop: "4px" }}>
                     Configura el periodo de retención para los respaldos locales automáticos de la base de datos.
                  </p>
                </div>

                {systemDataLoading ? (
                  <p style={{ fontSize: "13px", color: "var(--color-text-3)" }}>Cargando configuración...</p>
                ) : (
                  <form onSubmit={handleSaveSystemConfig} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                    {systemSuccess && <div className="alert success">{systemSuccess}</div>}
                    {systemError && <div className="alert error">{systemError}</div>}

                    <label className="field">
                      <span>Días de retención de respaldos</span>
                      <select
                        value={retentionDays}
                        onChange={(e) => setRetentionDays(e.target.value)}
                        style={{
                          width: "100%",
                          padding: "8px",
                          borderRadius: "4px",
                          border: "1px solid var(--color-border)",
                          background: "var(--color-card)",
                          color: "var(--color-text-1)"
                        }}
                      >
                        <option value="30">30 días (1 mes)</option>
                        <option value="90">90 días (3 meses)</option>
                        <option value="180">180 días (6 meses)</option>
                        <option value="365">365 días (1 año)</option>
                        <option value="730">730 días (2 años)</option>
                        <option value="99999">Mantener para siempre</option>
                      </select>
                    </label>

                    <button
                      className="btn primary"
                      type="submit"
                      style={{ alignSelf: "flex-start", marginTop: "8px" }}
                      disabled={systemLoading}
                    >
                      {systemLoading ? "Guardando..." : "Guardar Configuración"}
                    </button>
                  </form>
                )}

                {!systemDataLoading && (
                  <div style={{ borderTop: "1px solid var(--color-border)", paddingTop: "16px", marginTop: "24px" }}>
                    <h4 style={{ fontSize: "13px", fontWeight: "600", marginBottom: "4px", color: "var(--color-text-1)" }}>Generar Respaldo Manual</h4>
                    <p style={{ fontSize: "11px", color: "var(--color-text-3)", marginBottom: "12px" }}>
                      Descarga una copia de seguridad comprimida completa de la base de datos directamente en tu ordenador.
                    </p>
                    <button
                      type="button"
                      className="btn secondary"
                      disabled={systemLoading}
                      onClick={handleGenerateBackup}
                    >
                      {systemLoading ? "Procesando..." : "Generar y Descargar Respaldo Ahora"}
                    </button>
                  </div>
                )}
              </div>
            )}
          </main>
        </div>

      {showConfirmSyncModal && (
        <div className="modal-overlay" onClick={() => setShowConfirmSyncModal(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: "450px" }}>
            <div className="panel-heading">
              <h3>Sincronización inicial</h3>
              <button className="icon-btn" type="button" aria-label="Cerrar" onClick={() => setShowConfirmSyncModal(false)}>
                <IconX size={18} />
              </button>
            </div>
            
            <div style={{ padding: "16px 0", display: "flex", flexDirection: "column", gap: "12px" }}>
              <p style={{ fontSize: "14px", color: "var(--color-text-2)" }}>
                Elige qué citas deseas importar desde tu cuenta de Google Calendar:
              </p>
              
              <button
                className="btn secondary"
                type="button"
                style={{ justifyContent: "flex-start", padding: "12px", height: "auto", textAlign: "left" }}
                onClick={() => handleConnectFlow(false)}
              >
                <div>
                  <strong style={{ display: "block", color: "var(--color-text-1)" }}>Solo citas próximas (Recomendado)</strong>
                  <small style={{ color: "var(--color-text-3)", fontSize: "11px" }}>Importa de hoy en adelante para mantener tu agenda limpia.</small>
                </div>
              </button>

              <button
                className="btn secondary"
                type="button"
                style={{ justifyContent: "flex-start", padding: "12px", height: "auto", textAlign: "left" }}
                onClick={() => handleConnectFlow(true)}
              >
                <div>
                  <strong style={{ display: "block", color: "var(--color-text-1)" }}>Todas mis citas (Historial completo)</strong>
                  <small style={{ color: "var(--color-text-3)", fontSize: "11px" }}>Importa citas pasadas e históricas, además de las futuras.</small>
                </div>
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
