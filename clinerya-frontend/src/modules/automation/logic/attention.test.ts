import { describe, expect, it } from "vitest";
import { attentionSubtitle, humanAttentionChats, messageAuthor, replyWindow } from "./attention";

const staff = [{ userId: "u-maria", fullName: "María García", role: "RECEPTIONIST" }];

const chat = (phone: string, humanAttention: boolean, attentionUserId: string | null = null) => ({
  phone,
  patientNames: ["Ana López"],
  lastMessageAt: "2026-09-29T12:00:00",
  messageCount: 3,
  unread: false,
  humanAttention,
  attentionUserId,
  attentionSince: humanAttention ? "2026-09-29T12:10:00" : null
});

describe("humanAttentionChats", () => {
  it("keeps only the chats a person is attending when the filter is on", () => {
    const chats = [chat("1", false), chat("2", true, "u-maria"), chat("3", true)];

    expect(humanAttentionChats(chats, true).map((c) => c.phone)).toEqual(["2", "3"]);
    expect(humanAttentionChats(chats, false)).toHaveLength(3);
  });
});

describe("attentionSubtitle", () => {
  it("says the agent asked for help when nobody has taken the chat", () => {
    expect(attentionSubtitle(chat("1", true), staff)).toBe("El agente pidió ayuda");
  });

  it("says who is attending the chat", () => {
    expect(attentionSubtitle(chat("1", true, "u-maria"), staff)).toBe("Lo atiende María García");
  });

  it("says nothing for chats the agent is attending", () => {
    expect(attentionSubtitle(chat("1", false), staff)).toBeNull();
  });
});

describe("replyWindow", () => {
  const now = new Date("2026-09-29T12:20:00");

  it("is open until 24 h after the patient's last message", () => {
    const window = replyWindow("2026-09-30T12:16:00", now);

    expect(window.open).toBe(true);
    expect(window.message).toContain("24 h desde su último mensaje");
  });

  it("is closed once WhatsApp's 24 hours have passed", () => {
    const window = replyWindow("2026-09-29T12:16:00", now);

    expect(window.open).toBe(false);
    expect(window.message).toContain("cuando vuelva a escribir");
  });

  it("is closed when the patient has never written", () => {
    expect(replyWindow(null, now).open).toBe(false);
  });
});

describe("messageAuthor", () => {
  it("names the patient, the agent and the staff member who wrote", () => {
    const base = { id: "m", phone: "1", text: "hola", optionLabels: [], at: "2026-09-29T12:00:00", authorUserId: null };

    expect(messageAuthor({ ...base, direction: "INBOUND" }, staff)).toBe("Paciente");
    expect(messageAuthor({ ...base, direction: "OUTBOUND" }, staff)).toBe("Agente");
    expect(messageAuthor({ ...base, direction: "STAFF", authorUserId: "u-maria" }, staff)).toBe("María García · Recepción");
    expect(messageAuthor({ ...base, direction: "STAFF", authorUserId: "u-gone" }, staff)).toBe("Personal de la clínica");
  });
});
