import { describe, expect, it } from "vitest";
import { applyChatActivity, chatTitle, describeAccess } from "./chats";

const chat = (phone: string, lastMessageAt: string, messageCount = 3, patientNames: string[] = ["Juan Pérez"]) => ({
  phone,
  patientNames,
  lastMessageAt,
  messageCount,
  unread: false
});

describe("applyChatActivity", () => {
  const juan = chat("5215512345678", "2026-09-25T16:33:00", 9);
  const maria = chat("5215511112210", "2026-09-25T12:00:00", 14, ["María López"]);

  it("moves the chat to the top, counts the message and marks it as new", () => {
    const next = applyChatActivity([juan, maria], { phone: maria.phone, at: "2026-09-25T16:40:00" }, juan.phone);

    expect(next.map((c) => c.phone)).toEqual([maria.phone, juan.phone]);
    expect(next[0]).toMatchObject({ lastMessageAt: "2026-09-25T16:40:00", messageCount: 15, unread: true });
  });

  it("does not mark as new the chat the user is reading", () => {
    const next = applyChatActivity([juan, maria], { phone: juan.phone, at: "2026-09-25T16:41:00" }, juan.phone);

    expect(next[0]).toMatchObject({ phone: juan.phone, messageCount: 10, unread: false });
  });

  it("adds a chat that was not on the list", () => {
    const next = applyChatActivity([juan], { phone: "5213399990917", at: "2026-09-25T17:00:00" });

    expect(next[0]).toEqual({
      phone: "5213399990917",
      patientNames: [],
      lastMessageAt: "2026-09-25T17:00:00",
      messageCount: 1,
      unread: true
    });
    expect(next).toHaveLength(2);
  });

  it("leaves the original list untouched", () => {
    const list = [juan, maria];
    applyChatActivity(list, { phone: maria.phone, at: "2026-09-25T16:40:00" });
    expect(list[1].messageCount).toBe(14);
  });
});

describe("chatTitle", () => {
  it("names the patients, or says the number is not registered", () => {
    expect(chatTitle({ patientNames: ["Juan Pérez"] })).toBe("Juan Pérez");
    expect(chatTitle({ patientNames: ["Ana Gómez", "Luis Gómez"] })).toBe("Ana Gómez y Luis Gómez");
    expect(chatTitle({ patientNames: [] })).toBe("Número sin registrar");
  });
});

describe("describeAccess", () => {
  const staff = [
    { userId: "u-1", fullName: "Doctora Demo", role: "DOCTOR" },
    { userId: "u-2", fullName: "Ana Torres", role: "RECEPTIONIST" }
  ];

  it("shows who read the chat with their role", () => {
    expect(describeAccess("u-1", staff)).toEqual({ name: "Doctora Demo", role: "Médico" });
    expect(describeAccess("u-2", staff)).toEqual({ name: "Ana Torres", role: "Recepción" });
  });

  it("still shows a row for someone no longer on staff", () => {
    expect(describeAccess("u-9", staff)).toEqual({ name: "Usuario que ya no está en la clínica", role: "" });
  });
});
