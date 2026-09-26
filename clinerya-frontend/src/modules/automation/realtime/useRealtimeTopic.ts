import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import { refreshSession, sessionStore } from "@shared/api/api";
import { isTokenExpiring } from "@shared/api/sessionRefresh";
import { realtimeUrl } from "./destinations";

export type RealtimeStatus = "connecting" | "live" | "offline";

const RECONNECT_DELAY_MS = 5_000;
const HEARTBEAT_MS = 10_000;
// Renueva un poco antes del vencimiento: evita conectar con un token que caduca en el camino.
const TOKEN_MARGIN_MS = 30_000;

/**
 * Escucha un canal STOMP mientras la pantalla esta abierta. El JWT viaja en el CONNECT (el navegador
 * no puede mandar cabeceras en el handshake) y se vuelve a leer en cada reconexion, por si se renovo.
 * Un aviso perdido mientras no habia conexion no se recupera: por eso, al reconectar, se llama
 * {@code onReconnect} para que la pantalla vuelva a consultar.
 */
export function useRealtimeTopic<T>(
  topic: string | null,
  onMessage: (payload: T) => void,
  onReconnect?: () => void
): RealtimeStatus {
  const [status, setStatus] = useState<RealtimeStatus>("connecting");
  const handlers = useRef({ onMessage, onReconnect });
  handlers.current = { onMessage, onReconnect };

  useEffect(() => {
    if (!topic) {
      setStatus("offline");
      return undefined;
    }
    let connectedBefore = false;
    const client = new Client({
      brokerURL: realtimeUrl(window.location),
      reconnectDelay: RECONNECT_DELAY_MS,
      heartbeatIncoming: HEARTBEAT_MS,
      heartbeatOutgoing: HEARTBEAT_MS,
      beforeConnect: async () => {
        // El CONNECT no puede reintentarse ante un 401 como el REST: se renueva antes si hace falta.
        // Si no se puede renovar, refreshSession cierra la sesion y la pantalla se desmonta.
        if (isTokenExpiring(sessionStore.getAccessToken(), Date.now(), TOKEN_MARGIN_MS)) {
          await refreshSession().catch(() => undefined);
        }
        client.connectHeaders = { Authorization: `Bearer ${sessionStore.getAccessToken() ?? ""}` };
      },
      onConnect: () => {
        setStatus("live");
        client.subscribe(topic, (frame) => {
          try {
            handlers.current.onMessage(JSON.parse(frame.body) as T);
          } catch {
            // Un aviso ilegible se ignora: la pantalla se corrige en la siguiente consulta.
          }
        });
        if (connectedBefore) {
          handlers.current.onReconnect?.();
        }
        connectedBefore = true;
      },
      onWebSocketClose: () => setStatus("offline"),
      onStompError: () => setStatus("offline")
    });
    setStatus("connecting");
    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [topic]);

  return status;
}
