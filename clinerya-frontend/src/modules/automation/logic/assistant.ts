import type { ChannelSettingsView, DoctorChannelView } from "../types";

export type Tone = "ok" | "warn" | "off";

export function assistantStatus(_view: ChannelSettingsView): { tone: Tone; label: string } {
  throw new Error("pendiente");
}

export function setupSteps(_view: ChannelSettingsView): {
  whatsapp: boolean;
  gemini: boolean;
  tested: boolean;
  templates: boolean;
} {
  throw new Error("pendiente");
}

export function doctorChannelStatus(_channel: DoctorChannelView): { tone: Tone; label: string } {
  throw new Error("pendiente");
}
