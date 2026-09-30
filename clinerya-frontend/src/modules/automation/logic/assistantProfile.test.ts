import { describe, expect, it } from "vitest";
import { catalogReview, greetingPreview } from "./assistantProfile";

const service = (overrides: Record<string, unknown> = {}) => ({
  name: "Limpieza dental",
  description: "Retiramos sarro y placa con ultrasonido y pulimos tus dientes.",
  estimatedDurationMinutes: 45,
  availableInAssistant: true,
  active: true,
  ...overrides
});

describe("greetingPreview", () => {
  it("introduces the assistant by name or speaks for the clinic", () => {
    expect(greetingPreview("Sofi", "Clínica Sonrisa")).toBe("¡Hola! Soy Sofi, de Clínica Sonrisa 😊 ¿En qué te ayudo?");
    expect(greetingPreview("  ", "Clínica Sonrisa")).toBe("¡Hola! Te saluda Clínica Sonrisa 😊 ¿En qué te podemos ayudar?");
  });
});

describe("catalogReview", () => {
  it("counts what the assistant offers, what is incomplete and whether a consultation exists", () => {
    const review = catalogReview([
      service({ name: "Consulta general odontológica" }),
      service(),
      service({ name: "Blanqueamiento", description: "", estimatedDurationMinutes: undefined }),
      service({ name: "Resina", availableInAssistant: false }),
      service({ name: "Viejo", active: false, description: "" })
    ]);

    expect(review).toEqual({ available: 2, incomplete: 1, consultation: "Consulta general odontológica" });
  });

  it("says when there is no consultation the assistant can price", () => {
    expect(catalogReview([service()]).consultation).toBeNull();
  });
});
