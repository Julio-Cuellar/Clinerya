import type { Slot } from "../types";

/** Maximo de horarios que el medico puede proponer (igual que el backend). */
export const MAX_PROPOSED_OPTIONS = 3;
/** Menos de este tiempo para vencer se marca como urgente (coincide con el recordatorio al medico). */
const URGENT_MINUTES = 4 * 60;
const MINUTE_MS = 60_000;

// Los mismos nombres que usa el asistente en WhatsApp ("Mar 29/09 10:00").
const SHORT_DAYS = ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"];
const LONG_DAYS = ["Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"];

export interface TimeLeft {
  label: string;
  urgent: boolean;
  expired: boolean;
}

/** Las fechas del backend no traen zona: se leen como hora local de la clinica. */
function toDate(localDateTime: string): Date {
  return new Date(localDateTime);
}

const pad = (value: number) => String(value).padStart(2, "0");

function dayAndMonth(date: Date): string {
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}`;
}

export function timeLeft(expiresAt: string, now: Date): TimeLeft {
  const minutes = Math.floor((toDate(expiresAt).getTime() - now.getTime()) / MINUTE_MS);
  if (minutes <= 0) {
    return { label: "venció", urgent: true, expired: true };
  }
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  const label = hours === 0 ? `vence en ${rest} min` : rest === 0 ? `vence en ${hours} h` : `vence en ${hours} h ${rest} min`;
  return { label, urgent: minutes < URGENT_MINUTES, expired: false };
}

export function receivedAgo(createdAt: string, now: Date): string {
  const minutes = Math.floor((now.getTime() - toDate(createdAt).getTime()) / MINUTE_MS);
  if (minutes < 1) {
    return "hace un momento";
  }
  if (minutes < 60) {
    return `hace ${minutes} min`;
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    return `hace ${hours} h`;
  }
  const days = Math.floor(hours / 24);
  return days === 1 ? "hace 1 día" : `hace ${days} días`;
}

export function slotTime(start: string): string {
  const date = toDate(start);
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function slotLabel(start: string): string {
  const date = toDate(start);
  return `${SHORT_DAYS[date.getDay()]} ${dayAndMonth(date)} ${slotTime(start)}`;
}

/** Marca o desmarca un horario; nunca pasa de tres y conserva el orden cronologico. */
export function toggleOption(selected: Slot[], slot: Slot): Slot[] {
  if (selected.some((option) => option.start === slot.start)) {
    return selected.filter((option) => option.start !== slot.start);
  }
  if (selected.length >= MAX_PROPOSED_OPTIONS) {
    return selected;
  }
  return [...selected, slot].sort((a, b) => a.start.localeCompare(b.start));
}

export function groupSlotsByDay(slots: Slot[]): Array<{ day: string; label: string; slots: Slot[] }> {
  const groups = new Map<string, Slot[]>();
  [...slots]
    .sort((a, b) => a.start.localeCompare(b.start))
    .forEach((slot) => {
      const day = slot.start.slice(0, 10);
      groups.set(day, [...(groups.get(day) ?? []), slot]);
    });
  return [...groups.entries()].map(([day, daySlots]) => {
    const date = toDate(daySlots[0].start);
    return { day, label: `${LONG_DAYS[date.getDay()]} ${dayAndMonth(date)}`, slots: daySlots };
  });
}
