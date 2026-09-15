import { useEffect, useState } from "react";
import type { ClinicStaffResponse, StaffPayrollLineResponse } from "@shared/api/api";
import { estimateLineNet, formatCurrency } from "@modules/staff/lib/payroll";
import type { PayrollLineValues } from "@modules/staff/hooks/usePayroll";
import { CurrencyInput } from "@modules/staff/components/payroll/CurrencyInput";

const asNumber = (value: number | null) => value ?? 0;

/** Capture / edit a single payroll line. Owns its field state and net preview. */
export function PayrollLineForm({
  staff,
  selectedStaffId,
  editingLine,
  canModify,
  onSelectStaff,
  onSubmit,
  onCancelEdit
}: {
  staff: ClinicStaffResponse[];
  selectedStaffId: string;
  editingLine: StaffPayrollLineResponse | undefined;
  canModify: boolean;
  onSelectStaff: (staffId: string) => void;
  onSubmit: (values: PayrollLineValues) => void;
  onCancelEdit: () => void;
}) {
  const [baseSalary, setBaseSalary] = useState<number | null>(null);
  const [commissionAmount, setCommissionAmount] = useState<number | null>(null);
  const [bonusAmount, setBonusAmount] = useState<number | null>(null);
  const [deductionAmount, setDeductionAmount] = useState<number | null>(null);
  const [notes, setNotes] = useState("");

  // Reload the fields whenever the target line (or employee) changes.
  useEffect(() => {
    setBaseSalary(editingLine?.baseSalary ?? null);
    setCommissionAmount(editingLine?.commissionAmount ?? null);
    setBonusAmount(editingLine?.bonusAmount ?? null);
    setDeductionAmount(editingLine?.deductionAmount ?? null);
    setNotes(editingLine?.notes ?? "");
  }, [editingLine, selectedStaffId]);

  const previewNet = estimateLineNet({
    baseSalary: asNumber(baseSalary),
    commissionAmount: asNumber(commissionAmount),
    bonusAmount: asNumber(bonusAmount),
    deductionAmount: asNumber(deductionAmount)
  });
  const hasInput = [baseSalary, commissionAmount, bonusAmount, deductionAmount].some((value) => value !== null);

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    onSubmit({
      baseSalary: asNumber(baseSalary),
      commissionAmount: asNumber(commissionAmount),
      bonusAmount: asNumber(bonusAmount),
      deductionAmount: asNumber(deductionAmount),
      notes
    });
  };

  return (
    <form className="profile-form" onSubmit={handleSubmit}>
      <label className="field">
        <span>Empleado</span>
        <select value={selectedStaffId} disabled={Boolean(editingLine)} onChange={(event) => onSelectStaff(event.target.value)}>
          {staff.map((member) => (
            <option key={member.staffId} value={member.staffId}>{member.fullName}</option>
          ))}
        </select>
      </label>
      <label className="field">
        <span>Sueldo base</span>
        <CurrencyInput ariaLabel="Sueldo base" value={baseSalary} onValueChange={setBaseSalary} />
      </label>
      <label className="field">
        <span>Comision</span>
        <CurrencyInput ariaLabel="Comision" value={commissionAmount} onValueChange={setCommissionAmount} />
      </label>
      <label className="field">
        <span>Bono</span>
        <CurrencyInput ariaLabel="Bono" value={bonusAmount} onValueChange={setBonusAmount} />
      </label>
      <label className="field">
        <span>Deduccion</span>
        <CurrencyInput ariaLabel="Deduccion" value={deductionAmount} onValueChange={setDeductionAmount} />
      </label>
      <label className="field">
        <span>Notas</span>
        <input placeholder="Pago semanal, ajuste, etc." value={notes} onChange={(event) => setNotes(event.target.value)} />
      </label>
      {hasInput && (
        <p className={`field-full description${previewNet < 0 ? " alert error" : ""}`}>
          Neto estimado de esta linea: {formatCurrency(previewNet)}
          {previewNet < 0 && " · la deduccion supera al bruto"}
        </p>
      )}
      <div className="form-actions">
        <button className="btn primary" type="submit" disabled={!canModify || !selectedStaffId}>
          {editingLine ? "Actualizar linea" : "Guardar linea"}
        </button>
        {editingLine && (
          <button className="btn ghost" type="button" onClick={onCancelEdit}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  );
}
