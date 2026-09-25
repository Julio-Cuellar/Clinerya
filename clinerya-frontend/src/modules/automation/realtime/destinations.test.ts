import { describe, expect, it } from "vitest";
import { chatsTopic, doctorInboxTopic, realtimeUrl } from "./destinations";

// El socket va al mismo origen que la app: Vite (desarrollo) y nginx (produccion) lo pasan a /ws.
describe("realtimeUrl", () => {
  it("uses wss behind https and ws otherwise", () => {
    expect(realtimeUrl({ protocol: "https:", host: "www.dentalterapy.com.mx" })).toBe("wss://www.dentalterapy.com.mx/ws");
    expect(realtimeUrl({ protocol: "http:", host: "localhost:5173" })).toBe("ws://localhost:5173/ws");
  });
});

// Deben coincidir con RealtimeDestinations del backend: la politica de suscripcion rechaza cualquier otro.
describe("realtime destinations", () => {
  const clinicId = "b42730d8-0000-4000-8000-000000000001";
  const staffId = "80899c95-0000-4000-8000-000000000002";

  it("chat activity of a clinic", () => {
    expect(chatsTopic(clinicId)).toBe(`/topic/clinics/${clinicId}/chats`);
  });

  it("inbox of one doctor", () => {
    expect(doctorInboxTopic(clinicId, staffId)).toBe(`/topic/clinics/${clinicId}/doctors/${staffId}/appointment-requests`);
  });
});
