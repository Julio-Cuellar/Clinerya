import { useEffect, useState } from "react";
import { IconArrowRight, IconBell, IconCheck, IconRefresh } from "@tabler/icons-react";
import { getFriendlyError, notificationsApi } from "@shared/api/api";
import type { NotificationResponse } from "@modules/notifications/types";

function formatNotificationDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleString("es-MX", { dateStyle: "short", timeStyle: "short" });
}

export function NotificationCenter({ clinicId }: { clinicId?: string }) {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<NotificationResponse[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const load = async (silent = false) => {
    if (!clinicId) {
      setItems([]);
      setUnreadCount(0);
      return;
    }
    if (!silent) setLoading(true);
    try {
      const response = await notificationsApi.list(clinicId);
      setItems(response.items);
      setUnreadCount(response.unreadCount);
      setError("");
    } catch (caught) {
      if (!silent) setError(getFriendlyError(caught));
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => {
    load();
    const intervalId = window.setInterval(() => load(true), 60_000);
    return () => window.clearInterval(intervalId);
  }, [clinicId]);

  const markRead = async (notification: NotificationResponse) => {
    if (!clinicId || notification.readAt) return;
    try {
      await notificationsApi.markRead(clinicId, notification.id);
      setItems((current) => current.map((item) => item.id === notification.id ? { ...item, readAt: new Date().toISOString() } : item));
      setUnreadCount((current) => Math.max(0, current - 1));
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  const openNotification = async (notification: NotificationResponse) => {
    await markRead(notification);
    setOpen(false);
    if (notification.actionPath) {
      window.history.pushState({}, "", notification.actionPath);
      window.dispatchEvent(new PopStateEvent("popstate"));
    }
  };

  const markAllRead = async () => {
    if (!clinicId || unreadCount === 0) return;
    try {
      await notificationsApi.markAllRead(clinicId);
      setItems((current) => current.map((item) => ({ ...item, readAt: item.readAt ?? new Date().toISOString() })));
      setUnreadCount(0);
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  return (
    <div className="notification-center">
      <button
        className="icon-btn notification-trigger"
        type="button"
        aria-label="Abrir notificaciones"
        title="Notificaciones"
        onClick={() => setOpen((current) => !current)}
      >
        <IconBell size={18} />
        {unreadCount > 0 && <span className="notification-count">{unreadCount > 99 ? "99+" : unreadCount}</span>}
      </button>

      {open && (
        <section className="notification-panel" aria-label="Centro de notificaciones">
          <header className="notification-panel-heading">
            <div>
              <strong>Notificaciones</strong>
              <small>{unreadCount} sin leer</small>
            </div>
            <div className="notification-panel-actions">
              <button className="icon-btn" type="button" title="Actualizar" aria-label="Actualizar notificaciones" onClick={() => load()} disabled={loading}>
                <IconRefresh size={16} className={loading ? "is-spinning" : ""} />
              </button>
              <button className="btn ghost notification-mark-all" type="button" onClick={markAllRead} disabled={unreadCount === 0}>
                <IconCheck size={14} /> Marcar leídas
              </button>
            </div>
          </header>
          {error && <p className="alert error notification-error">{error}</p>}
          <div className="notification-list">
            {items.length === 0 && !loading ? (
              <div className="notification-empty">No hay notificaciones pendientes.</div>
            ) : items.map((notification) => (
              <article className={`notification-item ${notification.readAt ? "is-read" : "is-unread"} is-${notification.severity.toLowerCase()}`} key={notification.id}>
                <button type="button" className="notification-item-main" onClick={() => openNotification(notification)}>
                  <span className="notification-item-dot" aria-hidden="true" />
                  <span>
                    <strong>{notification.title}</strong>
                    <span>{notification.message}</span>
                    <small>{formatNotificationDate(notification.createdAt)} · {notification.sourceModule}</small>
                  </span>
                </button>
                {notification.actionPath && <IconArrowRight size={15} aria-hidden="true" />}
              </article>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
