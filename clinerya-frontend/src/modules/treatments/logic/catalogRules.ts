/** Reglas del catalogo de servicios (plan v2): las mismas que valida el servidor, para avisar antes de guardar. */

export type PricingType = "FIXED" | "VARIES_BY_PATIENT";

export const MIN_ASSISTANT_DESCRIPTION = 20;
export const MAX_ASSISTANT_DESCRIPTION = 300;
export const MAX_DURATION_MINUTES = 600;

export interface ServiceRulesInput {
  description?: string | null;
  defaultPrice?: number | null;
  estimatedDurationMinutes?: number | null;
  pricingType?: PricingType;
  availableInAssistant?: boolean;
  active?: boolean;
}

const MONEY = new Intl.NumberFormat("en-US", { maximumFractionDigits: 2 });

function descriptionOk(description?: string | null): boolean {
  const text = (description ?? "").trim();
  return text.length >= MIN_ASSISTANT_DESCRIPTION && text.length <= MAX_ASSISTANT_DESCRIPTION;
}

function durationOk(minutes?: number | null): boolean {
  return typeof minutes === "number" && minutes > 0 && minutes <= MAX_DURATION_MINUTES;
}

/** Lo que le falta a un servicio para que el asistente lo pueda ofrecer. */
export function assistantGaps(item: ServiceRulesInput): string[] {
  const gaps: string[] = [];
  if (!descriptionOk(item.description)) gaps.push("descripción");
  if (!durationOk(item.estimatedDurationMinutes)) gaps.push("duración");
  return gaps;
}

/** Etiqueta de la columna "Asistente" del catalogo. */
export function assistantStatus(item: ServiceRulesInput): { tone: "ok" | "warn" | "off"; label: string } {
  const gaps = assistantGaps(item);
  if (gaps.length > 0) {
    return { tone: "warn", label: `Falta ${gaps.join(" y ")}` };
  }
  return item.availableInAssistant ? { tone: "ok", label: "Disponible" } : { tone: "off", label: "No disponible" };
}

/** Aviso arriba del catalogo: cuantos servicios activos no puede ofrecer el asistente. */
export function catalogWarning(items: ServiceRulesInput[]): string | null {
  const incomplete = items.filter((item) => item.active !== false && assistantGaps(item).length > 0).length;
  if (incomplete === 0) return null;
  return `${incomplete} ${incomplete === 1 ? "servicio no se puede" : "servicios no se pueden"} ofrecer por el asistente: `
    + "les falta descripción o duración.";
}

/** "$650" si es fijo; "desde $18,000" si varia; "Por valoración" si varia y no tiene referencia. */
export function priceLabel(item: ServiceRulesInput): string {
  const price = item.defaultPrice;
  if (item.pricingType === "VARIES_BY_PATIENT") {
    return typeof price === "number" ? `desde $${MONEY.format(price)}` : "Por valoración";
  }
  return typeof price === "number" ? `$${MONEY.format(price)}` : "Sin precio";
}

/** Mensaje del primer problema, o null si el servicio se puede guardar. */
export function validateService(item: ServiceRulesInput): string | null {
  if (!item.pricingType) return "Elige si el servicio tiene precio fijo o varía por paciente.";
  if (item.pricingType === "FIXED" && typeof item.defaultPrice !== "number") return "Un servicio de precio fijo necesita su precio.";
  if (typeof item.defaultPrice === "number" && item.defaultPrice < 0) return "El precio debe ser mayor o igual a cero.";
  if (!durationOk(item.estimatedDurationMinutes)) {
    return `La duración del servicio es obligatoria (entre 1 y ${MAX_DURATION_MINUTES} minutos).`;
  }
  if (item.availableInAssistant && !descriptionOk(item.description)) {
    return `Para ofrecerlo por el asistente escribe una descripción de ${MIN_ASSISTANT_DESCRIPTION} a ${MAX_ASSISTANT_DESCRIPTION} caracteres.`;
  }
  return null;
}
