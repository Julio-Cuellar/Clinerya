import type { ChannelSettingsView, DoctorChannelView } from "../types";

export type Tone = "ok" | "warn" | "off";

export function assistantStatus(view: ChannelSettingsView): { tone: Tone; label: string } {
  if (view.enabled) {
    return { tone: "ok", label: "Activo" };
  }
  const missing = view.missingToEnable.length;
  if (missing === 0) {
    return { tone: "off", label: "Apagado · listo para activar" };
  }
  return { tone: "warn", label: missing === 1 ? "Apagado · falta 1 paso" : `Apagado · faltan ${missing} pasos` };
}

/** Que pasos de la tarjeta estan completos (para numerarlos en verde). */
export function setupSteps(view: ChannelSettingsView) {
  return {
    whatsapp: Boolean(view.whatsappPhoneNumberId && view.accessTokenHint && view.appSecretHint),
    gemini: Boolean(view.geminiApiKeyHint),
    tested: Boolean(view.whatsappVerifiedAt && view.geminiVerifiedAt),
    templates: Boolean(view.patientTemplateName && view.doctorTemplateName)
  };
}

export function doctorChannelStatus(channel: DoctorChannelView): { tone: Tone; label: string } {
  if (!channel.phone) {
    return { tone: "warn", label: "Solo verá la bandeja" };
  }
  if (channel.active) {
    return { tone: "ok", label: "Recibe avisos" };
  }
  return channel.consentAt ? { tone: "off", label: "Pausado" } : { tone: "warn", label: "Sin consentimiento" };
}
