import { useState } from "react";
import { IconReceipt } from "@tabler/icons-react";
import type { StaffPayrollPeriodResponse } from "@shared/api/api";
import type { BankAccountResponse } from "@modules/accounting/types";
import { formatCurrency } from "@modules/staff/lib/payroll";
import { ConfirmDialog } from "@modules/staff/components/ConfirmDialog";

const accountKindLabel = (kind: BankAccountResponse["accountKind"]) => {
  switch (kind) {
    case "PETTY_CASH":
      return "Caja chica";
    case "RESERVE":
      return "Fondo especifico";
    case "OTHER":
      return "Otra operativa";
    default:
      return "Banco";
  }
};

/** "Pago" view: settle a closed period against an operational account. */
export function PayrollPayment({
  period,
  lineCount,
  bankAccounts,
  selectedBankAccountId,
  onSelectBankAccount,
  onPay,
  onGoToPeriods
}: {
  period: StaffPayrollPeriodResponse | undefined;
  lineCount: number;
  bankAccounts: BankAccountResponse[];
  selectedBankAccountId: string;
  onSelectBankAccount: (bankAccountId: string) => void;
  onPay: () => void;
  onGoToPeriods: () => void;
}) {
  const [confirmingPay, setConfirmingPay] = useState(false);

  if (!period) {
    return (
      <article className="panel full">
        <div className="empty-table-state">
          <strong>Sin periodo seleccionado</strong>
          <span>Elige un periodo en la pestana Periodos para pagarlo.</span>
          <button className="btn secondary" type="button" onClick={onGoToPeriods}>Ir a Periodos</button>
        </div>
      </article>
    );
  }

  const isPaid = period.paymentStatus === "PAID";
  const selectedAccount = bankAccounts.find((account) => account.id === selectedBankAccountId);
  const canPay =
    period.status === "CLOSED" && !isPaid && Boolean(selectedBankAccountId) && Number(period.netAmount ?? 0) > 0;

  return (
    <>
      <article className="panel full">
        <div className="panel-heading">
          <div>
            <h2>Pago de {period.name}</h2>
            <span className="panel-subtitle">{period.periodStart} — {period.periodEnd}</span>
          </div>
          <span className={`badge ${isPaid ? "success" : "warning"}`}>
            {isPaid ? "Pagada" : "Pendiente de pago"}
          </span>
        </div>

        <dl className="payroll-payment-summary">
          <div><dt>Empleados</dt><dd>{lineCount}</dd></div>
          <div><dt>Bruto</dt><dd>{formatCurrency(period.grossAmount)}</dd></div>
          <div><dt>Neto a pagar</dt><dd>{formatCurrency(period.netAmount)}</dd></div>
        </dl>

        {period.status !== "CLOSED" && !isPaid && (
          <p className="description">Cierra el periodo en la pestana Captura antes de pagarlo.</p>
        )}

        <div className="profile-form payroll-payment-form">
          <label className="field">
            <span>Cuenta operativa de pago</span>
            <select
              value={selectedBankAccountId}
              onChange={(event) => onSelectBankAccount(event.target.value)}
              disabled={bankAccounts.length === 0 || isPaid}
            >
              <option value="">Selecciona una cuenta operativa</option>
              {bankAccounts.map((account) => (
                <option key={account.id} value={account.id}>
                  {account.alias || account.bankName} - {accountKindLabel(account.accountKind)}
                  {account.accountLast4 ? ` ****${account.accountLast4}` : ""}
                </option>
              ))}
            </select>
            {bankAccounts.length === 0 && (
              <small className="description">Registra una cuenta bancaria de débito en Contabilidad.</small>
            )}
          </label>
          <div className="form-actions">
            <button className="btn primary" type="button" disabled={!canPay} onClick={() => setConfirmingPay(true)}>
              <IconReceipt size={16} aria-hidden="true" />
              Pagar nómina
            </button>
          </div>
          {isPaid && period.paymentJournalEntryId && (
            <p className="field-full description">
              Poliza contable: <span style={{ fontFamily: "monospace" }}>{period.paymentJournalEntryId}</span>
              {" · "}
              <a href="/contabilidad/diario">Ver en Contabilidad</a>
            </p>
          )}
        </div>
      </article>

      {confirmingPay && (
        <ConfirmDialog
          title="Registrar pago de nomina"
          confirmLabel="Pagar nómina"
          onCancel={() => setConfirmingPay(false)}
          onConfirm={() => {
            setConfirmingPay(false);
            onPay();
          }}
        >
          <p>Se registrara la poliza del pago en Contabilidad. Esta accion no se puede deshacer desde aqui.</p>
          <ul>
            <li>Periodo: <strong>{period.name}</strong></li>
            <li>Empleados: <strong>{lineCount}</strong></li>
            <li>Neto: <strong>{formatCurrency(period.netAmount)}</strong></li>
            <li>Cuenta: <strong>{selectedAccount ? (selectedAccount.alias || selectedAccount.bankName) : "—"}</strong></li>
          </ul>
        </ConfirmDialog>
      )}
    </>
  );
}
