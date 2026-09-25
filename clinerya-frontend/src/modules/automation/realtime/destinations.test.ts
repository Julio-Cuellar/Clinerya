import { describe, expect, it } from "vitest";
import { chatsTopic, doctorInboxTopic } from "./destinations";

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
