import type { ContactSignal } from "../types";

/** Ladas de las ciudades con mas pacientes; el resto se muestra solo como "Mexico". */
const MEXICAN_CITIES: Record<string, string> = {
  "55": "CDMX",
  "56": "CDMX",
  "33": "Guadalajara",
  "81": "Monterrey"
};

const SIGNAL_LABELS: Record<ContactSignal, string> = {
  FOREIGN_NUMBER: "Número del extranjero",
  FIRST_MESSAGE_HAS_LINK: "Primer mensaje con enlace",
  NEVER_HAD_APPOINTMENT: "Nunca ha tenido cita"
};

/** Los 10 digitos nacionales de un celular de Mexico (WhatsApp lo manda como 521 o 52 + 10). */
function mexicanNational(digits: string): string | null {
  if (digits.length === 13 && digits.startsWith("521")) return digits.slice(3);
  if (digits.length === 12 && digits.startsWith("52")) return digits.slice(2);
  return null;
}

/** Celular completo como se marca hoy: "+52 55 1234 5678", "+1 305 555 0142" o "+<digitos>". */
export function chatPhone(phone: string): string {
  const digits = phone.replace(/\D/g, "");
  if (!digits) return "";
  const national = mexicanNational(digits);
  if (national) {
    const twoDigitArea = national.slice(0, 2) in MEXICAN_CITIES;
    return twoDigitArea
      ? `+52 ${national.slice(0, 2)} ${national.slice(2, 6)} ${national.slice(6)}`
      : `+52 ${national.slice(0, 3)} ${national.slice(3, 6)} ${national.slice(6)}`;
  }
  if (digits.length === 11 && digits.startsWith("1")) {
    return `+1 ${digits.slice(1, 4)} ${digits.slice(4, 7)} ${digits.slice(7)}`;
  }
  return `+${digits}`;
}

/** De donde es el numero, por su lada. */
export function phoneOrigin(phone: string): string {
  const digits = phone.replace(/\D/g, "");
  const national = mexicanNational(digits);
  if (national) {
    const city = MEXICAN_CITIES[national.slice(0, 2)];
    return city ? `México (${city})` : "México";
  }
  if (digits.length === 11 && digits.startsWith("1")) return "Estados Unidos o Canadá";
  return "Otro país";
}

export function signalLabel(signal: ContactSignal): string {
  return SIGNAL_LABELS[signal];
}

function plural(count: number, one: string, many: string): string {
  return `${count} ${count === 1 ? one : many}`;
}

/** "7 atendidas · 1 cancelada": solo lo que hay; sin citas lo dice. */
export function visitsLine(visits: { attended: number; cancelled: number; noShows: number }): string {
  const parts = [
    visits.attended > 0 ? plural(visits.attended, "atendida", "atendidas") : null,
    visits.cancelled > 0 ? plural(visits.cancelled, "cancelada", "canceladas") : null,
    visits.noShows > 0 ? plural(visits.noShows, "falta", "faltas") : null
  ].filter((part): part is string => part !== null);
  return parts.length > 0 ? parts.join(" · ") : "Sin citas";
}
