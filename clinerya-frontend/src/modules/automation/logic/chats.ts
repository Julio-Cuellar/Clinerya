import type { ChatActivityPush, ChatListItem } from "../types";

export function applyChatActivity(_chats: ChatListItem[], _push: ChatActivityPush, _openPhone?: string): ChatListItem[] {
  throw new Error("pendiente");
}

export function chatTitle(_chat: { patientNames: string[] }): string {
  throw new Error("pendiente");
}

export function describeAccess(
  _userId: string,
  _staff: Array<{ userId: string; fullName: string; role: string }>
): { name: string; role: string } {
  throw new Error("pendiente");
}
