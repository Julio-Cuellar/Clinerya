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
