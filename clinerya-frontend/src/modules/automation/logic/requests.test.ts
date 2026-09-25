import { describe, expect, it } from "vitest";
import { groupSlotsByDay, receivedAgo, slotLabel, slotTime, timeLeft, toggleOption } from "./requests";

const NOW = new Date("2026-09-28T12:00:00");

describe("timeLeft", () => {
  it("shows hours and minutes until the request expires", () => {
    expect(timeLeft("2026-09-29T11:58:00", NOW)).toEqual({ label: "vence en 23 h 58 min", urgent: false, expired: false });
  });

  it("marks as urgent when less than 4 hours remain", () => {
    expect(timeLeft("2026-09-28T14:40:00", NOW)).toEqual({ label: "vence en 2 h 40 min", urgent: true, expired: false });
  });

  it("shows only minutes in the last hour", () => {
    expect(timeLeft("2026-09-28T12:12:00", NOW).label).toBe("vence en 12 min");
  });

  it("reports an already expired request", () => {
    expect(timeLeft("2026-09-28T11:59:00", NOW)).toEqual({ label: "venció", urgent: true, expired: true });
  });
});

describe("receivedAgo", () => {
  it("uses minutes, hours or days", () => {
    expect(receivedAgo("2026-09-28T11:58:00", NOW)).toBe("hace 2 min");
    expect(receivedAgo("2026-09-27T15:00:00", NOW)).toBe("hace 21 h");
    expect(receivedAgo("2026-09-26T09:00:00", NOW)).toBe("hace 2 días");
    expect(receivedAgo("2026-09-28T12:00:00", NOW)).toBe("hace un momento");
  });
});

describe("slot labels", () => {
  it("match the labels the assistant sends by WhatsApp", () => {
    expect(slotLabel("2026-09-29T10:00:00")).toBe("Mar 29/09 10:00");
    expect(slotTime("2026-09-29T09:30:00")).toBe("09:30");
  });
});

describe("toggleOption", () => {
  const slot = (start: string) => ({ start, end: start.replace(":00:00", ":30:00") });
  const a = slot("2026-09-29T10:00:00");
  const b = slot("2026-09-29T11:00:00");
  const c = slot("2026-09-29T12:00:00");
  const d = slot("2026-09-29T13:00:00");

  it("adds and removes without mutating the selection", () => {
    const selected = [a];
    const added = toggleOption(selected, b);
    expect(added).toEqual([a, b]);
    expect(selected).toEqual([a]);
    expect(toggleOption(added, a)).toEqual([b]);
  });

  it("never goes beyond three options", () => {
    expect(toggleOption([a, b, c], d)).toEqual([a, b, c]);
  });

  it("keeps the options in chronological order", () => {
    expect(toggleOption([c], a)).toEqual([a, c]);
  });
});

describe("groupSlotsByDay", () => {
  it("groups by day with a readable heading, in order", () => {
    const groups = groupSlotsByDay([
      { start: "2026-09-30T09:00:00", end: "2026-09-30T09:30:00" },
      { start: "2026-09-29T16:00:00", end: "2026-09-29T16:30:00" },
      { start: "2026-09-29T10:30:00", end: "2026-09-29T11:00:00" }
    ]);

    expect(groups.map((group) => group.label)).toEqual(["Martes 29/09", "Miércoles 30/09"]);
    expect(groups[0].slots.map((slot) => slot.start)).toEqual(["2026-09-29T10:30:00", "2026-09-29T16:00:00"]);
  });

  it("returns nothing for no slots", () => {
    expect(groupSlotsByDay([])).toEqual([]);
  });
});
