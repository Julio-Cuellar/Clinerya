import { useEffect, useState } from "react";
import type { ClinicStaffResponse } from "@shared/api/api";
import { payrollStage } from "@modules/staff/lib/payroll";
import { usePayroll } from "@modules/staff/hooks/usePayroll";
import { PayrollLifecycleStepper } from "@modules/staff/components/payroll/PayrollLifecycleStepper";
import { PayrollPeriodList } from "@modules/staff/components/payroll/PayrollPeriodList";
import { PayrollPeriodForm } from "@modules/staff/components/payroll/PayrollPeriodForm";
import { PayrollCapture } from "@modules/staff/components/payroll/PayrollCapture";
import { PayrollPayment } from "@modules/staff/components/payroll/PayrollPayment";

type PayrollView = "periods" | "new-period" | "capture" | "payment";

/**
 * Self-contained payroll module: owns its data via {@link usePayroll} and splits
 * the work into focused views (Periodos / Captura / Pago).
 */
export function PayrollPanel({ clinicId, staff }: { clinicId?: string; staff: ClinicStaffResponse[] }) {
  const [view, setView] = useState<PayrollView>("periods");
  const payroll = usePayroll(clinicId, staff, true);

  const {
    periods,
    lines,
    bankAccounts,
    selectedPeriod,
    selectedPeriodId,
    selectedStaffId,
    selectedBankAccountId,
    editingLine,
    loading,
    error,
    status,
    clearFeedback,
    selectPeriod,
    selectStaff,
    selectBankAccount,
    startEditLine,
    createPeriod,
    saveLine,
    generateLines,
    commissionPreview,
    loadCommissionPreview,
    applyCommissions,
    deleteLine,
    closePeriod,
    payPeriod
  } = payroll;

  // When a period is picked from the list, move the user to the right next step.
  const handleSelectPeriod = (periodId: string) => {
    selectPeriod(periodId);
    const period = periods.find((item) => item.id === periodId);
    setView(period?.status === "CLOSED" ? "payment" : "capture");
  };

  useEffect(() => {
    clearFeedback();
  }, [view, clearFeedback]);

  return (
    <>
      <div className="tab-switch payroll-subnav">
        <button
          className={view === "periods" || view === "new-period" ? "active" : ""}
          type="button"
          onClick={() => setView("periods")}
        >
          Periodos
        </button>
        <button
          className={view === "capture" ? "active" : ""}
          type="button"
          disabled={!selectedPeriodId}
          onClick={() => setView("capture")}
        >
          Captura
        </button>
        <button
          className={view === "payment" ? "active" : ""}
          type="button"
          disabled={!selectedPeriodId}
          onClick={() => setView("payment")}
        >
          Pago
        </button>
      </div>

      {selectedPeriod && <PayrollLifecycleStepper stage={payrollStage(selectedPeriod, lines.length)} />}

      {view === "periods" && (
        <PayrollPeriodList
          periods={periods}
          selectedPeriodId={selectedPeriodId}
          onSelectPeriod={handleSelectPeriod}
          onNewPeriod={() => setView("new-period")}
        />
      )}

      {view === "new-period" && (
        <PayrollPeriodForm
          onBack={() => setView("periods")}
          onCreatePeriod={async (input) => {
            await createPeriod(input);
            setView("capture");
          }}
        />
      )}

      {view === "capture" && (
        <PayrollCapture
          staff={staff}
          period={selectedPeriod}
          lines={lines}
          loading={loading}
          selectedStaffId={selectedStaffId}
          editingLine={editingLine}
          onSelectStaff={selectStaff}
          onSaveLine={saveLine}
          onGenerateLines={generateLines}
          commissionPreview={commissionPreview}
          onLoadCommissionPreview={loadCommissionPreview}
          onApplyCommissions={applyCommissions}
          onEditLine={startEditLine}
          onDeleteLine={deleteLine}
          onClosePeriod={closePeriod}
          onGoToPeriods={() => setView("periods")}
        />
      )}

      {view === "payment" && (
        <PayrollPayment
          period={selectedPeriod}
          lineCount={lines.length}
          bankAccounts={bankAccounts}
          selectedBankAccountId={selectedBankAccountId}
          onSelectBankAccount={selectBankAccount}
          onPay={payPeriod}
          onGoToPeriods={() => setView("periods")}
        />
      )}

      {status && <p className="alert success">{status}</p>}
      {error && <p className="alert error">{error}</p>}
    </>
  );
}
