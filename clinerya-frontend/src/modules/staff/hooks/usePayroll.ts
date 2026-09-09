import { useCallback, useEffect, useRef, useState } from "react";
import {
  accountingApi,
  getFriendlyError,
  staffApi,
  type ClinicStaffResponse,
  type CommissionPreviewEntry,
  type PayrollLineSource,
  type StaffPayrollLineResponse,
  type StaffPayrollPeriodResponse
} from "@shared/api/api";
import type { BankAccountResponse } from "@modules/accounting/types";
import { sumPayrollLines, todayIsoDate } from "@modules/staff/lib/payroll";

export interface PayrollLineValues {
  baseSalary: number;
  commissionAmount: number;
  bonusAmount: number;
  deductionAmount: number;
  notes: string;
}

export interface UsePayrollResult {
  periods: StaffPayrollPeriodResponse[];
  lines: StaffPayrollLineResponse[];
  bankAccounts: BankAccountResponse[];
  selectedPeriod: StaffPayrollPeriodResponse | undefined;
  selectedPeriodId: string;
  selectedStaffId: string;
  selectedBankAccountId: string;
  editingLineId: string;
  editingLine: StaffPayrollLineResponse | undefined;
  loading: boolean;
  error: string;
  status: string;
  clearFeedback: () => void;
  selectPeriod: (periodId: string) => void;
  selectStaff: (staffId: string) => void;
  selectBankAccount: (bankAccountId: string) => void;
  startEditLine: (line: StaffPayrollLineResponse | null) => void;
  createPeriod: (input: { name: string; periodStart: string; periodEnd: string }) => Promise<void>;
  saveLine: (values: PayrollLineValues) => Promise<void>;
  generateLines: (source: PayrollLineSource) => Promise<void>;
  commissionPreview: CommissionPreviewEntry[] | null;
  loadCommissionPreview: () => Promise<void>;
  applyCommissions: () => Promise<void>;
  deleteLine: (line: StaffPayrollLineResponse) => Promise<void>;
  closePeriod: () => Promise<void>;
  payPeriod: () => Promise<void>;
}

/**
 * Owns every piece of payroll state and the API calls behind it, so the payroll
 * UI is decoupled from the rest of the Personal screen (attendance / activity).
 */
export function usePayroll(
  clinicId: string | undefined,
  staff: ClinicStaffResponse[],
  enabled: boolean
): UsePayrollResult {
  const [periods, setPeriods] = useState<StaffPayrollPeriodResponse[]>([]);
  const [lines, setLines] = useState<StaffPayrollLineResponse[]>([]);
  const [bankAccounts, setBankAccounts] = useState<BankAccountResponse[]>([]);
  const [selectedPeriodId, setSelectedPeriodId] = useState("");
  const [selectedStaffId, setSelectedStaffId] = useState("");
  const [selectedBankAccountId, setSelectedBankAccountId] = useState("");
  const [editingLineId, setEditingLineId] = useState("");
  const [commissionPreview, setCommissionPreview] = useState<CommissionPreviewEntry[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [status, setStatus] = useState("");

  const loadVersion = useRef(0);
  const bumpVersion = () => {
    loadVersion.current += 1;
    return loadVersion.current;
  };

  const selectedPeriod = periods.find((period) => period.id === selectedPeriodId);
  const editingLine = lines.find((line) => line.id === editingLineId);

  const clearFeedback = useCallback(() => {
    setError("");
    setStatus("");
  }, []);

  const recomputePeriodTotals = useCallback((periodId: string, nextLines: StaffPayrollLineResponse[]) => {
    const totals = sumPayrollLines(nextLines);
    setPeriods((current) =>
      current.map((period) =>
        period.id === periodId ? { ...period, grossAmount: totals.grossAmount, netAmount: totals.netAmount } : period
      )
    );
  }, []);

  const loadLines = useCallback(
    async (periodId: string, version: number) => {
      if (!clinicId || !periodId) {
        setLines([]);
        return;
      }
      try {
        const nextLines = await staffApi.listPayrollLines(clinicId, periodId);
        if (loadVersion.current === version) {
          setLines(nextLines);
        }
      } catch (caught) {
        if (loadVersion.current === version) {
          setLines([]);
          setError(getFriendlyError(caught));
        }
      }
    },
    [clinicId]
  );

  const refresh = useCallback(
    async (periodIdOverride?: string) => {
      if (!clinicId) return;
      const version = bumpVersion();
      setLoading(true);
      setError("");
      try {
        const [periodsResult, bankAccountsResult] = await Promise.allSettled([
          staffApi.listPayrollPeriods(clinicId),
          accountingApi.listBankAccounts(clinicId)
        ]);
        if (loadVersion.current !== version) return;

        if (bankAccountsResult.status === "fulfilled") {
          const operationalAccounts = bankAccountsResult.value.filter(
            (account) => account.active && account.accountType === "DEBIT"
          );
          setBankAccounts(operationalAccounts);
          setSelectedBankAccountId((current) => current || operationalAccounts[0]?.id || "");
        }

        if (periodsResult.status === "rejected") {
          throw periodsResult.reason;
        }
        const nextPeriods = periodsResult.value;
        setPeriods(nextPeriods);
        const nextPeriodId = periodIdOverride || selectedPeriodId || nextPeriods[0]?.id || "";
        setSelectedPeriodId(nextPeriodId);
        await loadLines(nextPeriodId, version);
      } catch (caught) {
        if (loadVersion.current === version) {
          setError(getFriendlyError(caught));
        }
      } finally {
        if (loadVersion.current === version) {
          setLoading(false);
        }
      }
    },
    [clinicId, selectedPeriodId, loadLines]
  );

  useEffect(() => {
    if (enabled && clinicId) {
      void refresh();
    }
    // Only re-run when the panel is enabled or the clinic changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, clinicId]);

  useEffect(() => {
    if (staff.length > 0) {
      setSelectedStaffId((current) => current || staff[0].staffId);
    }
  }, [staff]);

  const selectPeriod = useCallback(
    (periodId: string) => {
      setSelectedPeriodId(periodId);
      setEditingLineId("");
      setCommissionPreview(null);
      const version = bumpVersion();
      setLoading(true);
      void loadLines(periodId, version).finally(() => {
        if (loadVersion.current === version) setLoading(false);
      });
    },
    [loadLines]
  );

  const selectStaff = useCallback((staffId: string) => setSelectedStaffId(staffId), []);
  const selectBankAccount = useCallback((bankAccountId: string) => setSelectedBankAccountId(bankAccountId), []);

  const startEditLine = useCallback((line: StaffPayrollLineResponse | null) => {
    setEditingLineId(line?.id ?? "");
    if (line) {
      setSelectedStaffId(line.staffId);
    }
  }, []);

  const createPeriod = useCallback(
    async (input: { name: string; periodStart: string; periodEnd: string }) => {
      if (!clinicId) return;
      bumpVersion();
      setError("");
      try {
        const period = await staffApi.createPayrollPeriod(clinicId, {
          name: input.name.trim(),
          periodStart: input.periodStart,
          periodEnd: input.periodEnd
        });
        setPeriods((current) => [period, ...current.filter((item) => item.id !== period.id)]);
        setSelectedPeriodId(period.id);
        setLines([]);
        setEditingLineId("");
        setStatus("Periodo de nomina creado.");
      } catch (caught) {
        setError(getFriendlyError(caught));
        throw caught;
      }
    },
    [clinicId]
  );

  const saveLine = useCallback(
    async (values: PayrollLineValues) => {
      if (!clinicId || !selectedPeriodId || !selectedStaffId) return;
      bumpVersion();
      setError("");
      const request = {
        baseSalary: values.baseSalary,
        commissionAmount: values.commissionAmount,
        bonusAmount: values.bonusAmount,
        deductionAmount: values.deductionAmount,
        notes: values.notes.trim()
      };
      try {
        const savedLine = await staffApi.upsertPayrollLine(clinicId, selectedPeriodId, selectedStaffId, request);
        setLines((current) => {
          const nextLines = [...current.filter((line) => line.id !== savedLine.id), savedLine];
          recomputePeriodTotals(selectedPeriodId, nextLines);
          return nextLines;
        });
        setEditingLineId("");
        setStatus("Linea de nomina guardada.");
      } catch (caught) {
        // The write may have persisted even though the response failed; reconcile.
        try {
          const persistedLines = await staffApi.listPayrollLines(clinicId, selectedPeriodId);
          const persistedLine = persistedLines.find(
            (line) =>
              line.staffId === selectedStaffId &&
              line.baseSalary === request.baseSalary &&
              line.commissionAmount === request.commissionAmount &&
              line.bonusAmount === request.bonusAmount &&
              line.deductionAmount === request.deductionAmount &&
              (line.notes ?? "") === request.notes
          );
          if (persistedLine) {
            setLines(persistedLines);
            recomputePeriodTotals(selectedPeriodId, persistedLines);
            setEditingLineId("");
            setStatus("Linea de nomina guardada.");
            return;
          }
        } catch {
          // Keep the original error when the reconciliation request also fails.
        }
        setError(getFriendlyError(caught));
        throw caught;
      }
    },
    [clinicId, selectedPeriodId, selectedStaffId, recomputePeriodTotals]
  );

  const generateLines = useCallback(
    async (source: PayrollLineSource) => {
      if (!clinicId || !selectedPeriodId) return;
      bumpVersion();
      setError("");
      try {
        const nextLines = await staffApi.generatePayrollLines(clinicId, selectedPeriodId, source);
        setLines(nextLines);
        recomputePeriodTotals(selectedPeriodId, nextLines);
        setStatus(
          source === "PREVIOUS_PERIOD"
            ? "Lineas copiadas del periodo anterior."
            : "Lineas generadas desde los sueldos base."
        );
      } catch (caught) {
        setError(getFriendlyError(caught));
        throw caught;
      }
    },
    [clinicId, selectedPeriodId, recomputePeriodTotals]
  );

  const loadCommissionPreview = useCallback(async () => {
    if (!clinicId || !selectedPeriodId) return;
    setError("");
    try {
      setCommissionPreview(await staffApi.previewPeriodCommissions(clinicId, selectedPeriodId));
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  }, [clinicId, selectedPeriodId]);

  const applyCommissions = useCallback(async () => {
    if (!clinicId || !selectedPeriodId) return;
    bumpVersion();
    setError("");
    try {
      const nextLines = await staffApi.applyPeriodCommissions(clinicId, selectedPeriodId);
      setLines(nextLines);
      recomputePeriodTotals(selectedPeriodId, nextLines);
      setCommissionPreview(null);
      setStatus("Comisiones de actividad aplicadas a las lineas.");
    } catch (caught) {
      setError(getFriendlyError(caught));
      throw caught;
    }
  }, [clinicId, selectedPeriodId, recomputePeriodTotals]);

  const deleteLine = useCallback(
    async (line: StaffPayrollLineResponse) => {
      if (!clinicId || !selectedPeriodId) return;
      if (selectedPeriod?.status !== "DRAFT") {
        setError("Solo puedes eliminar lineas de un periodo en borrador.");
        return;
      }
      bumpVersion();
      setError("");
      try {
        const updatedPeriod = await staffApi.deletePayrollLine(clinicId, selectedPeriodId, line.staffId);
        setLines((current) => current.filter((item) => item.id !== line.id));
        setPeriods((current) => current.map((item) => (item.id === updatedPeriod.id ? updatedPeriod : item)));
        setEditingLineId((current) => (current === line.id ? "" : current));
        setStatus("Linea de nomina eliminada.");
      } catch (caught) {
        setError(getFriendlyError(caught));
      }
    },
    [clinicId, selectedPeriodId, selectedPeriod]
  );

  const closePeriod = useCallback(async () => {
    if (!clinicId || !selectedPeriodId) return;
    bumpVersion();
    setError("");
    try {
      const closedPeriod = await staffApi.closePayrollPeriod(clinicId, selectedPeriodId);
      setPeriods((current) => current.map((period) => (period.id === closedPeriod.id ? closedPeriod : period)));
      setStatus("Periodo de nomina cerrado.");
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  }, [clinicId, selectedPeriodId]);

  const payPeriod = useCallback(async () => {
    if (!clinicId || !selectedPeriodId || !selectedBankAccountId) return;
    bumpVersion();
    setError("");
    try {
      const paidPeriod = await staffApi.payPayrollPeriod(clinicId, selectedPeriodId, {
        bankAccountId: selectedBankAccountId,
        paymentDate: todayIsoDate()
      });
      setPeriods((current) => current.map((period) => (period.id === paidPeriod.id ? paidPeriod : period)));
      setStatus("Pago de nomina registrado en contabilidad.");
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  }, [clinicId, selectedPeriodId, selectedBankAccountId]);

  return {
    periods,
    lines,
    bankAccounts,
    selectedPeriod,
    selectedPeriodId,
    selectedStaffId,
    selectedBankAccountId,
    editingLineId,
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
  };
}
