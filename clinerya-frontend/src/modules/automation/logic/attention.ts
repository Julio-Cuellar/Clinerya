import type { ChatListItem, ChatMessage } from "../types";
import { describeAccess } from "./chats";

type StaffMember = { userId: string; fullName: string; role: string };

const TIME = new Intl.DateTimeFormat("es-MX", { timeStyle: "short" });
const DAY = new Intl.DateTimeFormat("es-MX", { day: "numeric", month: "short" });

/** Con el filtro "Atención humana" encendido, solo los chats que atiende una persona. */
export function humanAttentionChats(chats: ChatListItem[], onlyHuman: boolean): ChatListItem[] {
  return onlyHuman ? chats.filter((chat) => chat.humanAttention) : chats;
}

/** Linea de la lista para un chat en atencion humana: quien lo atiende o que lo pidio el agente. */
export function attentionSubtitle(chat: ChatListItem, staff: StaffMember[]): string | null {
  if (!chat.humanAttention) {
    return null;
  }
  if (!chat.attentionUserId) {
    return "El agente pidió ayuda";
  }
  return `Lo atiende ${describeAccess(chat.attentionUserId, staff).name}`;
}

/**
 * WhatsApp solo permite texto libre dentro de las 24 h siguientes al ultimo mensaje del paciente.
 * {@code replyUntil} lo calcula el backend; aqui solo se decide si la caja se habilita y que se dice.
 */
export function replyWindow(replyUntil: string | null, now: Date): { open: boolean; message: string } {
  if (!replyUntil) {
    return { open: false, message: "El paciente aún no ha escrito. WhatsApp solo permite escribirle después de que él escriba." };
  }
  const until = new Date(replyUntil);
  if (until.getTime() <= now.getTime()) {
    return {
      open: false,
      message: "La conversación de WhatsApp se cerró (pasaron 24 h desde su último mensaje). Podrás escribirle cuando vuelva a escribir."
    };
  }
  const sameDay = until.toDateString() === now.toDateString();
  const when = sameDay ? `hoy a las ${TIME.format(until)}` : `el ${DAY.format(until)} a las ${TIME.format(until)}`;
  return { open: true, message: `Puedes escribirle hasta ${when} (24 h desde su último mensaje).` };
}

/** Quien escribio cada burbuja: el paciente, el agente o la persona de la clinica con su rol. */
export function messageAuthor(message: ChatMessage, staff: StaffMember[]): string {
  if (message.direction === "INBOUND") {
    return "Paciente";
  }
  if (message.direction === "OUTBOUND") {
    return "Agente";
  }
  const member = staff.find((person) => person.userId === message.authorUserId);
  if (!member) {
    return "Personal de la clínica";
  }
  const who = describeAccess(member.userId, staff);
  return who.role ? `${who.name} · ${who.role}` : who.name;
}
