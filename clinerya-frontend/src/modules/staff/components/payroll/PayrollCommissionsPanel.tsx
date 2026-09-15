import { useState } from "react";
import { IconCoin, IconRefresh } from "@tabler/icons-react";
import type { ClinicStaffResponse, CommissionPreviewEntry } from "@shared/api/api";
import { formatCurrency, staffMemberName } from "@modules/staff/lib/payroll";

/** Comisiones del periodo: suma de actividad registrada por empleado en el rango, y volcado a las líneas. */
export function PayrollCommissionsPanel({
  staff,
  preview,
  onLoadPreview,
  onApply
}: {
  staff: ClinicStaffResponse[];
  preview: CommissionPreviewEntry[] | null;
  onLoadPreview: () => Promise<void>;
  onApply: () => Promise<void>;
}) {
  const [busy, setBusy] = useState<"load" | "apply" | null>(null);

  const run = async (kind: "load" | "apply", action: () => Promise<void>) => {
    setBusy(kind);
    try {
      await action();
    } catch {
      // feedback surfaced by the panel
    } finally {
      setBusy(null);
    }
  };

  const total = (preview ?? []).reduce((sum, entry) => sum + (entry.activityTotal ?? 0), 0);

  return (
    <div className="payroll-commissions">
      <div className="payroll-commissions-head">
        <div>
          <strong>Comisiones desde actividad</strong>
          <small className="description">Suma la actividad registrada de cada empleado dentro del rango del periodo.</small>
        </div>
        <button className="btn ghost" type="button" disabled={busy !== null} onClick={() => run("load", onLoadPreview)}>
          <IconRefresh size={15} aria-hidden="true" />
          {preview === null ? (busy === "load" ? "Calculando..." : "Ver comisiones") : "Recalcular"}
        </button>
      </div>

      {preview !== null && preview.length === 0 && (
        <p className="description">Sin actividad registrada en el rango de este periodo.</p>
      )}

      {preview !== null && preview.length > 0 && (
        <>
          <div className="table-wrapper">
            <table className="data-table no-row-click">
              <thead>
                <tr>
                  <th>Empleado</th>
                  <th>Actividad bruta</th>
                  <th>Comision actual</th>
                </tr>
              </thead>
              <tbody>
                {preview.map((entry) => (
                  <tr key={entry.staffId}>
                    <td>{staffMemberName(staff, entry.staffId)}</td>
                    <td>{formatCurrency(entry.activityTotal)}</td>
                    <td>{formatCurrency(entry.currentCommission)}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr>
                  <th>Total ({preview.length})</th>
                  <td>{formatCurrency(total)}</td>
                  <td aria-hidden="true" />
                </tr>
              </tfoot>
            </table>
          </div>
          <div className="form-actions">
            <button className="btn secondary" type="button" disabled={busy !== null} onClick={() => run("apply", onApply)}>
              <IconCoin size={16} aria-hidden="true" />
              {busy === "apply" ? "Aplicando..." : "Aplicar a las lineas"}
            </button>
            <small className="description">Reemplaza la comisión de cada empleado con actividad y línea capturada.</small>
          </div>
        </>
      )}
    </div>
  );
}
