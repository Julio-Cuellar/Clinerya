// Mismos canales que RealtimeDestinations del backend; cualquier otro destino se rechaza al suscribirse.

export function chatsTopic(clinicId: string): string {
  return `/topic/clinics/${clinicId}/chats`;
}

export function realtimeUrl(_location: { protocol: string; host: string }): string {
  throw new Error("pendiente");
}

export function doctorInboxTopic(clinicId: string, staffId: string): string {
  return `/topic/clinics/${clinicId}/doctors/${staffId}/appointment-requests`;
}
