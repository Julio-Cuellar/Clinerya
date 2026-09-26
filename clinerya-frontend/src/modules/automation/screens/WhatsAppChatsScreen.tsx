import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { IconLock } from "@tabler/icons-react";
import { getFriendlyError, staffApi, whatsAppChatsApi, type ClinicStaffResponse } from "@shared/api/api";
import type { ChatAccess, ChatActivityPush, ChatListItem, ChatMessage } from "../types";
import { appendNewMessages, applyChatActivity, chatTitle, describeAccess } from "../logic/chats";
import { maskPhone } from "../logic/phone";
import { receivedAgo } from "../logic/requests";
import { chatsTopic } from "../realtime/destinations";
import { useRealtimeTopic } from "../realtime/useRealtimeTopic";
import { LiveBadge } from "../components/LiveBadge";

const DATE_TIME = new Intl.DateTimeFormat("es-MX", { dateStyle: "short", timeStyle: "short" });
const TIME = new Intl.DateTimeFormat("es-MX", { timeStyle: "short" });
/** Tamano de pagina del backend: si llega completa, puede haber mensajes anteriores. */
const PAGE_SIZE = 50;

function formatDateTime(value: string) {
  return DATE_TIME.format(new Date(value));
}

/**
 * Chats de WhatsApp de la clinica (medicos y recepcion). La lista no trae contenido; abrir un chat
 * queda auditado, asi que el hilo abierto no se relee solo: se ofrece "Ver mensajes nuevos".
 */
export function WhatsAppChatsScreen({ clinicId, canSeeAccessLog }: { clinicId?: string; canSeeAccessLog: boolean }) {
  const [view, setView] = useState<"chats" | "audit">("chats");
  const [chats, setChats] = useState<ChatListItem[]>([]);
  const [openPhone, setOpenPhone] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [openedAt, setOpenedAt] = useState<Date | null>(null);
  // El aviso en vivo llega por un callback: con la ref siempre ve los mensajes que hay en pantalla.
  const messagesRef = useRef<ChatMessage[]>([]);
  const bottomRef = useRef<HTMLDivElement | null>(null);
  const [hasMoreOlder, setHasMoreOlder] = useState(false);
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const loadChats = useCallback(async () => {
    if (!clinicId) return;
    try {
      const list = await whatsAppChatsApi.list(clinicId);
      setChats((current) =>
        list.map((chat) => ({ ...chat, unread: current.find((c) => c.phone === chat.phone)?.unread ?? false }))
      );
      setError("");
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  }, [clinicId]);

  useEffect(() => {
    void loadChats();
  }, [loadChats]);

  const liveStatus = useRealtimeTopic<ChatActivityPush>(
    clinicId ? chatsTopic(clinicId) : null,
    (push) => {
      setChats((current) => applyChatActivity(current, push, openPhone ?? undefined));
      if (push.phone === openPhone) {
        void loadNewer(push.phone);
      }
    },
    () => void loadChats()
  );

  const openChat = async (phone: string) => {
    if (!clinicId) return;
    setOpenPhone(phone);
    setChats((current) => current.map((chat) => (chat.phone === phone ? { ...chat, unread: false } : chat)));
    try {
      const page = await whatsAppChatsApi.messages(clinicId, phone);
      setMessages([...page].reverse());
      setHasMoreOlder(page.length >= PAGE_SIZE);
      setOpenedAt(new Date());
      setError("");
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  /**
   * Un aviso en vivo dice que el chat abierto tuvo actividad: se piden solo los mensajes posteriores al
   * ultimo que se muestra. El backend no vuelve a auditar si esta persona lo leyo hace poco.
   */
  const loadNewer = async (phone: string) => {
    if (!clinicId) return;
    const newest = messagesRef.current[messagesRef.current.length - 1];
    if (!newest) {
      await openChat(phone);
      return;
    }
    try {
      const fresh = await whatsAppChatsApi.messages(clinicId, phone, { after: newest.at });
      setMessages((current) => appendNewMessages(current, fresh));
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  const loadOlder = async () => {
    if (!clinicId || !openPhone || messages.length === 0) return;
    try {
      const page = await whatsAppChatsApi.messages(clinicId, openPhone, { before: messages[0].at });
      setMessages((current) => [...[...page].reverse(), ...current]);
      setHasMoreOlder(page.length >= PAGE_SIZE);
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  useEffect(() => {
    const previous = messagesRef.current;
    messagesRef.current = messages;
    // Solo baja al final cuando llega algo nuevo abajo, no al cargar mensajes anteriores arriba.
    if (messages.length > 0 && previous[previous.length - 1]?.id !== messages[messages.length - 1].id) {
      bottomRef.current?.scrollIntoView({ block: "end" });
    }
  }, [messages]);

  const visibleChats = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return chats;
    const digits = term.replace(/\D/g, "");
    return chats.filter((chat) => (digits !== "" && chat.phone.includes(digits)) || chatTitle(chat).toLowerCase().includes(term));
  }, [chats, search]);

  const openChatSummary = chats.find((chat) => chat.phone === openPhone);

  if (!clinicId) {
    return <section className="wa-page"><p className="wa-empty">Selecciona una clínica para ver sus chats.</p></section>;
  }

  return (
    <section className="wa-page">
      <div className="wa-page-head">
        <p className="wa-help" style={{ margin: 0 }}>Conversaciones del asistente con los pacientes. Solo lectura.</p>
        <div className="wa-row">
          {canSeeAccessLog && (
            <button className="btn ghost" type="button" onClick={() => setView(view === "chats" ? "audit" : "chats")}>
              {view === "chats" ? "Quién leyó los chats" : "Volver a los chats"}
            </button>
          )}
          <LiveBadge status={liveStatus} />
        </div>
      </div>
      {error && <p className="alert error">{error}</p>}

      {view === "audit" && canSeeAccessLog ? (
        <AccessLogView clinicId={clinicId} chats={chats} />
      ) : (
        <div className="wa-chats">
          <div className="wa-chat-list">
            <div style={{ padding: 12, borderBottom: "1px solid var(--color-border)" }}>
              <label className="field">
                <span className="sr-only" style={{ position: "absolute", left: -9999 }}>Buscar chat</span>
                <input placeholder="Buscar por nombre o celular" value={search} onChange={(event) => setSearch(event.target.value)} />
              </label>
            </div>
            {loading ? (
              <p className="wa-empty">Cargando chats…</p>
            ) : visibleChats.length === 0 ? (
              <p className="wa-empty">{chats.length === 0 ? "Aún no hay conversaciones." : "Ningún chat coincide."}</p>
            ) : (
              visibleChats.map((chat) => (
                <button
                  key={chat.phone}
                  type="button"
                  className={`wa-chat${chat.phone === openPhone ? " selected" : ""}`}
                  onClick={() => openChat(chat.phone)}
                >
                  <div className="wa-request-top">
                    <strong>{chatTitle(chat)}</strong>
                    {chat.unread ? (
                      <span className="wa-pill info">Actividad nueva</span>
                    ) : (
                      <span className="wa-sub">{receivedAgo(chat.lastMessageAt, new Date())}</span>
                    )}
                  </div>
                  <div className="wa-sub">
                    {maskPhone(chat.phone)} · {chat.messageCount} {chat.messageCount === 1 ? "mensaje" : "mensajes"}
                  </div>
                </button>
              ))
            )}
            <p className="wa-help" style={{ padding: "12px 16px" }}>La lista no muestra el contenido. Abrir un chat queda registrado.</p>
          </div>

          {openPhone && openChatSummary ? (
            <div className="wa-thread">
              <div className="wa-thread-head">
                <h2 style={{ margin: 0, fontSize: 16 }}>{chatTitle(openChatSummary)}</h2>
                <div className="wa-sub">{maskPhone(openPhone)}</div>
              </div>
              {openedAt && (
                <div className="wa-audit-note" role="note">
                  <IconLock size={14} aria-hidden="true" /> Registramos que abriste este chat a las {TIME.format(openedAt)}; el administrador puede consultarlo.
                </div>
              )}
              <div className="wa-messages">
                {hasMoreOlder && (
                  <button className="btn ghost" type="button" style={{ alignSelf: "center" }} onClick={loadOlder}>
                    Ver mensajes anteriores
                  </button>
                )}
                {messages.map((message) => (
                  <div key={message.id} className={`wa-bubble ${message.direction === "INBOUND" ? "in" : "out"}`}>
                    {message.text}
                    {message.optionLabels.length > 0 && (
                      <div>
                        {message.optionLabels.map((label) => (
                          <span key={label} className="wa-option">{label}</span>
                        ))}
                      </div>
                    )}
                    <div className="wa-bubble-meta">
                      {message.direction === "INBOUND" ? "Paciente" : "Asistente"} · {formatDateTime(message.at)}
                    </div>
                  </div>
                ))}
                <div ref={bottomRef} />
              </div>
              <div className="wa-thread-foot">Solo lectura: aquí responde el asistente. Las citas se aprueban en Solicitudes de cita.</div>
            </div>
          ) : (
            <div className="wa-empty" style={{ alignSelf: "center" }}>Elige un chat para leerlo.</div>
          )}
        </div>
      )}
    </section>
  );
}

function AccessLogView({ clinicId, chats }: { clinicId: string; chats: ChatListItem[] }) {
  const [staff, setStaff] = useState<ClinicStaffResponse[]>([]);
  const [rows, setRows] = useState<ChatAccess[]>([]);
  const [phone, setPhone] = useState("");
  const [userId, setUserId] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const search = useCallback(async () => {
    setLoading(true);
    try {
      setRows(await whatsAppChatsApi.accessLog(clinicId, {
        phone: phone.replace(/\D/g, "") || undefined,
        userId: userId || undefined,
        from: from ? `${from}T00:00:00` : undefined,
        to: to ? `${to}T23:59:59` : undefined
      }));
      setError("");
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  }, [clinicId, phone, userId, from, to]);

  useEffect(() => {
    staffApi.list(clinicId).then(setStaff).catch(() => setStaff([]));
    void search();
    // Solo al abrir; despues filtra el boton.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId]);

  return (
    <div className="wa-page">
      <div>
        <h2 style={{ margin: 0, fontSize: 18 }}>Quién leyó los chats</h2>
        <p className="wa-help">Cada vez que alguien abre un chat queda registrado. Solo el administrador de la clínica ve esta pantalla.</p>
      </div>
      <form
        className="wa-card wa-filters"
        onSubmit={(event) => {
          event.preventDefault();
          void search();
        }}
      >
        <label className="field"><span>Chat (celular)</span><input value={phone} placeholder="Todos" onChange={(e) => setPhone(e.target.value)} /></label>
        <label className="field">
          <span>Quién</span>
          <select value={userId} onChange={(e) => setUserId(e.target.value)}>
            <option value="">Todo el personal</option>
            {staff.map((member) => <option key={member.userId} value={member.userId}>{member.fullName}</option>)}
          </select>
        </label>
        <label className="field"><span>Desde</span><input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></label>
        <label className="field"><span>Hasta</span><input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></label>
        <button className="btn primary" type="submit" disabled={loading}>Filtrar</button>
      </form>
      {error && <p className="alert error">{error}</p>}
      <div className="wa-card" style={{ padding: 0, overflow: "hidden" }}>
        <table className="wa-table" style={{ marginTop: 0 }}>
          <thead><tr><th scope="col">Fecha y hora</th><th scope="col">Quién lo abrió</th><th scope="col">Chat</th></tr></thead>
          <tbody>
            {rows.map((row) => {
              const who = describeAccess(row.userId, staff);
              const chat = chats.find((c) => c.phone === row.phone);
              return (
                <tr key={row.id}>
                  <td>{formatDateTime(row.accessedAt)}</td>
                  <td>{who.name}{who.role && <div className="wa-sub">{who.role}</div>}</td>
                  <td>{chat ? chatTitle(chat) : "Chat"}<div className="wa-sub">{maskPhone(row.phone)}</div></td>
                </tr>
              );
            })}
          </tbody>
        </table>
        <div className="wa-sub" style={{ padding: "12px 16px" }}>
          {loading ? "Buscando…" : `${rows.length} ${rows.length === 1 ? "lectura" : "lecturas"} · se muestran hasta 500 por consulta; acota las fechas para ver más.`}
        </div>
      </div>
    </div>
  );
}
