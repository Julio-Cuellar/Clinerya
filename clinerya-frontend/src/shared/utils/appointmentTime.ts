import type { AppointmentResponse } from "@modules/agenda/types";

/**
 * Checks if an appointment is ready to start based on system time and confirmation status.
 * An appointment is ready if:
 * 1. Status is strictly CONFIRMED.
 * 2. Current time is at least 15 minutes before scheduled start time (or past start time).
 */
export function isAppointmentReadyToStart(appointment: AppointmentResponse): boolean {
  if (appointment.status !== "CONFIRMED") {
    return false;
  }
  const now = new Date().getTime();
  const start = new Date(appointment.scheduledStart).getTime();
  // 15 minutes in milliseconds
  const fifteenMinutesMs = 15 * 60 * 1000;
  return now >= (start - fifteenMinutesMs);
}

/**
 * Formats elapsed seconds as HH:MM:SS string.
 */
export function formatTimerSeconds(seconds: number): string {
  const hrs = Math.floor(seconds / 3600);
  const mins = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;
  const pad = (n: number) => String(n).padStart(2, "0");
  return hrs > 0 ? `${pad(hrs)}:${pad(mins)}:${pad(secs)}` : `${pad(mins)}:${pad(secs)}`;
}
