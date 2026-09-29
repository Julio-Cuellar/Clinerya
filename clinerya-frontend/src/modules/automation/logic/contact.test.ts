import { describe, expect, it } from "vitest";
import { chatPhone, phoneOrigin, signalLabel, visitsLine } from "./contact";

describe("chatPhone", () => {
  it("shows a Mexican mobile as people dial it today", () => {
    expect(chatPhone("5215512345678")).toBe("+52 55 1234 5678");
    expect(chatPhone("525512345678")).toBe("+52 55 1234 5678");
  });

  it("groups a US or Canada number", () => {
    expect(chatPhone("13055550142")).toBe("+1 305 555 0142");
  });

  it("shows any other number complete", () => {
    expect(chatPhone("447700900123")).toBe("+447700900123");
    expect(chatPhone("")).toBe("");
  });
});

describe("phoneOrigin", () => {
  it("names the country and, for the big Mexican cities, the city", () => {
    expect(phoneOrigin("5215512345678")).toBe("México (CDMX)");
    expect(phoneOrigin("5213312345678")).toBe("México (Guadalajara)");
    expect(phoneOrigin("5212221234567")).toBe("México");
    expect(phoneOrigin("13055550142")).toBe("Estados Unidos o Canadá");
    expect(phoneOrigin("447700900123")).toBe("Otro país");
  });
});

describe("signalLabel", () => {
  it("explains every alert in plain words", () => {
    expect(signalLabel("FOREIGN_NUMBER")).toBe("Número del extranjero");
    expect(signalLabel("FIRST_MESSAGE_HAS_LINK")).toBe("Primer mensaje con enlace");
    expect(signalLabel("NEVER_HAD_APPOINTMENT")).toBe("Nunca ha tenido cita");
  });
});

describe("visitsLine", () => {
  it("counts attended, cancelled and missed visits, skipping zeros", () => {
    expect(visitsLine({ attended: 7, cancelled: 1, noShows: 0 })).toBe("7 atendidas · 1 cancelada");
    expect(visitsLine({ attended: 1, cancelled: 2, noShows: 1 })).toBe("1 atendida · 2 canceladas · 1 falta");
    expect(visitsLine({ attended: 0, cancelled: 0, noShows: 0 })).toBe("Sin citas");
  });
});
