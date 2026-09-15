import { IconPlus } from "@tabler/icons-react";
import type { StaffPayrollPeriodResponse } from "@shared/api/api";
import { formatCurrency } from "@modules/staff/lib/payroll";

/** "Periodos" view: pick an existing payroll period; create one from its own page. */
export function PayrollPeriodList({
  periods,
  selectedPeriodId,
  onSelectPeriod,
  onNewPeriod
}: {
  periods: StaffPayrollPeriodResponse[];
  selectedPeriodId: string;
  onSelectPeriod: (periodId: string) => void;
  onNewPeriod: () => void;
}) {
  return (
    <article className="panel full">
      <div className="panel-heading">
        <div>
          <h2>Periodos de nomina</h2>
          <span className="panel-subtitle">Selecciona un periodo para capturarlo o pagarlo</span>
        </div>
        <div className="clinic-row-actions">
          <span className="badge neutral">{periods.length}</span>
          <button className="btn primary" type="button" onClick={onNewPeriod}>
            <IconPlus size={16} aria-hidden="true" />
            Nuevo periodo
          </button>
        </div>
      </div>
      <div className="clinic-list">
        {periods.length === 0 && (
          <div className="clinic-row">
            <strong>Sin periodos</strong>
            <span>Crea el primero con el boton "Nuevo periodo".</span>
          </div>
        )}
        {periods.map((period) => {
          const isPaid = period.paymentStatus === "PAID";
          return (
            <div
              className={`clinic-row staff-selectable-row${period.id === selectedPeriodId ? " is-selected" : ""}`}
              key={period.id}
              role="button"
              tabIndex={0}
              onClick={() => onSelectPeriod(period.id)}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault();
                  onSelectPeriod(period.id);
                }
              }}
            >
              <div>
                <strong>{period.name}</strong>
                <span>{period.periodStart} — {period.periodEnd} · Neto {formatCurrency(period.netAmount)}</span>
              </div>
              <div className="clinic-row-actions">
                <span className={`badge ${period.status === "DRAFT" ? "warning" : "success"}`}>
                  {period.status === "DRAFT" ? "Borrador" : "Cerrada"}
                </span>
                <span className={`badge ${isPaid ? "success" : "neutral"}`}>
                  {isPaid ? "Pagada" : "Pendiente de pago"}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </article>
  );
}
