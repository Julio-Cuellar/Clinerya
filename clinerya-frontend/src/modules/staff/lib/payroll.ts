import type {
  ClinicStaffResponse,
  StaffPayrollLineResponse,
  StaffPayrollPeriodResponse
} from "@shared/api/api";

/** ISO date (yyyy-mm-dd) for "today" in the browser's local timezone. */
export function todayIsoDate() {
  return isoDateLocal(new Date());
}

/** ISO date (yyyy-mm-dd) for a Date, using its local calendar values (no UTC shift). */
export function isoDateLocal(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

const monthNamesEs = [
  "enero", "febrero", "marzo", "abril", "mayo", "junio",
  "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
];

export type PayrollPeriodPreset = "fortnight" | "month" | "week";

/** Suggested date range + name for a payroll period, given a quick preset. */
export function payrollPeriodPreset(
  preset: PayrollPeriodPreset,
  today = new Date()
): { start: string; end: string; name: string } {
  const year = today.getFullYear();
  const month = today.getMonth();
  const monthName = monthNamesEs[month];
  if (preset === "month") {
    return {
      start: isoDateLocal(new Date(year, month, 1)),
      end: isoDateLocal(new Date(year, month + 1, 0)),
      name: `Nomina ${monthName} ${year}`
    };
  }
  if (preset === "fortnight") {
    const inFirstHalf = today.getDate() <= 15;
    return {
      start: isoDateLocal(inFirstHalf ? new Date(year, month, 1) : new Date(year, month, 16)),
      end: isoDateLocal(inFirstHalf ? new Date(year, month, 15) : new Date(year, month + 1, 0)),
      name: `Nomina ${inFirstHalf ? "1a" : "2a"} quincena ${monthName} ${year}`
    };
  }
  const diffToMonday = (today.getDay() + 6) % 7;
  const start = new Date(year, month, today.getDate() - diffToMonday);
  const end = new Date(start.getFullYear(), start.getMonth(), start.getDate() + 6);
  return {
    start: isoDateLocal(start),
    end: isoDateLocal(end),
    name: `Nomina semana del ${start.getDate()} ${monthNamesEs[start.getMonth()].slice(0, 3)} al ${end.getDate()} ${monthNamesEs[end.getMonth()].slice(0, 3)} ${end.getFullYear()}`
  };
}

const currencyFormatter = new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" });

export function formatCurrency(value: number | null | undefined) {
  return currencyFormatter.format(value ?? 0);
}

export function staffMemberName(staff: ClinicStaffResponse[], staffId: string) {
  return staff.find((member) => member.staffId === staffId)?.fullName ?? "Personal no encontrado";
}

export interface PayrollLineTotals {
  baseSalary: number;
  commissionAmount: number;
  bonusAmount: number;
  deductionAmount: number;
  grossAmount: number;
  netAmount: number;
}

/** Column sums for a list of payroll lines (the table footer / period totals). */
export function sumPayrollLines(lines: StaffPayrollLineResponse[]): PayrollLineTotals {
  return lines.reduce<PayrollLineTotals>(
    (acc, line) => ({
      baseSalary: acc.baseSalary + (line.baseSalary ?? 0),
      commissionAmount: acc.commissionAmount + (line.commissionAmount ?? 0),
      bonusAmount: acc.bonusAmount + (line.bonusAmount ?? 0),
      deductionAmount: acc.deductionAmount + (line.deductionAmount ?? 0),
      grossAmount: acc.grossAmount + (line.grossAmount ?? 0),
      netAmount: acc.netAmount + (line.netAmount ?? 0)
    }),
    { baseSalary: 0, commissionAmount: 0, bonusAmount: 0, deductionAmount: 0, grossAmount: 0, netAmount: 0 }
  );
}

/** Net of a line being edited before it is saved (base + commission + bonus - deduction). */
export function estimateLineNet(values: {
  baseSalary: number;
  commissionAmount: number;
  bonusAmount: number;
  deductionAmount: number;
}) {
  return values.baseSalary + values.commissionAmount + values.bonusAmount - values.deductionAmount;
}

export type PayrollStage = "capture" | "review" | "closed" | "paid";

export interface PayrollStageStep {
  key: PayrollStage;
  label: string;
}

export const PAYROLL_STAGE_STEPS: PayrollStageStep[] = [
  { key: "capture", label: "Capturar" },
  { key: "review", label: "Revisar" },
  { key: "closed", label: "Cerrar" },
  { key: "paid", label: "Pagar" }
];

/** Which lifecycle step a period is currently on, for the stepper. */
export function payrollStage(
  period: StaffPayrollPeriodResponse | undefined,
  lineCount: number
): PayrollStage {
  if (!period) return "capture";
  if (period.paymentStatus === "PAID") return "paid";
  if (period.status === "CLOSED") return "closed";
  return lineCount > 0 ? "review" : "capture";
}
