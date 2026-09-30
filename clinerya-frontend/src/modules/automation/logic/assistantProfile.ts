import { assistantGaps, type ServiceRulesInput } from "@modules/treatments/logic/catalogRules";

/** Vista previa del saludo en Ajustes: con nombre se presenta; sin nombre habla la clinica. */
export function greetingPreview(assistantName: string, clinicName: string): string {
  const name = assistantName.trim();
  return name
    ? `¡Hola! Soy ${name}, de ${clinicName} 😊 ¿En qué te ayudo?`
    : `¡Hola! Te saluda ${clinicName} 😊 ¿En qué te podemos ayudar?`;
}

/**
 * Revision del catalogo para Ajustes del asistente: cuantos servicios ofrece, cuantos estan incompletos y
 * si hay una "consulta" que pueda cotizar.
 */
export function catalogReview(items: Array<ServiceRulesInput & { name: string }>): {
  available: number;
  incomplete: number;
  consultation: string | null;
} {
  const active = items.filter((item) => item.active !== false);
  const ready = (item: ServiceRulesInput) => assistantGaps(item).length === 0;
  const offered = active.filter((item) => item.availableInAssistant && ready(item));
  const consultation = offered.find((item) => item.name.toLowerCase().includes("consulta"));
  return {
    available: offered.length,
    incomplete: active.filter((item) => !ready(item)).length,
    consultation: consultation?.name ?? null
  };
}

/** Cuantas horas antes puede salir el recordatorio (el servidor acepta de 1 a 72). */
export const REMINDER_HOURS = [2, 12, 24, 48] as const;

/** Ejemplo del recordatorio que recibe el paciente, con los tres botones de la plantilla. */
export function reminderPreview(clinicName: string): { text: string; buttons: string[] } {
  return {
    text: `Hola Ana, te recordamos tu cita en ${clinicName} el Jue 02/10 16:00 con Dra. Ramírez (Limpieza dental). ¿Nos confirmas tu asistencia?`,
    buttons: ["Confirmo", "Cancelar", "Reprogramar"]
  };
}

/** El nombre de la plantilla es como Meta la aprobo: minusculas, numeros y guion bajo (vacio: sin plantilla). */
export function reminderTemplateError(name: string): string | null {
  const value = name.trim();
  if (!value) return null;
  return /^[a-z0-9_]{1,512}$/.test(value)
    ? null
    : "Escribe el nombre tal como aparece en Meta: minúsculas, números y guion bajo (ej. recordatorio_cita).";
}
