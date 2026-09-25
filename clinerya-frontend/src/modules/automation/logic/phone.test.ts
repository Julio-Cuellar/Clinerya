import { describe, expect, it } from "vitest";
import { formatPhone, maskPhone } from "./phone";

describe("maskPhone", () => {
  it("hides the middle of a Mexican mobile as WhatsApp reports it", () => {
    expect(maskPhone("5215512345678")).toBe("+52 1 55 **** 5678");
  });

  it("hides the middle of any other number", () => {
    expect(maskPhone("14155550100")).toBe("+14 **** 0100");
  });

  it("does not break on short or empty values", () => {
    expect(maskPhone("")).toBe("");
    expect(maskPhone("1234")).toBe("****");
  });
});

describe("formatPhone", () => {
  it("groups a Mexican mobile for reading", () => {
    expect(formatPhone("5215599990000")).toBe("+52 1 55 9999 0000");
  });

  it("prefixes any other number with +", () => {
    expect(formatPhone("14155550100")).toBe("+14155550100");
    expect(formatPhone(null)).toBe("");
  });
});
