export interface JournalLineResponse {
  id: string;
  bankAccountId: string | null;
  accountCode: string;
  accountName: string;
  debit: number;
  credit: number;
}

export interface JournalEntryResponse {
  id: string;
  clinicId: string;
  description: string;
  entryDate: string;
  sourceEventType: string;
  sourceEventId: string;
  createdAt: string;
  lines: JournalLineResponse[];
  totalDebit: number;
  totalCredit: number;
}

export interface JournalQueryResponse {
  entries: JournalEntryResponse[];
  totalEntries: number;
  totalDebit: number;
  totalCredit: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface TrialBalanceMovementResponse {
  id: string;
  journalEntryId: string;
  date: string;
  description: string;
  sourceEventType: string;
  debit: number;
  credit: number;
}

export interface TrialBalanceAccountResponse {
  code: string;
  name: string;
  nature: "DEBIT" | "CREDIT";
  category: "ASSET" | "LIABILITY" | "EQUITY" | "INCOME" | "EXPENSE" | "CUSTOM";
  initialBalance: number;
  debit: number;
  credit: number;
  finalBalance: number;
  initialDebit: number;
  initialCredit: number;
  balanceAmount: number;
  balanceNature: "DEBIT" | "CREDIT";
  closingSide: "DEBIT" | "CREDIT";
  balancedTotal: number;
  activity: number;
  movements: TrialBalanceMovementResponse[];
}

export interface TrialBalanceResponse {
  from: string;
  to: string;
  accounts: TrialBalanceAccountResponse[];
  totals: {
    initialDebit: number;
    initialCredit: number;
    debit: number;
    credit: number;
    finalDebit: number;
    finalCredit: number;
    initialBalance: number;
    finalBalance: number;
  };
}

export interface WasteReportResponse {
  from: string;
  to: string;
  totalAmount: number;
  totalEvents: number;
  lines: Array<{
    journalEntryId: string;
    sourceEventId: string;
    date: string;
    description: string;
    amount: number;
  }>;
}

export interface CreateJournalLineRequest {
  accountCode: string;
  accountName: string;
  debit: number;
  credit: number;
}

export interface CreateJournalEntryRequest {
  description: string;
  entryDate: string;
  lines: CreateJournalLineRequest[];
}

export type BankAccountType = "DEBIT" | "CREDIT";
export type OperationalAccountKind = "BANK" | "PETTY_CASH" | "RESERVE" | "OTHER";

export interface BankAccountOpeningRequest {
  bankName: string;
  alias: string;
  accountLast4?: string | null;
  currency: string;
  openingBalance: number;
  accountType: BankAccountType;
  accountKind: OperationalAccountKind;
}

export interface CreateOpeningBalanceSetupRequest {
  entryDate: string;
  cashOpeningAmount: number;
  inventoryOpeningAmount: number;
  bankAccounts: BankAccountOpeningRequest[];
  notes?: string | null;
}

export interface BankAccountResponse {
  id: string;
  clinicId: string;
  openingBalanceSetupId: string | null;
  openingJournalEntryId: string | null;
  accountCode: string;
  bankName: string;
  alias: string;
  accountLast4: string | null;
  currency: string;
  openingBalance: number;
  accountType: BankAccountType;
  accountKind: OperationalAccountKind;
  openingDate: string;
  creditCutoffDate: string | null;
  creditPaymentDueDate: string | null;
  creditLimit: number | null;
  creditCurrentAmount: number | null;
  creditMinimumPayment: number | null;
  creditNoInterestPayment: number | null;
  creditCurrentPaymentDue: number | null;
  active: boolean;
  deactivatedAt: string | null;
  deactivationReason: string | null;
  lastModifiedAt: string | null;
  lastModificationReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreditAccountAlertResponse {
  accountId: string;
  accountName: string;
  type: "CUTOFF" | "PAYMENT_DUE";
  alertDate: string;
  daysRemaining: number;
  severity: "critical" | "warning";
  title: string;
  detail: string;
}

export interface CreateBankAccountRequest {
  bankName: string;
  alias: string;
  accountLast4?: string | null;
  currency: string;
  openingBalance: number;
  accountType: BankAccountType;
  accountKind: OperationalAccountKind;
  openingDate: string;
  creditCutoffDate?: string | null;
  creditPaymentDueDate?: string | null;
  creditLimit?: number | null;
  creditCurrentAmount?: number | null;
  creditMinimumPayment?: number | null;
  creditNoInterestPayment?: number | null;
  creditCurrentPaymentDue?: number | null;
  notes?: string | null;
}

export interface UpdateBankAccountRequest {
  bankName: string;
  alias: string;
  accountLast4?: string | null;
  currency: string;
  accountType: BankAccountType;
  accountKind: OperationalAccountKind;
  creditCutoffDate?: string | null;
  creditPaymentDueDate?: string | null;
  creditLimit?: number | null;
  creditCurrentAmount?: number | null;
  creditMinimumPayment?: number | null;
  creditNoInterestPayment?: number | null;
  creditCurrentPaymentDue?: number | null;
  reason: string;
}

export interface TransferFundsRequest {
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  entryDate: string;
  description?: string | null;
}

export interface CorrectBankAccountBalanceRequest {
  entryDate: string;
  correctedBalance: number;
  reason: string;
}

export interface DeactivateBankAccountRequest {
  entryDate: string;
  reason: string;
}

export interface BankAccountMovementResponse {
  journalEntryId: string;
  journalLineId: string;
  entryDate: string;
  description: string;
  sourceEventType: string;
  debit: number;
  credit: number;
  movementAmount: number;
  balanceAfter: number;
}

export interface OpeningBalanceSetupResponse {
  id: string;
  clinicId: string;
  entryDate: string;
  cashOpeningAmount: number;
  inventoryOpeningAmount: number;
  totalOpeningAssets: number;
  capitalAccountCode: string;
  capitalAccountName: string;
  journalEntryId: string;
  notes: string | null;
  createdAt: string;
  bankAccounts: BankAccountResponse[];
}

export interface AccountingPeriodResponse {
  from: string;
  to: string;
}

export interface IncomeStatementSummaryResponse {
  income: number;
  directCosts: number;
  grossProfit: number;
  waste: number;
  otherExpenses: number;
  totalExpenses: number;
  netProfit: number;
  directCostPercentage: number;
  wastePercentage: number;
  otherExpensesPercentage: number;
  grossMarginPercentage: number;
  netMarginPercentage: number;
}

export interface IncomeStatementAccountResponse {
  accountCode: string;
  accountName: string;
  category: "INCOME" | "DIRECT_COST" | "WASTE" | "OTHER_EXPENSE";
  currentAmount: number;
  previousAmount: number;
  changePercentage: number | null;
  percentageOfIncome: number;
}

export interface IncomeStatementTrendResponse {
  from: string;
  to: string;
  income: number;
  expenses: number;
  netProfit: number;
}

export interface IncomeStatementResponse {
  period: AccountingPeriodResponse;
  comparisonPeriod: AccountingPeriodResponse | null;
  current: IncomeStatementSummaryResponse;
  previous: IncomeStatementSummaryResponse;
  change: {
    incomePercentage: number | null;
    expensesPercentage: number | null;
    netProfitPercentage: number | null;
  };
  accounts: IncomeStatementAccountResponse[];
  trend: IncomeStatementTrendResponse[];
}
