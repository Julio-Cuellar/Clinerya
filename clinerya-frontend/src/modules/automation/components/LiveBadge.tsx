import type { RealtimeStatus } from "../realtime/useRealtimeTopic";

const LABELS: Record<RealtimeStatus, { label: string; tone: string }> = {
  live: { label: "En vivo", tone: "ok" },
  connecting: { label: "Conectando…", tone: "off" },
  offline: { label: "Sin conexión en vivo · reintentando", tone: "warn" }
};

/** Estado del canal en tiempo real; sin conexion la pantalla sigue funcionando al recargar. */
export function LiveBadge({ status }: { status: RealtimeStatus }) {
  const { label, tone } = LABELS[status];
  return (
    <span className={`wa-pill ${tone}`} role="status">
      <span className="wa-live-dot" aria-hidden="true" />
      {label}
    </span>
  );
}
