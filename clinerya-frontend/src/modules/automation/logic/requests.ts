import type { Slot } from "../types";

export interface TimeLeft {
  label: string;
  urgent: boolean;
  expired: boolean;
}

export function timeLeft(_expiresAt: string, _now: Date): TimeLeft {
  throw new Error("pendiente");
}

export function receivedAgo(_createdAt: string, _now: Date): string {
  throw new Error("pendiente");
}

export function slotLabel(_start: string): string {
  throw new Error("pendiente");
}

export function slotTime(_start: string): string {
  throw new Error("pendiente");
}

export function toggleOption(_selected: Slot[], _slot: Slot): Slot[] {
  throw new Error("pendiente");
}

export function groupSlotsByDay(_slots: Slot[]): Array<{ day: string; label: string; slots: Slot[] }> {
  throw new Error("pendiente");
}
