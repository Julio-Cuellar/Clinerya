// Mismos canales que RealtimeDestinations del backend; cualquier otro destino se rechaza al suscribirse.

export function chatsTopic(clinicId: string): string {
  return `/topic/clinics/${clinicId}/chats`;
}

/** El socket va al mismo origen que la app; Vite (desarrollo) y nginx (produccion) lo pasan a /ws. */
export function realtimeUrl(location: { protocol: string; host: string }): string {
  return `${location.protocol === "https:" ? "wss" : "ws"}://${location.host}/ws`;
}

export function doctorInboxTopic(clinicId: string, staffId: string): string {
  return `/topic/clinics/${clinicId}/doctors/${staffId}/appointment-requests`;
}
