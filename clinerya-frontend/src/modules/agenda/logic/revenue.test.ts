import { describe, expect, it } from "vitest";
import { endForService, estimatedRevenue } from "./revenue";

const appt = (overrides: Record<string, unknown> = {}) => ({
  id: String(Math.random()),
  status: "SCHEDULED",
  quotationId: undefined as string | undefined,
  quotationItemId: undefined as string | undefined,
  quotationItemIds: [] as string[],
  servicePricing: undefined as string | undefined,
  servicePrice: undefined as number | undefined,
  ...overrides
});

describe("estimatedRevenue", () => {
  const quotations = new Map([["q1", { grandTotal: 3000, items: [{ id: "i1", subtotal: 1200 }, { id: "i2", subtotal: 1800 }] }]]);

  it("adds fixed service prices and quotation amounts, and counts prices still to be defined", () => {
    const result = estimatedRevenue([
      appt({ servicePricing: "FIXED", servicePrice: 650 }),
      appt({ servicePricing: "FIXED", servicePrice: 500, status: "CONFIRMED" }),
      appt({ servicePricing: "VARIES_BY_PATIENT" }),
      appt({ quotationId: "q1", quotationItemId: "i1" }),
      appt({ servicePricing: "FIXED", servicePrice: 900, status: "CANCELLED" })
    ], quotations);

    expect(result).toEqual({ total: 2350, toDefine: 1 });
  });

  it("does not count twice an appointment with a quotation and a service: the quotation wins", () => {
    const result = estimatedRevenue([appt({ quotationId: "q1", servicePricing: "FIXED", servicePrice: 650 })], quotations);

    expect(result).toEqual({ total: 3000, toDefine: 0 });
  });
});

describe("endForService", () => {
  it("ends the appointment when the service is done", () => {
    expect(endForService("2026-10-02T16:00", 45)).toBe("2026-10-02T16:45");
    expect(endForService("2026-10-02T23:30", 45)).toBe("2026-10-03T00:15");
    expect(endForService("", 45)).toBe("");
  });
});
