import { FormEvent, useEffect, useState } from "react";
import { IconDotsVertical, IconHistory, IconMinus, IconPlus, IconX } from "@tabler/icons-react";
import { accountingApi, agendaApi, cashExpensesApi, cashSessionsApi, getFriendlyError, quotationsApi, ticketsApi } from "@shared/api/api";
import { PAYMENT_METHOD_LABELS, type CashExpenseResponse, type CashSessionResponse, type PaymentLineRequest, type PaymentMethod, type PendingAppointmentChargeResponse, type TicketResponse } from "@modules/cash/types";
import type { DoctorResponse } from "@modules/agenda/types";
import type { QuotationResponse } from "@modules/treatments/quotationTypes";
import type { PatientResponse } from "@modules/patients/types";
import type { BankAccountResponse } from "@modules/accounting/types";

const currencyFormatter = new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN", maximumFractionDigits: 2 });

function patientName(patients: PatientResponse[], patientId: string): string {
  const patient = patients.find((p) => p.id === patientId);
  return patient ? `${patient.firstName} ${patient.lastNamePaterno}` : "Paciente";
}

function doctorLabel(doctors: DoctorResponse[], staffId: string | null | undefined): string {
  if (!staffId) return "—";
  return doctors.find((doctor) => doctor.staffId === staffId)?.fullName ?? "—";
}

function formatTime(value: string): string {
  return new Date(value).toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit", hour12: false });
}

function formatDateTime(value: string): string {
  return new Date(value).toLocaleString("es-MX");
}

function paymentMethodsSummary(lines: TicketResponse["paymentLines"]): string {
  if (lines.length === 0) return "—";
  if (lines.length === 1) return PAYMENT_METHOD_LABELS[lines[0].method];
  return "Dividido: " + lines.map((line) => PAYMENT_METHOD_LABELS[line.method]).join(" + ");
}

function bankAccountLabel(accounts: BankAccountResponse[], bankAccountId?: string | null): string {
  if (!bankAccountId) return "Sin cuenta";
  const account = accounts.find((item) => item.id === bankAccountId);
  if (!account) return "Cuenta no encontrada";
  return `${account.alias}${account.accountLast4 ? ` ****${account.accountLast4}` : ""}`;
}

interface EditablePaymentLine {
  method: PaymentMethod;
  amount: string;
  reference: string;
  bankAccountId: string;
}

function OpenSessionModal({
  clinicId,
  doctors,
  onClose,
  onSaved
}: {
  clinicId: string;
  doctors: DoctorResponse[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const [openingAmount, setOpeningAmount] = useState("0");
  const [openedByStaffId, setOpenedByStaffId] = useState(doctors[0]?.staffId ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    if (!openedByStaffId) {
      setError("Selecciona quién abre el turno.");
      return;
    }
    const amount = parseFloat(openingAmount);
    if (isNaN(amount) || amount < 0) {
      setError("El monto de apertura debe ser un número mayor o igual a cero.");
      return;
    }
    setSaving(true);
    try {
      await cashSessionsApi.open(clinicId, { openedByStaffId, openingAmount: amount });
      onSaved();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Abrir turno</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>
        <form className="profile-form" onSubmit={submit}>
          <label className="field">
            <span>Monto de apertura</span>
            <input
              type="number"
              step="0.01"
              min="0"
              value={openingAmount}
              onChange={(event) => setOpeningAmount(event.target.value)}
              required
            />
          </label>
          <label className="field">
            <span>Atendido por</span>
            <select value={openedByStaffId} onChange={(event) => setOpenedByStaffId(event.target.value)} required>
              <option value="">Selecciona</option>
              {doctors.map((doctor) => (
                <option key={doctor.staffId} value={doctor.staffId}>
                  {doctor.fullName}
                </option>
              ))}
            </select>
          </label>

          {error && <p className="alert error">{error}</p>}
          <div className="form-actions">
            <button className="btn primary" type="submit" disabled={saving}>
              {saving ? "Abriendo..." : "Abrir turno"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function CloseSessionModal({
  clinicId,
  currentSession,
  doctors,
  onClose,
  onClosed
}: {
  clinicId: string;
  currentSession: CashSessionResponse;
  doctors: DoctorResponse[];
  onClose: () => void;
  onClosed: () => void;
}) {
  const [countedCashAmount, setCountedCashAmount] = useState("0");
  const [closedByStaffId, setClosedByStaffId] = useState(doctors[0]?.staffId ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState<CashSessionResponse | null>(null);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    if (!closedByStaffId) {
      setError("Selecciona quién cierra el turno.");
      return;
    }
    const amount = parseFloat(countedCashAmount);
    if (isNaN(amount) || amount < 0) {
      setError("El efectivo contado debe ser un número mayor o igual a cero.");
      return;
    }
    setSaving(true);
    try {
      const closed = await cashSessionsApi.close(clinicId, { closedByStaffId, countedCashAmount: amount });
      setResult(closed);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  if (result) {
    const difference = result.cashDifference ?? 0;
    const differenceBadgeClass = difference === 0 ? "success" : difference > 0 ? "neutral" : "warning";
    const differenceLabel = difference === 0 ? "Cuadra exacto" : difference > 0 ? "Sobrante" : "Faltante";

    return (
      <div className="modal-overlay" onClick={onClose}>
        <div className="modal-card" onClick={(event) => event.stopPropagation()}>
          <div className="panel-heading">
            <h2>Turno cerrado</h2>
            <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
              <IconX size={18} />
            </button>
          </div>
          <div className="clinic-list">
            <div className="clinic-row">
              <strong>Monto de apertura</strong>
              <span>{currencyFormatter.format(result.openingAmount)}</span>
            </div>
            <div className="clinic-row">
              <strong>Efectivo esperado</strong>
              <span>{currencyFormatter.format(result.expectedCashAmount ?? 0)}</span>
            </div>
            <div className="clinic-row">
              <strong>Efectivo contado</strong>
              <span>{currencyFormatter.format(result.countedCashAmount ?? 0)}</span>
            </div>
            <div className="clinic-row">
              <strong>Diferencia</strong>
              <span className={`badge ${differenceBadgeClass}`}>
                {differenceLabel} · {currencyFormatter.format(Math.abs(difference))}
              </span>
            </div>
          </div>
          <div className="form-actions">
            <button className="btn primary" type="button" onClick={onClosed}>
              Cerrar
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Cerrar turno</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>
        <form className="profile-form" onSubmit={submit}>
          <label className="field">
            <span>Efectivo contado</span>
            <input
              type="number"
              step="0.01"
              min="0"
              value={countedCashAmount}
              onChange={(event) => setCountedCashAmount(event.target.value)}
              required
            />
          </label>
          <label className="field">
            <span>Cerrado por</span>
            <select value={closedByStaffId} onChange={(event) => setClosedByStaffId(event.target.value)} required>
              <option value="">Selecciona</option>
              {doctors.map((doctor) => (
                <option key={doctor.staffId} value={doctor.staffId}>
                  {doctor.fullName}
                </option>
              ))}
            </select>
          </label>

          {error && <p className="alert error">{error}</p>}
          <div className="form-actions">
            <button className="btn destructive" type="submit" disabled={saving}>
              {saving ? "Cerrando..." : "Cerrar turno"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function PatientStatementModal({
  clinicId,
  patients,
  patientId,
  onClose
}: {
  clinicId: string;
  patients: PatientResponse[];
  patientId: string;
  onClose: () => void;
}) {
  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    ticketsApi
      .listByPatient(clinicId, patientId)
      .then(setTickets)
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  }, [clinicId, patientId]);

  const activeTickets = tickets.filter((ticket) => ticket.status === "ACTIVE");
  const totalActive = activeTickets.reduce((sum, ticket) => sum + ticket.totalAmount, 0);

  return (
    <div
      className="modal-overlay"
      onClick={(event) => {
        event.stopPropagation();
        onClose();
      }}
    >
      <div className="modal-card modal-card-wide" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Estado de cuenta — {patientName(patients, patientId)}</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>

        {error && <p className="alert error">{error}</p>}
        {loading && <p className="panel-subtitle">Cargando historial...</p>}

        {!loading && (
          <div className="table-wrapper">
            <table className="data-table no-row-click">
              <thead>
                <tr>
                  <th>Folio</th>
                  <th>Fecha</th>
                  <th>Concepto</th>
                  <th>Monto</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {tickets.map((ticket) => (
                  <tr key={ticket.id}>
                    <td>#{ticket.folio}</td>
                    <td>{formatDateTime(ticket.createdAt)}</td>
                    <td>{ticket.concept || "Sin concepto"}</td>
                    <td>{currencyFormatter.format(ticket.totalAmount)}</td>
                    <td>
                      <span className={`agenda-status-badge ${ticket.status === "ACTIVE" ? "status-confirmed" : "status-cancelled"}`}>
                        {ticket.status === "ACTIVE" ? "Activo" : "Anulado"}
                      </span>
                    </td>
                  </tr>
                ))}
                {tickets.length === 0 && (
                  <tr>
                    <td colSpan={5}>
                      <div className="empty-table-state">Este paciente no tiene tickets registrados todavía.</div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        <div className="form-actions" style={{ justifyContent: "space-between" }}>
          <span>Total pagado (activo)</span>
          <strong>{currencyFormatter.format(totalActive)}</strong>
        </div>
      </div>
    </div>
  );
}

function NewTicketModal({
  clinicId,
  patients,
  doctors,
  bankAccounts,
  onClose,
  onSaved,
  preFill
}: {
  clinicId: string;
  patients: PatientResponse[];
  doctors: DoctorResponse[];
  bankAccounts: BankAccountResponse[];
  onClose: () => void;
  onSaved: () => void;
  preFill?: {
    patientId: string;
    concept: string;
    amount: number;
    quotationId?: string;
    doctorStaffId?: string;
  };
}) {
  const [patientId, setPatientId] = useState(preFill?.patientId ?? "");
  const [concept, setConcept] = useState(preFill?.concept ?? "");
  const [createdByStaffId, setCreatedByStaffId] = useState(preFill?.doctorStaffId ?? doctors[0]?.staffId ?? "");
  const [quotations, setQuotations] = useState<QuotationResponse[]>([]);
  const [quotationId, setQuotationId] = useState(preFill?.quotationId ?? "");
  const [balance, setBalance] = useState<number | null>(null);
  const activeBankAccounts = bankAccounts.filter((account) => account.active && account.accountType === "DEBIT");
  const defaultBankAccountId = activeBankAccounts[0]?.id ?? "";
  const [lines, setLines] = useState<EditablePaymentLine[]>(() => {
    if (preFill && preFill.amount > 0) {
      return [{ method: "CASH", amount: String(preFill.amount), reference: "", bankAccountId: "" }];
    }
    return [{ method: "CASH", amount: "", reference: "", bankAccountId: "" }];
  });
  const [applyDiscount, setApplyDiscount] = useState(false);
  const [discountAmount, setDiscountAmount] = useState("");
  const [discountAuthorizedByStaffId, setDiscountAuthorizedByStaffId] = useState("");
  const [discountReason, setDiscountReason] = useState("");
  const [showStatement, setShowStatement] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!createdByStaffId && doctors.length > 0) {
      setCreatedByStaffId(preFill?.doctorStaffId ?? doctors[0]?.staffId ?? "");
    }
  }, [doctors, createdByStaffId, preFill?.doctorStaffId]);

  useEffect(() => {
    if (!patientId) {
      setQuotations([]);
      setQuotationId("");
      setBalance(null);
      return;
    }
    quotationsApi
      .listByPatient(patientId, clinicId)
      .then((list) => setQuotations(list.filter((q) => q.status === "ACCEPTED")))
      .catch(() => setQuotations([]));
  }, [patientId, clinicId]);

  useEffect(() => {
    if (!quotationId || !patientId) {
      setBalance(null);
      return;
    }
    ticketsApi
      .getQuotationBalance(clinicId, quotationId, patientId)
      .then((response) => setBalance(response.remainingBalance))
      .catch(() => setBalance(null));
  }, [quotationId, patientId, clinicId]);

  const updateLine = (index: number, patch: Partial<EditablePaymentLine>) => {
    setLines((prev) => prev.map((line, i) => (i === index ? { ...line, ...patch } : line)));
  };

  const removeLine = (index: number) => {
    setLines((prev) => prev.filter((_, i) => i !== index));
  };

  const addLine = () => {
    setLines((prev) => [...prev, { method: "CASH", amount: "", reference: "", bankAccountId: "" }]);
  };

  const subtotal = lines.reduce((sum, line) => {
    const amount = parseFloat(line.amount);
    return sum + (isNaN(amount) ? 0 : amount);
  }, 0);
  const parsedDiscount = applyDiscount ? parseFloat(discountAmount) : 0;
  const discountForTotal = isNaN(parsedDiscount) ? 0 : parsedDiscount;
  const total = subtotal + discountForTotal;

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");

    if (!patientId) {
      setError("Selecciona un paciente.");
      return;
    }
    if (!createdByStaffId) {
      setError("Selecciona quién atiende el ticket.");
      return;
    }
    if (lines.length === 0) {
      setError("Agrega al menos una forma de pago.");
      return;
    }
    const paymentLines: PaymentLineRequest[] = [];
    for (const line of lines) {
      if (!line.method) {
        setError("Selecciona la forma de pago en todas las líneas.");
        return;
      }
      const amount = parseFloat(line.amount);
      if (isNaN(amount) || amount <= 0) {
        setError("Cada línea de pago debe tener un monto mayor a cero.");
        return;
      }
      if (line.method !== "CASH" && !line.bankAccountId) {
        setError(
          activeBankAccounts.length === 0
            ? "Configura una cuenta bancaria activa en Contabilidad para registrar pagos no efectivos."
            : "Selecciona la cuenta destino para cada pago no efectivo."
        );
        return;
      }
      const paymentLine: PaymentLineRequest = {
        method: line.method,
        amount,
        reference: line.reference.trim() || undefined
      };
      if (line.method !== "CASH") {
        paymentLine.bankAccountId = line.bankAccountId;
      }
      paymentLines.push(paymentLine);
    }

    let discountPayload: { discountAmount?: number; discountAuthorizedByStaffId?: string; discountReason?: string } = {};
    if (applyDiscount) {
      const parsedAmount = parseFloat(discountAmount);
      if (isNaN(parsedAmount) || parsedAmount <= 0) {
        setError("El monto del descuento debe ser mayor a cero.");
        return;
      }
      if (!discountAuthorizedByStaffId) {
        setError("Selecciona quién autoriza el descuento.");
        return;
      }
      if (!discountReason.trim()) {
        setError("Indica la razón del descuento.");
        return;
      }
      discountPayload = {
        discountAmount: parsedAmount,
        discountAuthorizedByStaffId,
        discountReason: discountReason.trim()
      };
    }

    setSaving(true);
    try {
      await ticketsApi.register(clinicId, {
        patientId,
        quotationId: quotationId || undefined,
        concept: concept.trim() || undefined,
        createdByStaffId,
        paymentLines,
        ...discountPayload
      });
      onSaved();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card modal-card-wide" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Nuevo ticket</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>
        <form className="profile-form" onSubmit={submit}>
          <label className="field">
            <span>Paciente</span>
            <select
              value={patientId}
              onChange={(event) => {
                const nextId = event.target.value;
                setPatientId(nextId);
                setQuotationId("");
                setBalance(null);
              }}
              required
            >
              <option value="">Selecciona un paciente</option>
              {patients.map((patient) => (
                <option key={patient.id} value={patient.id}>
                  {patient.firstName} {patient.lastNamePaterno} {patient.lastNameMaterno ?? ""}
                </option>
              ))}
            </select>
            {patientId && (
              <button
                type="button"
                className="btn ghost"
                style={{ marginTop: "var(--space-2)" }}
                onClick={() => setShowStatement(true)}
              >
                Ver estado de cuenta
              </button>
            )}
          </label>
          <label className="field">
            <span>Atendido por</span>
            <select value={createdByStaffId} onChange={(event) => setCreatedByStaffId(event.target.value)} required>
              <option value="">Selecciona</option>
              {doctors.map((doctor) => (
                <option key={doctor.staffId} value={doctor.staffId}>
                  {doctor.fullName}
                </option>
              ))}
            </select>
          </label>
          <label className="field field-full">
            <span>Concepto (opcional)</span>
            <input type="text" value={concept} onChange={(event) => setConcept(event.target.value)} placeholder="Consulta general, abono a tratamiento..." />
          </label>

          {quotations.length > 0 && (
            <label className="field field-full">
              <span>Cotización aceptada (opcional)</span>
              <select value={quotationId} onChange={(event) => setQuotationId(event.target.value)}>
                <option value="">Sin vincular</option>
                {quotations.map((quotation) => (
                  <option key={quotation.id} value={quotation.id}>
                    {quotation.quotationDate} — {currencyFormatter.format(quotation.grandTotal)}
                  </option>
                ))}
              </select>
              {quotationId && balance !== null && (
                <span className="table-subtext">Saldo pendiente: {currencyFormatter.format(balance)}</span>
              )}
            </label>
          )}

          <div className="field field-full">
            <span>Formas de pago</span>
            <div className="table-wrapper">
              <table className="data-table no-row-click">
                <thead>
                  <tr>
                    <th>Forma de pago</th>
                    <th>Monto</th>
                    <th>Cuenta destino</th>
                    <th>Referencia</th>
                    <th aria-label="Quitar" />
                  </tr>
                </thead>
                <tbody>
                  {lines.map((line, index) => (
                    <tr key={index}>
                      <td>
                        <select
                          value={line.method}
                          onChange={(event) => {
                            const method = event.target.value as PaymentMethod;
                            updateLine(index, {
                              method,
                              bankAccountId: method === "CASH" ? "" : line.bankAccountId || defaultBankAccountId
                            });
                          }}
                        >
                          {(Object.keys(PAYMENT_METHOD_LABELS) as PaymentMethod[]).map((method) => (
                            <option key={method} value={method}>
                              {PAYMENT_METHOD_LABELS[method]}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td>
                        <input
                          type="number"
                          step="0.01"
                          min="0"
                          value={line.amount}
                          onChange={(event) => updateLine(index, { amount: event.target.value })}
                          style={{ width: "110px" }}
                        />
                      </td>
                      <td>
                        {line.method === "CASH" ? (
                          <span className="table-subtext">Caja operativa</span>
                        ) : (
                          <select
                            value={line.bankAccountId}
                            onChange={(event) => updateLine(index, { bankAccountId: event.target.value })}
                            required
                          >
                            <option value="">Selecciona cuenta</option>
                            {activeBankAccounts.map((account) => (
                              <option key={account.id} value={account.id}>
                                {bankAccountLabel(activeBankAccounts, account.id)}
                              </option>
                            ))}
                          </select>
                        )}
                      </td>
                      <td>
                        <input
                          type="text"
                          value={line.reference}
                          onChange={(event) => updateLine(index, { reference: event.target.value })}
                          placeholder="Opcional"
                        />
                      </td>
                      <td>
                        <button className="icon-btn" type="button" aria-label="Quitar línea" onClick={() => removeLine(index)}>
                          <IconX size={14} />
                        </button>
                      </td>
                    </tr>
                  ))}
                  {lines.length === 0 && (
                    <tr>
                      <td colSpan={5}>
                        <div className="empty-table-state">No se ha agregado ninguna forma de pago.</div>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div className="form-actions" style={{ justifyContent: "space-between" }}>
            <button className="btn ghost" type="button" onClick={addLine}>
              <IconPlus size={14} />
              Agregar forma de pago
            </button>
            <strong>Subtotal: {currencyFormatter.format(subtotal)}</strong>
          </div>

          <label className="checkbox-field field-full">
            <input type="checkbox" checked={applyDiscount} onChange={(event) => setApplyDiscount(event.target.checked)} />
            <span>Aplicar descuento (requiere autorización)</span>
          </label>

          {applyDiscount && (
            <>
              <label className="field">
                <span>Monto del descuento</span>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={discountAmount}
                  onChange={(event) => setDiscountAmount(event.target.value)}
                  required
                />
              </label>
              <label className="field">
                <span>Autorizado por</span>
                <select
                  value={discountAuthorizedByStaffId}
                  onChange={(event) => setDiscountAuthorizedByStaffId(event.target.value)}
                  required
                >
                  <option value="">Selecciona</option>
                  {doctors.map((doctor) => (
                    <option key={doctor.staffId} value={doctor.staffId}>
                      {doctor.fullName}
                    </option>
                  ))}
                </select>
              </label>
              <label className="field field-full">
                <span>Razón del descuento</span>
                <input
                  type="text"
                  value={discountReason}
                  onChange={(event) => setDiscountReason(event.target.value)}
                  placeholder="Cortesía, pago de contado, promoción..."
                  required
                />
              </label>
            </>
          )}

          <div className="form-actions" style={{ justifyContent: "space-between" }}>
            <span>Valor total del servicio (cobrado + descuento)</span>
            <strong>{currencyFormatter.format(total)}</strong>
          </div>

          {error && <p className="alert error">{error}</p>}
          <div className="form-actions">
            <button className="btn primary" type="submit" disabled={saving}>
              {saving ? "Guardando..." : "Registrar ticket"}
            </button>
          </div>
        </form>
      </div>

      {showStatement && patientId && (
        <PatientStatementModal
          clinicId={clinicId}
          patients={patients}
          patientId={patientId}
          onClose={() => setShowStatement(false)}
        />
      )}
    </div>
  );
}

function TicketDetailModal({
  clinicId,
  ticket,
  patients,
  doctors,
  bankAccounts,
  onClose,
  onChanged
}: {
  clinicId: string;
  ticket: TicketResponse;
  patients: PatientResponse[];
  doctors: DoctorResponse[];
  bankAccounts: BankAccountResponse[];
  onClose: () => void;
  onChanged: (updated: TicketResponse) => void;
}) {
  const [voiding, setVoiding] = useState(false);
  const [reason, setReason] = useState("");
  const [staffId, setStaffId] = useState(doctors[0]?.staffId ?? "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const confirmVoid = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    if (!staffId) {
      setError("Selecciona quién anula el ticket.");
      return;
    }
    if (!reason.trim()) {
      setError("Indica la razón de la anulación.");
      return;
    }
    setBusy(true);
    try {
      const updated = await ticketsApi.void(clinicId, ticket.id, { staffId, reason: reason.trim() });
      onChanged(updated);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy(false);
    }
  };

  if (voiding) {
    return (
      <div className="modal-overlay" onClick={onClose}>
        <div className="modal-card" onClick={(event) => event.stopPropagation()}>
          <div className="panel-heading">
            <h2>Anular ticket #{ticket.folio}</h2>
            <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
              <IconX size={18} />
            </button>
          </div>
          <form className="profile-form" onSubmit={confirmVoid}>
            <label className="field">
              <span>Anulado por</span>
              <select value={staffId} onChange={(event) => setStaffId(event.target.value)} required>
                <option value="">Selecciona</option>
                {doctors.map((doctor) => (
                  <option key={doctor.staffId} value={doctor.staffId}>
                    {doctor.fullName}
                  </option>
                ))}
              </select>
            </label>
            <label className="field field-full">
              <span>Razón de la anulación</span>
              <textarea value={reason} onChange={(event) => setReason(event.target.value)} required />
            </label>
            {error && <p className="alert error">{error}</p>}
            <div className="form-actions">
              <button className="btn secondary" type="button" disabled={busy} onClick={() => setVoiding(false)}>
                Atrás
              </button>
              <button className="btn destructive" type="submit" disabled={busy}>
                {busy ? "Anulando..." : "Confirmar anulación"}
              </button>
            </div>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Ticket #{ticket.folio}</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Paciente</strong>
            <span>{patientName(patients, ticket.patientId)}</span>
          </div>
          <div className="clinic-row">
            <strong>Concepto</strong>
            <span>{ticket.concept || "Sin concepto"}</span>
          </div>
          <div className="clinic-row">
            <strong>Fecha</strong>
            <span>{formatDateTime(ticket.createdAt)}</span>
          </div>
          <div className="clinic-row">
            <strong>Estado</strong>
            <span className={`badge ${ticket.status === "ACTIVE" ? "success" : "neutral"}`}>
              {ticket.status === "ACTIVE" ? "Activo" : "Anulado"}
            </span>
          </div>
          {ticket.status === "VOIDED" && ticket.voidReason && (
            <div className="clinic-row">
              <strong>Razón de anulación</strong>
              <span>{ticket.voidReason}</span>
            </div>
          )}
        </div>

        <div className="table-wrapper">
          <table className="data-table no-row-click">
            <thead>
              <tr>
                <th>Forma de pago</th>
                <th>Monto</th>
                <th>Cuenta</th>
                <th>Referencia</th>
              </tr>
            </thead>
            <tbody>
              {ticket.paymentLines.map((line) => (
                <tr key={line.id}>
                  <td>{PAYMENT_METHOD_LABELS[line.method]}</td>
                  <td>{currencyFormatter.format(line.amount)}</td>
                  <td>{line.method === "CASH" ? "Caja operativa" : bankAccountLabel(bankAccounts, line.bankAccountId)}</td>
                  <td>{line.reference || "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="form-actions" style={{ justifyContent: "space-between" }}>
          <span>Cobrado</span>
          <strong>{currencyFormatter.format(ticket.totalAmount)}</strong>
        </div>

        {ticket.discountAmount != null && ticket.discountAmount > 0 && (
          <div className="clinic-list">
            <div className="clinic-row">
              <strong>Descuento autorizado</strong>
              <span>{currencyFormatter.format(ticket.discountAmount)}</span>
            </div>
            <div className="clinic-row">
              <strong>Autorizado por</strong>
              <span>{doctorLabel(doctors, ticket.discountAuthorizedByStaffId)}</span>
            </div>
            <div className="clinic-row">
              <strong>Razón</strong>
              <span>{ticket.discountReason}</span>
            </div>
            <div className="clinic-row">
              <strong>Valor total del servicio</strong>
              <span>{currencyFormatter.format(ticket.totalAmount + ticket.discountAmount)}</span>
            </div>
          </div>
        )}

        {error && <p className="alert error">{error}</p>}

        {ticket.status === "ACTIVE" && (
          <div className="form-actions">
            <button className="btn destructive" type="button" onClick={() => setVoiding(true)}>
              Anular ticket
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function NewExpenseModal({
  clinicId,
  doctors,
  onClose,
  onSaved
}: {
  clinicId: string;
  doctors: DoctorResponse[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const [concept, setConcept] = useState("");
  const [amount, setAmount] = useState("");
  const [createdByStaffId, setCreatedByStaffId] = useState(doctors[0]?.staffId ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");

    if (!concept.trim()) {
      setError("El concepto del egreso es obligatorio.");
      return;
    }
    const parsedAmount = parseFloat(amount);
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setError("El monto del egreso debe ser mayor a cero.");
      return;
    }
    if (!createdByStaffId) {
      setError("Selecciona quién registra el egreso.");
      return;
    }

    setSaving(true);
    try {
      await cashExpensesApi.register(clinicId, {
        concept: concept.trim(),
        amount: parsedAmount,
        createdByStaffId
      });
      onSaved();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Nuevo egreso</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>
        <form className="profile-form" onSubmit={submit}>
          <label className="field field-full">
            <span>Concepto</span>
            <input
              type="text"
              value={concept}
              onChange={(event) => setConcept(event.target.value)}
              placeholder="Propina, papelería, reembolso..."
              required
            />
          </label>
          <label className="field">
            <span>Monto</span>
            <input
              type="number"
              step="0.01"
              min="0.01"
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
              required
            />
          </label>
          <label className="field">
            <span>Registrado por</span>
            <select value={createdByStaffId} onChange={(event) => setCreatedByStaffId(event.target.value)} required>
              <option value="">Selecciona</option>
              {doctors.map((doctor) => (
                <option key={doctor.staffId} value={doctor.staffId}>
                  {doctor.fullName}
                </option>
              ))}
            </select>
          </label>

          {error && <p className="alert error">{error}</p>}
          <div className="form-actions">
            <button className="btn primary" type="submit" disabled={saving}>
              {saving ? "Guardando..." : "Registrar egreso"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function ExpenseDetailModal({
  clinicId,
  expense,
  doctors,
  onClose,
  onChanged
}: {
  clinicId: string;
  expense: CashExpenseResponse;
  doctors: DoctorResponse[];
  onClose: () => void;
  onChanged: (updated: CashExpenseResponse) => void;
}) {
  const [voiding, setVoiding] = useState(false);
  const [reason, setReason] = useState("");
  const [staffId, setStaffId] = useState(doctors[0]?.staffId ?? "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const confirmVoid = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    if (!staffId) {
      setError("Selecciona quién anula el egreso.");
      return;
    }
    if (!reason.trim()) {
      setError("Indica la razón de la anulación.");
      return;
    }
    setBusy(true);
    try {
      const updated = await cashExpensesApi.void(clinicId, expense.id, { staffId, reason: reason.trim() });
      onChanged(updated);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy(false);
    }
  };

  if (voiding) {
    return (
      <div className="modal-overlay" onClick={onClose}>
        <div className="modal-card" onClick={(event) => event.stopPropagation()}>
          <div className="panel-heading">
            <h2>Anular egreso</h2>
            <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
              <IconX size={18} />
            </button>
          </div>
          <form className="profile-form" onSubmit={confirmVoid}>
            <label className="field">
              <span>Anulado por</span>
              <select value={staffId} onChange={(event) => setStaffId(event.target.value)} required>
                <option value="">Selecciona</option>
                {doctors.map((doctor) => (
                  <option key={doctor.staffId} value={doctor.staffId}>
                    {doctor.fullName}
                  </option>
                ))}
              </select>
            </label>
            <label className="field field-full">
              <span>Razón de la anulación</span>
              <textarea value={reason} onChange={(event) => setReason(event.target.value)} required />
            </label>
            {error && <p className="alert error">{error}</p>}
            <div className="form-actions">
              <button className="btn secondary" type="button" disabled={busy} onClick={() => setVoiding(false)}>
                Atrás
              </button>
              <button className="btn destructive" type="submit" disabled={busy}>
                {busy ? "Anulando..." : "Confirmar anulación"}
              </button>
            </div>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Egreso</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Concepto</strong>
            <span>{expense.concept}</span>
          </div>
          <div className="clinic-row">
            <strong>Monto</strong>
            <span>{currencyFormatter.format(expense.amount)}</span>
          </div>
          <div className="clinic-row">
            <strong>Fecha</strong>
            <span>{formatDateTime(expense.createdAt)}</span>
          </div>
          <div className="clinic-row">
            <strong>Estado</strong>
            <span className={`badge ${expense.status === "ACTIVE" ? "success" : "neutral"}`}>
              {expense.status === "ACTIVE" ? "Activo" : "Anulado"}
            </span>
          </div>
          {expense.status === "VOIDED" && expense.voidReason && (
            <div className="clinic-row">
              <strong>Razón de anulación</strong>
              <span>{expense.voidReason}</span>
            </div>
          )}
        </div>

        {error && <p className="alert error">{error}</p>}

        {expense.status === "ACTIVE" && (
          <div className="form-actions">
            <button className="btn destructive" type="button" onClick={() => setVoiding(true)}>
              Anular egreso
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function SessionHistoryModal({
  clinicId,
  patients,
  onClose
}: {
  clinicId: string;
  patients: PatientResponse[];
  onClose: () => void;
}) {
  const [sessions, setSessions] = useState<CashSessionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedSession, setSelectedSession] = useState<CashSessionResponse | null>(null);
  const [sessionTickets, setSessionTickets] = useState<TicketResponse[]>([]);
  const [sessionExpenses, setSessionExpenses] = useState<CashExpenseResponse[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);

  useEffect(() => {
    cashSessionsApi
      .list(clinicId)
      .then(setSessions)
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  }, [clinicId]);

  const openDetail = (session: CashSessionResponse) => {
    setSelectedSession(session);
    setError("");
    setDetailLoading(true);
    Promise.all([ticketsApi.listBySession(clinicId, session.id), cashExpensesApi.listBySession(clinicId, session.id)])
      .then(([loadedTickets, loadedExpenses]) => {
        setSessionTickets(loadedTickets);
        setSessionExpenses(loadedExpenses);
      })
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setDetailLoading(false));
  };

  if (selectedSession) {
    const activeTickets = sessionTickets.filter((ticket) => ticket.status === "ACTIVE");
    const activeExpenses = sessionExpenses.filter((expense) => expense.status === "ACTIVE");
    const totalCollected = activeTickets.reduce((sum, ticket) => sum + ticket.totalAmount, 0);
    const totalExpenses = activeExpenses.reduce((sum, expense) => sum + expense.amount, 0);
    const difference = selectedSession.cashDifference ?? 0;
    const differenceBadgeClass = difference === 0 ? "success" : difference > 0 ? "neutral" : "warning";
    const differenceLabel = difference === 0 ? "Cuadra exacto" : difference > 0 ? "Sobrante" : "Faltante";

    return (
      <div className="modal-overlay" onClick={onClose}>
        <div className="modal-card modal-card-wide" onClick={(event) => event.stopPropagation()}>
          <div className="panel-heading">
            <h2>Turno del {formatDateTime(selectedSession.openedAt)}</h2>
            <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
              <IconX size={18} />
            </button>
          </div>

          <div className="clinic-list">
            <div className="clinic-row">
              <strong>Apertura</strong>
              <span>
                {formatDateTime(selectedSession.openedAt)} · {currencyFormatter.format(selectedSession.openingAmount)}
              </span>
            </div>
            <div className="clinic-row">
              <strong>Cierre</strong>
              <span>{selectedSession.closedAt ? formatDateTime(selectedSession.closedAt) : "Turno abierto"}</span>
            </div>
            {selectedSession.status === "CLOSED" && (
              <>
                <div className="clinic-row">
                  <strong>Efectivo esperado</strong>
                  <span>{currencyFormatter.format(selectedSession.expectedCashAmount ?? 0)}</span>
                </div>
                <div className="clinic-row">
                  <strong>Efectivo contado</strong>
                  <span>{currencyFormatter.format(selectedSession.countedCashAmount ?? 0)}</span>
                </div>
                <div className="clinic-row">
                  <strong>Diferencia</strong>
                  <span className={`badge ${differenceBadgeClass}`}>
                    {differenceLabel} · {currencyFormatter.format(Math.abs(difference))}
                  </span>
                </div>
              </>
            )}
          </div>

          {detailLoading && <p className="panel-subtitle">Cargando detalle...</p>}
          {error && <p className="alert error">{error}</p>}

          {!detailLoading && (
            <>
              <div className="agenda-stats-row">
                <div className="agenda-stat-card">
                  <span>Tickets</span>
                  <strong>{activeTickets.length}</strong>
                </div>
                <div className="agenda-stat-card">
                  <span>Total cobrado</span>
                  <strong>{currencyFormatter.format(totalCollected)}</strong>
                </div>
                <div className="agenda-stat-card">
                  <span>Egresos</span>
                  <strong>{currencyFormatter.format(totalExpenses)}</strong>
                </div>
              </div>

              <p className="panel-subtitle">Tickets</p>
              <div className="agenda-list">
                {sessionTickets.length === 0 && <div className="empty-table-state">No hay tickets en este turno.</div>}
                {sessionTickets.map((ticket) => (
                  <div key={ticket.id} className={`agenda-list-row ticket-row ${ticket.status === "VOIDED" ? "ticket-voided" : ""}`}>
                    <div className="agenda-list-time">
                      Folio #{ticket.folio} · {formatTime(ticket.createdAt)}
                    </div>
                    <div className="agenda-list-info">
                      <strong>{patientName(patients, ticket.patientId)}</strong>
                      <span>
                        {ticket.concept || "Sin concepto"} · {paymentMethodsSummary(ticket.paymentLines)}
                      </span>
                    </div>
                    <span className="agenda-list-info">
                      <strong>{currencyFormatter.format(ticket.totalAmount)}</strong>
                    </span>
                    <span className={`agenda-status-badge ${ticket.status === "ACTIVE" ? "status-confirmed" : "status-cancelled"}`}>
                      {ticket.status === "ACTIVE" ? "Activo" : "Anulado"}
                    </span>
                    <span />
                  </div>
                ))}
              </div>

              <p className="panel-subtitle">Egresos</p>
              <div className="agenda-list">
                {sessionExpenses.length === 0 && <div className="empty-table-state">No hay egresos en este turno.</div>}
                {sessionExpenses.map((expense) => (
                  <div key={expense.id} className={`agenda-list-row ticket-row ${expense.status === "VOIDED" ? "ticket-voided" : ""}`}>
                    <div className="agenda-list-time">{formatTime(expense.createdAt)}</div>
                    <div className="agenda-list-info">
                      <strong>{expense.concept}</strong>
                    </div>
                    <span className="agenda-list-info">
                      <strong>{currencyFormatter.format(expense.amount)}</strong>
                    </span>
                    <span className={`agenda-status-badge ${expense.status === "ACTIVE" ? "status-confirmed" : "status-cancelled"}`}>
                      {expense.status === "ACTIVE" ? "Activo" : "Anulado"}
                    </span>
                    <span />
                  </div>
                ))}
              </div>
            </>
          )}

          <div className="form-actions">
            <button className="btn secondary" type="button" onClick={() => setSelectedSession(null)}>
              Volver al historial
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Historial de turnos</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>

        {error && <p className="alert error">{error}</p>}
        {loading && <p className="panel-subtitle">Cargando turnos...</p>}

        {!loading && (
          <div className="agenda-list">
            {sessions.length === 0 && <div className="empty-table-state">No hay turnos registrados todavía.</div>}
            {sessions.map((session) => (
              <div key={session.id} className="agenda-list-row ticket-row" onClick={() => openDetail(session)}>
                <div className="agenda-list-time">{formatTime(session.openedAt)}</div>
                <div className="agenda-list-info">
                  <strong>
                    {new Date(session.openedAt).toLocaleDateString("es-MX", { day: "2-digit", month: "short", year: "numeric" })}
                  </strong>
                  <span>Apertura: {currencyFormatter.format(session.openingAmount)}</span>
                </div>
                <span className="agenda-list-info">
                  <strong>{session.status === "CLOSED" ? currencyFormatter.format(session.countedCashAmount ?? 0) : "—"}</strong>
                </span>
                <span className={`agenda-status-badge ${session.status === "OPEN" ? "status-confirmed" : "status-completed"}`}>
                  {session.status === "OPEN" ? "Abierto" : "Cerrado"}
                </span>
                <button className="icon-btn" type="button" aria-label="Ver turno" onClick={(event) => { event.stopPropagation(); openDetail(session); }}>
                  <IconDotsVertical size={16} />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export function CajaScreen({
  clinicId,
  hasClinic,
  patients
}: {
  clinicId?: string;
  hasClinic: boolean;
  patients: PatientResponse[];
}) {
  const [doctors, setDoctors] = useState<DoctorResponse[]>([]);
  const [bankAccounts, setBankAccounts] = useState<BankAccountResponse[]>([]);
  const [currentSession, setCurrentSession] = useState<CashSessionResponse | null>(null);
  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [expenses, setExpenses] = useState<CashExpenseResponse[]>([]);
  const [pendingNotifications, setPendingNotifications] = useState<PendingAppointmentChargeResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [showOpenModal, setShowOpenModal] = useState(false);
  const [showCloseModal, setShowCloseModal] = useState(false);
  const [showNewTicketModal, setShowNewTicketModal] = useState(false);
  const [showNewExpenseModal, setShowNewExpenseModal] = useState(false);
  const [viewingTicket, setViewingTicket] = useState<TicketResponse | null>(null);
  const [viewingExpense, setViewingExpense] = useState<CashExpenseResponse | null>(null);
  const [showHistoryModal, setShowHistoryModal] = useState(false);
  const [ticketPreFill, setTicketPreFill] = useState<{
    patientId: string;
    concept: string;
    amount: number;
    quotationId?: string;
    doctorStaffId?: string;
  } | null>(null);


  useEffect(() => {
    if (!clinicId) return;
    agendaApi.listDoctors(clinicId).then(setDoctors).catch(() => setDoctors([]));
  }, [clinicId]);

  useEffect(() => {
    if (!clinicId) {
      setBankAccounts([]);
      return;
    }
    accountingApi
      .listBankAccounts(clinicId)
      .then((accounts) => setBankAccounts(accounts.filter((account) => account.active && account.accountType === "DEBIT")))
      .catch(() => setBankAccounts([]));
  }, [clinicId]);

  const loadCurrentSession = () => {
    if (!clinicId) return;
    setLoading(true);
    setError("");
    cashSessionsApi
      .getCurrent(clinicId)
      .then((session) => setCurrentSession(session ?? null))
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadCurrentSession();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId]);

  const loadTickets = (sessionId: string) => {
    if (!clinicId) return;
    ticketsApi
      .listBySession(clinicId, sessionId)
      .then(setTickets)
      .catch((caught) => setError(getFriendlyError(caught)));
  };

  const loadExpenses = (sessionId: string) => {
    if (!clinicId) return;
    cashExpensesApi
      .listBySession(clinicId, sessionId)
      .then(setExpenses)
      .catch((caught) => setError(getFriendlyError(caught)));
  };

  const loadPendingCharges = () => {
    if (!clinicId) {
      setPendingNotifications([]);
      return;
    }
    ticketsApi
      .listPendingAppointmentCharges(clinicId)
      .then(setPendingNotifications)
      .catch((caught) => setError(getFriendlyError(caught)));
  };

  useEffect(() => {
    loadPendingCharges();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId]);

  useEffect(() => {
    if (currentSession) {
      loadTickets(currentSession.id);
      loadExpenses(currentSession.id);
    } else {
      setTickets([]);
      setExpenses([]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentSession?.id]);

  if (!hasClinic || !clinicId) {
    return (
      <section className="dashboard-grid">
        <article className="panel full">
          <div className="panel-heading">
            <h2>Caja</h2>
          </div>
          <p className="panel-subtitle">Selecciona o crea una clínica para gestionar la caja.</p>
        </article>
      </section>
    );
  }

  const activeTickets = tickets.filter((ticket) => ticket.status === "ACTIVE");
  const totalCollected = activeTickets.reduce((sum, ticket) => sum + ticket.totalAmount, 0);
  const activeExpenses = expenses.filter((expense) => expense.status === "ACTIVE");
  const totalExpenses = activeExpenses.reduce((sum, expense) => sum + expense.amount, 0);

  return (
    <section className="dashboard-grid">
      <article className="panel full">
        <div className="panel-heading">
          <div className="inventory-heading-main">
            <h2>Caja</h2>
            <p className="panel-subtitle">
              {currentSession
                ? `Turno abierto desde ${formatDateTime(currentSession.openedAt)}`
                : "No hay un turno de caja abierto"}
            </p>
          </div>
          <div className="topbar-actions">
            <button className="btn ghost" type="button" onClick={() => setShowHistoryModal(true)}>
              <IconHistory size={16} />
              Historial de turnos
            </button>
            {!currentSession ? (
              <button className="btn primary" type="button" onClick={() => setShowOpenModal(true)}>
                <IconPlus size={16} />
                Abrir turno
              </button>
            ) : (
              <>
                <button className="btn primary" type="button" onClick={() => setShowNewTicketModal(true)}>
                  <IconPlus size={16} />
                  Nuevo ticket
                </button>
                <button className="btn secondary" type="button" onClick={() => setShowNewExpenseModal(true)}>
                  <IconMinus size={16} />
                  Nuevo egreso
                </button>
                <button className="btn destructive" type="button" onClick={() => setShowCloseModal(true)}>
                  Cerrar turno
                </button>
              </>
            )}
          </div>
        </div>

        {currentSession && (
          <div className="agenda-stats-row">
            <div className="agenda-stat-card">
              <span>Monto de apertura</span>
              <strong>{currencyFormatter.format(currentSession.openingAmount)}</strong>
            </div>
            <div className="agenda-stat-card">
              <span>Tickets registrados</span>
              <strong>{activeTickets.length}</strong>
            </div>
            <div className="agenda-stat-card">
              <span>Total cobrado</span>
              <strong>{currencyFormatter.format(totalCollected)}</strong>
            </div>
            <div className="agenda-stat-card">
              <span>Egresos</span>
              <strong>{currencyFormatter.format(totalExpenses)}</strong>
            </div>
          </div>
        )}

        {pendingNotifications.length > 0 && (
          <div style={{
            margin: "0 var(--space-6) var(--space-6)",
            padding: "var(--space-4)",
            backgroundColor: "var(--color-card)",
            border: "1px solid var(--color-border)",
            borderRadius: "8px",
            display: "flex",
            flexDirection: "column",
            gap: "var(--space-3)"
          }}>
            <h4 style={{ margin: 0, color: "var(--color-text-1)", display: "flex", alignItems: "center", gap: "var(--space-2)", fontSize: "14px", fontWeight: "600" }}>
              <span style={{
                display: "inline-block",
                width: "8px",
                height: "8px",
                backgroundColor: "var(--color-error)",
                borderRadius: "50%"
              }} />
              Cobros Pendientes de Citas Completadas ({pendingNotifications.length})
            </h4>
            <div style={{ display: "grid", gridTemplateColumns: "1fr", gap: "var(--space-2)" }}>
              {pendingNotifications.map((notif) => (
                <div
                  key={notif.appointmentId}
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    backgroundColor: "var(--color-surface)",
                    padding: "var(--space-3) var(--space-4)",
                    borderRadius: "6px",
                    borderLeft: "4px solid var(--color-primary)",
                    boxShadow: "var(--shadow-sm)"
                  }}
                >
                  <div style={{ display: "flex", flexDirection: "column", gap: "2px" }}>
                    <strong style={{ fontSize: "14px" }}>{patientName(patients, notif.patientId)}</strong>
                    <span style={{ fontSize: "12px", color: "var(--color-text-3)" }}>
                      {notif.concept} · Total estimado: <strong>{currencyFormatter.format(notif.amount)}</strong>
                    </span>
                  </div>
                  <button
                    className="btn primary"
                    type="button"
                    disabled={!currentSession}
                    style={{ padding: "var(--space-1) var(--space-3)", fontSize: "12px", height: "30px", display: "inline-flex", alignItems: "center" }}
                    onClick={() => {
                      setTicketPreFill({
                        patientId: notif.patientId,
                        concept: notif.concept,
                        amount: notif.amount,
                        quotationId: notif.quotationId,
                        doctorStaffId: notif.doctorStaffId
                      });
                      setShowNewTicketModal(true);
                    }}
                  >
                    {!currentSession ? "Abrir turno para cobrar" : "Cobrar ahora"}
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {error && <p className="alert error">{error}</p>}

        {loading && <p className="panel-subtitle">Cargando turno...</p>}

        {currentSession && (
          <div className="agenda-list">
            {tickets.length === 0 && <div className="empty-table-state">No hay tickets registrados en este turno.</div>}
            {tickets.map((ticket) => (
              <div
                key={ticket.id}
                className={`agenda-list-row ticket-row ${ticket.status === "VOIDED" ? "ticket-voided" : ""}`}
                onClick={() => setViewingTicket(ticket)}
              >
                <div className="agenda-list-time">Folio #{ticket.folio} · {formatTime(ticket.createdAt)}</div>
                <div className="agenda-list-info">
                  <strong>{patientName(patients, ticket.patientId)}</strong>
                  <span>
                    {ticket.concept || "Sin concepto"} · {paymentMethodsSummary(ticket.paymentLines)}
                  </span>
                </div>
                <span className="agenda-list-info">
                  <strong>{currencyFormatter.format(ticket.totalAmount)}</strong>
                </span>
                <span className={`agenda-status-badge ${ticket.status === "ACTIVE" ? "status-confirmed" : "status-cancelled"}`}>
                  {ticket.status === "ACTIVE" ? "Activo" : "Anulado"}
                </span>
                <button
                  className="icon-btn"
                  type="button"
                  aria-label="Ver ticket"
                  onClick={(event) => {
                    event.stopPropagation();
                    setViewingTicket(ticket);
                  }}
                >
                  <IconDotsVertical size={16} />
                </button>
              </div>
            ))}
          </div>
        )}

        {currentSession && (
          <>
            <p className="panel-subtitle">Egresos del turno</p>
            <div className="agenda-list">
              {expenses.length === 0 && (
                <div className="empty-table-state">No hay egresos registrados en este turno.</div>
              )}
              {expenses.map((expense) => (
                <div
                  key={expense.id}
                  className={`agenda-list-row ticket-row ${expense.status === "VOIDED" ? "ticket-voided" : ""}`}
                  onClick={() => setViewingExpense(expense)}
                >
                  <div className="agenda-list-time">{formatTime(expense.createdAt)}</div>
                  <div className="agenda-list-info">
                    <strong>{expense.concept}</strong>
                  </div>
                  <span className="agenda-list-info">
                    <strong>{currencyFormatter.format(expense.amount)}</strong>
                  </span>
                  <span className={`agenda-status-badge ${expense.status === "ACTIVE" ? "status-confirmed" : "status-cancelled"}`}>
                    {expense.status === "ACTIVE" ? "Activo" : "Anulado"}
                  </span>
                  <button
                    className="icon-btn"
                    type="button"
                    aria-label="Ver egreso"
                    onClick={(event) => {
                      event.stopPropagation();
                      setViewingExpense(expense);
                    }}
                  >
                    <IconDotsVertical size={16} />
                  </button>
                </div>
              ))}
            </div>
          </>
        )}
      </article>

      {showOpenModal && (
        <OpenSessionModal
          clinicId={clinicId}
          doctors={doctors}
          onClose={() => setShowOpenModal(false)}
          onSaved={() => {
            setShowOpenModal(false);
            loadCurrentSession();
          }}
        />
      )}

      {showCloseModal && currentSession && (
        <CloseSessionModal
          clinicId={clinicId}
          currentSession={currentSession}
          doctors={doctors}
          onClose={() => setShowCloseModal(false)}
          onClosed={() => {
            setShowCloseModal(false);
            loadCurrentSession();
          }}
        />
      )}

      {showNewTicketModal && (
        <NewTicketModal
          clinicId={clinicId}
          patients={patients}
          doctors={doctors}
          bankAccounts={bankAccounts}
          preFill={ticketPreFill || undefined}
          onClose={() => {
            setShowNewTicketModal(false);
            setTicketPreFill(null);
          }}
          onSaved={() => {
            setShowNewTicketModal(false);
            setTicketPreFill(null);
            if (currentSession) loadTickets(currentSession.id);
            loadPendingCharges();
          }}
        />
      )}

      {viewingTicket && (
        <TicketDetailModal
          clinicId={clinicId}
          ticket={viewingTicket}
          patients={patients}
          doctors={doctors}
          bankAccounts={bankAccounts}
          onClose={() => setViewingTicket(null)}
          onChanged={(updated) => {
            setTickets((prev) => prev.map((ticket) => (ticket.id === updated.id ? updated : ticket)));
            setViewingTicket(updated);
            loadPendingCharges();
          }}
        />
      )}

      {showNewExpenseModal && (
        <NewExpenseModal
          clinicId={clinicId}
          doctors={doctors}
          onClose={() => setShowNewExpenseModal(false)}
          onSaved={() => {
            setShowNewExpenseModal(false);
            if (currentSession) loadExpenses(currentSession.id);
          }}
        />
      )}

      {viewingExpense && (
        <ExpenseDetailModal
          clinicId={clinicId}
          expense={viewingExpense}
          doctors={doctors}
          onClose={() => setViewingExpense(null)}
          onChanged={(updated) => {
            setExpenses((prev) => prev.map((expense) => (expense.id === updated.id ? updated : expense)));
            setViewingExpense(updated);
          }}
        />
      )}

      {showHistoryModal && (
        <SessionHistoryModal clinicId={clinicId} patients={patients} onClose={() => setShowHistoryModal(false)} />
      )}
    </section>
  );
}
