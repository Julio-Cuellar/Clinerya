import { useState } from "react";
import type { ClinicStaffResponse, StaffPayrollLineResponse, StaffPayrollPeriodResponse } from "@shared/api/api";
import { formatCurrency } from "@modules/staff/lib/payroll";
import type { PayrollLineValues } from "@modules/staff/hooks/usePayroll";
import { ConfirmDialog } from "@modules/staff/components/ConfirmDialog";
import { PayrollLineForm } from "@modules/staff/components/payroll/PayrollLineForm";
import { PayrollLinesTable } from "@modules/staff/components/payroll/PayrollLinesTable";

/** "Captura" view: everything you do while a period is still a draft. */
export function PayrollCapture({
  staff,
  period,
  lines,
  loading,
  selectedStaffId,
  editingLine,
  onSelectStaff,
  onSaveLine,
  onEditLine,
  onDeleteLine,
  onClosePeriod,
  onGoToPeriods
}: {
  staff: ClinicStaffResponse[];
  period: StaffPayrollPeriodResponse | undefined;
  lines: StaffPayrollLineResponse[];
  loading: boolean;
  selectedStaffId: string;
  editingLine: StaffPayrollLineResponse | undefined;
  onSelectStaff: (staffId: string) => void;
  onSaveLine: (values: PayrollLineValues) => void;
  onEditLine: (line: StaffPayrollLineResponse | null) => void;
  onDeleteLine: (line: StaffPayrollLineResponse) => void;
  onClosePeriod: () => void;
  onGoToPeriods: () => void;
}) {
  const [confirmingClose, setConfirmingClose] = useState(false);
  const canModify = period?.status === "DRAFT";

  if (!period) {
    return (
      <article className="panel full">
        <div className="empty-table-state">
          <strong>Sin periodo seleccionado</strong>
          <span>Elige un periodo en la pestana Periodos para capturar su nomina.</span>
          <button className="btn secondary" type="button" onClick={onGoToPeriods}>Ir a Periodos</button>
        </div>
      </article>
    );
  }

  return (
    <>
      <article className="panel full">
        <div className="panel-heading">
          <div>
            <h2>{period.name}</h2>
            <span className="panel-subtitle">{period.periodStart} — {period.periodEnd}</span>
          </div>
          <div className="clinic-row-actions">
            <span className="badge neutral">
              {lines.length} de {staff.length} capturados
              {staff.length > 0 ? ` (${Math.round((lines.length / staff.length) * 100)}%)` : ""}
            </span>
            <span className="badge neutral">Bruto {formatCurrency(period.grossAmount)}</span>
            <span className="badge success">Neto {formatCurrency(period.netAmount)}</span>
            <span className={`badge ${period.status === "DRAFT" ? "warning" : "success"}`}>
              {period.status === "DRAFT" ? "Borrador" : "Cerrada"}
            </span>
            <button
              className="btn secondary"
              type="button"
              disabled={period.status !== "DRAFT"}
              onClick={() => setConfirmingClose(true)}
            >
              Cerrar periodo
            </button>
          </div>
        </div>

        {!canModify && (
          <p className="description">Este periodo esta cerrado. Continua en la pestana Pago.</p>
        )}

        <PayrollLineForm
          staff={staff}
          selectedStaffId={selectedStaffId}
          editingLine={editingLine}
          canModify={canModify}
          onSelectStaff={onSelectStaff}
          onSubmit={onSaveLine}
          onCancelEdit={() => onEditLine(null)}
        />
      </article>

      <article className="panel full">
        <PayrollLinesTable
          staff={staff}
          lines={lines}
          loading={loading}
          canModify={canModify}
          onEditLine={onEditLine}
          onDeleteLine={onDeleteLine}
        />
      </article>

      {confirmingClose && (
        <ConfirmDialog
          title="Cerrar periodo de nomina"
          confirmLabel="Cerrar periodo"
          onCancel={() => setConfirmingClose(false)}
          onConfirm={() => {
            setConfirmingClose(false);
            onClosePeriod();
          }}
        >
          <p>Al cerrar <strong>{period.name}</strong> ya no podras agregar ni modificar sus lineas.</p>
          <ul>
            <li>Empleados en el periodo: <strong>{lines.length}</strong></li>
            <li>Bruto: <strong>{formatCurrency(period.grossAmount)}</strong></li>
            <li>Neto a pagar: <strong>{formatCurrency(period.netAmount)}</strong></li>
          </ul>
        </ConfirmDialog>
      )}
    </>
  );
}
