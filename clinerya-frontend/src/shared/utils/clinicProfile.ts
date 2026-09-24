import type { ClinicResponse, ClinicSpecialty } from "@modules/clinics/types";

/**
 * Perfil de presentación de una clínica según su especialidad.
 *
 * Sólo cambia cómo se ve el módulo de tratamientos: vocabulario, visibilidad del localizador
 * clínico y qué plantilla de historia clínica se recomienda. Ninguna estructura de datos cambia:
 * una cotización guardada en una clínica odontológica conserva su número de diente aunque la
 * clínica cambie de perfil, sólo deja de mostrarse.
 */
export interface ClinicProfile {
  specialty: ClinicSpecialty;
  /** Nombre de la especialidad para mostrar al usuario. */
  label: string;
  /**
   * Si la partida de cotización expone un localizador clínico. Hoy sólo odontología tiene uno
   * (el número de diente en notación FDI); en el resto de especialidades la zona anatómica va en
   * la descripción de la partida.
   */
  showsClinicalLocator: boolean;
  /** Etiqueta del campo localizador en el editor. */
  locatorFieldLabel: string;
  /** Encabezado de la columna localizadora en tablas y PDF. */
  locatorColumnLabel: string;
  /** Prefijo al citar el localizador junto a una partida, p. ej. "Pieza 21". */
  locatorItemPrefix: string;
  /** Cómo se llama el importe de trabajo profesional de una partida. */
  laborLabel: string;
  /** Lo mismo, abreviado, para columnas estrechas como las del PDF. */
  laborShortLabel: string;
  /** Lo mismo, en el catálogo de servicios, donde es un precio base. */
  laborPriceLabel: string;
  /** Plantilla de historia clínica que se recomienda al crear las primeras. */
  recommendedHistoryTemplate: "NOM_013" | "NOM_004";
  /**
   * Si existe un catálogo de servicios sugerido para esta especialidad. Cuántos servicios trae lo
   * dice el backend al sembrar: repetir el número aquí sería una segunda fuente que se desfasa.
   */
  hasSuggestedCatalog: boolean;
}

const GENERIC_PROFILE: Omit<ClinicProfile, "specialty" | "label"> = {
  showsClinicalLocator: false,
  locatorFieldLabel: "",
  locatorColumnLabel: "",
  locatorItemPrefix: "",
  laborLabel: "Honorarios",
  laborShortLabel: "Honorarios",
  laborPriceLabel: "Precio de honorarios",
  recommendedHistoryTemplate: "NOM_004",
  hasSuggestedCatalog: true
};

const DENTAL_PROFILE: Omit<ClinicProfile, "specialty" | "label"> = {
  showsClinicalLocator: true,
  locatorFieldLabel: "Diente (opcional, FDI)",
  locatorColumnLabel: "Diente",
  locatorItemPrefix: "Pieza",
  laborLabel: "Mano de obra",
  laborShortLabel: "M. Obra",
  laborPriceLabel: "Precio de mano de obra",
  recommendedHistoryTemplate: "NOM_013",
  hasSuggestedCatalog: true
};

const PROFILES: Record<ClinicSpecialty, ClinicProfile> = {
  SIN_CONFIGURAR: {
    specialty: "SIN_CONFIGURAR",
    label: "Sin configurar",
    ...GENERIC_PROFILE,
    hasSuggestedCatalog: false
  },
  ODONTOLOGIA: { specialty: "ODONTOLOGIA", label: "Odontología", ...DENTAL_PROFILE },
  MEDICINA_GENERAL: { specialty: "MEDICINA_GENERAL", label: "Medicina general", ...GENERIC_PROFILE },
  NUTRICION: { specialty: "NUTRICION", label: "Nutrición", ...GENERIC_PROFILE },
  FISIOTERAPIA: { specialty: "FISIOTERAPIA", label: "Fisioterapia", ...GENERIC_PROFILE },
  OFTALMOLOGIA: { specialty: "OFTALMOLOGIA", label: "Oftalmología", ...GENERIC_PROFILE },
  OTRA: { specialty: "OTRA", label: "Otra especialidad", ...GENERIC_PROFILE, hasSuggestedCatalog: false }
};

/**
 * Perfil por omisión cuando todavía no sabemos la especialidad: la clínica no terminó de cargar,
 * o el backend es anterior a la migración V54. Se resuelve como odontología a propósito, para
 * que la interfaz no parpadee quitando el localizador a las clínicas dentales, que hoy son
 * todas las que existen. Una clínica que sí declaró `SIN_CONFIGURAR` es otra cosa: ya sabemos
 * que no eligió perfil, y se comporta como genérica hasta que elija.
 */
export const DEFAULT_CLINIC_PROFILE = PROFILES.ODONTOLOGIA;

/**
 * Especialidades que una clínica puede elegir. `SIN_CONFIGURAR` queda fuera a propósito: es un
 * estado inicial, no una opción.
 */
export const SELECTABLE_CLINIC_SPECIALTIES: ClinicSpecialty[] = [
  "ODONTOLOGIA",
  "MEDICINA_GENERAL",
  "NUTRICION",
  "FISIOTERAPIA",
  "OFTALMOLOGIA",
  "OTRA"
];

export function getClinicProfile(specialty?: ClinicSpecialty | null): ClinicProfile {
  if (!specialty) return DEFAULT_CLINIC_PROFILE;
  return PROFILES[specialty] ?? DEFAULT_CLINIC_PROFILE;
}

export function getClinicProfileFor(clinic?: ClinicResponse | null): ClinicProfile {
  return getClinicProfile(clinic?.specialty);
}
