import type { ChatActivityPush, ChatListItem, ChatMessage } from "../types";

const ROLE_LABELS: Record<string, string> = {
  DOCTOR: "Médico",
  RECEPTIONIST: "Recepción",
  ASSISTANT: "Asistente",
  CLINIC_ADMIN: "Administración",
  ADMIN: "Administración",
  ACCOUNTANT: "Contabilidad",
  CLEANING: "Limpieza"
};

/**
 * Un aviso en vivo de actividad: el chat sube al principio, suma el mensaje y queda marcado como
 * nuevo, salvo que sea el que el usuario tiene abierto. El aviso no trae texto (se lee auditado).
 */
export function applyChatActivity(chats: ChatListItem[], push: ChatActivityPush, openPhone?: string): ChatListItem[] {
  const current = chats.find((chat) => chat.phone === push.phone);
  const unread = push.phone !== openPhone;
  const updated: ChatListItem = current
    ? { ...current, lastMessageAt: push.at, messageCount: current.messageCount + 1, unread }
    : { phone: push.phone, patientNames: [], lastMessageAt: push.at, messageCount: 1, unread };
  return [updated, ...chats.filter((chat) => chat.phone !== push.phone)];
}

/** Mensajes que llegaron con el chat abierto: al final, en orden, sin repetir ninguno. */
export function appendNewMessages(shown: ChatMessage[], incoming: ChatMessage[]): ChatMessage[] {
  const known = new Set(shown.map((message) => message.id));
  const fresh = incoming.filter((message) => !known.has(message.id)).sort((a, b) => a.at.localeCompare(b.at));
  return [...shown, ...fresh];
}

export function chatTitle(chat: { patientNames: string[] }): string {
  const names = chat.patientNames;
  if (names.length === 0) {
    return "Número sin registrar";
  }
  if (names.length === 1) {
    return names[0];
  }
  return `${names.slice(0, -1).join(", ")} y ${names[names.length - 1]}`;
}

export function describeAccess(
  userId: string,
  staff: Array<{ userId: string; fullName: string; role: string }>
): { name: string; role: string } {
  const member = staff.find((person) => person.userId === userId);
  if (!member) {
    return { name: "Usuario que ya no está en la clínica", role: "" };
  }
  return { name: member.fullName, role: ROLE_LABELS[member.role] ?? member.role };
}
