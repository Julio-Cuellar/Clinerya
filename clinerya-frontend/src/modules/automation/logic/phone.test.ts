import { describe, expect, it } from "vitest";
import { formatPhone } from "./phone";

describe("formatPhone", () => {
  it("groups a Mexican mobile for reading", () => {
    expect(formatPhone("5215599990000")).toBe("+52 1 55 9999 0000");
  });

  it("prefixes any other number with +", () => {
    expect(formatPhone("14155550100")).toBe("+14155550100");
    expect(formatPhone(null)).toBe("");
  });
});
