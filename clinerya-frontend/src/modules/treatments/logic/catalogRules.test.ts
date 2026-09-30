import { describe, expect, it } from "vitest";
import { assistantGaps, assistantStatus, catalogWarning, priceLabel, validateService } from "./catalogRules";

const DESCRIPTION = "Retiramos sarro y placa con ultrasonido y pulimos tus dientes.";

const item = (overrides: Record<string, unknown> = {}) => ({
  name: "Limpieza dental",
  description: DESCRIPTION,
  defaultPrice: 650,
  estimatedDurationMinutes: 45,
  pricingType: "FIXED" as const,
  availableInAssistant: true,
  active: true,
  ...overrides
});

describe("assistantGaps", () => {
  it("lists what a service lacks to be offered by the assistant", () => {
    expect(assistantGaps(item())).toEqual([]);
    expect(assistantGaps(item({ description: "corta", estimatedDurationMinutes: undefined }))).toEqual(["descripción", "duración"]);
  });
});

describe("assistantStatus", () => {
  it("says whether the assistant offers it, what is missing, or that it is off", () => {
    expect(assistantStatus(item())).toEqual({ tone: "ok", label: "Disponible" });
    expect(assistantStatus(item({ description: undefined, estimatedDurationMinutes: undefined }))).toEqual({
      tone: "warn",
      label: "Falta descripción y duración"
    });
    expect(assistantStatus(item({ availableInAssistant: false }))).toEqual({ tone: "off", label: "No disponible" });
  });
});

describe("catalogWarning", () => {
  it("counts the active services that the assistant cannot offer", () => {
    const items = [item(), item({ description: "" }), item({ estimatedDurationMinutes: undefined }), item({ active: false, description: "" })];

    expect(catalogWarning(items)).toBe("2 servicios no se pueden ofrecer por el asistente: les falta descripción o duración.");
    expect(catalogWarning([item()])).toBeNull();
  });
});

describe("priceLabel", () => {
  it("shows a fixed price as it is and a variable one as from", () => {
    expect(priceLabel(item())).toBe("$650");
    expect(priceLabel(item({ pricingType: "VARIES_BY_PATIENT", defaultPrice: 18000 }))).toBe("desde $18,000");
    expect(priceLabel(item({ pricingType: "VARIES_BY_PATIENT", defaultPrice: undefined }))).toBe("Por valoración");
  });
});

describe("validateService", () => {
  it("applies the same rules as the server", () => {
    expect(validateService(item())).toBeNull();
    expect(validateService(item({ pricingType: "FIXED", defaultPrice: undefined }))).toContain("precio");
    expect(validateService(item({ estimatedDurationMinutes: 0 }))).toContain("duración");
    expect(validateService(item({ description: "corta" }))).toContain("descripción");
    expect(validateService(item({ availableInAssistant: false, description: "" }))).toBeNull();
    expect(validateService(item({ pricingType: "VARIES_BY_PATIENT", defaultPrice: undefined }))).toBeNull();
  });
});
