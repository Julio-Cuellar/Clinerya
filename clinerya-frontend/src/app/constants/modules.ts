import {
  IconBuildingHospital,
  IconCalendar,
  IconCash,
  IconClipboardText,
  IconDental,
  IconAdjustments,
  IconFileInvoice,
  IconHeartHandshake,
  IconLayoutDashboard,
  IconPackage,
  IconStethoscope,
  IconUsers
} from "@tabler/icons-react";

export type ModuleKey =
  | "dashboard"
  | "agenda"
  | "consultorios"
  | "atencion"
  | "pacientes"
  | "expediente"
  | "tratamientos"
  | "caja"
  | "inventario"
  | "contabilidad"
  | "personal"
  | "configuracion";

export const modules: Array<{ key: ModuleKey; label: string; section: string; icon: typeof IconCalendar }> = [
  { key: "dashboard", label: "Dashboard", section: "Inicio", icon: IconLayoutDashboard },
  { key: "agenda", label: "Agenda", section: "Clinica", icon: IconCalendar },
  { key: "consultorios", label: "Consultorios", section: "Clinica", icon: IconBuildingHospital },
  { key: "atencion", label: "Atencion al Paciente", section: "Clinica", icon: IconHeartHandshake },
  { key: "pacientes", label: "Pacientes", section: "Clinica", icon: IconUsers },
  { key: "expediente", label: "Expediente", section: "Clinica", icon: IconClipboardText },
  { key: "tratamientos", label: "Tratamientos y Cotizaciones", section: "Clinica", icon: IconDental },
  { key: "caja", label: "Caja", section: "Administracion", icon: IconCash },
  { key: "inventario", label: "Inventario", section: "Administracion", icon: IconPackage },
  { key: "contabilidad", label: "Contabilidad", section: "Administracion", icon: IconFileInvoice },
  { key: "personal", label: "Personal", section: "Administracion", icon: IconStethoscope },
  { key: "configuracion", label: "Configuracion", section: "Administracion", icon: IconAdjustments }
];

export const moduleCopy: Record<ModuleKey, { subtitle: string; primary?: string; secondary?: string }> = {
  dashboard: { subtitle: "Resumen general de la clinica y sus modulos" },
  agenda: { subtitle: "Citas y horarios de la clinica" },
  consultorios: { subtitle: "Espacios de atencion, estado y disponibilidad de la clinica" },
  atencion: { subtitle: "Consulta medica activa, historia clinica, notas SOAP, recetas y comparativo de insumos" },
  pacientes: { subtitle: "Catalogo activo de pacientes", primary: "+ Nuevo paciente", secondary: "Exportar listado" },
  expediente: { subtitle: "Expedientes por paciente: historia, tratamientos, citas, pagos y estudios" },
  tratamientos: { subtitle: "Catalogo de servicios y cotizaciones para pacientes" },
  caja: { subtitle: "Turnos de caja, cobros y tickets de venta" },
  inventario: { subtitle: "Materiales, existencias y kardex de la clinica" },
  contabilidad: {
    subtitle: "Saldos iniciales, libro diario, cuentas bancarias, balanza de comprobacion y estado de resultados."
  },
  personal: { subtitle: "Accesos externos: comparte expedientes con especialistas de otras clinicas" },
  configuracion: { subtitle: "Perfil, clinica activa, integraciones y preferencias del sistema" }
};


export const mobileRestricted = new Set<ModuleKey>(["agenda", "atencion", "expediente", "tratamientos", "caja", "contabilidad"]);
