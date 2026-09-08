import type { ReactNode } from "react";
import { IconX } from "@tabler/icons-react";

/**
 * Confirmation modal that can show a summary of what is about to happen, instead
 * of a bare `window.confirm`. Used for irreversible payroll steps (close / pay).
 */
export function ConfirmDialog({
  title,
  children,
  confirmLabel = "Confirmar",
  cancelLabel = "Cancelar",
  tone = "primary",
  busy = false,
  onConfirm,
  onCancel
}: {
  title: string;
  children?: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: "primary" | "danger";
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>{title}</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onCancel}>
            <IconX size={18} />
          </button>
        </div>
        {children && <div className="confirm-dialog-body">{children}</div>}
        <div className="form-actions" style={{ marginTop: "20px" }}>
          <button
            className={`btn ${tone === "danger" ? "destructive" : "primary"}`}
            type="button"
            disabled={busy}
            onClick={onConfirm}
          >
            {busy ? "Procesando..." : confirmLabel}
          </button>
          <button className="btn ghost" type="button" disabled={busy} onClick={onCancel}>
            {cancelLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
