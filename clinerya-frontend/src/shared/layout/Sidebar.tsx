import { useEffect, useRef, useState } from "react";
import { IconAdjustments, IconChevronLeft, IconLogout } from "@tabler/icons-react";
import { modules, type ModuleKey } from "@app/constants/modules";
import type { UserProfile } from "@modules/auth/types";

export const allowedModulesByRole: Record<string, Set<ModuleKey>> = {
  ADMIN: new Set(["dashboard", "agenda", "pacientes", "expediente", "tratamientos", "caja", "inventario", "contabilidad", "personal"]),
  CLINIC_ADMIN: new Set(["dashboard", "agenda", "pacientes", "expediente", "tratamientos", "caja", "inventario", "contabilidad", "personal"]),
  DOCTOR: new Set(["dashboard", "agenda", "pacientes", "expediente", "tratamientos", "caja", "inventario", "contabilidad", "personal"]),
  RECEPTIONIST: new Set(["dashboard", "agenda", "pacientes", "caja"]),
  ASSISTANT: new Set(["dashboard", "agenda", "pacientes", "expediente", "inventario"]),
  ACCOUNTANT: new Set(["dashboard", "caja", "contabilidad"]),
  CLEANING: new Set(["dashboard", "agenda"])
};

export const modulePermissionByKey: Partial<Record<ModuleKey, string | string[]>> = {
  dashboard: ["VIEW_DASHBOARD", "VIEW_DASHBOARD_METRICS"],
  agenda: ["VIEW_AGENDA", "MANAGE_AGENDA"],
  pacientes: ["VIEW_PATIENTS", "MANAGE_PATIENTS"],
  expediente: ["VIEW_MEDICAL_RECORDS"],
  tratamientos: ["VIEW_TREATMENTS", "MANAGE_TREATMENTS"],
  caja: ["VIEW_CASH", "MANAGE_CASH"],
  inventario: ["VIEW_INVENTORY", "MANAGE_INVENTORY"],
  contabilidad: ["VIEW_ACCOUNTING"],
  personal: ["VIEW_STAFF", "MANAGE_STAFF"],
  configuracion: ["VIEW_CLINIC_SETTINGS", "MANAGE_CLINIC"],
  consultorios: ["VIEW_ROOMS", "MANAGE_ROOMS"],
  atencion: ["VIEW_PATIENT_CARE", "MANAGE_PATIENT_CARE", "VIEW_MEDICAL_RECORDS"]
};

export function getAllowedModulesForUser(userRole: string, permissions?: string[]) {
  const roleModules = allowedModulesByRole[userRole] ?? new Set<ModuleKey>();
  if (!permissions) return roleModules;
  return new Set([...roleModules].filter((moduleKey) => {
    const requiredPermission = modulePermissionByKey[moduleKey];
    if (!requiredPermission) return true;
    const requiredPermissions = Array.isArray(requiredPermission) ? requiredPermission : [requiredPermission];
    return requiredPermissions.some((permission) => permissions.includes(permission));
  }));
}

Object.values(allowedModulesByRole).forEach((allowed) => {
  allowed.add("configuracion");
  allowed.add("consultorios");
});

const sidebarSections = [
  { key: "Inicio", label: "Inicio" },
  { key: "Clinica", label: "Clínica" },
  { key: "Administracion", label: "Administración" }
];

const APP_VERSION = "0.10.0-beta.1";

export function Sidebar({
  active,
  setActive,
  collapsed,
  setCollapsed,
  user,
  initials,
  activeClinic,
  onLogout,
  onConfigurationClick,
  cajaNotificationCount = 0,
  userRole = "DOCTOR",
  userPermissions
}: {
  active: ModuleKey;
  setActive: (key: ModuleKey) => void;
  collapsed: boolean;
  setCollapsed: (next: boolean) => void;
  user: UserProfile;
  initials: string;
  activeClinic?: string;
  onLogout: () => void | Promise<void>;
  onConfigurationClick?: () => void;
  cajaNotificationCount?: number;
  userRole?: string;
  userPermissions?: string[];
}) {
  const [profileOpen, setProfileOpen] = useState(false);
  const sidebarBottomRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const handlePointerDown = (event: MouseEvent | TouchEvent) => {
      if (!profileOpen) return;
      if (sidebarBottomRef.current?.contains(event.target as Node)) return;
      setProfileOpen(false);
    };

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setProfileOpen(false);
      }
    };

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("touchstart", handlePointerDown);
    document.addEventListener("keydown", handleEscape);

    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("touchstart", handlePointerDown);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [profileOpen]);

  const handleConfigurationClick = () => {
    onConfigurationClick?.();
    setProfileOpen(false);
  };

  const handleLogout = async () => {
    setProfileOpen(false);
    await onLogout();
  };

  const allowedModules = getAllowedModulesForUser(userRole, userPermissions);

  return (
    <aside className="sidebar">
      <div className="brand-row sidebar-brand">
        <span className="brand-dot" />
        <span className="brand-word">Clinerya</span>
      </div>
      {activeClinic && <button className="clinic-selector" type="button">{activeClinic}</button>}
      {sidebarSections.map((section) => (
        <nav key={section.key} aria-label={section.label}>
          <p className="nav-section">{section.label}</p>
          {modules.filter((item) => item.section === section.key && allowedModules.has(item.key)).map((item) => {
            const Icon = item.icon;
            return (
              <button
                className={`nav-item ${active === item.key ? "active" : ""}`}
                key={item.key}
                title={collapsed ? item.label : undefined}
                type="button"
                onClick={() => setActive(item.key)}
              >
                <Icon size={18} aria-hidden="true" />
                <span className="nav-item-label">{item.label}</span>
                {item.key === "caja" && cajaNotificationCount > 0 && (
                  <span className="sidebar-badge">{cajaNotificationCount}</span>
                )}
              </button>
            );
          })}
        </nav>
      ))}
      <div className="sidebar-bottom" ref={sidebarBottomRef}>
        {profileOpen && (
          <div className="profile-menu" role="menu" aria-label="Opciones de usuario">
            <button className="profile-menu-item" type="button" onClick={handleConfigurationClick}>
              <IconAdjustments size={16} aria-hidden="true" />
              <span>Configuración</span>
            </button>
            <button className="profile-menu-item danger" type="button" onClick={() => void handleLogout()}>
              <IconLogout size={16} aria-hidden="true" />
              <span>Cerrar sesión</span>
            </button>
          </div>
        )}
        <button
          className="user-chip user-chip-trigger"
          title={user.fullName}
          type="button"
          onClick={() => setProfileOpen((current) => !current)}
        >
          <span>{initials}</span>
          <small className="user-chip-label">{user.fullName}</small>
        </button>
        <div className={`sidebar-footer ${collapsed ? "collapsed" : ""}`}>
          <div className="sidebar-release">
            <span className="sidebar-version">v{APP_VERSION}</span>
            <span className="sidebar-update-marker">Infra update test</span>
          </div>
          <button
            className="collapse-btn"
            aria-label={collapsed ? "Expandir sidebar" : "Colapsar sidebar"}
            aria-expanded={!collapsed}
            type="button"
            onClick={() => setCollapsed(!collapsed)}
          >
            <IconChevronLeft className="collapse-icon" size={18} aria-hidden="true" />
          </button>
        </div>
      </div>
    </aside>
  );
}
