import { describe, expect, it } from "vitest";
import type { ChannelSettingsView, DoctorChannelView } from "../types";
import { assistantStatus, doctorChannelStatus, setupSteps } from "./assistant";

const base: ChannelSettingsView = {
  whatsappPhoneNumberId: null,
  whatsappBusinessAccountId: null,
  accessTokenHint: null,
  appSecretHint: null,
  geminiApiKeyHint: null,
  geminiModel: "gemini-2.5-flash",
  promptMode: "DEFAULT",
  customPrompt: null,
  defaultPrompt: "Cordial y breve.",
  webhookPath: "/api/v1/public/whatsapp/k7Qp",
  verifyToken: "vt_1",
  chatRetentionMonths: 12,
  enabled: false,
  whatsappVerifiedAt: null,
  geminiVerifiedAt: null,
  missingToEnable: [],
  patientTemplateName: null,
  doctorTemplateName: null,
  templateLanguage: "es_MX"
};

describe("assistantStatus", () => {
  it("is active when enabled", () => {
    expect(assistantStatus({ ...base, enabled: true })).toEqual({ tone: "ok", label: "Activo" });
  });

  it("counts what is missing", () => {
    expect(assistantStatus({ ...base, missingToEnable: ["probar la conexión de Gemini"] }))
      .toEqual({ tone: "warn", label: "Apagado · falta 1 paso" });
    expect(assistantStatus({ ...base, missingToEnable: ["a", "b", "c"] }))
      .toEqual({ tone: "warn", label: "Apagado · faltan 3 pasos" });
  });

  it("is ready to switch on when nothing is missing", () => {
    expect(assistantStatus(base)).toEqual({ tone: "off", label: "Apagado · listo para activar" });
  });
});

describe("setupSteps", () => {
  it("marks each step done only when its data is complete", () => {
    expect(setupSteps(base)).toEqual({ whatsapp: false, gemini: false, tested: false, templates: false });

    const configured = {
      ...base,
      whatsappPhoneNumberId: "106540352242922",
      accessTokenHint: "••••7890",
      appSecretHint: "••••9876",
      geminiApiKeyHint: "••••4321",
      whatsappVerifiedAt: "2026-09-25T15:32:00",
      patientTemplateName: "clinerya_aviso",
      doctorTemplateName: "clinerya_aviso_medico"
    };
    expect(setupSteps(configured)).toEqual({ whatsapp: true, gemini: true, tested: false, templates: true });
    expect(setupSteps({ ...configured, geminiVerifiedAt: "2026-09-25T15:33:00" }).tested).toBe(true);
  });
});

describe("doctorChannelStatus", () => {
  const channel: DoctorChannelView = { staffId: "s-1", phone: null, active: false, consentAt: null, updatedAt: null };

  it("describes each state of the doctor's notices", () => {
    expect(doctorChannelStatus(channel)).toEqual({ tone: "warn", label: "Solo verá la bandeja" });
    expect(doctorChannelStatus({ ...channel, phone: "5215599990000", active: true, consentAt: "2026-09-25T15:32:00" }))
      .toEqual({ tone: "ok", label: "Recibe avisos" });
    expect(doctorChannelStatus({ ...channel, phone: "5215599990000", consentAt: "2026-09-25T15:32:00" }))
      .toEqual({ tone: "off", label: "Pausado" });
    expect(doctorChannelStatus({ ...channel, phone: "5215599990000" }))
      .toEqual({ tone: "warn", label: "Sin consentimiento" });
  });
});
