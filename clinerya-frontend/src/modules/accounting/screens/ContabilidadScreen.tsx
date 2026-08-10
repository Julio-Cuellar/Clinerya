import { useEffect, useState, useMemo } from "react";
import {
  IconAlertTriangle,
  IconArrowRight,
  IconBuildingBank,
  IconCalendarStats,
  IconChevronDown,
  IconChevronUp,
  IconLoader2,
  IconSearch,
  IconCash,
  IconPackage,
  IconInfoCircle,
  IconPlus,
  IconScale,
  IconTrendingDown,
  IconTrendingUp,
  IconX,
  IconTrash,
  IconDownload,
  IconReportMoney
} from "@tabler/icons-react";
import { moduleOnboardingCopy } from "@app/constants/moduleOnboarding";
import { accountingApi, getFriendlyError } from "@shared/api/api";
import type {
  BankAccountMovementResponse,
  BankAccountResponse,
  BankAccountType,
  CreateJournalLineRequest,
  IncomeStatementResponse,
  JournalEntryResponse,
  JournalQueryResponse,
  OpeningBalanceSetupResponse,
  OperationalAccountKind,
  TrialBalanceResponse,
  WasteReportResponse
} from "@modules/accounting/types";

const currencyFormatter = new Intl.NumberFormat("es-MX", {
  style: "currency",
  currency: "MXN"
});

const numberFormatter = new Intl.NumberFormat("es-MX", {
  minimumFractionDigits: 0,
  maximumFractionDigits: 4
});

const eventLabels: Record<string, string> = {
  PagoRegistrado: "Cobro en Caja",
  ConsumoConciliado: "Consumo de Material",
  MermaCaducidad: "Merma o Caducidad",
  Manual: "Póliza Manual",
  SaldoInicial: "Saldos Iniciales",
  CuentaBancariaAlta: "Alta de cuenta bancaria",
  CuentaBancariaCorreccion: "Correccion bancaria",
  CuentaBancariaBaja: "Baja de cuenta bancaria",
  CuentaOperativaAlta: "Alta de cuenta operativa",
  TransferenciaCuentaOperativa: "Transferencia entre cuentas",
  PagoNomina: "Pago de nomina"
};

const eventBadgeTypes: Record<string, string> = {
  PagoRegistrado: "success",
  ConsumoConciliado: "progress",
  MermaCaducidad: "warning",
  Manual: "neutral",
  SaldoInicial: "progress",
  CuentaBancariaAlta: "success",
  CuentaBancariaCorreccion: "warning",
  CuentaBancariaBaja: "neutral",
  CuentaOperativaAlta: "success",
  TransferenciaCuentaOperativa: "progress",
  PagoNomina: "warning"
};

type AccountNature = "DEBIT" | "CREDIT";
type AccountCategory = "ASSET" | "LIABILITY" | "EQUITY" | "INCOME" | "EXPENSE" | "CUSTOM";

const accountCategoryLabels: Record<AccountCategory, string> = {
  ASSET: "Activo",
  LIABILITY: "Pasivo",
  EQUITY: "Capital",
  INCOME: "Ingreso",
  EXPENSE: "Costo o gasto",
  CUSTOM: "Personalizada"
};

const TYPICAL_ACCOUNTS: Array<{
  code: string;
  name: string;
  nature?: AccountNature;
  category: AccountCategory;
}> = [
  { code: "11100", name: "Caja Operativa", nature: "DEBIT", category: "ASSET" },
  { code: "11200", name: "Bancos", nature: "DEBIT", category: "ASSET" },
  { code: "12100", name: "Almacén de Insumos Clínicos", nature: "DEBIT", category: "ASSET" },
  { code: "21100", name: "Anticipos de Pacientes", nature: "CREDIT", category: "LIABILITY" },
  { code: "21200", name: "Créditos bancarios", nature: "CREDIT", category: "LIABILITY" },
  { code: "31000", name: "Capital inicial", nature: "CREDIT", category: "EQUITY" },
  { code: "41000", name: "Ingresos por Servicios Médicos", nature: "CREDIT", category: "INCOME" },
  { code: "51000", name: "Costo Directo del Servicio", nature: "DEBIT", category: "EXPENSE" },
  { code: "52100", name: "Merma y Caducidad", nature: "DEBIT", category: "EXPENSE" },
  { code: "CUSTOM", name: "Otro (Especificar)", category: "CUSTOM" }
];

const getAccountMetadata = (code: string): { nature: AccountNature; category: AccountCategory; inferred: boolean } => {
  const configured = TYPICAL_ACCOUNTS.find((account) => account.code === code && account.nature);
  if (configured?.nature) return { nature: configured.nature, category: configured.category, inferred: false };
  if (code.startsWith("1")) return { nature: "DEBIT", category: "ASSET", inferred: true };
  if (code.startsWith("2")) return { nature: "CREDIT", category: "LIABILITY", inferred: true };
  if (code.startsWith("3")) return { nature: "CREDIT", category: "EQUITY", inferred: true };
  if (code.startsWith("4")) return { nature: "CREDIT", category: "INCOME", inferred: true };
  if (code.startsWith("5")) return { nature: "DEBIT", category: "EXPENSE", inferred: true };
  return { nature: "CREDIT", category: "CUSTOM", inferred: true };
};

type Tab = "reportes" | "saldos" | "diario" | "bancos" | "balanza" | "resultados" | "mermas";

const accountingTabs: Array<{ key: Tab; label: string }> = [
  { key: "reportes", label: "Resumen" },
  { key: "saldos", label: "Saldos iniciales" },
  { key: "diario", label: "Libro Diario" },
  { key: "mermas", label: "Mermas" },
  { key: "bancos", label: "Cuentas operativas" },
  { key: "balanza", label: "Balanza de Comprobación" },
  { key: "resultados", label: "Estado de Resultados" }
];

type OpeningBankDraft = {
  bankName: string;
  alias: string;
  accountLast4: string;
  currency: string;
  openingBalance: string;
  accountType: BankAccountType;
  accountKind: OperationalAccountKind;
};

const blankOpeningBank = (): OpeningBankDraft => ({
  bankName: "",
  alias: "",
  accountLast4: "",
  currency: "MXN",
  openingBalance: "",
  accountType: "DEBIT",
  accountKind: "BANK"
});

const toAmount = (value: string) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
};

const toOptionalAmount = (value: string) => {
  if (!value.trim()) {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

const isOptionalNonNegative = (value: string) => {
  if (!value.trim()) {
    return true;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0;
};

const todayIso = () => new Date().toISOString().split("T")[0];

const bankTypeLabel = (type: BankAccountType) => type === "CREDIT" ? "Credito" : "Debito";
const accountKindLabel = (kind: OperationalAccountKind | null | undefined) => {
  switch (kind) {
    case "PETTY_CASH": return "Caja chica";
    case "RESERVE": return "Fondo especifico";
    case "OTHER": return "Otra operativa";
    default: return "Banco";
  }
};

const accountingPathByTab: Record<Tab, string> = {
  reportes: "reportes-generales",
  saldos: "saldos-iniciales",
  diario: "diario",
  bancos: "cuentas-bancarias",
  balanza: "balanza",
  resultados: "resultados",
  mermas: "mermas"
};

const accountingTabByPath: Record<string, Tab> = {
  "reportes-generales": "reportes",
  reportes: "reportes",
  "saldos-iniciales": "saldos",
  saldos: "saldos",
  diario: "diario",
  "cuentas-bancarias": "bancos",
  bancos: "bancos",
  balanza: "balanza",
  resultados: "resultados",
  mermas: "mermas"
};

const readAccountingRoute = (): { tab: Tab; bankAccountId: string | null } => {
  const segments = window.location.pathname.split("/").filter(Boolean);
  if (segments[0] !== "contabilidad") {
    return { tab: "reportes", bankAccountId: null };
  }
  const tab = accountingTabByPath[segments[1] ?? "reportes-generales"] ?? "reportes";
  return {
    tab,
    bankAccountId: tab === "bancos" ? segments[2] ?? null : null
  };
};

const buildAccountingPath = (tab: Tab, bankAccountId?: string | null) => {
  const base = `/contabilidad/${accountingPathByTab[tab]}`;
  return tab === "bancos" && bankAccountId ? `${base}/${bankAccountId}` : base;
};

export function ContabilidadScreen({ clinicId, hasClinic }: { clinicId?: string; hasClinic: boolean }) {
  const [entries, setEntries] = useState<JournalEntryResponse[]>([]);
  const [journalQuery, setJournalQuery] = useState<JournalQueryResponse | null>(null);
  const [journalQueryLoading, setJournalQueryLoading] = useState(false);
  const [journalPage, setJournalPage] = useState(0);
  const [trialBalance, setTrialBalance] = useState<TrialBalanceResponse | null>(null);
  const [trialBalanceLoading, setTrialBalanceLoading] = useState(false);
  const [wasteReport, setWasteReport] = useState<WasteReportResponse | null>(null);
  const [wasteReportLoading, setWasteReportLoading] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [status, setStatus] = useState("");
  const [search, setSearch] = useState("");
  const [selectedEvent, setSelectedEvent] = useState<string>("ALL");
  const [expandedEntries, setExpandedEntries] = useState<Record<string, boolean>>({});
  const [balanzaView, setBalanzaView] = useState<"tabla" | "cuentasT">("tabla");
  const [tAccountSearch, setTAccountSearch] = useState("");
  const [tAccountCategory, setTAccountCategory] = useState<AccountCategory | "ALL">("ALL");
  const [tAccountSort, setTAccountSort] = useState<"code" | "balance" | "activity">("code");
  const [expandedTAccounts, setExpandedTAccounts] = useState<Record<string, boolean>>({});
  
  // Pestañas e intervalos de fecha
  const [tab, setTab] = useState<Tab>(() => readAccountingRoute().tab);
  const [startDate, setStartDate] = useState(() => {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().split("T")[0];
  });
  const [endDate, setEndDate] = useState(() => {
    return new Date().toISOString().split("T")[0];
  });
  const [incomeStatement, setIncomeStatement] = useState<IncomeStatementResponse | null>(null);
  const [incomeStatementLoading, setIncomeStatementLoading] = useState(true);
  const [incomeStatementError, setIncomeStatementError] = useState("");
  const [incomeStatementRevision, setIncomeStatementRevision] = useState(0);

  // Modal de registro manual
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [manualDescription, setManualDescription] = useState("");
  const [manualDate, setManualDate] = useState(() => new Date().toISOString().split("T")[0]);
  const [manualLines, setManualLines] = useState<Array<{
    accountCode: string;
    accountName: string;
    debit: string;
    credit: string;
    isCustom: boolean;
  }>>([
    { accountCode: "11100", accountName: "Caja Operativa", debit: "", credit: "", isCustom: false },
    { accountCode: "41000", accountName: "Ingresos por Servicios Médicos", debit: "", credit: "", isCustom: false }
  ]);

  const [openingSetup, setOpeningSetup] = useState<OpeningBalanceSetupResponse | null>(null);
  const [bankAccounts, setBankAccounts] = useState<BankAccountResponse[]>([]);
  const [openingLoading, setOpeningLoading] = useState(true);
  const [openingSaving, setOpeningSaving] = useState(false);
  const [openingDate, setOpeningDate] = useState(() => new Date().toISOString().split("T")[0]);
  const [openingCash, setOpeningCash] = useState("");
  const [openingInventory, setOpeningInventory] = useState("");
  const [openingNotes, setOpeningNotes] = useState("");
  const [openingBanks, setOpeningBanks] = useState<OpeningBankDraft[]>([blankOpeningBank()]);
  const [selectedBankAccountId, setSelectedBankAccountId] = useState<string | null>(() => readAccountingRoute().bankAccountId);
  const [bankMovements, setBankMovements] = useState<BankAccountMovementResponse[]>([]);
  const [bankMovementsLoading, setBankMovementsLoading] = useState(false);
  const [transferAmount, setTransferAmount] = useState("");
  const [transferDestinationId, setTransferDestinationId] = useState("");
  const [transferDate, setTransferDate] = useState(todayIso());
  const [transferDescription, setTransferDescription] = useState("");
  const [showBankForm, setShowBankForm] = useState(false);
  const [showBankEditForm, setShowBankEditForm] = useState(false);
  const [bankActionSaving, setBankActionSaving] = useState(false);
  const [bankForm, setBankForm] = useState({
    bankName: "",
    alias: "",
    accountLast4: "",
    currency: "MXN",
    openingBalance: "",
    accountType: "DEBIT" as BankAccountType,
    accountKind: "BANK" as OperationalAccountKind,
    openingDate: todayIso(),
    creditCutoffDate: "",
    creditPaymentDueDate: "",
    creditLimit: "",
    creditCurrentAmount: "",
    creditMinimumPayment: "",
    creditNoInterestPayment: "",
    creditCurrentPaymentDue: "",
    notes: ""
  });
  const [bankEditForm, setBankEditForm] = useState({
    bankName: "",
    alias: "",
    accountLast4: "",
    currency: "MXN",
    accountType: "DEBIT" as BankAccountType,
    accountKind: "BANK" as OperationalAccountKind,
    creditCutoffDate: "",
    creditPaymentDueDate: "",
    creditLimit: "",
    creditCurrentAmount: "",
    creditMinimumPayment: "",
    creditNoInterestPayment: "",
    creditCurrentPaymentDue: "",
    reason: ""
  });
  const [correctionBalance, setCorrectionBalance] = useState("");
  const [correctionReason, setCorrectionReason] = useState("");
  const [correctionDate, setCorrectionDate] = useState(todayIso());
  const [deactivationReason, setDeactivationReason] = useState("");
  const [deactivationDate, setDeactivationDate] = useState(todayIso());

  const applyAccountingRoute = () => {
    const route = readAccountingRoute();
    setTab(route.tab);
    setSelectedBankAccountId(route.bankAccountId);
    setShowBankForm(false);
    setShowBankEditForm(false);
  };

  const navigateAccounting = (nextTab: Tab, bankAccountId: string | null = null, replace = false) => {
    const targetPath = buildAccountingPath(nextTab, bankAccountId);
    if (window.location.pathname !== targetPath) {
      if (replace) {
        window.history.replaceState({ module: "contabilidad", tab: nextTab, bankAccountId }, "", targetPath);
      } else {
        window.history.pushState({ module: "contabilidad", tab: nextTab, bankAccountId }, "", targetPath);
      }
    }
    setTab(nextTab);
    setSelectedBankAccountId(nextTab === "bancos" ? bankAccountId : null);
    setShowBankForm(false);
    setShowBankEditForm(false);
  };

  const loadEntries = (refreshIncomeStatement = true) => {
    if (!clinicId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError("");
    accountingApi
      .listJournalEntries(clinicId)
      .then((data) => {
        const sorted = [...data].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        setEntries(sorted);
      })
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => {
        setLoading(false);
        if (refreshIncomeStatement) setIncomeStatementRevision((current) => current + 1);
      });
  };

  const loadJournalQuery = () => {
    if (!clinicId) {
      setJournalQuery(null);
      return;
    }
    setJournalQueryLoading(true);
    accountingApi.queryJournalEntries(clinicId, {
      from: startDate,
      to: endDate,
      search,
      sourceEventType: selectedEvent,
      page: journalPage,
      size: 20
    })
      .then(setJournalQuery)
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setJournalQueryLoading(false));
  };

  const loadOpeningData = () => {
    if (!clinicId) {
      setOpeningLoading(false);
      return;
    }
    setOpeningLoading(true);
    Promise.all([
      accountingApi.getOpeningBalances(clinicId),
      accountingApi.listBankAccounts(clinicId)
    ])
      .then(([setup, accounts]) => {
        setOpeningSetup(setup);
        setBankAccounts(accounts);
        const route = readAccountingRoute();
        setSelectedBankAccountId((current) => {
          const routeAccountId = route.tab === "bancos" ? route.bankAccountId : null;
          if (routeAccountId) {
            return accounts.some((account) => account.id === routeAccountId) ? routeAccountId : null;
          }
          return current && accounts.some((account) => account.id === current) ? current : null;
        });
      })
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setOpeningLoading(false));
  };

  const loadBankMovements = (bankAccountId = selectedBankAccountId) => {
    if (!clinicId || !bankAccountId) {
      setBankMovements([]);
      return;
    }

    setBankMovementsLoading(true);
    accountingApi
      .listBankAccountMovements(clinicId, bankAccountId)
      .then(setBankMovements)
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setBankMovementsLoading(false));
  };

  useEffect(() => {
    if (window.location.pathname === "/contabilidad" || window.location.pathname === "/contabilidad/") {
      navigateAccounting("reportes", null, true);
    }
    const handlePopState = () => applyAccountingRoute();
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    loadEntries(false);
    loadOpeningData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId]);

  useEffect(() => {
    setJournalPage(0);
  }, [endDate, search, selectedEvent, startDate]);

  useEffect(() => {
    loadJournalQuery();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId, endDate, journalPage, search, selectedEvent, startDate]);

  useEffect(() => {
    let active = true;
    if (!clinicId || !startDate || !endDate || startDate > endDate) {
      setTrialBalance(null);
      setTrialBalanceLoading(false);
      return () => { active = false; };
    }
    setTrialBalanceLoading(true);
    accountingApi.getTrialBalance(clinicId, startDate, endDate)
      .then((report) => {
        if (active) setTrialBalance(report);
      })
      .catch((caught) => {
        if (active) setError(getFriendlyError(caught));
      })
      .finally(() => {
        if (active) setTrialBalanceLoading(false);
      });
    return () => { active = false; };
  }, [clinicId, endDate, startDate]);

  useEffect(() => {
    let active = true;
    if (!clinicId || !startDate || !endDate || startDate > endDate || tab !== "mermas") {
      setWasteReport(null);
      setWasteReportLoading(false);
      return () => { active = false; };
    }
    setWasteReportLoading(true);
    accountingApi.getWasteReport(clinicId, startDate, endDate)
      .then((report) => {
        if (active) setWasteReport(report);
      })
      .catch((caught) => {
        if (active) setError(getFriendlyError(caught));
      })
      .finally(() => {
        if (active) setWasteReportLoading(false);
      });
    return () => { active = false; };
  }, [clinicId, endDate, startDate, tab]);

  useEffect(() => {
    let active = true;
    if (!clinicId || !startDate || !endDate || startDate > endDate) {
      setIncomeStatement(null);
      setIncomeStatementLoading(false);
      setIncomeStatementError(startDate > endDate ? "El inicio del periodo debe ser anterior al fin." : "");
      return () => {
        active = false;
      };
    }

    setIncomeStatementLoading(true);
    setIncomeStatementError("");
    accountingApi
      .getIncomeStatement(clinicId, startDate, endDate)
      .then((report) => {
        if (active) setIncomeStatement(report);
      })
      .catch((caught) => {
        if (!active) return;
        setIncomeStatement(null);
        setIncomeStatementError(getFriendlyError(caught));
      })
      .finally(() => {
        if (active) setIncomeStatementLoading(false);
      });

    return () => {
      active = false;
    };
  }, [clinicId, endDate, incomeStatementRevision, startDate]);

  useEffect(() => {
    loadBankMovements();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId, selectedBankAccountId]);

  const toggleExpand = (id: string) => {
    setExpandedEntries((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const openJournalEntry = (entry: JournalEntryResponse) => {
    setSearch(entry.description);
    setSelectedEvent("ALL");
    setExpandedEntries((current) => ({ ...current, [entry.id]: true }));
    navigateAccounting("diario");
  };

  // Cálculo de saldos dinámicos de las 5 cuentas principales (Acumulado histórico total)
  const accountBalances = useMemo(() => {
    const balances: Record<string, { name: string; amount: number }> = {
      "11100": { name: "Caja Operativa", amount: 0 },
      "11200": { name: "Bancos", amount: 0 },
      "12100": { name: "Almacén de Insumos Clínicos", amount: 0 },
      "21200": { name: "Creditos bancarios", amount: 0 },
      "51000": { name: "Costo Directo del Servicio", amount: 0 },
      "52100": { name: "Merma y Caducidad", amount: 0 }
    };

    entries.forEach((entry) => {
      entry.lines.forEach((line) => {
        const code = line.accountCode;
        const debit = Number(line.debit) || 0;
        const credit = Number(line.credit) || 0;

        if (!balances[code]) {
          balances[code] = { name: line.accountName, amount: 0 };
        }

        if (code.startsWith("1") || code.startsWith("5")) {
          balances[code].amount += debit - credit;
        } else {
          balances[code].amount += credit - debit;
        }
      });
    });

    return balances;
  }, [entries]);

  // Filtrado del Libro Diario (búsqueda y tipo de evento)
  const filteredEntries = journalQuery?.entries ?? [];

  // Sumas de débitos del Libro Diario filtrado
  const filteredTotals = journalQuery?.totalDebit ?? 0;

  // --- Lógica del Reporte: Balanza de Comprobación ---
  const balanzaDataLegacy = useMemo(() => {
    return [];
    const dataMap: Record<string, {
      code: string;
      name: string;
      initialBalance: number;
      debit: number;
      credit: number;
    }> = {};

    // Inicializar cuentas típicas
    TYPICAL_ACCOUNTS.forEach(acc => {
      if (acc.code !== "CUSTOM") {
        dataMap[acc.code] = { code: acc.code, name: acc.name, initialBalance: 0, debit: 0, credit: 0 };
      }
    });

    entries.forEach((entry) => {
      const entryDate = entry.entryDate;
      const isBeforeStart = entryDate < startDate;
      const isWithinRange = entryDate >= startDate && entryDate <= endDate;

      entry.lines.forEach((line) => {
        const code = line.accountCode;
        const debit = Number(line.debit) || 0;
        const credit = Number(line.credit) || 0;

        if (!dataMap[code]) {
          dataMap[code] = { code, name: line.accountName, initialBalance: 0, debit: 0, credit: 0 };
        }

        if (isBeforeStart) {
          if (code.startsWith("1") || code.startsWith("5")) {
            dataMap[code].initialBalance += debit - credit;
          } else {
            dataMap[code].initialBalance += credit - debit;
          }
        } else if (isWithinRange) {
          dataMap[code].debit += debit;
          dataMap[code].credit += credit;
        }
      });
    });

    // Calcular saldo final para cada cuenta
    return Object.values(dataMap).map((row) => {
      let finalBalance = 0;
      if (row.code.startsWith("1") || row.code.startsWith("5")) {
        finalBalance = row.initialBalance + row.debit - row.credit;
      } else {
        finalBalance = row.initialBalance + row.credit - row.debit;
      }
      return { ...row, finalBalance };
    }).filter(row => row.initialBalance !== 0 || row.debit !== 0 || row.credit !== 0);
  }, [entries, startDate, endDate]);

  const balanzaData = trialBalance?.accounts ?? [];

  const tAccountDataLegacy = useMemo(() => {
    return [];
    const movementsByCode: Record<string, Array<{
      id: string;
      entry: JournalEntryResponse;
      date: string;
      description: string;
      eventLabel: string;
      debit: number;
      credit: number;
    }>> = {};

    [...entries]
      .filter((entry) => entry.entryDate >= startDate && entry.entryDate <= endDate)
      .sort((left, right) => left.entryDate.localeCompare(right.entryDate) || left.createdAt.localeCompare(right.createdAt))
      .forEach((entry) => {
        entry.lines.forEach((line) => {
          const debit = Number(line.debit) || 0;
          const credit = Number(line.credit) || 0;
          if (debit === 0 && credit === 0) return;
          if (!movementsByCode[line.accountCode]) movementsByCode[line.accountCode] = [];
          movementsByCode[line.accountCode].push({
            id: line.id,
            entry,
            date: entry.entryDate,
            description: entry.description,
            eventLabel: eventLabels[entry.sourceEventType] || entry.sourceEventType,
            debit,
            credit
          });
        });
      });

    const normalizedSearch = tAccountSearch.trim().toLowerCase();
    const accounts = balanzaData.map((row) => {
      const metadata = getAccountMetadata(row.code);
      const initialOnDebit = metadata.nature === "DEBIT" ? row.initialBalance >= 0 : row.initialBalance < 0;
      const initialAmount = Math.abs(row.initialBalance);
      const debitTotal = row.debit + (initialOnDebit ? initialAmount : 0);
      const creditTotal = row.credit + (!initialOnDebit ? initialAmount : 0);
      const balanceAmount = Math.abs(debitTotal - creditTotal);
      const balanceNature: AccountNature = debitTotal >= creditTotal ? "DEBIT" : "CREDIT";
      return {
        ...row,
        ...metadata,
        initialOnDebit,
        initialAmount,
        debitTotal,
        creditTotal,
        balanceAmount,
        balanceNature,
        closingSide: balanceNature === "DEBIT" ? "CREDIT" as const : "DEBIT" as const,
        balancedTotal: Math.max(debitTotal, creditTotal),
        movements: movementsByCode[row.code] || [],
        activity: row.debit + row.credit
      };
    }).filter((account) => {
      const matchesSearch = !normalizedSearch || account.code.toLowerCase().includes(normalizedSearch) || account.name.toLowerCase().includes(normalizedSearch);
      const matchesCategory = tAccountCategory === "ALL" || account.category === tAccountCategory;
      return matchesSearch && matchesCategory;
    });

    return accounts.sort((left, right) => {
      if (tAccountSort === "balance") return right.balanceAmount - left.balanceAmount || left.code.localeCompare(right.code);
      if (tAccountSort === "activity") return right.activity - left.activity || left.code.localeCompare(right.code);
      return left.code.localeCompare(right.code);
    });
  }, [balanzaData, endDate, entries, startDate, tAccountCategory, tAccountSearch, tAccountSort]);

  const tAccountData = useMemo(() => {
    const normalizedSearch = tAccountSearch.trim().toLowerCase();
    const accounts = balanzaData.map((row) => {
      const movements = row.movements
        .map((movement) => {
          const entry = entries.find((candidate) => candidate.id === movement.journalEntryId);
          return entry ? {
            ...movement,
            entry,
            eventLabel: eventLabels[movement.sourceEventType] || movement.sourceEventType
          } : null;
        })
        .filter((movement): movement is NonNullable<typeof movement> => movement !== null);
      return {
        ...row,
        inferred: false,
        initialOnDebit: row.initialDebit > 0,
        initialAmount: row.initialDebit + row.initialCredit,
        movements
      };
    }).filter((account) => {
      const matchesSearch = !normalizedSearch
        || account.code.toLowerCase().includes(normalizedSearch)
        || account.name.toLowerCase().includes(normalizedSearch);
      const matchesCategory = tAccountCategory === "ALL" || account.category === tAccountCategory;
      return matchesSearch && matchesCategory;
    });

    return accounts.sort((left, right) => {
      if (tAccountSort === "balance") return right.balanceAmount - left.balanceAmount || left.code.localeCompare(right.code);
      if (tAccountSort === "activity") return right.activity - left.activity || left.code.localeCompare(right.code);
      return left.code.localeCompare(right.code);
    });
  }, [balanzaData, entries, tAccountCategory, tAccountSearch, tAccountSort]);

  const balanzaTotals = useMemo(() => {
    return {
      initialSum: trialBalance?.totals.initialBalance ?? 0,
      debitSum: trialBalance?.totals.debit ?? 0,
      creditSum: trialBalance?.totals.credit ?? 0,
      finalSum: trialBalance?.totals.finalBalance ?? 0
    };
  }, [trialBalance]);

  const reportSummary = incomeStatement?.current;
  const resultadosData = {
    ingresos: Number(reportSummary?.income || 0),
    costos: Number(reportSummary?.directCosts || 0),
    utilidadBruta: Number(reportSummary?.grossProfit || 0),
    mermas: Number(reportSummary?.waste || 0),
    otrosGastos: Number(reportSummary?.otherExpenses || 0),
    utilidadNeta: Number(reportSummary?.netProfit || 0),
    porcentajeCostos: Number(reportSummary?.directCostPercentage || 0),
    porcentajeMermas: Number(reportSummary?.wastePercentage || 0),
    porcentajeOtrosGastos: Number(reportSummary?.otherExpensesPercentage || 0),
    margenUtilidad: Number(reportSummary?.netMarginPercentage || 0),
    margenBruto: Number(reportSummary?.grossMarginPercentage || 0)
  };

  const overviewData = useMemo(() => {
    const periodEntries = entries.filter((entry) => entry.entryDate >= startDate && entry.entryDate <= endDate);
    const buckets = (incomeStatement?.trend || []).map((point) => ({
      label: new Date(`${point.from}T00:00:00`).toLocaleDateString("es-MX", { day: "numeric", month: "short" }),
      income: Number(point.income),
      expenses: Number(point.expenses)
    }));

    const maxTrendAmount = Math.max(1, ...buckets.flatMap((bucket) => [bucket.income, bucket.expenses]));
    const unbalancedEntries = periodEntries.filter((entry) => {
      const totals = entry.lines.reduce(
        (sum, line) => ({ debit: sum.debit + Number(line.debit || 0), credit: sum.credit + Number(line.credit || 0) }),
        { debit: 0, credit: 0 }
      );
      return Math.abs(totals.debit - totals.credit) > 0.01;
    });
    const creditAccountsDue = bankAccounts.filter(
      (account) => account.active && account.accountType === "CREDIT" && Number(account.creditCurrentPaymentDue || 0) > 0
    );

    return {
      periodEntries,
      recentEntries: periodEntries.slice(0, 6),
      buckets,
      maxTrendAmount,
      unbalancedEntries,
      creditAccountsDue,
      creditDueAmount: creditAccountsDue.reduce((sum, account) => sum + Number(account.creditCurrentPaymentDue || 0), 0)
    };
  }, [bankAccounts, endDate, entries, incomeStatement, startDate]);

  const overviewExpenses = Number(reportSummary?.totalExpenses || 0);
  const liquidity = (accountBalances["11100"]?.amount || 0) + (accountBalances["11200"]?.amount || 0);
  const incomeChange = incomeStatement?.change.incomePercentage ?? null;
  const expenseChange = incomeStatement?.change.expensesPercentage ?? null;
  const netChange = incomeStatement?.change.netProfitPercentage ?? null;
  const formatReportAmount = (amount: number) =>
    incomeStatementLoading && !incomeStatement ? "Calculando..." : currencyFormatter.format(amount);
  const formatReportChange = (change: number | null) =>
    change === null ? "Sin base comparable" : `${change >= 0 ? "+" : ""}${change.toFixed(1)}% vs. anterior`;

  const overviewPriorities = [
    ...(overviewData.unbalancedEntries.length > 0
      ? [{
          id: "unbalanced",
          severity: "critical",
          title: `${overviewData.unbalancedEntries.length} póliza(s) descuadrada(s)`,
          detail: "Los cargos y abonos no coinciden y requieren revisión.",
          actionLabel: "Abrir libro diario",
          action: () => navigateAccounting("diario")
        }]
      : []),
    ...(liquidity < 0
      ? [{
          id: "negative-liquidity",
          severity: "critical",
          title: "Liquidez negativa",
          detail: `Caja y bancos presentan un saldo combinado de ${currencyFormatter.format(liquidity)}.`,
          actionLabel: "Revisar balanza",
          action: () => navigateAccounting("balanza")
        }]
      : []),
    ...(overviewData.creditAccountsDue.length > 0
      ? [{
          id: "credit-due",
          severity: "warning",
          title: `${overviewData.creditAccountsDue.length} pago(s) bancario(s) por cubrir`,
          detail: `${currencyFormatter.format(overviewData.creditDueAmount)} registrados como pago actual.`,
          actionLabel: "Ver cuentas",
          action: () => navigateAccounting("bancos")
        }]
      : []),
    ...(overviewData.periodEntries.length === 0
      ? [{
          id: "no-activity",
          severity: "info",
          title: "Sin movimientos en el periodo",
          detail: "Amplía el rango de fechas o registra una póliza para comenzar el análisis.",
          actionLabel: "Abrir libro diario",
          action: () => navigateAccounting("diario")
        }]
      : [])
  ];

  // --- Lógica del Formulario de Póliza Manual ---
  const handleLineAccountChange = (index: number, code: string) => {
    setManualLines((prev) => {
      const copy = [...prev];
      if (code === "CUSTOM") {
        copy[index] = { ...copy[index], accountCode: "", accountName: "", isCustom: true };
      } else {
        const selected = TYPICAL_ACCOUNTS.find((acc) => acc.code === code);
        copy[index] = {
          ...copy[index],
          accountCode: code,
          accountName: selected?.name || "",
          isCustom: false
        };
      }
      return copy;
    });
  };

  const updateLineValue = (index: number, field: "accountCode" | "accountName" | "debit" | "credit", value: string) => {
    setManualLines((prev) => {
      const copy = [...prev];
      copy[index] = { ...copy[index], [field]: value };

      // Limpiar el campo opuesto en la misma línea
      if (field === "debit" && value) {
        copy[index].credit = "";
      } else if (field === "credit" && value) {
        copy[index].debit = "";
      }

      // Si hay exactamente 2 líneas, auto-balancear la contrapartida en la otra línea
      if (copy.length === 2) {
        const otherIndex = index === 0 ? 1 : 0;
        if (field === "debit") {
          copy[otherIndex] = { ...copy[otherIndex], credit: value, debit: "" };
        } else if (field === "credit") {
          copy[otherIndex] = { ...copy[otherIndex], debit: value, credit: "" };
        }
      }

      return copy;
    });
  };

  const addManualLine = () => {
    setManualLines((prev) => [
      ...prev,
      { accountCode: "11100", accountName: "Caja Operativa", debit: "", credit: "", isCustom: false }
    ]);
  };

  const removeManualLine = (index: number) => {
    if (manualLines.length <= 2) return;
    setManualLines((prev) => prev.filter((_, i) => i !== index));
  };

  const manualTotals = useMemo(() => {
    const debits = manualLines.reduce((sum, line) => sum + (Number(line.debit) || 0), 0);
    const credits = manualLines.reduce((sum, line) => sum + (Number(line.credit) || 0), 0);
    return { debits, credits, difference: Math.abs(debits - credits) };
  }, [manualLines]);

  const canSaveManual = useMemo(() => {
    return (
      manualDescription.trim().length > 0 &&
      manualTotals.debits > 0 &&
      manualTotals.difference === 0 &&
      manualLines.every((line) => line.accountCode.trim() && line.accountName.trim() && (Number(line.debit) > 0 || Number(line.credit) > 0))
    );
  }, [manualDescription, manualTotals, manualLines]);

  const saveManualEntry = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!clinicId || !canSaveManual) return;
    setSaving(true);
    setError("");
    setStatus("");

    const reqLines: CreateJournalLineRequest[] = manualLines.map((line) => ({
      accountCode: line.accountCode.trim(),
      accountName: line.accountName.trim(),
      debit: Number(line.debit) || 0,
      credit: Number(line.credit) || 0
    }));

    try {
      await accountingApi.createJournalEntry(clinicId, {
        description: manualDescription.trim(),
        entryDate: manualDate,
        lines: reqLines
      });
      setStatus("Póliza registrada con éxito.");
      setIsModalOpen(false);
      // Resetear formulario
      setManualDescription("");
      setManualDate(new Date().toISOString().split("T")[0]);
      setManualLines([
        { accountCode: "11100", accountName: "Caja Operativa", debit: "", credit: "", isCustom: false },
        { accountCode: "41000", accountName: "Ingresos por Servicios Médicos", debit: "", credit: "", isCustom: false }
      ]);
      loadEntries();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  const openingBankDrafts = useMemo(() => {
    return openingBanks.filter((bank) =>
      bank.bankName.trim() ||
      bank.alias.trim() ||
      bank.accountLast4.trim() ||
      bank.currency.trim() !== "MXN" ||
      bank.accountType !== "DEBIT" ||
      toAmount(bank.openingBalance) > 0
    );
  }, [openingBanks]);

  const openingTotals = useMemo(() => {
    const cash = toAmount(openingCash);
    const inventory = toAmount(openingInventory);
    const banks = openingBankDrafts.reduce((sum, bank) => sum + toAmount(bank.openingBalance), 0);
    return { cash, inventory, banks, total: cash + inventory + banks };
  }, [openingCash, openingInventory, openingBankDrafts]);

  const canSaveOpening = useMemo(() => {
    return (
      openingDate.trim().length > 0 &&
      openingTotals.cash >= 0 &&
      openingTotals.inventory >= 0 &&
      openingTotals.total > 0 &&
      openingBankDrafts.every((bank) =>
        bank.bankName.trim().length > 0 &&
        bank.currency.trim().length === 3 &&
        (bank.accountType === "DEBIT" || bank.accountType === "CREDIT") &&
        Boolean(bank.accountKind) &&
        toAmount(bank.openingBalance) >= 0 &&
        (!bank.accountLast4.trim() || /^\d{4}$/.test(bank.accountLast4.trim()))
      )
    );
  }, [openingDate, openingTotals, openingBankDrafts]);

  const updateOpeningBank = (index: number, patch: Partial<OpeningBankDraft>) => {
    setOpeningBanks((prev) => prev.map((bank, itemIndex) => itemIndex === index ? { ...bank, ...patch } : bank));
  };

  const addOpeningBank = () => {
    setOpeningBanks((prev) => [...prev, blankOpeningBank()]);
  };

  const removeOpeningBank = (index: number) => {
    setOpeningBanks((prev) => prev.length <= 1 ? prev : prev.filter((_, itemIndex) => itemIndex !== index));
  };

  const saveOpeningBalances = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!clinicId || !canSaveOpening) return;

    setOpeningSaving(true);
    setError("");
    setStatus("");

    try {
      const created = await accountingApi.createOpeningBalances(clinicId, {
        entryDate: openingDate,
        cashOpeningAmount: openingTotals.cash,
        inventoryOpeningAmount: openingTotals.inventory,
        notes: openingNotes.trim() || null,
        bankAccounts: openingBankDrafts.map((bank) => ({
          bankName: bank.bankName.trim(),
          alias: bank.alias.trim(),
          accountLast4: bank.accountLast4.trim() || null,
          currency: bank.currency.trim().toUpperCase(),
          openingBalance: toAmount(bank.openingBalance),
          accountType: bank.accountType,
          accountKind: bank.accountKind
        }))
      });

      setOpeningSetup(created);
      setBankAccounts(created.bankAccounts);
      setOpeningBanks([blankOpeningBank()]);
      setOpeningCash("");
      setOpeningInventory("");
      setOpeningNotes("");
      setStatus("Saldos iniciales registrados con exito.");
      loadEntries();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setOpeningSaving(false);
    }
  };

  const selectedBankAccount = useMemo(
    () => bankAccounts.find((account) => account.id === selectedBankAccountId) ?? null,
    [bankAccounts, selectedBankAccountId]
  );

  useEffect(() => {
    if (!selectedBankAccount) {
      setShowBankEditForm(false);
      return;
    }
    setBankEditForm({
      bankName: selectedBankAccount.bankName,
      alias: selectedBankAccount.alias,
      accountLast4: selectedBankAccount.accountLast4 ?? "",
      currency: selectedBankAccount.currency,
      accountType: selectedBankAccount.accountType,
      accountKind: selectedBankAccount.accountKind ?? "BANK",
      creditCutoffDate: selectedBankAccount.creditCutoffDate ?? "",
      creditPaymentDueDate: selectedBankAccount.creditPaymentDueDate ?? "",
      creditLimit: selectedBankAccount.creditLimit != null ? String(selectedBankAccount.creditLimit) : "",
      creditCurrentAmount: selectedBankAccount.creditCurrentAmount != null ? String(selectedBankAccount.creditCurrentAmount) : "",
      creditMinimumPayment: selectedBankAccount.creditMinimumPayment != null ? String(selectedBankAccount.creditMinimumPayment) : "",
      creditNoInterestPayment: selectedBankAccount.creditNoInterestPayment != null ? String(selectedBankAccount.creditNoInterestPayment) : "",
      creditCurrentPaymentDue: selectedBankAccount.creditCurrentPaymentDue != null ? String(selectedBankAccount.creditCurrentPaymentDue) : "",
      reason: ""
    });
    setShowBankEditForm(false);
  }, [selectedBankAccount?.id]);

  const selectedBankBalance = useMemo(() => {
    return bankMovements.length > 0
      ? Number(bankMovements[bankMovements.length - 1].balanceAfter || 0)
      : Number(selectedBankAccount?.openingBalance || 0);
  }, [bankMovements, selectedBankAccount]);

  const canSaveBankAccount = useMemo(() => {
    return (
      bankForm.bankName.trim().length > 0 &&
      bankForm.alias.trim().length > 0 &&
      bankForm.currency.trim().length === 3 &&
      (bankForm.accountType === "DEBIT" || bankForm.accountType === "CREDIT") &&
      Boolean(bankForm.accountKind) &&
      toAmount(bankForm.openingBalance) >= 0 &&
      (!bankForm.accountLast4.trim() || /^\d{4}$/.test(bankForm.accountLast4.trim())) &&
      bankForm.openingDate.trim().length > 0
    );
  }, [bankForm]);

  const canSaveBankEdit = useMemo(() => {
    return (
      selectedBankAccount?.active === true &&
      bankEditForm.bankName.trim().length > 0 &&
      bankEditForm.alias.trim().length > 0 &&
      bankEditForm.currency.trim().length === 3 &&
      (bankEditForm.accountType === "DEBIT" || bankEditForm.accountType === "CREDIT") &&
      Boolean(bankEditForm.accountKind) &&
      (!bankEditForm.accountLast4.trim() || /^\d{4}$/.test(bankEditForm.accountLast4.trim())) &&
      (
        bankEditForm.accountType !== "CREDIT" ||
        (
          isOptionalNonNegative(bankEditForm.creditLimit) &&
          isOptionalNonNegative(bankEditForm.creditCurrentAmount) &&
          isOptionalNonNegative(bankEditForm.creditMinimumPayment) &&
          isOptionalNonNegative(bankEditForm.creditNoInterestPayment) &&
          isOptionalNonNegative(bankEditForm.creditCurrentPaymentDue)
        )
      ) &&
      bankEditForm.reason.trim().length > 0
    );
  }, [bankEditForm, selectedBankAccount]);

  const resetBankForm = () => {
    setBankForm({
      bankName: "",
      alias: "",
      accountLast4: "",
      currency: "MXN",
      openingBalance: "",
      accountType: "DEBIT",
      accountKind: "BANK",
      openingDate: todayIso(),
      creditCutoffDate: "",
      creditPaymentDueDate: "",
      creditLimit: "",
      creditCurrentAmount: "",
      creditMinimumPayment: "",
      creditNoInterestPayment: "",
      creditCurrentPaymentDue: "",
      notes: ""
    });
  };

  const saveBankAccount = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!clinicId || !canSaveBankAccount) return;

    setBankActionSaving(true);
    setError("");
    setStatus("");

    try {
      const created = await accountingApi.createBankAccount(clinicId, {
        bankName: bankForm.bankName.trim(),
        alias: bankForm.alias.trim(),
        accountLast4: bankForm.accountLast4.trim() || null,
        currency: bankForm.currency.trim().toUpperCase(),
        openingBalance: toAmount(bankForm.openingBalance),
        accountType: bankForm.accountType,
        accountKind: bankForm.accountKind,
        openingDate: bankForm.openingDate,
        creditCutoffDate: bankForm.accountType === "CREDIT" ? bankForm.creditCutoffDate || null : null,
        creditPaymentDueDate: bankForm.accountType === "CREDIT" ? bankForm.creditPaymentDueDate || null : null,
        creditLimit: bankForm.accountType === "CREDIT" ? toOptionalAmount(bankForm.creditLimit) : null,
        creditCurrentAmount: bankForm.accountType === "CREDIT" ? toOptionalAmount(bankForm.creditCurrentAmount) : null,
        creditMinimumPayment: bankForm.accountType === "CREDIT" ? toOptionalAmount(bankForm.creditMinimumPayment) : null,
        creditNoInterestPayment: bankForm.accountType === "CREDIT" ? toOptionalAmount(bankForm.creditNoInterestPayment) : null,
        creditCurrentPaymentDue: bankForm.accountType === "CREDIT" ? toOptionalAmount(bankForm.creditCurrentPaymentDue) : null,
        notes: bankForm.notes.trim() || null
      });

      setBankAccounts((prev) => [created, ...prev.filter((account) => account.id !== created.id)]);
      navigateAccounting("bancos", created.id);
      setShowBankForm(false);
      resetBankForm();
      setStatus("Cuenta bancaria registrada con poliza de apertura.");
      loadEntries();
      loadOpeningData();
      loadBankMovements(created.id);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBankActionSaving(false);
    }
  };

  const saveBankAccountChanges = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!clinicId || !selectedBankAccount || !canSaveBankEdit) return;

    setBankActionSaving(true);
    setError("");
    setStatus("");

    try {
      const creditPayload = bankEditForm.accountType === "CREDIT"
        ? {
            creditCutoffDate: bankEditForm.creditCutoffDate || null,
            creditPaymentDueDate: bankEditForm.creditPaymentDueDate || null,
            creditLimit: toOptionalAmount(bankEditForm.creditLimit),
            creditCurrentAmount: toOptionalAmount(bankEditForm.creditCurrentAmount),
            creditMinimumPayment: toOptionalAmount(bankEditForm.creditMinimumPayment),
            creditNoInterestPayment: toOptionalAmount(bankEditForm.creditNoInterestPayment),
            creditCurrentPaymentDue: toOptionalAmount(bankEditForm.creditCurrentPaymentDue)
          }
        : {
            creditCutoffDate: null,
            creditPaymentDueDate: null,
            creditLimit: null,
            creditCurrentAmount: null,
            creditMinimumPayment: null,
            creditNoInterestPayment: null,
            creditCurrentPaymentDue: null
          };
      const updated = await accountingApi.updateBankAccount(clinicId, selectedBankAccount.id, {
        bankName: bankEditForm.bankName.trim(),
        alias: bankEditForm.alias.trim(),
        accountLast4: bankEditForm.accountLast4.trim() || null,
        currency: bankEditForm.currency.trim().toUpperCase(),
        accountType: bankEditForm.accountType,
        accountKind: bankEditForm.accountKind,
        ...creditPayload,
        reason: bankEditForm.reason.trim()
      });

      setBankAccounts((prev) => prev.map((account) => account.id === updated.id ? updated : account));
      setShowBankEditForm(false);
      setBankEditForm({
        bankName: updated.bankName,
        alias: updated.alias,
         accountLast4: updated.accountLast4 ?? "",
         currency: updated.currency,
         accountType: updated.accountType,
         accountKind: updated.accountKind ?? "BANK",
        creditCutoffDate: updated.creditCutoffDate ?? "",
        creditPaymentDueDate: updated.creditPaymentDueDate ?? "",
        creditLimit: updated.creditLimit != null ? String(updated.creditLimit) : "",
        creditCurrentAmount: updated.creditCurrentAmount != null ? String(updated.creditCurrentAmount) : "",
        creditMinimumPayment: updated.creditMinimumPayment != null ? String(updated.creditMinimumPayment) : "",
        creditNoInterestPayment: updated.creditNoInterestPayment != null ? String(updated.creditNoInterestPayment) : "",
        creditCurrentPaymentDue: updated.creditCurrentPaymentDue != null ? String(updated.creditCurrentPaymentDue) : "",
        reason: ""
      });
      setStatus("Datos de la cuenta bancaria actualizados.");
      loadBankMovements(updated.id);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBankActionSaving(false);
    }
  };

  const saveAccountTransfer = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!clinicId || !selectedBankAccount || !transferDestinationId || toAmount(transferAmount) <= 0) return;
    setBankActionSaving(true);
    setError("");
    setStatus("");
    try {
      await accountingApi.transferFunds(clinicId, {
        sourceAccountId: selectedBankAccount.id,
        destinationAccountId: transferDestinationId,
        amount: toAmount(transferAmount),
        entryDate: transferDate,
        description: transferDescription.trim() || null
      });
      setTransferAmount("");
      setTransferDestinationId("");
      setTransferDescription("");
      setStatus("Transferencia registrada en contabilidad.");
      loadEntries();
      loadOpeningData();
      loadBankMovements(selectedBankAccount.id);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBankActionSaving(false);
    }
  };

  const saveBankCorrection = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!clinicId || !selectedBankAccount || !correctionReason.trim() || !correctionDate || !correctionBalance) return;

    setBankActionSaving(true);
    setError("");
    setStatus("");

    try {
      await accountingApi.correctBankAccountBalance(clinicId, selectedBankAccount.id, {
        entryDate: correctionDate,
        correctedBalance: toAmount(correctionBalance),
        reason: correctionReason.trim()
      });

      setCorrectionBalance("");
      setCorrectionReason("");
      setStatus("Poliza de correccion bancaria registrada.");
      loadEntries();
      loadBankMovements(selectedBankAccount.id);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBankActionSaving(false);
    }
  };

  const deactivateSelectedBankAccount = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!clinicId || !selectedBankAccount || !deactivationReason.trim() || !deactivationDate) return;

    const confirmed = window.confirm("La cuenta se dara de baja y se conservara su historial contable. Deseas continuar?");
    if (!confirmed) return;

    setBankActionSaving(true);
    setError("");
    setStatus("");

    try {
      const updated = await accountingApi.deactivateBankAccount(clinicId, selectedBankAccount.id, {
        entryDate: deactivationDate,
        reason: deactivationReason.trim()
      });

      setBankAccounts((prev) => prev.map((account) => account.id === updated.id ? updated : account));
      setDeactivationReason("");
      setStatus("Cuenta bancaria eliminada con baja contable.");
      loadEntries();
      loadBankMovements(updated.id);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBankActionSaving(false);
    }
  };

  // --- Lógica de Exportación a CSV ---
  const handleExportDiario = () => {
    const headers = ["Fecha", "Tipo Evento", "Descripción", "Código Cuenta", "Nombre Cuenta", "Debe", "Haber"];
    const rows: string[][] = [];

    filteredEntries.forEach((entry) => {
      entry.lines.forEach((line) => {
        rows.push([
          entry.entryDate,
          eventLabels[entry.sourceEventType] || entry.sourceEventType,
          `"${entry.description.replace(/"/g, '""')}"`,
          line.accountCode,
          `"${line.accountName.replace(/"/g, '""')}"`,
          line.debit ? String(line.debit) : "0",
          line.credit ? String(line.credit) : "0"
        ]);
      });
    });

    downloadCsv("libro_diario.csv", headers, rows);
  };

  const handleExportBalanza = () => {
    const headers = ["Código Cuenta", "Nombre Cuenta", "Saldo Inicial", "Cargos (Debe)", "Abonos (Haber)", "Saldo Final"];
    const rows = balanzaData.map((row) => [
      row.code,
      `"${row.name.replace(/"/g, '""')}"`,
      String(row.initialBalance),
      String(row.debit),
      String(row.credit),
      String(row.finalBalance)
    ]);

    downloadCsv("balanza_comprobacion.csv", headers, rows);
  };

  const downloadCsv = (filename: string, headers: string[], rows: string[][]) => {
    const csvContent = "\uFEFF" + [headers.join(","), ...rows.map(e => e.join(","))].join("\n");
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", filename);
    link.style.visibility = "hidden";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const renderOpeningSetupForm = (required = false) => (
    <form
      className={`profile-form opening-setup-form ${required ? "opening-setup-form-required" : ""}`}
      onSubmit={saveOpeningBalances}
    >
      <div className="opening-setup-grid">
        <label className="field">
          <span>Fecha contable</span>
          <input type="date" value={openingDate} onChange={(event) => setOpeningDate(event.target.value)} required />
        </label>
        <label className="field">
          <span>Efectivo en caja</span>
          <input type="number" min="0" step="0.01" value={openingCash} onChange={(event) => setOpeningCash(event.target.value)} placeholder="0.00" />
        </label>
        <label className="field">
          <span>Inventario inicial</span>
          <input type="number" min="0" step="0.01" value={openingInventory} onChange={(event) => setOpeningInventory(event.target.value)} placeholder="0.00" />
        </label>
      </div>

      <div className="opening-bank-section">
        <div className="opening-bank-heading">
          <strong>Cuentas bancarias</strong>
          <button className="btn secondary" type="button" onClick={addOpeningBank}>
            <IconPlus size={14} />
            Agregar banco
          </button>
        </div>

        <div className="table-wrapper">
          <table className="data-table no-row-click opening-bank-table">
            <thead>
              <tr>
                <th>Banco</th>
                <th>Alias</th>
                <th>Tipo</th>
                <th>Ultimos 4</th>
                <th>Moneda</th>
                <th style={{ textAlign: "right" }}>Saldo inicial</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {openingBanks.map((bank, index) => (
                <tr key={index}>
                  <td>
                    <input value={bank.bankName} onChange={(event) => updateOpeningBank(index, { bankName: event.target.value })} placeholder="BBVA" />
                  </td>
                  <td>
                    <input value={bank.alias} onChange={(event) => updateOpeningBank(index, { alias: event.target.value })} placeholder="Cuenta principal" />
                  </td>
                  <td>
                    <select value={bank.accountType} onChange={(event) => updateOpeningBank(index, { accountType: event.target.value as BankAccountType })}>
                      <option value="DEBIT">Debito</option>
                      <option value="CREDIT">Credito</option>
                    </select>
                  </td>
                  <td>
                    <input maxLength={4} value={bank.accountLast4} onChange={(event) => updateOpeningBank(index, { accountLast4: event.target.value.replace(/\D/g, "").slice(0, 4) })} placeholder="1234" />
                  </td>
                  <td>
                    <input maxLength={3} value={bank.currency} onChange={(event) => updateOpeningBank(index, { currency: event.target.value.toUpperCase().slice(0, 3) })} />
                  </td>
                  <td>
                    <input type="number" min="0" step="0.01" value={bank.openingBalance} onChange={(event) => updateOpeningBank(index, { openingBalance: event.target.value })} placeholder="0.00" style={{ textAlign: "right" }} />
                  </td>
                  <td style={{ width: "44px" }}>
                    <button className="icon-btn text-error" type="button" disabled={openingBanks.length <= 1} onClick={() => removeOpeningBank(index)} aria-label="Eliminar banco">
                      <IconTrash size={16} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <label className="field">
        <span>Notas</span>
        <input value={openingNotes} onChange={(event) => setOpeningNotes(event.target.value)} placeholder="Apertura inicial del consultorio" />
      </label>

      <div className="opening-totals-bar">
        <div className="opening-totals">
          <div>
            <span>Debe</span>
            <strong>{currencyFormatter.format(openingTotals.total)}</strong>
          </div>
          <div>
            <span>Haber capital inicial</span>
            <strong>{currencyFormatter.format(openingTotals.total)}</strong>
          </div>
        </div>
        <button className="btn primary" type="submit" disabled={!canSaveOpening || openingSaving}>
          {openingSaving ? "Guardando..." : "Registrar saldos iniciales"}
        </button>
      </div>
    </form>
  );

  const renderAccountingTopActions = () => {
    if (tab === "reportes" || tab === "mermas") {
      return (
        <div className="accounting-top-actions accounting-top-actions-period accounting-overview-period">
          <IconCalendarStats size={17} aria-hidden="true" />
          <div className="accounting-period-controls">
            <span>Periodo del</span>
            <input aria-label="Inicio del periodo" type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
            <span>al</span>
            <input aria-label="Fin del periodo" type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} />
          </div>
        </div>
      );
    }

    if (tab === "diario") {
      return (
        <div className="accounting-top-actions accounting-top-actions-diario">
          <div className="accounting-search-field">
            <IconSearch size={18} aria-hidden="true" />
            <input
              placeholder="Buscar póliza por descripción, cuenta o código..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <select
            value={selectedEvent}
            onChange={(event) => setSelectedEvent(event.target.value)}
            aria-label="Filtrar por evento"
          >
            <option value="ALL">Todos los eventos</option>
            <option value="PagoRegistrado">Cobro en Caja</option>
            <option value="ConsumoConciliado">Consumos de Consulta</option>
            <option value="MermaCaducidad">Mermas y Caducidad</option>
            <option value="SaldoInicial">Saldos Iniciales</option>
            <option value="CuentaBancariaAlta">Altas bancarias</option>
            <option value="CuentaBancariaCorreccion">Correcciones bancarias</option>
            <option value="CuentaBancariaBaja">Bajas bancarias</option>
            <option value="Manual">Pólizas Manuales</option>
          </select>
          <button className="btn secondary" type="button" onClick={handleExportDiario} disabled={filteredEntries.length === 0}>
            <IconDownload size={16} />
            Exportar Diario
          </button>
          <button className="btn primary" type="button" onClick={() => setIsModalOpen(true)}>
            <IconPlus size={16} />
            Nueva Póliza
          </button>
        </div>
      );
    }

    if (tab === "bancos") {
      return (
        <div className="accounting-top-actions accounting-top-actions-compact">
          {selectedBankAccountId && (
            <button className="btn ghost" type="button" onClick={() => navigateAccounting("bancos")}>
              Volver a cuentas
            </button>
          )}
          {!selectedBankAccountId && (
            <button className="btn secondary" type="button" onClick={() => setShowBankForm((current) => !current)}>
              <IconPlus size={14} />
              Nueva cuenta
            </button>
          )}
          {selectedBankAccount?.active && (
            <button className="btn secondary" type="button" onClick={() => setShowBankEditForm((current) => !current)}>
              Modificar datos
            </button>
          )}
        </div>
      );
    }

    if (tab === "balanza") {
      return (
        <div className="accounting-top-actions accounting-top-actions-period">
          <div className="accounting-view-toggle" role="group" aria-label="Vista de balanza">
            <button className={balanzaView === "tabla" ? "active" : ""} type="button" onClick={() => setBalanzaView("tabla")}>
              Tabla
            </button>
            <button className={balanzaView === "cuentasT" ? "active" : ""} type="button" onClick={() => setBalanzaView("cuentasT")}>
              Cuentas T
            </button>
          </div>
          <div className="accounting-period-controls">
            <span>Periodo contable del</span>
            <input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
            <span>al</span>
            <input type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} />
          </div>
          <button className="btn secondary" type="button" onClick={handleExportBalanza} disabled={balanzaData.length === 0}>
            <IconDownload size={16} />
            Exportar Balanza
          </button>
        </div>
      );
    }

    if (tab === "resultados") {
      return (
        <div className="accounting-top-actions accounting-top-actions-period">
          <div className="accounting-period-controls">
            <span>Periodo contable del</span>
            <input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
            <span>al</span>
            <input type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} />
          </div>
        </div>
      );
    }

    return null;
  };

  const renderAccountingTopMenu = () => (
    <section className="accounting-top-menu" aria-label="Menú de contabilidad">
      <div className="accounting-top-tabs" role="tablist" aria-label="Vistas de contabilidad">
        {accountingTabs.map((item) => (
          <button
            key={item.key}
            className={tab === item.key ? "active" : ""}
            type="button"
            role="tab"
            aria-selected={tab === item.key}
            onClick={() => navigateAccounting(item.key)}
          >
            {item.label}
          </button>
        ))}
      </div>
      {renderAccountingTopActions()}
    </section>
  );

  if (!hasClinic) {
    return (
      <section className="dashboard-grid">
        <article className="panel full">
          <div className="clinic-list">
            <div className="clinic-row">
              <strong>Completa los datos de tu clínica</strong>
              <span>Necesitas una clínica activa para poder gestionar la contabilidad automatizada.</span>
            </div>
          </div>
        </article>
      </section>
    );
  }

  if (openingLoading) {
    return (
      <div className="accounting-screen">
        {error && <div className="toast error">{error}</div>}
        <section className="module-welcome accounting-required-setup">
          <div className="module-welcome-main accounting-loading-panel">
            <div className="module-welcome-intro">
              <span className="module-welcome-badge">Configuracion requerida</span>
              <div className="module-welcome-icon">
                <IconLoader2 className="animate-spin" size={34} strokeWidth={1.8} aria-hidden="true" />
              </div>
              <div className="module-welcome-copy">
                <h2>Preparando contabilidad</h2>
                <p>Estamos revisando si la clinica ya tiene saldos iniciales registrados.</p>
              </div>
            </div>
          </div>
        </section>
      </div>
    );
  }

  if (!openingSetup) {
    const accountingWelcome = moduleOnboardingCopy.contabilidad;

    return (
      <div className="accounting-screen">
        {error && <div className="toast error">{error}</div>}
        {status && <div className="toast success">{status}</div>}
        <section className="module-welcome accounting-required-setup" aria-labelledby="accounting-opening-title">
          <div className="module-welcome-main accounting-required-main">
            <div className="module-welcome-intro accounting-required-intro">
              <span className="module-welcome-badge">{accountingWelcome.eyebrow}</span>
              <div className="module-welcome-icon">
                <IconReportMoney size={34} strokeWidth={1.8} aria-hidden="true" />
              </div>
              <div className="module-welcome-copy">
                <h2 id="accounting-opening-title">{accountingWelcome.title}</h2>
                <p>{accountingWelcome.description}</p>
              </div>
              <div className="module-welcome-details accounting-required-details">
                <div className="module-welcome-section">
                  <h3>Configuracion inicial</h3>
                  <ol>
                    {accountingWelcome.setupSteps.map((item) => (
                      <li key={item}>{item}</li>
                    ))}
                  </ol>
                </div>
              </div>
            </div>

            <div className="accounting-opening-setup-panel">
              <div className="accounting-opening-setup-heading">
                <div>
                  <h3>Saldos iniciales</h3>
                  <p>Este modulo se libera cuando registres la apertura contable de la clinica.</p>
                </div>
                <span className="badge warning">Pendiente</span>
              </div>
              {renderOpeningSetupForm(true)}
            </div>
          </div>
        </section>
      </div>
    );
  }

  return (
    <div className="accounting-screen">
      {error && <div className="toast error">{error}</div>}
      {status && <div className="toast success">{status}</div>}
      {renderAccountingTopMenu()}

      <div className="accounting-active-view">
      {/* VISTA DE REPORTES GENERALES */}
      {tab === "reportes" && (
      <section className="accounting-overview-section" aria-label="Resumen contable del periodo">
        {incomeStatementError && (
          <div className="accounting-report-notice is-error" role="alert">
            <IconAlertTriangle size={18} />
            <span><strong>No se pudo calcular el reporte financiero.</strong> {incomeStatementError}</span>
          </div>
        )}
        <div className="accounting-kpi-grid">
          <button className="accounting-kpi" type="button" onClick={() => navigateAccounting("resultados")}>
            <span className="accounting-kpi-icon is-income"><IconTrendingUp size={20} aria-hidden="true" /></span>
            <span className="accounting-kpi-copy"><small>Ingresos del periodo</small><strong>{formatReportAmount(resultadosData.ingresos)}</strong><span>{incomeChange === null ? "Sin periodo comparable" : `${incomeChange >= 0 ? "+" : ""}${incomeChange.toFixed(1)}% vs. periodo anterior`}</span></span>
          </button>
          <button className="accounting-kpi" type="button" onClick={() => navigateAccounting("resultados")}>
            <span className="accounting-kpi-icon is-expense"><IconTrendingDown size={20} aria-hidden="true" /></span>
            <span className="accounting-kpi-copy"><small>Costos y gastos</small><strong>{formatReportAmount(overviewExpenses)}</strong><span>{expenseChange === null ? "Sin periodo comparable" : `${expenseChange >= 0 ? "+" : ""}${expenseChange.toFixed(1)}% vs. periodo anterior`}</span></span>
          </button>
          <button className="accounting-kpi" type="button" onClick={() => navigateAccounting("resultados")}>
            <span className="accounting-kpi-icon is-result"><IconScale size={20} aria-hidden="true" /></span>
            <span className="accounting-kpi-copy"><small>Resultado neto</small><strong className={resultadosData.utilidadNeta < 0 ? "text-error" : ""}>{formatReportAmount(resultadosData.utilidadNeta)}</strong><span>{netChange === null ? `Margen ${resultadosData.margenUtilidad.toFixed(1)}%` : `${netChange >= 0 ? "+" : ""}${netChange.toFixed(1)}% vs. periodo anterior`}</span></span>
          </button>
          <button className="accounting-kpi" type="button" onClick={() => navigateAccounting("balanza")}>
            <span className="accounting-kpi-icon is-liquidity"><IconCash size={20} aria-hidden="true" /></span>
            <span className="accounting-kpi-copy"><small>Liquidez disponible</small><strong className={liquidity < 0 ? "text-error" : ""}>{currencyFormatter.format(liquidity)}</strong><span>Caja y bancos acumulados</span></span>
          </button>
        </div>

        <div className="accounting-overview-main-grid">
          <article className="panel accounting-trend-panel">
            <div className="accounting-overview-heading">
              <div><h2>Ingresos y gastos</h2><p>Comportamiento dentro del periodo seleccionado.</p></div>
              <div className="accounting-chart-legend" aria-label="Leyenda"><span><i className="is-income" /> Ingresos</span><span><i className="is-expense" /> Gastos</span></div>
            </div>
            {incomeStatementLoading && !incomeStatement ? (
              <div className="accounting-overview-empty"><IconLoader2 className="spin" size={28} aria-hidden="true" /><p>Calculando tendencia del periodo...</p></div>
            ) : overviewData.periodEntries.length === 0 ? (
              <div className="accounting-overview-empty"><IconCalendarStats size={28} aria-hidden="true" /><p>No hay movimientos para graficar en este periodo.</p></div>
            ) : (
              <div className="accounting-bar-chart" aria-label="Comparación de ingresos y gastos">
                {overviewData.buckets.map((bucket) => (
                  <div className="accounting-chart-column" key={bucket.label}>
                    <div className="accounting-chart-bars">
                      <span className="is-income" title={`Ingresos: ${currencyFormatter.format(bucket.income)}`} style={{ height: bucket.income > 0 ? `${Math.max(3, (bucket.income / overviewData.maxTrendAmount) * 100)}%` : 0 }} />
                      <span className="is-expense" title={`Gastos: ${currencyFormatter.format(bucket.expenses)}`} style={{ height: bucket.expenses > 0 ? `${Math.max(3, (bucket.expenses / overviewData.maxTrendAmount) * 100)}%` : 0 }} />
                    </div>
                    <small>{bucket.label}</small>
                  </div>
                ))}
              </div>
            )}
          </article>

          <aside className="panel accounting-attention-panel" aria-labelledby="accounting-attention-title">
            <div className="accounting-overview-heading"><div><h2 id="accounting-attention-title">Requiere atención</h2><p>Excepciones detectadas con los datos disponibles.</p></div>{overviewPriorities.length > 0 && <span className="accounting-attention-count">{overviewPriorities.length}</span>}</div>
            <div className="accounting-attention-list">
              {overviewPriorities.map((priority) => (
                <article className={`accounting-attention-item is-${priority.severity}`} key={priority.id}>
                  <IconAlertTriangle size={17} aria-hidden="true" />
                  <div><strong>{priority.title}</strong><p>{priority.detail}</p><button type="button" onClick={priority.action}>{priority.actionLabel} <IconArrowRight size={14} /></button></div>
                </article>
              ))}
              {overviewPriorities.length === 0 && <div className="accounting-all-clear"><IconScale size={20} aria-hidden="true" /><div><strong>Sin excepciones detectadas</strong><p>Las pólizas del periodo están cuadradas y no hay saldos críticos.</p></div></div>}
            </div>
          </aside>
        </div>

        <div className="accounting-overview-detail-grid">
          <article className="panel accounting-recent-panel">
            <div className="accounting-overview-heading"><div><h2>Movimientos recientes</h2><p>Últimas pólizas dentro del periodo.</p></div><button className="btn ghost" type="button" onClick={() => navigateAccounting("diario")}>Ver libro diario <IconArrowRight size={15} /></button></div>
            {overviewData.recentEntries.length === 0 ? <div className="accounting-inline-empty">No hay movimientos registrados.</div> : (
              <div className="accounting-recent-list">
                {overviewData.recentEntries.map((entry) => {
                  const total = entry.lines.reduce((sum, line) => sum + Number(line.debit || 0), 0);
                  return <button type="button" key={entry.id} onClick={() => navigateAccounting("diario")}><span className="accounting-recent-date">{new Date(`${entry.entryDate}T00:00:00`).toLocaleDateString("es-MX", { day: "2-digit", month: "short" })}</span><span><strong>{entry.description}</strong><small>{eventLabels[entry.sourceEventType] ?? entry.sourceEventType}</small></span><strong>{currencyFormatter.format(total)}</strong><IconArrowRight size={15} aria-hidden="true" /></button>;
                })}
              </div>
            )}
          </article>

          <article className="panel accounting-position-panel">
            <div className="accounting-overview-heading"><div><h2>Posición financiera</h2><p>Composición de activos disponibles.</p></div></div>
            <div className="accounting-position-list">
              <button type="button" onClick={() => navigateAccounting("balanza")}><span><IconCash size={17} /> Caja operativa</span><strong>{currencyFormatter.format(accountBalances["11100"]?.amount || 0)}</strong></button>
              <button type="button" onClick={() => navigateAccounting("bancos")}><span><IconBuildingBank size={17} /> Bancos</span><strong>{currencyFormatter.format(accountBalances["11200"]?.amount || 0)}</strong></button>
              <button type="button" onClick={() => navigateAccounting("balanza")}><span><IconPackage size={17} /> Inventario</span><strong>{currencyFormatter.format(accountBalances["12100"]?.amount || 0)}</strong></button>
              <div className="accounting-position-total"><span>Activos mostrados</span><strong>{currencyFormatter.format(liquidity + (accountBalances["12100"]?.amount || 0))}</strong></div>
            </div>
          </article>
        </div>
      </section>
      )}

      {/* VISTA DE SALDOS INICIALES */}
      {tab === "saldos" && (
      <section className="panel accounting-opening-summary accounting-workspace-panel">
        <div className="panel-heading" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div>
            <h2>Saldos iniciales</h2>
            <p style={{ margin: 0, fontSize: "12px", color: "var(--color-text-3)" }}>
              Apertura contable del consultorio
            </p>
          </div>
          {openingSetup && (
            <span className="badge success">
              Registrados
            </span>
          )}
        </div>

        {openingLoading ? (
          <div style={{ padding: "var(--space-5)", display: "flex", alignItems: "center", gap: "var(--space-2)", color: "var(--color-text-2)" }}>
            <IconLoader2 className="animate-spin" size={18} />
            <span>Cargando saldos iniciales...</span>
          </div>
        ) : openingSetup ? (
          <div style={{ padding: "var(--space-5)", display: "grid", gap: "var(--space-4)" }}>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(4, minmax(150px, 1fr))", gap: "var(--space-3)" }}>
              <div>
                <span style={{ display: "block", fontSize: "11px", color: "var(--color-text-3)" }}>Fecha</span>
                <strong>{openingSetup.entryDate}</strong>
              </div>
              <div>
                <span style={{ display: "block", fontSize: "11px", color: "var(--color-text-3)" }}>Caja</span>
                <strong>{currencyFormatter.format(openingSetup.cashOpeningAmount)}</strong>
              </div>
              <div>
                <span style={{ display: "block", fontSize: "11px", color: "var(--color-text-3)" }}>Bancos</span>
                <strong>{currencyFormatter.format(bankAccounts.reduce((sum, account) => sum + Number(account.openingBalance || 0), 0))}</strong>
              </div>
              <div>
                <span style={{ display: "block", fontSize: "11px", color: "var(--color-text-3)" }}>Total apertura</span>
                <strong>{currencyFormatter.format(openingSetup.totalOpeningAssets)}</strong>
              </div>
            </div>

            {bankAccounts.length > 0 && (
              <div className="table-wrapper">
                <table className="data-table no-row-click" style={{ margin: 0 }}>
                  <thead>
                    <tr>
                      <th>Banco</th>
                      <th>Alias</th>
                      <th>Tipo</th>
                      <th>Cuenta</th>
                      <th>Moneda</th>
                      <th style={{ textAlign: "right" }}>Saldo inicial</th>
                    </tr>
                  </thead>
                  <tbody>
                    {bankAccounts.map((account) => (
                      <tr key={account.id}>
                        <td>{account.bankName}</td>
                        <td>{account.alias}</td>
                        <td>
                          <span className={`badge ${account.accountType === "CREDIT" ? "warning" : "success"}`}>
                            {bankTypeLabel(account.accountType)}
                          </span>
                        </td>
                        <td>{account.accountLast4 ? `****${account.accountLast4}` : "Sin terminacion"}</td>
                        <td>{account.currency}</td>
                        <td style={{ textAlign: "right", fontWeight: 600 }}>{currencyFormatter.format(account.openingBalance)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div style={{ display: "flex", justifyContent: "space-between", gap: "var(--space-3)", alignItems: "center", fontSize: "12px", color: "var(--color-text-3)" }}>
              <span>Poliza: <span style={{ fontFamily: "monospace" }}>{openingSetup.journalEntryId}</span></span>
              <span>{openingSetup.capitalAccountCode} · {openingSetup.capitalAccountName}</span>
            </div>
          </div>
        ) : (
          renderOpeningSetupForm()
        )}
      </section>
      )}

        {/* VISTA DE LIBRO DIARIO */}
        {tab === "diario" && (
          <>
            <section className="panel accounting-workspace-panel">
            <div className="panel-heading" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <h2>Registro de Pólizas Diario</h2>
              <div style={{ display: "flex", gap: "var(--space-2)", alignItems: "center" }}>
                <span className="badge neutral" style={{ marginRight: "var(--space-2)" }}>
                  {journalQuery?.totalEntries ?? filteredEntries.length} pólizas · Total: {currencyFormatter.format(filteredTotals)}
                </span>
              </div>
            </div>

            {loading || journalQueryLoading ? (
              <div style={{ padding: "var(--space-8)", display: "flex", justifyContent: "center", alignItems: "center", gap: "var(--space-2)", color: "var(--color-text-2)" }}>
                <IconLoader2 className="animate-spin" size={20} />
                <span>Cargando libro diario...</span>
              </div>
            ) : filteredEntries.length === 0 ? (
              <div style={{ padding: "var(--space-8)", textAlign: "center", color: "var(--color-text-2)" }}>
                <IconInfoCircle size={32} style={{ color: "var(--color-text-3)", marginBottom: "var(--space-2)" }} />
                <h3 style={{ margin: "0 0 4px" }}>No hay registros en el diario</h3>
                <p style={{ margin: 0, fontSize: "13px" }}>
                  Intenta cambiar los filtros de búsqueda o registra una nueva póliza manual.
                </p>
              </div>
            ) : (
              <div className="accounting-entries-list" style={{ padding: "var(--space-4)", display: "flex", flexDirection: "column", gap: "var(--space-3)" }}>
                {filteredEntries.map((entry) => {
                  const isExpanded = expandedEntries[entry.id];
                  const entryTotal = entry.totalDebit;

                  return (
                    <div
                      key={entry.id}
                      className="accounting-entry-card"
                      style={{
                        border: "1px solid var(--color-border)",
                        borderRadius: "6px",
                        overflow: "hidden",
                        backgroundColor: "var(--color-card)",
                        transition: "box-shadow 0.2s ease"
                      }}
                    >
                      <div
                        onClick={() => toggleExpand(entry.id)}
                        style={{
                          padding: "var(--space-3) var(--space-4)",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "space-between",
                          cursor: "pointer",
                          backgroundColor: isExpanded ? "var(--color-surface)" : "transparent",
                          transition: "background-color 0.2s ease"
                        }}
                      >
                        <div style={{ display: "flex", alignItems: "center", gap: "var(--space-3)", flex: 1, minWidth: 0 }}>
                          <span className={`badge ${eventBadgeTypes[entry.sourceEventType] || "neutral"}`} style={{ fontSize: "11px", flexShrink: 0 }}>
                            {eventLabels[entry.sourceEventType] || entry.sourceEventType}
                          </span>
                          <div style={{ minWidth: 0 }}>
                            <strong style={{ display: "block", fontSize: "14px", color: "var(--color-text-1)", textOverflow: "ellipsis", overflow: "hidden", whiteSpace: "nowrap" }}>
                              {entry.description}
                            </strong>
                            <span style={{ fontSize: "11px", color: "var(--color-text-3)" }}>
                              Fecha: {entry.entryDate} · Creada: {entry.createdAt.slice(0, 16).replace("T", " ")}
                            </span>
                          </div>
                        </div>

                        <div style={{ display: "flex", alignItems: "center", gap: "var(--space-4)", marginLeft: "var(--space-3)" }}>
                          <strong style={{ fontSize: "14px", color: "var(--color-text-2)", flexShrink: 0 }}>
                            {currencyFormatter.format(entryTotal)}
                          </strong>
                          {isExpanded ? <IconChevronUp size={18} style={{ color: "var(--color-text-3)" }} /> : <IconChevronDown size={18} style={{ color: "var(--color-text-3)" }} />}
                        </div>
                      </div>

                      {isExpanded && (
                        <div style={{ padding: "var(--space-4)", borderTop: "1px solid var(--color-border)" }}>
                          <div className="table-wrapper">
                            <table className="data-table no-row-click" style={{ width: "100%", margin: 0, fontSize: "13px" }}>
                              <thead>
                                <tr>
                                  <th style={{ width: "120px" }}>Código</th>
                                  <th>Nombre de la Cuenta</th>
                                  <th style={{ textAlign: "right", width: "130px" }}>Cargo (Debe)</th>
                                  <th style={{ textAlign: "right", width: "130px" }}>Abono (Haber)</th>
                                </tr>
                              </thead>
                              <tbody>
                                {entry.lines.map((line) => {
                                  const isCredit = Number(line.credit) > 0;
                                  return (
                                    <tr key={line.id}>
                                      <td style={{ fontFamily: "monospace", color: "var(--color-text-2)" }}>{line.accountCode}</td>
                                      <td style={{
                                        paddingLeft: isCredit ? "var(--space-5)" : "var(--space-2)",
                                        color: isCredit ? "var(--color-text-2)" : "var(--color-text-1)",
                                        fontWeight: isCredit ? 400 : 500
                                      }}>
                                        {line.accountName}
                                      </td>
                                      <td style={{ textAlign: "right", color: "var(--color-text-1)" }}>
                                        {Number(line.debit) > 0 ? currencyFormatter.format(line.debit) : ""}
                                      </td>
                                      <td style={{ textAlign: "right", color: "var(--color-text-2)" }}>
                                        {Number(line.credit) > 0 ? currencyFormatter.format(line.credit) : ""}
                                      </td>
                                    </tr>
                                  );
                                })}
                                <tr style={{ fontWeight: 600, borderTop: "1px solid var(--color-border)", backgroundColor: "var(--color-surface)" }}>
                                  <td></td>
                                  <td style={{ textAlign: "right" }}>Sumas Iguales:</td>
                                  <td style={{ textAlign: "right", color: "var(--color-text-1)" }}>
                                    {currencyFormatter.format(entryTotal)}
                                  </td>
                                  <td style={{ textAlign: "right", color: "var(--color-text-1)" }}>
                                    {currencyFormatter.format(entryTotal)}
                                  </td>
                                </tr>
                              </tbody>
                            </table>
                          </div>
                          <div style={{ marginTop: "var(--space-3)", padding: "var(--space-2) var(--space-3)", backgroundColor: "var(--color-surface)", borderRadius: "4px", fontSize: "11px", color: "var(--color-text-3)", display: "flex", justifyContent: "space-between" }}>
                            <span>UUID Póliza: <span style={{ fontFamily: "monospace" }}>{entry.id}</span></span>
                            <span>Origen: <span style={{ fontFamily: "monospace" }}>{entry.sourceEventType} ({entry.sourceEventId})</span></span>
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}

            {journalQuery && journalQuery.totalPages > 1 && (
              <div style={{ padding: "0 var(--space-4) var(--space-4)", display: "flex", justifyContent: "space-between", alignItems: "center", gap: "var(--space-3)" }}>
                <span style={{ fontSize: "12px", color: "var(--color-text-3)" }}>
                  Página {journalQuery.page + 1} de {journalQuery.totalPages}
                </span>
                <div style={{ display: "flex", gap: "var(--space-2)" }}>
                  <button className="btn secondary" type="button" disabled={journalQueryLoading || journalQuery.page === 0} onClick={() => setJournalPage((current) => Math.max(0, current - 1))}>
                    Anterior
                  </button>
                  <button className="btn secondary" type="button" disabled={journalQueryLoading || journalQuery.page >= journalQuery.totalPages - 1} onClick={() => setJournalPage((current) => current + 1)}>
                    Siguiente
                  </button>
                </div>
              </div>
            )}
            </section>
          </>
        )}

        {tab === "mermas" && (
          <section className="panel accounting-workspace-panel">
            <div className="panel-heading" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div>
                <h2>Acumulado de mermas</h2>
                <p style={{ margin: 0, fontSize: "12px", color: "var(--color-text-3)" }}>
                  Ajustes de inventario registrados como merma o caducidad.
                </p>
              </div>
              <span className="badge warning">Cuenta 52100</span>
            </div>

            {wasteReportLoading ? (
              <div className="empty-table-state">Cargando acumulado de mermas...</div>
            ) : wasteReport ? (
              <>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: "var(--space-3)", padding: "var(--space-4)" }}>
                  <article className="accounting-kpi-card">
                    <span>Total del periodo</span>
                    <strong>{currencyFormatter.format(wasteReport.totalAmount)}</strong>
                  </article>
                  <article className="accounting-kpi-card">
                    <span>Eventos registrados</span>
                    <strong>{wasteReport.totalEvents}</strong>
                  </article>
                  <article className="accounting-kpi-card">
                    <span>Periodo consultado</span>
                    <strong>{wasteReport.from} a {wasteReport.to}</strong>
                  </article>
                </div>
                {wasteReport.lines.length === 0 ? (
                  <div className="empty-table-state">No hay mermas registradas en el periodo seleccionado.</div>
                ) : (
                  <div className="table-wrapper" style={{ margin: "0 var(--space-4) var(--space-4)" }}>
                    <table className="data-table no-row-click">
                      <thead>
                        <tr>
                          <th>Fecha</th>
                          <th>Descripción</th>
                          <th>Evento origen</th>
                          <th style={{ textAlign: "right" }}>Importe</th>
                        </tr>
                      </thead>
                      <tbody>
                        {wasteReport.lines.map((line) => (
                          <tr key={line.journalEntryId}>
                            <td>{line.date}</td>
                            <td>{line.description}</td>
                            <td style={{ fontFamily: "monospace", fontSize: "11px" }}>{line.sourceEventId}</td>
                            <td style={{ textAlign: "right", fontWeight: 600 }}>{currencyFormatter.format(line.amount)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </>
            ) : (
              <div className="empty-table-state">No se pudo cargar el acumulado de mermas.</div>
            )}
          </section>
        )}

        {/* VISTA DE CUENTAS OPERATIVAS */}
        {tab === "bancos" && (
          <div className="accounting-workspace-grid">
          {!selectedBankAccountId && (
          <section className="panel accounting-workspace-panel">
            <div className="panel-heading" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div>
                <h2>Cuentas operativas</h2>
                <p style={{ margin: 0, fontSize: "12px", color: "var(--color-text-3)" }}>
                  Bancos, caja chica y fondos reservados del consultorio
                </p>
              </div>
            </div>

            {showBankForm && (
              <form className="profile-form" onSubmit={saveBankAccount} style={{ padding: "var(--space-4)", borderBottom: "1px solid var(--color-border)" }}>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "var(--space-3)" }}>
                  <label className="field">
                    <span>Banco</span>
                    <input value={bankForm.bankName} onChange={(event) => setBankForm((prev) => ({ ...prev, bankName: event.target.value }))} placeholder={bankForm.accountKind === "BANK" ? "BBVA" : "Referencia o nombre"} required />
                  </label>
                  <label className="field">
                    <span>Alias</span>
                    <input value={bankForm.alias} onChange={(event) => setBankForm((prev) => ({ ...prev, alias: event.target.value }))} placeholder="Cuenta principal" required />
                  </label>
                  <label className="field">
                    <span>Clase de cuenta</span>
                    <select value={bankForm.accountKind} onChange={(event) => setBankForm((prev) => ({ ...prev, accountKind: event.target.value as OperationalAccountKind, accountType: event.target.value === "BANK" ? prev.accountType : "DEBIT" }))}>
                      <option value="BANK">Banco</option>
                      <option value="PETTY_CASH">Caja chica</option>
                      <option value="RESERVE">Fondo especifico</option>
                      <option value="OTHER">Otra cuenta operativa</option>
                    </select>
                  </label>
                  <label className="field">
                    <span>Tipo</span>
                    <select value={bankForm.accountType} onChange={(event) => setBankForm((prev) => ({ ...prev, accountType: event.target.value as BankAccountType }))}>
                      <option value="DEBIT">Debito</option>
                      <option value="CREDIT">Credito</option>
                    </select>
                  </label>
                  <label className="field">
                    <span>Ultimos 4 (banco)</span>
                    <input maxLength={4} disabled={bankForm.accountKind !== "BANK"} value={bankForm.accountLast4} onChange={(event) => setBankForm((prev) => ({ ...prev, accountLast4: event.target.value.replace(/\D/g, "").slice(0, 4) }))} placeholder="1234" />
                  </label>
                  <label className="field">
                    <span>Moneda</span>
                    <input maxLength={3} value={bankForm.currency} onChange={(event) => setBankForm((prev) => ({ ...prev, currency: event.target.value.toUpperCase().slice(0, 3) }))} required />
                  </label>
                  <label className="field">
                    <span>Fecha de apertura</span>
                    <input type="date" value={bankForm.openingDate} onChange={(event) => setBankForm((prev) => ({ ...prev, openingDate: event.target.value }))} required />
                  </label>
                  <label className="field">
                    <span>Saldo inicial</span>
                    <input type="number" min="0" step="0.01" value={bankForm.openingBalance} onChange={(event) => setBankForm((prev) => ({ ...prev, openingBalance: event.target.value }))} placeholder="0.00" />
                  </label>
                  <label className="field">
                    <span>Notas</span>
                    <input value={bankForm.notes} onChange={(event) => setBankForm((prev) => ({ ...prev, notes: event.target.value }))} placeholder="Alta posterior a la apertura" />
                  </label>
                </div>
                {bankForm.accountType === "CREDIT" && (
                  <div style={{ marginTop: "var(--space-4)", paddingTop: "var(--space-4)", borderTop: "1px solid var(--color-border)" }}>
                    <h3 style={{ margin: "0 0 var(--space-3)" }}>Datos de crédito</h3>
                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "var(--space-3)" }}>
                      <label className="field">
                        <span>Fecha de corte</span>
                        <input type="date" value={bankForm.creditCutoffDate} onChange={(event) => setBankForm((prev) => ({ ...prev, creditCutoffDate: event.target.value }))} />
                      </label>
                      <label className="field">
                        <span>Fecha límite de pago</span>
                        <input type="date" value={bankForm.creditPaymentDueDate} onChange={(event) => setBankForm((prev) => ({ ...prev, creditPaymentDueDate: event.target.value }))} />
                      </label>
                      <label className="field">
                        <span>Límite de crédito</span>
                        <input type="number" min="0" step="0.01" value={bankForm.creditLimit} onChange={(event) => setBankForm((prev) => ({ ...prev, creditLimit: event.target.value }))} placeholder="0.00" />
                      </label>
                      <label className="field">
                        <span>Saldo actual</span>
                        <input type="number" min="0" step="0.01" value={bankForm.creditCurrentAmount} onChange={(event) => setBankForm((prev) => ({ ...prev, creditCurrentAmount: event.target.value }))} placeholder="0.00" />
                      </label>
                      <label className="field">
                        <span>Pago mínimo</span>
                        <input type="number" min="0" step="0.01" value={bankForm.creditMinimumPayment} onChange={(event) => setBankForm((prev) => ({ ...prev, creditMinimumPayment: event.target.value }))} placeholder="0.00" />
                      </label>
                      <label className="field">
                        <span>Pago para no generar intereses</span>
                        <input type="number" min="0" step="0.01" value={bankForm.creditNoInterestPayment} onChange={(event) => setBankForm((prev) => ({ ...prev, creditNoInterestPayment: event.target.value }))} placeholder="0.00" />
                      </label>
                      <label className="field">
                        <span>Pago actual requerido</span>
                        <input type="number" min="0" step="0.01" value={bankForm.creditCurrentPaymentDue} onChange={(event) => setBankForm((prev) => ({ ...prev, creditCurrentPaymentDue: event.target.value }))} placeholder="0.00" />
                      </label>
                    </div>
                  </div>
                )}
                <div className="form-actions" style={{ marginTop: "var(--space-3)" }}>
                  <button className="btn primary" type="submit" disabled={!canSaveBankAccount || bankActionSaving}>
                    {bankActionSaving ? "Guardando..." : "Registrar cuenta"}
                  </button>
                  <button className="btn ghost" type="button" onClick={() => { setShowBankForm(false); resetBankForm(); }}>
                    Cancelar
                  </button>
                </div>
              </form>
            )}

            <div className="table-wrapper">
              <table className="data-table" style={{ margin: 0 }}>
                <thead>
                  <tr>
                    <th>Cuenta</th>
                    <th>Clase</th>
                    <th>Tipo</th>
                    <th>Estado</th>
                    <th style={{ textAlign: "right" }}>Saldo inicial</th>
                  </tr>
                </thead>
                <tbody>
                  {openingLoading ? (
                    <tr>
                      <td colSpan={5}>
                        <div className="empty-table-state">Cargando cuentas operativas...</div>
                      </td>
                    </tr>
                  ) : bankAccounts.length === 0 ? (
                    <tr>
                      <td colSpan={5}>
                        <div className="empty-table-state">Aun no hay cuentas operativas registradas.</div>
                      </td>
                    </tr>
                  ) : (
                    bankAccounts.map((account) => {
                      const isSelected = account.id === selectedBankAccountId;
                      return (
                        <tr
                          key={account.id}
                          onClick={() => navigateAccounting("bancos", account.id)}
                          style={{
                            cursor: "pointer",
                            backgroundColor: isSelected ? "var(--color-surface)" : undefined
                          }}
                        >
                          <td>
                            <strong style={{ display: "block" }}>{account.alias}</strong>
                            <span style={{ fontSize: "11px", color: "var(--color-text-3)" }}>
                              {account.bankName} {account.accountLast4 ? `****${account.accountLast4}` : ""}
                            </span>
                          </td>
                          <td>
                            <span className="badge progress">{accountKindLabel(account.accountKind)}</span>
                          </td>
                          <td>
                            <span className={`badge ${account.accountType === "CREDIT" ? "warning" : "success"}`}>
                              {bankTypeLabel(account.accountType)}
                            </span>
                          </td>
                          <td>
                            <span className={`badge ${account.active ? "success" : "neutral"}`}>
                              {account.active ? "Activa" : "Baja"}
                            </span>
                          </td>
                          <td style={{ textAlign: "right", fontWeight: 600 }}>{currencyFormatter.format(account.openingBalance)}</td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </section>
          )}

          {selectedBankAccountId && (
          <section className="panel accounting-workspace-panel">
            <div className="panel-heading" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div>
                <h2>Detalle de cuenta operativa</h2>
                <p style={{ margin: 0, fontSize: "12px", color: "var(--color-text-3)" }}>
                  Movimientos exactos ligados a la cuenta seleccionada
                </p>
              </div>
              {selectedBankAccount && (
                <div style={{ display: "flex", alignItems: "center", gap: "var(--space-2)" }}>
                  <span className={`badge ${selectedBankAccount.accountType === "CREDIT" ? "warning" : "success"}`}>
                    {accountKindLabel(selectedBankAccount.accountKind)} · {bankTypeLabel(selectedBankAccount.accountType)}
                  </span>
                </div>
              )}
            </div>

            {!selectedBankAccount ? (
              <div style={{ padding: "var(--space-8)", textAlign: "center", color: "var(--color-text-2)" }}>
                <IconInfoCircle size={32} style={{ color: "var(--color-text-3)", marginBottom: "var(--space-2)" }} />
                <h3 style={{ margin: "0 0 4px" }}>Selecciona una cuenta bancaria</h3>
                <p style={{ margin: 0, fontSize: "13px" }}>El desglose se mostrara aqui cuando exista una cuenta seleccionada.</p>
              </div>
            ) : (
              <div style={{ padding: "var(--space-5)", display: "grid", gap: "var(--space-5)" }}>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(4, minmax(120px, 1fr))", gap: "var(--space-3)" }}>
                  <div>
                    <span style={{ display: "block", fontSize: "11px", color: "var(--color-text-3)" }}>Referencia</span>
                    <strong>{selectedBankAccount.bankName}</strong>
                  </div>
                  <div>
                    <span style={{ display: "block", fontSize: "11px", color: "var(--color-text-3)" }}>Alias</span>
                    <strong>{selectedBankAccount.alias}</strong>
                  </div>
                  <div>
                    <span style={{ display: "block", fontSize: "11px", color: "var(--color-text-3)" }}>Cuenta</span>
                    <strong>{selectedBankAccount.accountLast4 ? `****${selectedBankAccount.accountLast4}` : "No aplica"}</strong>
                  </div>
                  <div>
                    <span style={{ display: "block", fontSize: "11px", color: "var(--color-text-3)" }}>Saldo actual</span>
                    <strong style={{ color: selectedBankBalance >= 0 ? "var(--color-text-1)" : "var(--color-error)" }}>
                      {currencyFormatter.format(selectedBankBalance)}
                    </strong>
                  </div>
                </div>

                {selectedBankAccount.lastModifiedAt && (
                  <div style={{ fontSize: "12px", color: "var(--color-text-3)", padding: "var(--space-2) var(--space-3)", backgroundColor: "var(--color-surface)", borderRadius: "6px" }}>
                    Ultima modificacion: {selectedBankAccount.lastModifiedAt.slice(0, 16).replace("T", " ")}
                    {selectedBankAccount.lastModificationReason ? ` - ${selectedBankAccount.lastModificationReason}` : ""}
                  </div>
                )}

                {!selectedBankAccount.active && (
                  <div className="badge neutral" style={{ justifySelf: "start" }}>
                    Cuenta dada de baja {selectedBankAccount.deactivatedAt ? selectedBankAccount.deactivatedAt.slice(0, 10) : ""}
                  </div>
                )}

                {selectedBankAccount.active && selectedBankAccount.accountType === "DEBIT" && (
                  <form className="profile-form" onSubmit={saveAccountTransfer} style={{ padding: "var(--space-4)", border: "1px solid var(--color-border)", borderRadius: "6px" }}>
                    <h3 style={{ margin: "0 0 4px" }}>Transferir fondos</h3>
                    <p style={{ margin: "0 0 var(--space-3)", fontSize: "12px", color: "var(--color-text-3)" }}>
                      Mueve saldo entre cuentas operativas y caja chica con una poliza balanceada.
                    </p>
                    <div style={{ display: "grid", gridTemplateColumns: "repeat(2, minmax(160px, 1fr))", gap: "var(--space-3)" }}>
                      <label className="field">
                        <span>Cuenta destino</span>
                        <select value={transferDestinationId} onChange={(event) => setTransferDestinationId(event.target.value)} required>
                          <option value="">Selecciona una cuenta</option>
                          {bankAccounts.filter((account) => account.id !== selectedBankAccount.id && account.active && account.accountType === "DEBIT").map((account) => (
                            <option key={account.id} value={account.id}>
                              {account.alias || account.bankName} - {accountKindLabel(account.accountKind)}
                            </option>
                          ))}
                        </select>
                      </label>
                      <label className="field">
                        <span>Monto</span>
                        <input type="number" min="0.01" step="0.01" value={transferAmount} onChange={(event) => setTransferAmount(event.target.value)} placeholder="0.00" required />
                      </label>
                      <label className="field">
                        <span>Fecha</span>
                        <input type="date" value={transferDate} onChange={(event) => setTransferDate(event.target.value)} required />
                      </label>
                      <label className="field">
                        <span>Descripcion</span>
                        <input value={transferDescription} onChange={(event) => setTransferDescription(event.target.value)} placeholder="Reposicion de caja chica" />
                      </label>
                    </div>
                    <button className="btn primary" type="submit" disabled={bankActionSaving || !transferDestinationId || toAmount(transferAmount) <= 0}>
                      {bankActionSaving ? "Registrando..." : "Registrar transferencia"}
                    </button>
                  </form>
                )}

                <div className="table-wrapper">
                  <table className="data-table no-row-click" style={{ margin: 0, fontSize: "13px" }}>
                    <thead>
                      <tr>
                        <th>Fecha</th>
                        <th>Movimiento</th>
                        <th>Origen</th>
                        <th style={{ textAlign: "right" }}>Debe</th>
                        <th style={{ textAlign: "right" }}>Haber</th>
                        <th style={{ textAlign: "right" }}>Variacion</th>
                        <th style={{ textAlign: "right" }}>Saldo</th>
                      </tr>
                    </thead>
                    <tbody>
                      {bankMovementsLoading ? (
                        <tr>
                          <td colSpan={7}>
                            <div className="empty-table-state">Cargando movimientos bancarios...</div>
                          </td>
                        </tr>
                      ) : bankMovements.length === 0 ? (
                        <tr>
                          <td colSpan={7}>
                            <div className="empty-table-state">Esta cuenta aun no tiene movimientos contables.</div>
                          </td>
                        </tr>
                      ) : (
                        bankMovements.map((movement) => (
                          <tr key={movement.journalLineId}>
                            <td>{movement.entryDate}</td>
                            <td>{movement.description}</td>
                            <td>
                              <span className={`badge ${eventBadgeTypes[movement.sourceEventType] || "neutral"}`}>
                                {eventLabels[movement.sourceEventType] || movement.sourceEventType}
                              </span>
                            </td>
                            <td style={{ textAlign: "right" }}>{Number(movement.debit) > 0 ? currencyFormatter.format(movement.debit) : ""}</td>
                            <td style={{ textAlign: "right" }}>{Number(movement.credit) > 0 ? currencyFormatter.format(movement.credit) : ""}</td>
                            <td style={{ textAlign: "right", color: Number(movement.movementAmount) >= 0 ? "var(--color-success)" : "var(--color-error)" }}>
                              {currencyFormatter.format(movement.movementAmount)}
                            </td>
                            <td style={{ textAlign: "right", fontWeight: 600 }}>{currencyFormatter.format(movement.balanceAfter)}</td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>

                {selectedBankAccount.active && (
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "var(--space-4)" }}>
                    <form className="profile-form" onSubmit={saveBankCorrection} style={{ padding: "var(--space-4)", border: "1px solid var(--color-border)", borderRadius: "6px" }}>
                      <h3 style={{ margin: "0 0 var(--space-3)" }}>Poliza de correccion</h3>
                      <div style={{ display: "grid", gap: "var(--space-3)" }}>
                        <label className="field">
                          <span>Fecha</span>
                          <input type="date" value={correctionDate} onChange={(event) => setCorrectionDate(event.target.value)} required />
                        </label>
                        <label className="field">
                          <span>Saldo correcto</span>
                          <input type="number" min="0" step="0.01" value={correctionBalance} onChange={(event) => setCorrectionBalance(event.target.value)} placeholder="0.00" required />
                        </label>
                        <label className="field">
                          <span>Motivo</span>
                          <input value={correctionReason} onChange={(event) => setCorrectionReason(event.target.value)} placeholder="Correccion por captura inicial" required />
                        </label>
                      </div>
                      <button className="btn primary" type="submit" disabled={bankActionSaving || !correctionReason.trim() || !correctionBalance}>
                        Registrar correccion
                      </button>
                    </form>

                    <form className="profile-form" onSubmit={deactivateSelectedBankAccount} style={{ padding: "var(--space-4)", border: "1px solid var(--color-border)", borderRadius: "6px" }}>
                      <h3 style={{ margin: "0 0 var(--space-3)" }}>Eliminar cuenta</h3>
                      <div style={{ display: "grid", gap: "var(--space-3)" }}>
                        <label className="field">
                          <span>Fecha</span>
                          <input type="date" value={deactivationDate} onChange={(event) => setDeactivationDate(event.target.value)} required />
                        </label>
                        <label className="field">
                          <span>Motivo</span>
                          <input value={deactivationReason} onChange={(event) => setDeactivationReason(event.target.value)} placeholder="Cuenta cerrada o sustituida" required />
                        </label>
                      </div>
                      <button className="btn secondary text-error" type="submit" disabled={bankActionSaving || !deactivationReason.trim()}>
                        Eliminar con baja contable
                      </button>
                    </form>
                  </div>
                )}
              </div>
            )}
          </section>
          )}
          </div>
        )}

        {/* VISTA DE BALANZA DE COMPROBACIÓN */}
        {tab === "balanza" && (
          <>
            <section className="panel accounting-workspace-panel">
            <div className="panel-heading">
              <h2>Balanza de Comprobación</h2>
            </div>

            {balanzaView === "cuentasT" ? (
              <div className="t-account-workspace">
                <div className="t-account-toolbar">
                  <label className="accounting-search-field">
                    <IconSearch size={16} aria-hidden="true" />
                    <input value={tAccountSearch} onChange={(event) => setTAccountSearch(event.target.value)} placeholder="Buscar cuenta o código" />
                  </label>
                  <select aria-label="Filtrar cuentas por tipo" value={tAccountCategory} onChange={(event) => setTAccountCategory(event.target.value as AccountCategory | "ALL")}>
                    <option value="ALL">Todos los tipos</option>
                    <option value="ASSET">Activos</option>
                    <option value="LIABILITY">Pasivos</option>
                    <option value="EQUITY">Capital</option>
                    <option value="INCOME">Ingresos</option>
                    <option value="EXPENSE">Costos y gastos</option>
                    <option value="CUSTOM">Personalizadas</option>
                  </select>
                  <select aria-label="Ordenar cuentas T" value={tAccountSort} onChange={(event) => setTAccountSort(event.target.value as "code" | "balance" | "activity")}>
                    <option value="code">Ordenar por código</option>
                    <option value="balance">Mayor saldo</option>
                    <option value="activity">Mayor actividad</option>
                  </select>
                  <span>{tAccountData.length} cuenta(s)</span>
                </div>

                {loading || trialBalanceLoading ? (
                  <div className="empty-table-state">Cargando cuentas T...</div>
                ) : balanzaData.length === 0 ? (
                  <div className="empty-table-state">No hay cuentas con saldo o movimientos en el periodo.</div>
                ) : tAccountData.length === 0 ? (
                  <div className="empty-table-state">Ninguna cuenta coincide con los filtros.</div>
                ) : (
                  <div className="t-account-grid">
                    {tAccountData.map((account) => {
                      const isExpanded = Boolean(expandedTAccounts[account.code]);
                      const debitMovements = account.movements.filter((movement) => movement.debit > 0);
                      const creditMovements = account.movements.filter((movement) => movement.credit > 0);
                      return (
                        <article className={`t-account-card ${isExpanded ? "is-expanded" : ""}`} key={account.code}>
                          <div className="t-account-heading">
                            <div>
                              <span className="t-account-code">{account.code}</span>
                              <strong>{account.name}</strong>
                              <small title={account.inferred ? "Naturaleza inferida por el código de la cuenta" : "Naturaleza definida en el catálogo"}>
                                {accountCategoryLabels[account.category]} · Naturaleza {account.nature === "DEBIT" ? "deudora" : "acreedora"}{account.inferred ? " (inferida)" : ""}
                              </small>
                            </div>
                            <button className="icon-btn" type="button" aria-label={`${isExpanded ? "Ocultar" : "Mostrar"} movimientos de ${account.name}`} aria-expanded={isExpanded} onClick={() => setExpandedTAccounts((current) => ({ ...current, [account.code]: !current[account.code] }))}>
                              {isExpanded ? <IconChevronUp size={17} /> : <IconChevronDown size={17} />}
                            </button>
                          </div>

                          <div className="t-account-body">
                            <div className="t-account-side">
                              <h3>Debe</h3>
                              {account.initialAmount > 0 && account.initialOnDebit && <div className="t-account-opening"><span>Saldo inicial</span><strong>{currencyFormatter.format(account.initialAmount)}</strong></div>}
                              {isExpanded ? debitMovements.map((movement) => (
                                <button className="t-account-movement" type="button" key={movement.id} onClick={() => openJournalEntry(movement.entry)} title={movement.description}>
                                  <span><small>{movement.date}</small><strong>{movement.description}</strong><small>{movement.eventLabel}</small></span>
                                  <b>{currencyFormatter.format(movement.debit)}</b>
                                </button>
                              )) : account.debit > 0 && <div><span>Cargos del periodo</span><strong>{currencyFormatter.format(account.debit)}</strong></div>}
                              {account.closingSide === "DEBIT" && account.balanceAmount > 0 && <div className="t-account-closing"><span>Saldo para cierre</span><strong>{currencyFormatter.format(account.balanceAmount)}</strong></div>}
                              <div className="t-account-side-total"><span>Total Debe</span><strong>{currencyFormatter.format(account.balancedTotal)}</strong></div>
                            </div>

                            <div className="t-account-side">
                              <h3>Haber</h3>
                              {account.initialAmount > 0 && !account.initialOnDebit && <div className="t-account-opening"><span>Saldo inicial</span><strong>{currencyFormatter.format(account.initialAmount)}</strong></div>}
                              {isExpanded ? creditMovements.map((movement) => (
                                <button className="t-account-movement" type="button" key={movement.id} onClick={() => openJournalEntry(movement.entry)} title={movement.description}>
                                  <span><small>{movement.date}</small><strong>{movement.description}</strong><small>{movement.eventLabel}</small></span>
                                  <b>{currencyFormatter.format(movement.credit)}</b>
                                </button>
                              )) : account.credit > 0 && <div><span>Abonos del periodo</span><strong>{currencyFormatter.format(account.credit)}</strong></div>}
                              {account.closingSide === "CREDIT" && account.balanceAmount > 0 && <div className="t-account-closing"><span>Saldo para cierre</span><strong>{currencyFormatter.format(account.balanceAmount)}</strong></div>}
                              <div className="t-account-side-total"><span>Total Haber</span><strong>{currencyFormatter.format(account.balancedTotal)}</strong></div>
                            </div>
                          </div>

                          <div className="t-account-footer">
                            <span>Saldo {account.balanceNature === "DEBIT" ? "deudor" : "acreedor"}</span>
                            <strong>{currencyFormatter.format(account.balanceAmount)}</strong>
                            <button type="button" onClick={() => setExpandedTAccounts((current) => ({ ...current, [account.code]: !current[account.code] }))}>
                              {isExpanded ? "Ocultar movimientos" : `Ver ${account.movements.length} movimiento(s)`}
                            </button>
                          </div>
                        </article>
                      );
                    })}
                  </div>
                )}
              </div>
            ) : (
            <div className="table-wrapper scrollable-table-panel">
              <table className="data-table no-row-click" style={{ fontSize: "13px" }}>
                <thead>
                  <tr>
                    <th>Código Cuenta</th>
                    <th>Nombre de Cuenta</th>
                    <th style={{ textAlign: "right" }}>Saldo Inicial</th>
                    <th style={{ textAlign: "right" }}>Cargos (Debe)</th>
                    <th style={{ textAlign: "right" }}>Abonos (Haber)</th>
                    <th style={{ textAlign: "right" }}>Saldo Final</th>
                  </tr>
                </thead>
                <tbody>
                  {loading || trialBalanceLoading ? (
                    <tr>
                      <td colSpan={6}>
                        <div className="empty-table-state">Cargando balanza contable...</div>
                      </td>
                    </tr>
                  ) : balanzaData.length === 0 ? (
                    <tr>
                      <td colSpan={6}>
                        <div className="empty-table-state">Sin movimientos registrados en este periodo contable.</div>
                      </td>
                    </tr>
                  ) : (
                    <>
                      {balanzaData.map((row) => (
                        <tr key={row.code}>
                          <td style={{ fontFamily: "monospace" }}>{row.code}</td>
                          <td style={{ fontWeight: 500 }}>{row.name}</td>
                          <td style={{ textAlign: "right" }}>{currencyFormatter.format(row.initialBalance)}</td>
                          <td style={{ textAlign: "right" }}>{currencyFormatter.format(row.debit)}</td>
                          <td style={{ textAlign: "right" }}>{currencyFormatter.format(row.credit)}</td>
                          <td style={{ textAlign: "right", fontWeight: 600 }}>{currencyFormatter.format(row.finalBalance)}</td>
                        </tr>
                      ))}
                      <tr style={{ fontWeight: 700, backgroundColor: "var(--color-surface)", borderTop: "2px solid var(--color-border)" }}>
                        <td></td>
                        <td style={{ textAlign: "right" }}>Totales del Periodo:</td>
                        <td style={{ textAlign: "right" }}>{currencyFormatter.format(balanzaTotals.initialSum)}</td>
                        <td style={{ textAlign: "right" }}>{currencyFormatter.format(balanzaTotals.debitSum)}</td>
                        <td style={{ textAlign: "right" }}>{currencyFormatter.format(balanzaTotals.creditSum)}</td>
                        <td style={{ textAlign: "right" }}>{currencyFormatter.format(balanzaTotals.finalSum)}</td>
                      </tr>
                    </>
                  )}
                </tbody>
              </table>
            </div>
            )}
            </section>
          </>
        )}

        {/* VISTA DE ESTADO DE RESULTADOS */}
        {tab === "resultados" && (
          <section className="accounting-results-view" aria-label="Estado de resultados">
            {incomeStatementError && (
              <div className="accounting-report-notice is-error" role="alert">
                <IconAlertTriangle size={18} />
                <span><strong>No se pudo calcular el Estado de Resultados.</strong> {incomeStatementError}</span>
              </div>
            )}

            <div className="accounting-results-kpis">
              <article><span>Ingresos</span><strong>{formatReportAmount(resultadosData.ingresos)}</strong><small>{formatReportChange(incomeChange)}</small></article>
              <article><span>Costos y gastos</span><strong>{formatReportAmount(overviewExpenses)}</strong><small>{formatReportChange(expenseChange)}</small></article>
              <article><span>Utilidad neta</span><strong className={resultadosData.utilidadNeta < 0 ? "text-error" : "text-success"}>{formatReportAmount(resultadosData.utilidadNeta)}</strong><small>{formatReportChange(netChange)}</small></article>
              <article><span>Margen neto</span><strong>{numberFormatter.format(resultadosData.margenUtilidad)}%</strong><small>Margen bruto {numberFormatter.format(resultadosData.margenBruto)}%</small></article>
            </div>

            {incomeStatementLoading && !incomeStatement ? (
              <div className="panel accounting-report-loading"><IconLoader2 className="spin" size={24} /><span>Calculando el reporte desde las pólizas contables...</span></div>
            ) : incomeStatement && (
              <>
                <div className="accounting-results-main-grid">
                  <section className="panel accounting-statement-panel">
                    <div className="panel-heading accounting-report-heading">
                      <div><h2>Estado de Resultados</h2><p>Importes consolidados por el motor contable.</p></div>
                      <span>{incomeStatement.period.from} al {incomeStatement.period.to}</span>
                    </div>
                    <div className="accounting-statement-table" role="table" aria-label="Estado de resultados comparativo">
                      <div className="accounting-statement-row is-header" role="row"><span>Concepto</span><span>Actual</span><span>% ingreso</span><span>Anterior</span></div>
                      <div className="accounting-statement-row is-income" role="row"><span>(+) Ingresos operativos</span><strong>{currencyFormatter.format(resultadosData.ingresos)}</strong><span>100%</span><span>{currencyFormatter.format(incomeStatement.previous.income)}</span></div>
                      <div className="accounting-statement-row" role="row"><span>(-) Costos directos</span><strong>{currencyFormatter.format(resultadosData.costos)}</strong><span>{numberFormatter.format(resultadosData.porcentajeCostos)}%</span><span>{currencyFormatter.format(incomeStatement.previous.directCosts)}</span></div>
                      <div className="accounting-statement-row is-subtotal" role="row"><span>(=) Utilidad bruta</span><strong>{currencyFormatter.format(resultadosData.utilidadBruta)}</strong><span>{numberFormatter.format(resultadosData.margenBruto)}%</span><span>{currencyFormatter.format(incomeStatement.previous.grossProfit)}</span></div>
                      <div className="accounting-statement-row" role="row"><span>(-) Mermas y caducidad</span><strong>{currencyFormatter.format(resultadosData.mermas)}</strong><span>{numberFormatter.format(resultadosData.porcentajeMermas)}%</span><span>{currencyFormatter.format(incomeStatement.previous.waste)}</span></div>
                      <div className="accounting-statement-row" role="row"><span>(-) Otros gastos operativos</span><strong>{currencyFormatter.format(resultadosData.otrosGastos)}</strong><span>{numberFormatter.format(resultadosData.porcentajeOtrosGastos)}%</span><span>{currencyFormatter.format(incomeStatement.previous.otherExpenses)}</span></div>
                      <div className="accounting-statement-row is-total" role="row"><span>(=) Utilidad neta</span><strong className={resultadosData.utilidadNeta < 0 ? "text-error" : "text-success"}>{currencyFormatter.format(resultadosData.utilidadNeta)}</strong><span>{numberFormatter.format(resultadosData.margenUtilidad)}%</span><span>{currencyFormatter.format(incomeStatement.previous.netProfit)}</span></div>
                    </div>
                  </section>

                  <section className="panel accounting-result-trend-panel">
                    <div className="panel-heading accounting-report-heading"><div><h2>Tendencia del periodo</h2><p>Ingresos frente a costos y gastos.</p></div></div>
                    <div className="accounting-chart-legend"><span><i className="is-income" /> Ingresos</span><span><i className="is-expense" /> Gastos</span></div>
                    <div className="accounting-bar-chart accounting-result-chart">
                      {overviewData.buckets.map((bucket) => (
                        <div className="accounting-chart-column" key={bucket.label}>
                          <div className="accounting-chart-bars">
                            <span className="is-income" title={`Ingresos: ${currencyFormatter.format(bucket.income)}`} style={{ height: bucket.income > 0 ? `${Math.max(3, bucket.income / overviewData.maxTrendAmount * 100)}%` : 0 }} />
                            <span className="is-expense" title={`Gastos: ${currencyFormatter.format(bucket.expenses)}`} style={{ height: bucket.expenses > 0 ? `${Math.max(3, bucket.expenses / overviewData.maxTrendAmount * 100)}%` : 0 }} />
                          </div>
                          <small>{bucket.label}</small>
                        </div>
                      ))}
                    </div>
                    <div className="accounting-margin-summary"><IconReportMoney size={20} /><span>Por cada $100 de ingreso quedan</span><strong>{currencyFormatter.format(resultadosData.margenUtilidad)}</strong></div>
                  </section>
                </div>

                <section className="panel accounting-account-breakdown">
                  <div className="panel-heading accounting-report-heading"><div><h2>Detalle por cuenta</h2><p>Origen exacto de los importes incluidos en el reporte.</p></div><span>{incomeStatement.accounts.length} cuentas con actividad</span></div>
                  {incomeStatement.accounts.length === 0 ? <div className="accounting-inline-empty">No hay cuentas de ingresos o gastos con actividad en este periodo.</div> : (
                    <div className="table-wrap"><table><thead><tr><th>Cuenta</th><th>Clasificación</th><th className="text-right">Actual</th><th className="text-right">% ingreso</th><th className="text-right">Anterior</th><th className="text-right">Variación</th></tr></thead><tbody>
                      {incomeStatement.accounts.map((account) => <tr key={account.accountCode}><td><strong>{account.accountCode}</strong><span className="accounting-account-name">{account.accountName}</span></td><td><span className={`accounting-account-kind is-${account.category.toLowerCase()}`}>{account.category === "INCOME" ? "Ingreso" : account.category === "DIRECT_COST" ? "Costo directo" : account.category === "WASTE" ? "Merma" : "Gasto"}</span></td><td className="text-right">{currencyFormatter.format(account.currentAmount)}</td><td className="text-right">{numberFormatter.format(account.percentageOfIncome)}%</td><td className="text-right">{currencyFormatter.format(account.previousAmount)}</td><td className="text-right">{account.changePercentage === null ? "Sin base" : `${account.changePercentage >= 0 ? "+" : ""}${numberFormatter.format(account.changePercentage)}%`}</td></tr>)}
                    </tbody></table></div>
                  )}
                </section>
              </>
            )}
          </section>
        )}
      </div>

      {/* MODAL PARA MODIFICAR CUENTA BANCARIA */}
      {showBankEditForm && selectedBankAccount && (
        <div className="modal-overlay" onClick={() => setShowBankEditForm(false)}>
          <div className="modal-card bank-account-edit-modal" onClick={(event) => event.stopPropagation()}>
            <div className="panel-heading bank-account-edit-modal-header">
              <div>
                <h2>Modificar cuenta bancaria</h2>
                <p style={{ margin: 0, fontSize: "12px", color: "var(--color-text-3)" }}>
                  {selectedBankAccount.bankName} {selectedBankAccount.accountLast4 ? `****${selectedBankAccount.accountLast4}` : ""}
                </p>
              </div>
              <button className="icon-btn" type="button" aria-label="Cerrar" onClick={() => setShowBankEditForm(false)}>
                <IconX size={18} />
              </button>
            </div>

            <form className="profile-form bank-account-edit-form" onSubmit={saveBankAccountChanges}>
              <section className="bank-account-edit-section">
                <h3>Datos generales</h3>
                <div className="bank-account-edit-grid">
                <label className="field">
                  <span>Referencia / institucion</span>
                  <input value={bankEditForm.bankName} onChange={(event) => setBankEditForm((prev) => ({ ...prev, bankName: event.target.value }))} required />
                </label>
                <label className="field">
                  <span>Alias</span>
                  <input value={bankEditForm.alias} onChange={(event) => setBankEditForm((prev) => ({ ...prev, alias: event.target.value }))} required />
                </label>
                <label className="field">
                  <span>Clase de cuenta</span>
                  <select value={bankEditForm.accountKind} onChange={(event) => setBankEditForm((prev) => ({ ...prev, accountKind: event.target.value as OperationalAccountKind, accountType: event.target.value === "BANK" ? prev.accountType : "DEBIT" }))}>
                    <option value="BANK">Banco</option>
                    <option value="PETTY_CASH">Caja chica</option>
                    <option value="RESERVE">Fondo especifico</option>
                    <option value="OTHER">Otra cuenta operativa</option>
                  </select>
                </label>
                <label className="field">
                   <span>Tipo</span>
                  <select value={bankEditForm.accountType} onChange={(event) => setBankEditForm((prev) => ({ ...prev, accountType: event.target.value as BankAccountType }))}>
                    <option value="DEBIT">Debito</option>
                    <option value="CREDIT">Credito</option>
                  </select>
                </label>
                <label className="field">
                  <span>Ultimos 4 (banco)</span>
                  <input maxLength={4} disabled={bankEditForm.accountKind !== "BANK"} value={bankEditForm.accountLast4} onChange={(event) => setBankEditForm((prev) => ({ ...prev, accountLast4: event.target.value.replace(/\D/g, "").slice(0, 4) }))} placeholder="1234" />
                </label>
                <label className="field">
                  <span>Moneda</span>
                  <input maxLength={3} value={bankEditForm.currency} onChange={(event) => setBankEditForm((prev) => ({ ...prev, currency: event.target.value.toUpperCase().slice(0, 3) }))} required />
                </label>
                <label className="field bank-account-edit-field-wide">
                  <span>Motivo</span>
                  <input value={bankEditForm.reason} onChange={(event) => setBankEditForm((prev) => ({ ...prev, reason: event.target.value }))} placeholder="Correccion de datos capturados" required />
                </label>
                </div>
              </section>

              {bankEditForm.accountType === "CREDIT" && (
                <section className="bank-account-edit-section bank-account-edit-credit-section">
                  <h3>Datos de credito</h3>
                  <div className="bank-account-edit-credit-grid">
                    <label className="field">
                      <span>Fecha de corte</span>
                      <input type="date" value={bankEditForm.creditCutoffDate} onChange={(event) => setBankEditForm((prev) => ({ ...prev, creditCutoffDate: event.target.value }))} />
                    </label>
                    <label className="field">
                      <span>Fecha limite de pago</span>
                      <input type="date" value={bankEditForm.creditPaymentDueDate} onChange={(event) => setBankEditForm((prev) => ({ ...prev, creditPaymentDueDate: event.target.value }))} />
                    </label>
                    <label className="field">
                      <span>Credito de la tarjeta</span>
                      <input type="number" min="0" step="0.01" value={bankEditForm.creditLimit} onChange={(event) => setBankEditForm((prev) => ({ ...prev, creditLimit: event.target.value }))} placeholder="0.00" />
                    </label>
                    <label className="field">
                      <span>Monto actual</span>
                      <input type="number" min="0" step="0.01" value={bankEditForm.creditCurrentAmount} onChange={(event) => setBankEditForm((prev) => ({ ...prev, creditCurrentAmount: event.target.value }))} placeholder="0.00" />
                    </label>
                    <label className="field">
                      <span>Monto minimo</span>
                      <input type="number" min="0" step="0.01" value={bankEditForm.creditMinimumPayment} onChange={(event) => setBankEditForm((prev) => ({ ...prev, creditMinimumPayment: event.target.value }))} placeholder="0.00" />
                    </label>
                    <label className="field">
                      <span>Monto para no generar intereses</span>
                      <input type="number" min="0" step="0.01" value={bankEditForm.creditNoInterestPayment} onChange={(event) => setBankEditForm((prev) => ({ ...prev, creditNoInterestPayment: event.target.value }))} placeholder="0.00" />
                    </label>
                    <label className="field">
                      <span>Monto actual a pagar</span>
                      <input type="number" min="0" step="0.01" value={bankEditForm.creditCurrentPaymentDue} onChange={(event) => setBankEditForm((prev) => ({ ...prev, creditCurrentPaymentDue: event.target.value }))} placeholder="0.00" />
                    </label>
                  </div>
                </section>
              )}

              <div className="form-actions bank-account-edit-actions">
                <button className="btn primary" type="submit" disabled={!canSaveBankEdit || bankActionSaving}>
                  {bankActionSaving ? "Guardando..." : "Guardar cambios"}
                </button>
                <button className="btn ghost" type="button" onClick={() => setShowBankEditForm(false)}>
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL PARA CREAR PÓLIZA MANUAL */}
      {isModalOpen && (
        <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: "880px", width: "95%" }}>
            <div className="panel-heading">
              <h2>Registrar Póliza Contable Manual</h2>
              <button className="icon-btn" type="button" aria-label="Cerrar" onClick={() => setIsModalOpen(false)}>
                <IconX size={18} />
              </button>
            </div>

            <form className="profile-form" onSubmit={saveManualEntry}>
              <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: "var(--space-4)", marginBottom: "var(--space-4)" }}>
                <label className="field">
                  <span>Descripción de la póliza *</span>
                  <input
                    required
                    placeholder="Ej. Pago de Renta Oficina mes Julio, Compra de Papelería..."
                    value={manualDescription}
                    onChange={(e) => setManualDescription(e.target.value)}
                  />
                </label>
                <label className="field">
                  <span>Fecha contable *</span>
                  <input
                    required
                    type="date"
                    value={manualDate}
                    onChange={(e) => setManualDate(e.target.value)}
                  />
                </label>
              </div>

              <div style={{ marginTop: "var(--space-4)" }}>
                <span style={{ fontWeight: 600, display: "block", marginBottom: "var(--space-2)" }}>Asientos Contables</span>
                
                <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-2)" }}>
                  {manualLines.map((line, index) => (
                    <div key={index} style={{ display: "flex", gap: "var(--space-2)", alignItems: "center" }}>
                      
                      {/* Selector de Cuentas */}
                      <select
                        style={{ width: "200px" }}
                        value={line.isCustom ? "CUSTOM" : line.accountCode}
                        onChange={(e) => handleLineAccountChange(index, e.target.value)}
                      >
                        {TYPICAL_ACCOUNTS.map((acc) => (
                          <option key={acc.code} value={acc.code}>{acc.code} · {acc.name}</option>
                        ))}
                      </select>

                      {/* Código de cuenta manual si es custom */}
                      {line.isCustom && (
                        <input
                          required
                          placeholder="Código Cuenta"
                          style={{ width: "120px", fontFamily: "monospace" }}
                          value={line.accountCode}
                          onChange={(e) => updateLineValue(index, "accountCode", e.target.value)}
                        />
                      )}

                      {/* Nombre de cuenta contable */}
                      <input
                        required
                        placeholder="Nombre de la Cuenta"
                        style={{ flex: 1 }}
                        disabled={!line.isCustom}
                        value={line.accountName}
                        onChange={(e) => updateLineValue(index, "accountName", e.target.value)}
                      />

                      {/* Cargos (Debe) */}
                      <input
                        placeholder="Debe"
                        type="number"
                        min="0"
                        step="0.01"
                        style={{ width: "110px", textAlign: "right" }}
                        value={line.debit}
                        onChange={(e) => updateLineValue(index, "debit", e.target.value)}
                      />

                      {/* Abonos (Haber) */}
                      <input
                        placeholder="Haber"
                        type="number"
                        min="0"
                        step="0.01"
                        style={{ width: "110px", textAlign: "right" }}
                        value={line.credit}
                        onChange={(e) => updateLineValue(index, "credit", e.target.value)}
                      />

                      {/* Eliminar fila */}
                      <button
                        className="icon-btn text-error"
                        type="button"
                        disabled={manualLines.length <= 2}
                        onClick={() => removeManualLine(index)}
                        style={{ padding: "8px" }}
                      >
                        <IconTrash size={16} />
                      </button>
                    </div>
                  ))}
                </div>

                <button
                  className="btn secondary"
                  type="button"
                  onClick={addManualLine}
                  style={{ marginTop: "var(--space-3)", padding: "var(--space-2) var(--space-3)", fontSize: "12px" }}
                >
                  <IconPlus size={14} />
                  Añadir fila
                </button>
              </div>

              {/* Sección de Totales de la Póliza */}
              <div style={{ marginTop: "var(--space-5)", padding: "var(--space-3) var(--space-4)", backgroundColor: "var(--color-surface)", borderRadius: "6px", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div style={{ display: "flex", gap: "var(--space-5)" }}>
                  <div>
                    <span style={{ fontSize: "11px", color: "var(--color-text-3)", display: "block" }}>Total Cargos (Debe)</span>
                    <strong>{currencyFormatter.format(manualTotals.debits)}</strong>
                  </div>
                  <div>
                    <span style={{ fontSize: "11px", color: "var(--color-text-3)", display: "block" }}>Total Abonos (Haber)</span>
                    <strong>{currencyFormatter.format(manualTotals.credits)}</strong>
                  </div>
                </div>

                <div>
                  {manualTotals.difference !== 0 ? (
                    <span className="badge warning" style={{ fontSize: "12px" }}>
                      No cuadra (Diferencia: {currencyFormatter.format(manualTotals.difference)})
                    </span>
                  ) : manualTotals.debits > 0 ? (
                    <span className="badge success" style={{ fontSize: "12px" }}>
                      Póliza Cuadrada y Balanceada
                    </span>
                  ) : (
                    <span className="badge neutral" style={{ fontSize: "12px" }}>
                      Ingrese cargos y abonos
                    </span>
                  )}
                </div>
              </div>

              <div className="form-actions" style={{ marginTop: "var(--space-5)" }}>
                <button
                  className="btn primary"
                  type="submit"
                  disabled={!canSaveManual || saving}
                >
                  {saving ? "Guardando..." : "Registrar Póliza"}
                </button>
                <button
                  className="btn ghost"
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                >
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
