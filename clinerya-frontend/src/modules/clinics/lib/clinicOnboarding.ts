import type { ClinicResponse } from "@modules/clinics/types";
import { getClinicProfileFor } from "@shared/utils/clinicProfile";

export type OnboardingStepId = 1 | 2 | 3 | 4;

export interface ClinicOnboardingState {
  /** Queda algo por configurar que cambia cómo se comporta la aplicación. */
  pending: boolean;
  /** Primer paso sin resolver; es donde debe retomar el asistente. */
  resumeStep: OnboardingStepId;
  specialtyDone: boolean;
  catalogDone: boolean;
}

/**
 * El avance del alta **se deriva del estado real de la clínica**, no de una bandera guardada.
 *
 * Eso tiene dos consecuencias buenas: no hace falta columna nueva en base de datos, y una clínica
 * que configuró las cosas por su cuenta —eligió especialidad en Ajustes, armó el catálogo a mano—
 * no vuelve a ver el asistente pidiéndole lo que ya hizo.
 *
 * Sólo los pasos 2 y 3 cuentan como pendientes. Los datos fiscales (paso 1) y el equipo (paso 4)
 * no cambian el comportamiento del sistema: son deseables, no requisitos, y marcarlos como
 * pendientes dejaría el aviso encendido para siempre en una clínica de un solo médico.
 */
export function getClinicOnboardingState(
  clinic: ClinicResponse | undefined,
  catalogItemCount: number | undefined
): ClinicOnboardingState {
  const specialtyDone = Boolean(clinic?.specialty && clinic.specialty !== "SIN_CONFIGURAR");

  // Una especialidad sin catálogo sugerido no tiene nada que sembrar, así que el paso ya está
  // resuelto por definición. Mientras no sepamos cuántos servicios hay, se asume resuelto para no
  // encender el aviso durante la carga.
  const profile = getClinicProfileFor(clinic);
  const catalogDone =
    !specialtyDone || !profile.hasSuggestedCatalog || catalogItemCount === undefined
      ? true
      : catalogItemCount > 0;

  const resumeStep: OnboardingStepId = !specialtyDone ? 2 : !catalogDone ? 3 : 4;

  return {
    pending: !specialtyDone || !catalogDone,
    resumeStep,
    specialtyDone,
    catalogDone
  };
}

/**
 * El aviso se puede posponer, y la decisión es por usuario y por clínica. Se guarda en el
 * navegador siguiendo el mismo criterio que las bienvenidas de módulo: es una preferencia de
 * presentación, no un dato clínico, y no vale una columna en la base.
 */
export function getOnboardingDismissKey(userId: string, clinicId: string): string {
  return `clinerya:onboarding-dismissed:${userId}:${clinicId}`;
}

export function isOnboardingDismissed(userId: string, clinicId: string): boolean {
  try {
    return localStorage.getItem(getOnboardingDismissKey(userId, clinicId)) === "1";
  } catch {
    return false;
  }
}

export function dismissOnboarding(userId: string, clinicId: string): void {
  try {
    localStorage.setItem(getOnboardingDismissKey(userId, clinicId), "1");
  } catch {
    // Modo privado o almacenamiento bloqueado: el aviso volverá a aparecer, que es preferible a
    // romper el flujo por no poder guardar una preferencia.
  }
}
