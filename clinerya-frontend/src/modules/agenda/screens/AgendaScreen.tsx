import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import {
  IconCalendar,
  IconChevronLeft,
  IconChevronRight,
  IconClock,
  IconDotsVertical,
  IconLayoutBoard,
  IconList,
  IconPlus,
  IconSearch,
  IconX,
  IconInbox,
  IconHeartHandshake
} from "@tabler/icons-react";
import {
  agendaApi,
  treatmentCatalogApi,
  clinicRoomsApi,
  clinicScheduleApi,
  getFriendlyError,
  materialsApi,
  quotationsApi,
  visitsApi,
  waitingListApi
} from "@shared/api/api";
import {
  APPOINTMENT_STATUS_LABELS,
  type AppointmentResponse,
  type AppointmentStatus,
  type ClinicRoomResponse,
  type DayScheduleRequest,
  type DoctorResponse,
  type RoomBlockResponse,
  type RoomBlockType,
  type WaitingListEntryResponse,
  type WeekDay
} from "@modules/agenda/types";
import { isAppointmentReadyToStart } from "@shared/utils/appointmentTime";
import type { QuotationResponse } from "@modules/treatments/quotationTypes";
import type { MaterialResponse } from "@modules/inventory/types";
import type { PatientResponse } from "@modules/patients/types";
import { CustomDateTimePicker } from "@modules/agenda/components/CustomDateTimePicker";
import { endForService, estimatedRevenue as computeEstimatedRevenue } from "@modules/agenda/logic/revenue";
import type { TreatmentCatalogItemResponse } from "@modules/treatments/types";
import { CalendarSlotPickerModal } from "@modules/agenda/components/CalendarSlotPickerModal";

interface EditableMaterialUsage {
  materialId?: string;
  materialName: string;
  estimatedQuantity?: number;
  actualQuantity: string;
  quotationItemId?: string;
}

const DAY_LABELS = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"];
const GRID_START_HOUR = 7;
const GRID_END_HOUR = 21;
const HOUR_HEIGHT_PX = 56;
const currencyFormatter = new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN", maximumFractionDigits: 0 });

const WEEKDAY_ORDER: WeekDay[] = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
const WEEKDAY_LABELS: Record<WeekDay, string> = {
  MONDAY: "Lunes",
  TUESDAY: "Martes",
  WEDNESDAY: "Miércoles",
  THURSDAY: "Jueves",
  FRIDAY: "Viernes",
  SATURDAY: "Sábado",
  SUNDAY: "Domingo"
};

function toInputTime(value: string | null): string {
  return value ? value.slice(0, 5) : "";
}

function toApiTime(value: string): string | null {
  return value ? (value.length === 5 ? `${value}:00` : value) : null;
}

type ViewMode = "list" | "week" | "board";

function startOfDay(date: Date): Date {
  const copy = new Date(date);
  copy.setHours(0, 0, 0, 0);
  return copy;
}

function startOfWeek(date: Date): Date {
  const day = date.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  const monday = startOfDay(date);
  monday.setDate(monday.getDate() + diff);
  return monday;
}

function addDays(date: Date, days: number): Date {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}

function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

function toLocalDateTimeInput(date: Date): string {
  const pad = (num: number) => String(num).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function toIsoDateInput(date: Date): string {
  const pad = (num: number) => String(num).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function formatDayHeader(date: Date): string {
  return date.toLocaleDateString("es-MX", { day: "2-digit", month: "short" });
}

function formatLongDate(date: Date): string {
  const raw = date.toLocaleDateString("es-MX", { weekday: "long", day: "numeric", month: "long", year: "numeric" });
  return raw.charAt(0).toUpperCase() + raw.slice(1);
}

function patientName(patients: PatientResponse[], patientId?: string): string {
  if (!patientId) return "Paciente no registrado";
  const found = patients.find((item) => item.id === patientId);
  return found ? `${found.firstName} ${found.lastNamePaterno}` : "Paciente desconocido";
}

function doctorName(doctors: DoctorResponse[], doctorStaffId: string): string {
  const found = doctors.find((item) => item.staffId === doctorStaffId);
  return found ? found.fullName : "Doctor desconocido";
}

function isActiveStatus(status: AppointmentStatus): boolean {
  return status !== "CANCELLED" && status !== "NO_SHOW";
}

const STATUS_CLASS: Record<AppointmentStatus, string> = {
  SCHEDULED: "status-scheduled",
  CONFIRMED: "status-confirmed",
  COMPLETED: "status-completed",
  CANCELLED: "status-cancelled",
  NO_SHOW: "status-no-show"
};

function PatientSearchSelect({
  patients,
  value,
  onChange,
  allowNew = true,
  unassignedLabel = "Sin asignar u motivo indistinto"
}: {
  patients: PatientResponse[];
  value: string;
  onChange: (patientId: string) => void;
  allowNew?: boolean;
  unassignedLabel?: string;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState("");
  const containerRef = useRef<HTMLDivElement>(null);

  const selectedPatient = patients.find((p) => p.id === value);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const filtered = useMemo(() => {
    if (!query.trim()) return patients;
    const q = query.toLowerCase();
    return patients.filter((p) => {
      const fullName = `${p.firstName} ${p.lastNamePaterno} ${p.lastNameMaterno ?? ""}`.toLowerCase();
      return fullName.includes(q);
    });
  }, [patients, query]);

  const displayValue = () => {
    if (isOpen) return query;
    if (selectedPatient) return `${selectedPatient.firstName} ${selectedPatient.lastNamePaterno} ${selectedPatient.lastNameMaterno ?? ""}`;
    if (value === "") return unassignedLabel;
    return "";
  };

  return (
    <div className="patient-search-select" ref={containerRef} style={{ position: "relative" }}>
      <div style={{ display: "flex", alignItems: "center", position: "relative" }}>
        <input
          type="text"
          placeholder="Buscar paciente por nombre..."
          value={displayValue()}
          onFocus={() => {
            setIsOpen(true);
            setQuery("");
          }}
          onChange={(e) => {
            setQuery(e.target.value);
            if (!isOpen) setIsOpen(true);
          }}
          style={{ width: "100%", paddingRight: "32px" }}
        />
        {value !== "" ? (
          <button
            type="button"
            className="icon-btn"
            style={{ position: "absolute", right: "6px", background: "none", border: "none", cursor: "pointer", opacity: 0.6 }}
            onClick={(e) => {
              e.stopPropagation();
              onChange("");
              setQuery("");
              setIsOpen(true);
            }}
            title="Limpiar selección"
          >
            <IconX size={16} />
          </button>
        ) : (
          <div style={{ position: "absolute", right: "8px", pointerEvents: "none", opacity: 0.5, display: "flex", alignItems: "center" }}>
            <IconSearch size={16} />
          </div>
        )}
      </div>

      {isOpen && (
        <div
          style={{
            position: "absolute",
            top: "100%",
            left: 0,
            right: 0,
            zIndex: 999,
            maxHeight: "220px",
            overflowY: "auto",
            backgroundColor: "var(--card-bg, #ffffff)",
            border: "1px solid var(--border-color, #cbd5e1)",
            borderRadius: "6px",
            boxShadow: "0 10px 25px -5px rgba(0,0,0,0.15), 0 8px 10px -6px rgba(0,0,0,0.1)",
            marginTop: "4px"
          }}
        >
          {allowNew && (
            <div
              style={{
                padding: "9px 12px",
                cursor: "pointer",
                borderBottom: "1px solid var(--border-color, #e2e8f0)",
                fontSize: "13px",
                fontWeight: value === "" ? 600 : 400,
                color: value === "" ? "#0284c7" : "inherit",
                backgroundColor: value === "" ? "rgba(2,132,199,0.08)" : "transparent"
              }}
              onClick={() => {
                onChange("");
                setIsOpen(false);
              }}
            >
              ✨ <strong>Paciente nuevo</strong> (Sin registrar / Primera vez)
            </div>
          )}

          {filtered.map((p) => {
            const isSel = p.id === value;
            return (
              <div
                key={p.id}
                style={{
                  padding: "9px 12px",
                  cursor: "pointer",
                  fontSize: "13px",
                  fontWeight: isSel ? 600 : 400,
                  backgroundColor: isSel ? "rgba(2,132,199,0.08)" : "transparent",
                  color: isSel ? "#0284c7" : "inherit"
                }}
                onClick={() => {
                  onChange(p.id);
                  setIsOpen(false);
                }}
              >
                {p.firstName} {p.lastNamePaterno} {p.lastNameMaterno ?? ""}
              </div>
            );
          })}

          {filtered.length === 0 && (
            <div style={{ padding: "12px", fontSize: "13px", color: "var(--text-muted, #64748b)", textAlign: "center" }}>
              Sin resultados para "{query}"
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function AppointmentFormModal({
  clinicId,
  patients,
  doctors,
  rooms,
  appointments = [],
  defaultStart,
  onClose,
  onSaved
}: {
  clinicId: string;
  patients: PatientResponse[];
  doctors: DoctorResponse[];
  rooms: ClinicRoomResponse[];
  appointments?: AppointmentResponse[];
  defaultStart: Date;
  onClose: () => void;
  onSaved: () => void;
}) {
  const defaultEnd = new Date(defaultStart.getTime() + 30 * 60000);
  const [patientId, setPatientId] = useState("");
  const [doctorStaffId, setDoctorStaffId] = useState(doctors[0]?.staffId ?? "");
  const [roomId, setRoomId] = useState("");
  const [scheduledStart, setScheduledStart] = useState(toLocalDateTimeInput(defaultStart));
  const [scheduledEnd, setScheduledEnd] = useState(toLocalDateTimeInput(defaultEnd));
  const [reason, setReason] = useState("");
  const [notes, setNotes] = useState("");
  const [quotations, setQuotations] = useState<QuotationResponse[]>([]);
  const [quotationId, setQuotationId] = useState("");
  const [quotationItemIds, setQuotationItemIds] = useState<string[]>([]);
  const [showSlotPicker, setShowSlotPicker] = useState(false);
  const [services, setServices] = useState<TreatmentCatalogItemResponse[]>([]);
  const [serviceId, setServiceId] = useState("");

  useEffect(() => {
    treatmentCatalogApi.list(clinicId, false).then(setServices).catch(() => setServices([]));
  }, [clinicId]);

  const selectedService = services.find((service) => service.id === serviceId);

  /** Al elegir el servicio, la cita dura lo que el servicio. */
  const chooseService = (id: string) => {
    setServiceId(id);
    const service = services.find((candidate) => candidate.id === id);
    if (service?.estimatedDurationMinutes && scheduledStart) {
      setScheduledEnd(endForService(scheduledStart, service.estimatedDurationMinutes));
    }
  };

  // Recurrence state
  const [isSeries, setIsSeries] = useState(false);
  const [frequency, setFrequency] = useState<"DAILY" | "WEEKLY" | "BIWEEKLY" | "MONTHLY">("WEEKLY");
  const [repeatCount, setRepeatCount] = useState(4);

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!patientId) {
      setQuotations([]);
      setQuotationId("");
      setQuotationItemIds([]);
      return;
    }
    quotationsApi
      .listByPatient(patientId, clinicId)
      .then((list) => setQuotations(list.filter((q) => q.status === "ACCEPTED")))
      .catch(() => setQuotations([]));
  }, [patientId, clinicId]);

  const selectedQuotation = quotations.find((q) => q.id === quotationId);

  const setDurationMinutes = (mins: number) => {
    if (!scheduledStart) return;
    const startDt = new Date(scheduledStart);
    const endDt = new Date(startDt.getTime() + mins * 60000);
    setScheduledEnd(toLocalDateTimeInput(endDt));
  };

  const handleStartChange = (newStartStr: string) => {
    setScheduledStart(newStartStr);
    if (!newStartStr) return;

    const oldStartMs = scheduledStart ? new Date(scheduledStart).getTime() : 0;
    const oldEndMs = scheduledEnd ? new Date(scheduledEnd).getTime() : 0;
    let durationMs = 30 * 60000;
    if (oldStartMs > 0 && oldEndMs > oldStartMs) {
      durationMs = oldEndMs - oldStartMs;
    }

    const newStartDt = new Date(newStartStr);
    if (!isNaN(newStartDt.getTime())) {
      const newEndDt = new Date(newStartDt.getTime() + durationMs);
      setScheduledEnd(toLocalDateTimeInput(newEndDt));
    }
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    if (!doctorStaffId) {
      setError("Selecciona un doctor.");
      return;
    }
    setSaving(true);
    try {
      if (isSeries) {
        await agendaApi.createSeries(clinicId, {
          patientId: patientId || undefined,
          doctorStaffId,
          roomId: roomId || undefined,
          quotationId: quotationId || undefined,
          quotationItemId: quotationItemIds[0] || undefined,
          quotationItemIds: quotationItemIds.length > 0 ? quotationItemIds : undefined,
          firstScheduledStart: `${scheduledStart}:00`,
          firstScheduledEnd: `${scheduledEnd}:00`,
          frequency,
          repeatCount,
          reason: reason.trim() || undefined,
          notes: notes.trim() || undefined
        });
      } else {
        await agendaApi.create(clinicId, {
          patientId: patientId || undefined,
          doctorStaffId,
          roomId: roomId || undefined,
          quotationId: quotationId || undefined,
          quotationItemId: quotationItemIds[0] || undefined,
          quotationItemIds: quotationItemIds.length > 0 ? quotationItemIds : undefined,
          scheduledStart: `${scheduledStart}:00`,
          scheduledEnd: `${scheduledEnd}:00`,
          reason: reason.trim() || selectedService?.name || undefined,
          notes: notes.trim() || undefined,
          serviceId: serviceId || undefined
        });
      }
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
          <h2>Nueva cita {isSeries ? "(Serie Recurrente)" : ""}</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>
        <form className="profile-form" onSubmit={submit}>
          <label className="field">
            <span>Paciente</span>
            <PatientSearchSelect
              patients={patients}
              value={patientId}
              onChange={setPatientId}
              allowNew
            />
          </label>

          <label className="field">
            <span>Doctor</span>
            <select value={doctorStaffId} onChange={(event) => setDoctorStaffId(event.target.value)} required>
              <option value="">Selecciona un doctor</option>
              {doctors.map((doctor) => (
                <option key={doctor.staffId} value={doctor.staffId}>
                  {doctor.fullName}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Servicio</span>
            <select value={serviceId} onChange={(event) => chooseService(event.target.value)} disabled={isSeries}>
              <option value="">Otro / sin servicio</option>
              {services.map((service) => (
                <option key={service.id} value={service.id}>
                  {service.name}
                  {service.estimatedDurationMinutes ? ` · ${service.estimatedDurationMinutes} min` : ""}
                  {service.pricingType === "VARIES_BY_PATIENT"
                    ? " · precio por definir"
                    : service.defaultPrice != null ? ` · ${currencyFormatter.format(service.defaultPrice)} (fijo)` : ""}
                </option>
              ))}
            </select>
            {selectedService && (
              <small className="agenda-service-note">
                {selectedService.pricingType === "VARIES_BY_PATIENT"
                  ? "Precio por definir: se acuerda en la valoración y no suma al ingreso estimado."
                  : `Precio de la cita: ${currencyFormatter.format(selectedService.defaultPrice ?? 0)} (precio fijo, se guarda con la cita aunque después cambie el catálogo).`}
              </small>
            )}
          </label>

          {rooms.length > 0 && (
            <label className="field">
              <span>Consultorio / Sillón</span>
              <select value={roomId} onChange={(event) => setRoomId(event.target.value)}>
                <option value="">Sin asignar / Cualquiera</option>
                {rooms.map((room) => (
                  <option key={room.id} value={room.id}>
                    {room.name} {room.code ? `(${room.code})` : ""}
                  </option>
                ))}
              </select>
            </label>
          )}

          <label className="field">
            <span>Inicio</span>
            <CustomDateTimePicker
              value={scheduledStart}
              onChange={(val) => handleStartChange(val)}
              onOpenPicker={() => setShowSlotPicker(true)}
              required
            />
          </label>

          <label className="field">
            <span>Fin</span>
            <CustomDateTimePicker
              value={scheduledEnd}
              onChange={(val) => setScheduledEnd(val)}
              onOpenPicker={() => setShowSlotPicker(true)}
              required
            />
          </label>

          <div className="field field-full">
            <span>Duración rápida</span>
            <div style={{ display: "flex", gap: "6px", marginTop: "4px" }}>
              {[15, 30, 45, 60, 90, 120].map((mins) => (
                <button
                  key={mins}
                  type="button"
                  className="btn ghost"
                  style={{ padding: "4px 8px", fontSize: "0.8rem" }}
                  onClick={() => setDurationMinutes(mins)}
                >
                  {mins} min
                </button>
              ))}
            </div>
          </div>

          {showSlotPicker && (
            <CalendarSlotPickerModal
              clinicId={clinicId}
              doctorStaffId={doctorStaffId}
              doctors={doctors}
              appointments={appointments}
              initialStartStr={scheduledStart}
              currentDurationMins={
                scheduledStart && scheduledEnd
                  ? Math.max(15, Math.round((new Date(scheduledEnd).getTime() - new Date(scheduledStart).getTime()) / 60000))
                  : 30
              }
              onSelectSlot={(newStart, newEnd) => {
                setScheduledStart(newStart);
                setScheduledEnd(newEnd);
              }}
              onClose={() => setShowSlotPicker(false)}
            />
          )}

          <label className="field field-full">
            <span>Motivo</span>
            <input type="text" value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Revisión, limpieza, endodoncia..." />
          </label>

          {quotations.length > 0 && (
            <>
              <label className="field">
                <span>Cotización aceptada (opcional)</span>
                <select value={quotationId} onChange={(event) => { setQuotationId(event.target.value); setQuotationItemIds([]); }}>
                  <option value="">Sin vincular</option>
                  {quotations.map((quotation) => (
                    <option key={quotation.id} value={quotation.id}>
                      {quotation.quotationDate} — {quotation.grandTotal}
                    </option>
                  ))}
                </select>
              </label>
              {selectedQuotation && (
                <div className="field field-full">
                  <span>Partidas de la cotización (opcional)</span>
                  <div style={{ display: "grid", gap: "8px", marginTop: "6px", maxHeight: "150px", overflowY: "auto", border: "1px solid var(--color-border)", padding: "8px", borderRadius: "4px" }}>
                    {selectedQuotation.items.map((item) => (
                      <label key={item.id} className="checkbox-field" style={{ justifyContent: "space-between" }}>
                        <span>
                          <input
                            type="checkbox"
                            checked={quotationItemIds.includes(item.id)}
                            onChange={(event) => setQuotationItemIds((current) => event.target.checked
                              ? [...current, item.id]
                              : current.filter((id) => id !== item.id))}
                          />
                          {item.description}
                        </span>
                        <small style={{ color: "var(--color-text-2)" }}>${item.subtotal}</small>
                      </label>
                    ))}
                  </div>
                  <small style={{ color: "var(--color-text-2)" }}>
                    Seleccionadas: {quotationItemIds.length}
                  </small>
                </div>
              )}
            </>
          )}

          <div className="field field-full" style={{ marginTop: "8px", borderTop: "1px solid #e2e8f0", paddingTop: "8px" }}>
            <label className="checkbox-field">
              <input type="checkbox" checked={isSeries} onChange={(e) => setIsSeries(e.target.checked)} />
              <strong>¿Cita recurrente (Serie de sesiones)?</strong>
            </label>
          </div>

          {isSeries && (
            <>
              <label className="field">
                <span>Frecuencia</span>
                <select value={frequency} onChange={(e) => setFrequency(e.target.value as any)}>
                  <option value="DAILY">Diaria</option>
                  <option value="WEEKLY">Semanal</option>
                  <option value="BIWEEKLY">Quincenal (Cada 2 semanas)</option>
                  <option value="MONTHLY">Mensual</option>
                </select>
              </label>

              <label className="field">
                <span>Número de sesiones</span>
                <input
                  type="number"
                  min={2}
                  max={52}
                  value={repeatCount}
                  onChange={(e) => setRepeatCount(parseInt(e.target.value) || 2)}
                />
              </label>
            </>
          )}

          <label className="field field-full">
            <span>Notas</span>
            <textarea value={notes} onChange={(event) => setNotes(event.target.value)} />
          </label>

          {error && <p className="alert error">{error}</p>}
          <div className="form-actions">
            <button className="btn primary" type="submit" disabled={saving}>
              {saving ? "Guardando..." : isSeries ? `Agendar serie (${repeatCount} citas)` : "Agendar cita"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function AppointmentDetailModal({
  clinicId,
  appointment,
  patients,
  doctors,
  rooms,
  onClose,
  onChanged,
  onDeleted,
  onStartCare
}: {
  clinicId: string;
  appointment: AppointmentResponse;
  patients: PatientResponse[];
  doctors: DoctorResponse[];
  rooms: ClinicRoomResponse[];
  onClose: () => void;
  onChanged: (updated: AppointmentResponse) => void;
  onDeleted: (appointmentId: string) => void;
  onStartCare?: (patient: PatientResponse, appointment?: AppointmentResponse) => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [completing, setCompleting] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [cancellingPrompt, setCancellingPrompt] = useState(false);
  const [cancellationReasonInput, setCancellationReasonInput] = useState("");
  const [materialsCatalog, setMaterialsCatalog] = useState<MaterialResponse[]>([]);
  const [usageLines, setUsageLines] = useState<EditableMaterialUsage[]>([]);

  const transition = async (status: AppointmentStatus, cancellationReason?: string) => {
    setBusy(true);
    setError("");
    try {
      const updated = await agendaApi.transitionStatus(clinicId, appointment.id, status, cancellationReason);
      onChanged(updated);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy(false);
    }
  };

  const roomObj = rooms.find((r) => r.id === appointment.roomId);

  const confirmCancel = () => {
    transition("CANCELLED", cancellationReasonInput.trim() || undefined);
    setCancellingPrompt(false);
  };

  const handleStartCare = async () => {
    if (!onStartCare || !appointment.patientId) return;
    setBusy(true);
    setError("");
    try {
      let currentAppt = appointment;
      if (appointment.status === "SCHEDULED") {
        currentAppt = await agendaApi.transitionStatus(clinicId, appointment.id, "CONFIRMED");
        onChanged(currentAppt);
      }
      onClose();
      const pObj = patients.find((p) => p.id === currentAppt.patientId);
      if (pObj) {
        onStartCare(pObj, currentAppt);
      } else {
        onStartCare(
          {
            id: currentAppt.patientId || "",
            clinicId: clinicId || "",
            firstName: "Paciente",
            lastNamePaterno: "Nuevo"
          } as PatientResponse,
          currentAppt
        );
      }
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy(false);
    }
  };

  const selectedQuotationItemIds = appointment.quotationItemIds?.length
    ? appointment.quotationItemIds
    : appointment.quotationItemId
      ? [appointment.quotationItemId]
      : [];
  const hasQuotationItem = Boolean(appointment.quotationId && selectedQuotationItemIds.length > 0);

  const startCompletion = async () => {
    if (!appointment.patientId) {
      transition("COMPLETED");
      return;
    }
    setBusy(true);
    setError("");
    try {
      if (appointment.quotationId) {

        const [quotation, materials] = await Promise.all([
          quotationsApi.get(appointment.patientId, appointment.quotationId, clinicId),
          materialsApi.list(clinicId)
        ]);
        const selectedItems = quotation.items.filter((item) => selectedQuotationItemIds.includes(item.id));
        setMaterialsCatalog(materials);
        setUsageLines(
          selectedItems.flatMap((item) => (item.materials ?? []).map((material) => ({
            materialId: material.materialId,
            materialName: material.materialName,
            estimatedQuantity: material.estimatedQuantity,
            actualQuantity: String(material.estimatedQuantity),
            quotationItemId: item.id
          })))
        );
      } else {
        const materials = await materialsApi.list(clinicId);
        setMaterialsCatalog(materials);
        setUsageLines([]);
      }
      setCompleting(true);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy(false);
    }
  };

  const updateUsageLine = (index: number, patch: Partial<EditableMaterialUsage>) => {
    setUsageLines((prev) => prev.map((line, i) => (i === index ? { ...line, ...patch } : line)));
  };

  const removeUsageLine = (index: number) => {
    setUsageLines((prev) => prev.filter((_, i) => i !== index));
  };

  const addInventoryUsageLine = (materialId: string) => {
    if (!materialId) return;
    const material = materialsCatalog.find((m) => m.id === materialId);
    if (!material || usageLines.some((line) => line.materialId === materialId)) return;
    setUsageLines((prev) => [...prev, { materialId: material.id, materialName: material.name, actualQuantity: "1" }]);
  };

  const addCustomUsageLine = () => {
    setUsageLines((prev) => [...prev, { materialName: "Material personalizado", actualQuantity: "1" }]);
  };

  const confirmCompletion = async () => {
    setError("");
    for (const line of usageLines) {
      const qty = parseFloat(line.actualQuantity);
      if (isNaN(qty) || qty <= 0) {
        setError(`La cantidad de '${line.materialName}' debe ser un número mayor a cero.`);
        return;
      }
    }
    setBusy(true);
    try {
      const materialsUsed = usageLines.map((line) => ({
        materialId: line.materialId,
        materialName: line.materialName,
        actualQuantity: parseFloat(line.actualQuantity)
      }));

      if (hasQuotationItem) {
        const materialsByQuotationItem = new Map<string, typeof materialsUsed>();
        materialsUsed.forEach((material, index) => {
          const quotationItemId = usageLines[index]?.quotationItemId || selectedQuotationItemIds[0];
          if (!quotationItemId) return;
          const existing = materialsByQuotationItem.get(quotationItemId) || [];
          existing.push(material);
          materialsByQuotationItem.set(quotationItemId, existing);
        });

        await visitsApi.create(appointment.patientId!, appointment.quotationId!, {
          clinicId,
          visitDate: appointment.scheduledStart.slice(0, 10),
          doctorId: appointment.doctorStaffId,
          notes: `Sesión completada desde Agenda — ${appointment.reason ?? "cita"}`,
          items: selectedQuotationItemIds.map((quotationItemId) => ({
            quotationItemId,
            materialsUsed: materialsByQuotationItem.get(quotationItemId) || []
          }))
        });
      } else if (materialsUsed.length > 0) {
        await visitsApi.createGeneral(appointment.patientId!, {
          clinicId,
          visitDate: appointment.scheduledStart.slice(0, 10),
          doctorId: appointment.doctorStaffId,
          notes: `Sesión completada desde Agenda — ${appointment.reason ?? "cita"}`,
          items: [{ materialsUsed }]
        });
      }


      const updated = await agendaApi.transitionStatus(clinicId, appointment.id, "COMPLETED");
      onChanged(updated);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy(false);
    }
  };

  const isTerminal = appointment.status === "COMPLETED" || appointment.status === "CANCELLED" || appointment.status === "NO_SHOW";

  const handleDelete = async () => {
    setBusy(true);
    setError("");
    try {
      await agendaApi.deleteAppointment(clinicId, appointment.id);
      onDeleted(appointment.id);
    } catch (caught) {
      setError(getFriendlyError(caught));
      setConfirmingDelete(false);
    } finally {
      setBusy(false);
    }
  };

  if (completing) {
    return (
      <div className="modal-overlay" onClick={onClose}>
        <div className="modal-card modal-card-wide" onClick={(event) => event.stopPropagation()}>
          <div className="panel-heading">
            <h2>Confirmar materiales usados</h2>
            <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
              <IconX size={18} />
            </button>
          </div>
          <p className="panel-subtitle">
            {hasQuotationItem
              ? "Compara lo presupuestado contra lo realmente usado en esta sesión antes de completarla. El inventario se descontará según la cantidad real que confirmes."
              : "Esta cita no está ligada a una cotización. Si se usó algún material (por ejemplo, en una urgencia), agrégalo aquí para descontarlo del inventario; si no se usó nada, puedes completar sin agregar materiales."}
          </p>

          <div className="table-wrapper">
            <table className="data-table no-row-click">
              <thead>
                <tr>
                  <th>Material</th>
                  <th>Presupuestado</th>
                  <th>Usado realmente</th>
                  <th aria-label="Quitar" />
                </tr>
              </thead>
              <tbody>
                {usageLines.map((line, index) => (
                  <tr key={index}>
                    <td>
                      {line.materialId ? (
                        line.materialName
                      ) : (
                        <input
                          type="text"
                          value={line.materialName}
                          onChange={(event) => updateUsageLine(index, { materialName: event.target.value })}
                        />
                      )}
                    </td>
                    <td>{line.estimatedQuantity !== undefined ? line.estimatedQuantity : "No presupuestado"}</td>
                    <td>
                      <input
                        type="number"
                        step="any"
                        min="0.0001"
                        value={line.actualQuantity}
                        onChange={(event) => updateUsageLine(index, { actualQuantity: event.target.value })}
                        style={{ width: "80px" }}
                      />
                    </td>
                    <td>
                      <button className="icon-btn" type="button" aria-label="Quitar material" onClick={() => removeUsageLine(index)}>
                        <IconX size={14} />
                      </button>
                    </td>
                  </tr>
                ))}
                {usageLines.length === 0 && (
                  <tr>
                    <td colSpan={4}>
                      <div className="empty-table-state">
                        {hasQuotationItem
                          ? "Esta partida no tiene materiales presupuestados."
                          : "No se ha agregado ningún material para esta cita."}
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="form-actions" style={{ justifyContent: "flex-start" }}>
            <select
              defaultValue=""
              onChange={(event) => {
                addInventoryUsageLine(event.target.value);
                event.target.value = "";
              }}
            >
              <option value="">+ Añadir del inventario...</option>
              {materialsCatalog
                .filter((material) => !usageLines.some((line) => line.materialId === material.id))
                .map((material) => (
                  <option key={material.id} value={material.id}>
                    {material.name} ({material.currentStock} disp.)
                  </option>
                ))}
            </select>
            <button className="btn ghost" type="button" onClick={addCustomUsageLine}>
              <IconPlus size={14} />
              Personalizado
            </button>
          </div>

          {error && <p className="alert error">{error}</p>}

          <div className="form-actions">
            <button className="btn secondary" type="button" disabled={busy} onClick={() => setCompleting(false)}>
              Atrás
            </button>
            <button className="btn primary" type="button" disabled={busy} onClick={confirmCompletion}>
              {busy ? "Guardando..." : "Confirmar y completar"}
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
          <h2>Detalle de la cita</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Paciente</strong>
            <span>{patientName(patients, appointment.patientId)}</span>
          </div>
          <div className="clinic-row">
            <strong>Doctor</strong>
            <span>{doctorName(doctors, appointment.doctorStaffId)}</span>
          </div>
          {roomObj && (
            <div className="clinic-row">
              <strong>Consultorio / Sillón</strong>
              <span>
                <span className="badge" style={{ backgroundColor: roomObj.colorHex ?? "#3B82F6", color: "#fff" }}>
                  {roomObj.name}
                </span>
              </span>
            </div>
          )}
          <div className="clinic-row">
            <strong>Horario</strong>
            <span>
              {new Date(appointment.scheduledStart).toLocaleString("es-MX")} —{" "}
              {new Date(appointment.scheduledEnd).toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit", hour12: false })}
            </span>
          </div>
          {appointment.serviceName && (
            <div className="clinic-row">
              <strong>Servicio</strong>
              <span>
                {appointment.serviceName} ·{" "}
                {appointment.servicePricing === "FIXED" && appointment.servicePrice != null
                  ? currencyFormatter.format(appointment.servicePrice)
                  : "precio por definir"}
              </span>
            </div>
          )}
          {appointment.reason && (
            <div className="clinic-row">
              <strong>Motivo</strong>
              <span>{appointment.reason}</span>
            </div>
          )}
          {appointment.notes && (
            <div className="clinic-row">
              <strong>Notas</strong>
              <span>{appointment.notes}</span>
            </div>
          )}
          {appointment.cancellationReason && (
            <div className="clinic-row">
              <strong>Motivo Cancelación</strong>
              <span style={{ color: "#ef4444" }}>{appointment.cancellationReason}</span>
            </div>
          )}
          <div className="clinic-row">
            <strong>Estado</strong>
            <span className="badge warning">{APPOINTMENT_STATUS_LABELS[appointment.status]}</span>
          </div>
        </div>

        {error && <p className="alert error">{error}</p>}

        {cancellingPrompt ? (
          <div className="form-actions" style={{ flexDirection: "column", alignItems: "stretch", gap: "8px" }}>
            <label className="field">
              <span>Motivo de la cancelación (opcional)</span>
              <input
                type="text"
                value={cancellationReasonInput}
                onChange={(e) => setCancellationReasonInput(e.target.value)}
                placeholder="Paciente solicitó cambio, inasistencia, imprevisto..."
                autoFocus
              />
            </label>
            <div style={{ display: "flex", gap: "8px", justifyContent: "flex-end" }}>
              <button className="btn secondary" type="button" disabled={busy} onClick={() => setCancellingPrompt(false)}>
                Atrás
              </button>
              <button className="btn destructive" type="button" disabled={busy} onClick={confirmCancel}>
                Confirmar cancelación
              </button>
            </div>
          </div>
        ) : (
          !isTerminal && (
            <div className="form-actions">
              {onStartCare && appointment.patientId && (
                <button
                  className="btn primary"
                  type="button"
                  style={{ backgroundColor: "#0284c7", borderColor: "#0284c7" }}
                  disabled={busy}
                  onClick={handleStartCare}
                >
                  <IconHeartHandshake size={16} /> Iniciar Atención
                </button>
              )}
              {appointment.status === "SCHEDULED" && (
                <button className="btn secondary" type="button" disabled={busy} onClick={() => transition("CONFIRMED")}>
                  Confirmar
                </button>
              )}
              <button className="btn primary" type="button" disabled={busy} onClick={startCompletion}>
                Marcar completada
              </button>
              <button className="btn secondary" type="button" disabled={busy} onClick={() => transition("NO_SHOW")}>
                No asistió
              </button>
              <button className="btn destructive" type="button" disabled={busy} onClick={() => setCancellingPrompt(true)}>
                Cancelar cita
              </button>
            </div>
          )
        )}

        <div className="form-actions">
          {confirmingDelete ? (
            <>
              <span className="alert error" style={{ flex: 1 }}>
                Esta acción no se puede deshacer. ¿Eliminar la cita definitivamente?
              </span>
              <button className="btn secondary" type="button" disabled={busy} onClick={() => setConfirmingDelete(false)}>
                No, mantener
              </button>
              <button className="btn destructive" type="button" disabled={busy} onClick={handleDelete}>
                Sí, eliminar
              </button>
            </>
          ) : (
            <button className="btn ghost" type="button" disabled={busy} onClick={() => setConfirmingDelete(true)}>
              Eliminar cita
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

function ClinicScheduleModal({ clinicId, onClose }: { clinicId: string; onClose: () => void }) {
  const [days, setDays] = useState<DayScheduleRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    clinicScheduleApi
      .get(clinicId)
      .then((list) => {
        const ordered = WEEKDAY_ORDER.map((day) => {
          const found = list.find((item) => item.dayOfWeek === day);
          return {
            dayOfWeek: day,
            open: found?.open ?? false,
            startTime: found?.startTime ?? null,
            endTime: found?.endTime ?? null
          };
        });
        setDays(ordered);
      })
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  }, [clinicId]);

  const updateDay = (day: WeekDay, patch: Partial<DayScheduleRequest>) => {
    setDays((prev) => prev.map((item) => (item.dayOfWeek === day ? { ...item, ...patch } : item)));
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    for (const day of days) {
      if (day.open && (!day.startTime || !day.endTime)) {
        setError(`Indica hora de inicio y fin para ${WEEKDAY_LABELS[day.dayOfWeek].toLowerCase()}, o márcalo como cerrado.`);
        return;
      }
    }
    setSaving(true);
    try {
      await clinicScheduleApi.update(clinicId, days);
      onClose();
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
          <h2>Horario de atención</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>

        {loading && <p className="panel-subtitle">Cargando horario...</p>}

        {!loading && (
          <form onSubmit={submit}>
            <div className="schedule-rows">
              {days.map((day) => (
                <div className="schedule-row" key={day.dayOfWeek}>
                  <label className="checkbox-field schedule-row-day">
                    <input
                      type="checkbox"
                      checked={day.open}
                      onChange={(event) => updateDay(day.dayOfWeek, { open: event.target.checked })}
                    />
                    <span>{WEEKDAY_LABELS[day.dayOfWeek]}</span>
                  </label>
                  <input
                    type="time"
                    value={toInputTime(day.startTime)}
                    disabled={!day.open}
                    onChange={(event) => updateDay(day.dayOfWeek, { startTime: toApiTime(event.target.value) })}
                  />
                  <span className="schedule-row-sep">—</span>
                  <input
                    type="time"
                    value={toInputTime(day.endTime)}
                    disabled={!day.open}
                    onChange={(event) => updateDay(day.dayOfWeek, { endTime: toApiTime(event.target.value) })}
                  />
                </div>
              ))}
            </div>

            {error && <p className="alert error">{error}</p>}
            <div className="form-actions">
              <button className="btn primary" type="submit" disabled={saving}>
                {saving ? "Guardando..." : "Guardar horario"}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

function ClinicRoomsModal({
  clinicId,
  onClose,
  onUpdated
}: {
  clinicId: string;
  onClose: () => void;
  onUpdated: () => void;
}) {
  const [rooms, setRooms] = useState<ClinicRoomResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [name, setName] = useState("");
  const [code, setCode] = useState("");
  const [colorHex, setColorHex] = useState("#3B82F6");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const load = () => {
    clinicRoomsApi.getRooms(clinicId)
      .then(setRooms)
      .catch((e) => setError(getFriendlyError(e)))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [clinicId]);

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setSaving(true);
    setError("");
    try {
      await clinicRoomsApi.createRoom(clinicId, { name: name.trim(), code: code.trim() || undefined, colorHex });
      setName("");
      setCode("");
      load();
      onUpdated();
    } catch (err) {
      setError(getFriendlyError(err));
    } finally {
      setSaving(false);
    }
  };

  const toggleActive = async (room: ClinicRoomResponse) => {
    try {
      if (room.active) {
        await clinicRoomsApi.deactivateRoom(clinicId, room.id);
      } else {
        await clinicRoomsApi.activateRoom(clinicId, room.id);
      }
      load();
      onUpdated();
    } catch (err) {
      setError(getFriendlyError(err));
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="panel-heading">
          <h2>Gestión de Consultorios / Sillones</h2>
          <button className="icon-btn" type="button" onClick={onClose}><IconX size={18} /></button>
        </div>

        <form onSubmit={handleCreate} className="profile-form" style={{ marginBottom: "16px" }}>
          <label className="field">
            <span>Nombre del Consultorio/Sillón</span>
            <input type="text" value={name} onChange={(e) => setName(e.target.value)} placeholder="Ej. Consultorio 1, Sillón A, Quirófano..." required />
          </label>
          <label className="field">
            <span>Código / Tag corto</span>
            <input type="text" value={code} onChange={(e) => setCode(e.target.value)} placeholder="Ej. CONS-01" />
          </label>
          <label className="field">
            <span>Color en Agenda</span>
            <input type="color" value={colorHex} onChange={(e) => setColorHex(e.target.value)} style={{ height: "40px", cursor: "pointer" }} />
          </label>
          {error && <p className="alert error">{error}</p>}
          <div className="form-actions">
            <button className="btn primary" type="submit" disabled={saving}>{saving ? "Guardando..." : "Agregar Consultorio"}</button>
          </div>
        </form>

        <h3>Consultorios Registrados</h3>
        {loading ? <p>Cargando...</p> : (
          <div className="clinic-list">
            {rooms.map((r) => (
              <div key={r.id} className="clinic-row" style={{ alignItems: "center" }}>
                <span className="badge" style={{ backgroundColor: r.colorHex ?? "#3B82F6", color: "#fff", padding: "4px 8px" }}>
                  {r.name} {r.code ? `(${r.code})` : ""}
                </span>
                <div>
                  <button className={`btn ${r.active ? "secondary" : "primary"}`} type="button" onClick={() => toggleActive(r)} style={{ fontSize: "0.8rem", padding: "2px 8px" }}>
                    {r.active ? "Desactivar" : "Activar"}
                  </button>
                </div>
              </div>
            ))}
            {rooms.length === 0 && <p className="panel-subtitle">No hay consultorios configurados aún.</p>}
          </div>
        )}
      </div>
    </div>
  );
}

const ROOM_BLOCK_TYPE_LABELS: Record<RoomBlockType, string> = {
  MAINTENANCE: "Mantenimiento",
  CLEANING: "Limpieza",
  INTERNAL_USE: "Uso interno",
  UNAVAILABLE: "No disponible",
  OTHER: "Otro"
};

function RoomBlocksModal({
  clinicId,
  rooms,
  onClose
}: {
  clinicId: string;
  rooms: ClinicRoomResponse[];
  onClose: () => void;
}) {
  const defaultStart = new Date(Date.now() + 60 * 60000);
  defaultStart.setMinutes(0, 0, 0);
  const defaultEnd = new Date(defaultStart.getTime() + 60 * 60000);
  const [blocks, setBlocks] = useState<RoomBlockResponse[]>([]);
  const [roomId, setRoomId] = useState(rooms[0]?.id ?? "");
  const [startsAt, setStartsAt] = useState(toLocalDateTimeInput(defaultStart));
  const [endsAt, setEndsAt] = useState(toLocalDateTimeInput(defaultEnd));
  const [type, setType] = useState<RoomBlockType>("MAINTENANCE");
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const load = () => {
    const from = `${toIsoDateInput(startOfDay(new Date()))}T00:00:00`;
    const to = `${toIsoDateInput(addDays(new Date(), 60))}T23:59:59`;
    setLoading(true);
    agendaApi.listRoomBlocks(clinicId, from, to)
      .then(setBlocks)
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, [clinicId]);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!roomId) {
      setError("Selecciona un consultorio.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      await agendaApi.createRoomBlock(clinicId, {
        roomId,
        startsAt: `${startsAt}:00`,
        endsAt: `${endsAt}:00`,
        type,
        reason: reason.trim() || undefined
      });
      setReason("");
      load();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  const deactivate = async (block: RoomBlockResponse) => {
    try {
      await agendaApi.deactivateRoomBlock(clinicId, block.id);
      load();
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card modal-card-wide" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Bloqueos de consultorios</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}><IconX size={18} /></button>
        </div>
        <p className="panel-subtitle">Reserva un consultorio para mantenimiento, limpieza o uso interno.</p>

        {rooms.length === 0 ? (
          <p className="alert error">Primero registra al menos un consultorio activo.</p>
        ) : (
          <form onSubmit={submit} className="profile-form" style={{ marginBottom: "18px" }}>
            <label className="field">
              <span>Consultorio</span>
              <select value={roomId} onChange={(event) => setRoomId(event.target.value)} required>
                <option value="">Selecciona un consultorio</option>
                {rooms.map((room) => <option key={room.id} value={room.id}>{room.name}</option>)}
              </select>
            </label>
            <label className="field">
              <span>Tipo de bloqueo</span>
              <select value={type} onChange={(event) => setType(event.target.value as RoomBlockType)}>
                {Object.entries(ROOM_BLOCK_TYPE_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Inicio</span>
              <input type="datetime-local" value={startsAt} onChange={(event) => setStartsAt(event.target.value)} required />
            </label>
            <label className="field">
              <span>Fin</span>
              <input type="datetime-local" value={endsAt} onChange={(event) => setEndsAt(event.target.value)} required />
            </label>
            <label className="field field-full">
              <span>Motivo</span>
              <input type="text" value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Ej. Cambio de unidad dental" />
            </label>
            {error && <p className="alert error">{error}</p>}
            <div className="form-actions">
              <button className="btn primary" type="submit" disabled={saving}>
                {saving ? "Guardando..." : "Bloquear consultorio"}
              </button>
            </div>
          </form>
        )}

        {rooms.length > 0 && <h3>Bloqueos próximos</h3>}
        {loading ? <p>Cargando...</p> : (
          <div className="clinic-list">
            {blocks.filter((block) => block.active).map((block) => {
              const room = rooms.find((item) => item.id === block.roomId);
              return (
                <div key={block.id} className="clinic-row" style={{ alignItems: "center" }}>
                  <div>
                    <strong>{room?.name ?? "Consultorio"}</strong>
                    <div className="panel-subtitle">
                      {ROOM_BLOCK_TYPE_LABELS[block.type]} · {new Date(block.startsAt).toLocaleString("es-MX")} - {new Date(block.endsAt).toLocaleString("es-MX")}
                    </div>
                    {block.reason && <div className="panel-subtitle">{block.reason}</div>}
                  </div>
                  <button className="btn secondary" type="button" onClick={() => deactivate(block)}>Liberar</button>
                </div>
              );
            })}
            {blocks.filter((block) => block.active).length === 0 && <p className="panel-subtitle">No hay bloqueos activos próximos.</p>}
          </div>
        )}
      </div>
    </div>
  );
}

function WaitingListModal({
  clinicId,
  patients,
  doctors,
  rooms,
  onClose
}: {
  clinicId: string;
  patients: PatientResponse[];
  doctors: DoctorResponse[];
  rooms: ClinicRoomResponse[];
  onClose: () => void;
}) {
  const [list, setList] = useState<WaitingListEntryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [patientId, setPatientId] = useState("");
  const [doctorStaffId, setDoctorStaffId] = useState("");
  const [roomId, setRoomId] = useState("");
  const [notes, setNotes] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const load = () => {
    waitingListApi.getWaitingList(clinicId, false)
      .then(setList)
      .catch((e) => setError(getFriendlyError(e)))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [clinicId]);

  const handleAdd = async (e: FormEvent) => {
    e.preventDefault();
    if (!patientId) { setError("Selecciona un paciente."); return; }
    setSaving(true);
    setError("");
    try {
      await waitingListApi.addToWaitingList(clinicId, {
        patientId,
        doctorStaffId: doctorStaffId || undefined,
        roomId: roomId || undefined,
        notes: notes.trim() || undefined
      });
      setPatientId("");
      setNotes("");
      load();
    } catch (err) {
      setError(getFriendlyError(err));
    } finally {
      setSaving(false);
    }
  };

  const updateStatus = async (id: string, status: any) => {
    try {
      await waitingListApi.updateStatus(clinicId, id, status);
      load();
    } catch (err) {
      setError(getFriendlyError(err));
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card modal-card-wide" onClick={(e) => e.stopPropagation()}>
        <div className="panel-heading">
          <h2>Lista de Espera / Sobreturnos</h2>
          <button className="icon-btn" type="button" onClick={onClose}><IconX size={18} /></button>
        </div>

        <form onSubmit={handleAdd} className="profile-form" style={{ marginBottom: "16px" }}>
          <label className="field">
            <span>Paciente</span>
            <PatientSearchSelect
              patients={patients}
              value={patientId}
              onChange={setPatientId}
              allowNew={false}
            />
          </label>
          <label className="field">
            <span>Doctor preferido</span>
            <select value={doctorStaffId} onChange={(e) => setDoctorStaffId(e.target.value)}>
              <option value="">Cualquier doctor</option>
              {doctors.map((d) => (
                <option key={d.staffId} value={d.staffId}>{d.fullName}</option>
              ))}
            </select>
          </label>
          <label className="field field-full">
            <span>Notas / Preferencia de horario</span>
            <input type="text" value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Ej. Solo por las mañanas, avisas si hay cancelación..." />
          </label>
          {error && <p className="alert error">{error}</p>}
          <div className="form-actions">
            <button className="btn primary" type="submit" disabled={saving}>{saving ? "Guardando..." : "Añadir a Lista de Espera"}</button>
          </div>
        </form>

        <h3>Pacientes en Espera</h3>
        {loading ? <p>Cargando...</p> : (
          <div className="table-wrapper">
            <table className="data-table no-row-click">
              <thead>
                <tr>
                  <th>Paciente</th>
                  <th>Doctor</th>
                  <th>Preferencia / Notas</th>
                  <th>Estado</th>
                  <th>Acción</th>
                </tr>
              </thead>
              <tbody>
                {list.map((item) => (
                  <tr key={item.id}>
                    <td>{patientName(patients, item.patientId)}</td>
                    <td>{item.doctorStaffId ? doctorName(doctors, item.doctorStaffId) : "Cualquiera"}</td>
                    <td>{item.notes || "—"}</td>
                    <td><span className="badge warning">{item.status}</span></td>
                    <td style={{ display: "flex", gap: "4px" }}>
                      {item.status === "WAITING" && (
                        <button className="btn secondary" type="button" onClick={() => updateStatus(item.id, "NOTIFIED")} style={{ fontSize: "0.75rem", padding: "2px 6px" }}>Notificado</button>
                      )}
                      <button className="btn destructive" type="button" onClick={() => updateStatus(item.id, "CANCELLED")} style={{ fontSize: "0.75rem", padding: "2px 6px" }}>Quitar</button>
                    </td>
                  </tr>
                ))}
                {list.length === 0 && (
                  <tr>
                    <td colSpan={5} style={{ textAlign: "center", color: "#64748b" }}>No hay pacientes actualmente en lista de espera.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export function AgendaScreen({
  clinicId,
  hasClinic,
  patients,
  onStartCare
}: {
  clinicId?: string;
  hasClinic: boolean;
  patients: PatientResponse[];
  onStartCare?: (patient: PatientResponse, appointment?: AppointmentResponse) => void;
}) {
  const [viewMode, setViewMode] = useState<ViewMode>("list");
  const [listDate, setListDate] = useState(() => startOfDay(new Date()));
  const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()));
  const [doctors, setDoctors] = useState<DoctorResponse[]>([]);
  const [rooms, setRooms] = useState<ClinicRoomResponse[]>([]);
  const [selectedDoctorId, setSelectedDoctorId] = useState("");
  const [selectedRoomId, setSelectedRoomId] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [todayAppointments, setTodayAppointments] = useState<AppointmentResponse[]>([]);
  const [quotationCache, setQuotationCache] = useState<Map<string, QuotationResponse>>(new Map());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [creatingAt, setCreatingAt] = useState<Date | null>(null);
  const [viewingAppointment, setViewingAppointment] = useState<AppointmentResponse | null>(null);
  const [showScheduleModal, setShowScheduleModal] = useState(false);
  const [showRoomsModal, setShowRoomsModal] = useState(false);
  const [showRoomBlocksModal, setShowRoomBlocksModal] = useState(false);
  const [showWaitingListModal, setShowWaitingListModal] = useState(false);
  const [inboxAppointments, setInboxAppointments] = useState<AppointmentResponse[]>([]);
  const [isInboxOpen, setIsInboxOpen] = useState(false);

  const weekDays = useMemo(() => Array.from({ length: 7 }, (_, i) => addDays(weekStart, i)), [weekStart]);
  const hours = useMemo(
    () => Array.from({ length: GRID_END_HOUR - GRID_START_HOUR }, (_, i) => GRID_START_HOUR + i),
    []
  );

  const loadRooms = () => {
    if (!clinicId) return;
    clinicRoomsApi.getRooms(clinicId, true).then(setRooms).catch(() => setRooms([]));
  };

  useEffect(() => {
    if (!clinicId) return;
    agendaApi.listDoctors(clinicId).then(setDoctors).catch(() => setDoctors([]));
    loadRooms();
  }, [clinicId]);

  const loadAppointments = () => {
    if (!clinicId) return;
    setLoading(true);
    setError("");
    const rangeStart = viewMode === "list" ? listDate : weekStart;
    const rangeDays = viewMode === "list" ? 1 : 7;
    const fromDt = startOfDay(addDays(rangeStart, -1));
    const toDt = startOfDay(addDays(rangeStart, rangeDays + 1));
    const from = `${toIsoDateInput(fromDt)}T00:00:00`;
    const to = `${toIsoDateInput(toDt)}T23:59:59`;

    agendaApi.listByRange(clinicId, from, to)
      .then((appts) => {
        setAppointments(appts);
      })
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  };



  useEffect(() => {
    loadAppointments();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId, viewMode, listDate, weekStart]);

  const loadTodayStats = () => {
    if (!clinicId) return;
    const today = startOfDay(new Date());
    agendaApi
      .listByRange(clinicId, today.toISOString(), addDays(today, 1).toISOString())
      .then(setTodayAppointments)
      .catch(() => setTodayAppointments([]));
  };

  useEffect(() => {
    loadTodayStats();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId]);

  useEffect(() => {
    const pending = todayAppointments.filter(
      (appointment) => appointment.quotationId && isActiveStatus(appointment.status) && !quotationCache.has(appointment.quotationId)
    );
    if (pending.length === 0 || !clinicId) return;
    const seen = new Set<string>();
    Promise.all(
      pending
        .filter((appointment) => {
          if (seen.has(appointment.quotationId!)) return false;
          seen.add(appointment.quotationId!);
          return true;
        })
        .map((appointment) =>
          quotationsApi
            .get(appointment.patientId!, appointment.quotationId!, clinicId)
            .then((quotation) => [appointment.quotationId!, quotation] as const)
            .catch(() => null)
        )

    ).then((results) => {
      setQuotationCache((prev) => {
        const next = new Map(prev);
        results.forEach((result) => {
          if (result) next.set(result[0], result[1]);
        });
        return next;
      });
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [todayAppointments, clinicId]);

  if (!hasClinic || !clinicId) {
    return (
      <section className="dashboard-grid">
        <article className="panel full">
          <div className="panel-heading">
            <h2>Agenda</h2>
          </div>
          <p className="panel-subtitle">Selecciona o crea una clínica para gestionar la agenda de citas.</p>
        </article>
      </section>
    );
  }

  const matchesDoctor = (appointment: AppointmentResponse) => !selectedDoctorId || appointment.doctorStaffId === selectedDoctorId;
  const matchesRoom = (appointment: AppointmentResponse) => !selectedRoomId || appointment.roomId === selectedRoomId;
  const matchesSearch = (appointment: AppointmentResponse) =>
    !searchQuery.trim() || patientName(patients, appointment.patientId).toLowerCase().includes(searchQuery.trim().toLowerCase());

  const visibleAppointments = appointments.filter((appointment) => matchesDoctor(appointment) && matchesRoom(appointment) && matchesSearch(appointment));
  const todayForStats = todayAppointments.filter(matchesDoctor);
  const confirmedToday = todayForStats.filter((appointment) => appointment.status === "CONFIRMED").length;
  // Presupuestos ligados + precio fijo de las citas con servicio; las de precio variable se cuentan aparte.
  const revenue = computeEstimatedRevenue(todayForStats, quotationCache);
  const estimatedRevenue = revenue.total;

  const listDayAppointments = visibleAppointments
    .filter((appointment) => isSameDay(new Date(appointment.scheduledStart), listDate))
    .sort((a, b) => new Date(a.scheduledStart).getTime() - new Date(b.scheduledStart).getTime());

  const openDetailFrom = (appointment: AppointmentResponse) => setViewingAppointment(appointment);



  return (
    <section className="dashboard-grid">
      <article className="panel full">
        <div className="panel-heading">
          <div className="inventory-heading-main">
            <h2>Agenda</h2>
            <label
              style={{
                position: "relative",
                cursor: "pointer",
                display: "inline-flex",
                alignItems: "center",
                gap: "6px",
                color: "var(--color-primary, #0284c7)"
              }}
              title="Haz clic para consultar cualquier fecha"
            >
              <IconCalendar size={16} />
              <span className="panel-subtitle" style={{ margin: 0, textDecoration: "underline", textDecorationStyle: "dotted" }}>
                {viewMode === "list"
                  ? formatLongDate(listDate)
                  : `${formatLongDate(weekDays[0])} – ${formatLongDate(weekDays[6])}`}
              </span>
              <input
                type="date"
                value={toIsoDateInput(listDate)}
                onChange={(e) => {
                  if (e.target.value) {
                    const [y, m, d] = e.target.value.split("-").map(Number);
                    const chosen = new Date(y, m - 1, d);
                    setListDate(chosen);
                    setWeekStart(startOfWeek(chosen));
                  }
                }}
                style={{
                  position: "absolute",
                  top: 0,
                  left: 0,
                  width: "100%",
                  height: "100%",
                  opacity: 0,
                  cursor: "pointer"
                }}
              />
            </label>
          </div>
          <div className="topbar-actions agenda-toolbar">
            <label className="agenda-search">
              <IconSearch size={16} aria-hidden="true" />
              <input
                type="text"
                placeholder="Buscar paciente..."
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
              />
            </label>
            <select value={selectedDoctorId} onChange={(event) => setSelectedDoctorId(event.target.value)}>
              <option value="">Todos los doctores</option>
              {doctors.map((doctor) => (
                <option key={doctor.staffId} value={doctor.staffId}>
                  {doctor.fullName}
                </option>
              ))}
            </select>
            {rooms.length > 0 && (
              <select value={selectedRoomId} onChange={(event) => setSelectedRoomId(event.target.value)}>
                <option value="">Todos los consultorios</option>
                {rooms.map((room) => (
                  <option key={room.id} value={room.id}>
                    {room.name}
                  </option>
                ))}
              </select>
            )}
            <div className="agenda-view-toggle">
              <button
                type="button"
                className={viewMode === "list" ? "active" : ""}
                aria-label="Vista de lista"
                onClick={() => setViewMode("list")}
              >
                <IconList size={18} />
              </button>
              <button
                type="button"
                className={viewMode === "week" ? "active" : ""}
                aria-label="Vista de semana"
                onClick={() => setViewMode("week")}
              >
                <IconCalendar size={18} />
              </button>
              <button
                type="button"
                className={viewMode === "board" ? "active" : ""}
                aria-label="Vista de tablero semanal"
                onClick={() => setViewMode("board")}
              >
                <IconLayoutBoard size={18} />
              </button>
            </div>
            <button
              className="btn secondary"
              type="button"
              onClick={() => setShowWaitingListModal(true)}
            >
              <IconInbox size={16} />
              Lista de Espera
            </button>
            <button
              className="btn secondary"
              type="button"
              onClick={() => setShowRoomsModal(true)}
            >
              Consultorios / Sillones
            </button>
            <button
              className="btn secondary"
              type="button"
              onClick={() => setShowRoomBlocksModal(true)}
            >
              Bloqueos
            </button>
            <button
              className="btn secondary"
              type="button"
              onClick={() => {
                setIsInboxOpen(true);
                agendaApi.listWithoutPatient(clinicId).then(setInboxAppointments).catch(() => setInboxAppointments([]));
              }}
            >
              <IconInbox size={16} />
              Inbox
            </button>
            <button className="btn secondary" type="button" onClick={() => setShowScheduleModal(true)}>
              <IconClock size={16} />
              Horario
            </button>

            <button className="btn primary" type="button" onClick={() => setCreatingAt(viewMode === "list" ? listDate : new Date())}>
              <IconPlus size={16} />
              Nueva cita
            </button>
          </div>
        </div>

        <div className="agenda-stats-row">
          <div className="agenda-stat-card">
            <span>Citas hoy</span>
            <strong>{todayForStats.length}</strong>
          </div>
          <div className="agenda-stat-card">
            <span>Confirmadas</span>
            <strong>{confirmedToday}</strong>
            <small>{todayForStats.length > 0 ? `${Math.round((confirmedToday / todayForStats.length) * 100)}% del total` : "—"}</small>
          </div>
          <div className="agenda-stat-card">
            <span>Ingresos estimados</span>
            <strong>{currencyFormatter.format(estimatedRevenue)}</strong>
            {revenue.toDefine > 0 && (
              <small>+ {revenue.toDefine} {revenue.toDefine === 1 ? "cita" : "citas"} con precio por definir</small>
            )}
          </div>
        </div>

        {error && <p className="alert error">{error}</p>}
        {loading && <p className="panel-subtitle">Cargando citas...</p>}
        {doctors.length === 0 && !loading && (
          <p className="panel-subtitle">
            No hay doctores registrados en esta clínica todavía. Agrega personal con rol "Doctor" para poder agendar citas.
          </p>
        )}

        {viewMode === "list" && (
          <>
            <div className="agenda-list-day-nav" style={{ display: "flex", alignItems: "center", gap: "12px", flexWrap: "wrap", marginBottom: "16px" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                <button className="icon-btn" type="button" aria-label="Día anterior" onClick={() => setListDate((prev) => addDays(prev, -1))}>
                  <IconChevronLeft size={18} />
                </button>
                <button className="btn ghost" type="button" onClick={() => setListDate(startOfDay(new Date()))}>
                  Hoy
                </button>
                <button className="icon-btn" type="button" aria-label="Día siguiente" onClick={() => setListDate((prev) => addDays(prev, 1))}>
                  <IconChevronRight size={18} />
                </button>
              </div>

              <span style={{ fontSize: "15px", fontWeight: 700, color: "var(--color-text-1, #1e293b)" }}>
                {formatLongDate(listDate)}
              </span>

              <div style={{ display: "flex", alignItems: "center", gap: "6px", marginLeft: "auto" }}>
                <span style={{ fontSize: "12px", color: "var(--color-text-3, #64748b)", fontWeight: 600 }}>
                  Ir a fecha:
                </span>
                <input
                  type="date"
                  value={toIsoDateInput(listDate)}
                  onChange={(e) => {
                    if (e.target.value) {
                      const [y, m, d] = e.target.value.split("-").map(Number);
                      const chosen = new Date(y, m - 1, d);
                      setListDate(chosen);
                      setWeekStart(startOfWeek(chosen));
                    }
                  }}
                  style={{
                    height: "34px",
                    padding: "0 8px",
                    borderRadius: "6px",
                    border: "1px solid #bae6fd",
                    backgroundColor: "rgba(2, 132, 199, 0.08)",
                    color: "#0369a1",
                    fontWeight: 600,
                    fontSize: "13px",
                    cursor: "pointer",
                    outline: "none"
                  }}
                />
              </div>
            </div>

            <div className="agenda-list">
              {listDayAppointments.length === 0 && (
                <div className="empty-table-state">No hay citas registradas para este día.</div>
              )}
              {listDayAppointments.map((appointment) => {
                const start = new Date(appointment.scheduledStart);
                const end = new Date(appointment.scheduledEnd);
                return (
                  <div
                    key={appointment.id}
                    className={`agenda-list-row ${STATUS_CLASS[appointment.status]}`}
                    onClick={() => openDetailFrom(appointment)}
                  >
                    <div className="agenda-list-time">
                      {start.toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit", hour12: false })} —{" "}
                      {end.toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit", hour12: false })}
                    </div>
                    <div className="agenda-list-info">
                      <strong>{patientName(patients, appointment.patientId)}</strong>
                      <span>
                        {appointment.serviceName || appointment.reason || "Consulta"} · {doctorName(doctors, appointment.doctorStaffId)}
                        {appointment.serviceName && (
                          <>
                            {" · "}
                            <b>
                              {appointment.servicePricing === "FIXED" && appointment.servicePrice != null
                                ? currencyFormatter.format(appointment.servicePrice)
                                : "precio por definir"}
                            </b>
                          </>
                        )}
                      </span>
                    </div>
                    <span className={`agenda-status-badge ${STATUS_CLASS[appointment.status]}`}>
                      {APPOINTMENT_STATUS_LABELS[appointment.status]}
                    </span>
                    {onStartCare && (
                      appointment.status === "SCHEDULED" ? (
                        <button
                          type="button"
                          className="btn secondary"
                          style={{ fontSize: "11px", padding: "4px 8px", opacity: 0.8 }}
                          title="Se requiere confirmar la cita antes de iniciar la consulta"
                          onClick={(event) => {
                            event.stopPropagation();
                            openDetailFrom(appointment);
                          }}
                        >
                          Requiere Confirmar
                        </button>
                      ) : isAppointmentReadyToStart(appointment) ? (
                        <button
                          type="button"
                          className="btn primary"
                          style={{
                            fontSize: "12px",
                            padding: "4px 10px",
                            backgroundColor: "#0284c7",
                            borderColor: "#0284c7",
                            display: "inline-flex",
                            alignItems: "center",
                            gap: "4px",
                            boxShadow: "0 0 8px rgba(2, 132, 199, 0.4)"
                          }}
                          onClick={(event) => {
                            event.stopPropagation();
                            const pat = patients.find((p) => p.id === appointment.patientId);
                            if (pat) {
                              onStartCare(pat, appointment);
                            } else {
                              onStartCare(
                                {
                                  id: appointment.patientId || "",
                                  clinicId: clinicId || "",
                                  firstName: "Paciente",
                                  lastNamePaterno: "Nuevo"
                                } as PatientResponse,
                                appointment
                              );
                            }
                          }}
                        >
                          <IconHeartHandshake size={14} /> Iniciar Consulta
                        </button>
                      ) : (
                        <button
                          type="button"
                          className="btn secondary"
                          disabled
                          style={{ fontSize: "11px", padding: "4px 8px", opacity: 0.6 }}
                          title="El botón se activará al iniciar el horario de la cita"
                        >
                          Hora pendiente
                        </button>
                      )
                    )}
                    <button
                      className="icon-btn"
                      type="button"
                      aria-label="Opciones"
                      onClick={(event) => {
                        event.stopPropagation();
                        openDetailFrom(appointment);
                      }}
                    >
                      <IconDotsVertical size={16} />
                    </button>
                  </div>
                );
              })}
            </div>


          </>
        )}

        {viewMode === "week" && (
          <>
            <div className="agenda-list-day-nav" style={{ display: "flex", alignItems: "center", gap: "12px", flexWrap: "wrap", marginBottom: "16px" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                <button className="icon-btn" type="button" aria-label="Semana anterior" onClick={() => setWeekStart((prev) => addDays(prev, -7))}>
                  <IconChevronLeft size={18} />
                </button>
                <button className="btn ghost" type="button" onClick={() => setWeekStart(startOfWeek(new Date()))}>
                  Hoy
                </button>
                <button className="icon-btn" type="button" aria-label="Semana siguiente" onClick={() => setWeekStart((prev) => addDays(prev, 7))}>
                  <IconChevronRight size={18} />
                </button>
              </div>

              <span style={{ fontSize: "15px", fontWeight: 700, color: "var(--color-text-1, #1e293b)" }}>
                {`${formatLongDate(weekDays[0])} – ${formatLongDate(weekDays[6])}`}
              </span>

              <div style={{ display: "flex", alignItems: "center", gap: "6px", marginLeft: "auto" }}>
                <span style={{ fontSize: "12px", color: "var(--color-text-3, #64748b)", fontWeight: 600 }}>
                  Ir a fecha:
                </span>
                <input
                  type="date"
                  value={toIsoDateInput(weekDays[0] || new Date())}
                  onChange={(e) => {
                    if (e.target.value) {
                      const [y, m, d] = e.target.value.split("-").map(Number);
                      const chosen = new Date(y, m - 1, d);
                      setListDate(chosen);
                      setWeekStart(startOfWeek(chosen));
                    }
                  }}
                  style={{
                    height: "34px",
                    padding: "0 8px",
                    borderRadius: "6px",
                    border: "1px solid #bae6fd",
                    backgroundColor: "rgba(2, 132, 199, 0.08)",
                    color: "#0369a1",
                    fontWeight: 600,
                    fontSize: "13px",
                    cursor: "pointer",
                    outline: "none"
                  }}
                />
              </div>
            </div>

            <div className="agenda-week-grid">
              <div className="agenda-grid-header">
                <div className="agenda-time-gutter" />
                {weekDays.map((day) => (
                  <div className="agenda-day-header" key={day.toISOString()}>
                    <strong>{DAY_LABELS[(day.getDay() + 6) % 7]}</strong>
                    <span>{formatDayHeader(day)}</span>
                  </div>
                ))}
              </div>
              <div className="agenda-grid-body" style={{ height: hours.length * HOUR_HEIGHT_PX }}>
                <div className="agenda-time-gutter">
                  {hours.map((hour) => (
                    <div className="agenda-hour-label" key={hour} style={{ height: HOUR_HEIGHT_PX }}>
                      {hour}:00
                    </div>
                  ))}
                </div>
                {weekDays.map((day) => {
                  const dayAppointments = visibleAppointments.filter((appointment) => isSameDay(new Date(appointment.scheduledStart), day));

                  return (
                    <div className="agenda-day-column" key={day.toISOString()}>
                      {hours.map((hour) => (
                        <div
                          className="agenda-hour-cell"
                          key={hour}
                          style={{ height: HOUR_HEIGHT_PX }}
                          onClick={() => {
                            const clickedDate = new Date(day);
                            clickedDate.setHours(hour, 0, 0, 0);
                            setCreatingAt(clickedDate);
                          }}
                        />
                      ))}
                      {dayAppointments.map((appointment) => {
                        const start = new Date(appointment.scheduledStart);
                        const end = new Date(appointment.scheduledEnd);
                        const startMinutes = (start.getHours() - GRID_START_HOUR) * 60 + start.getMinutes();
                        const durationMinutes = Math.max(15, (end.getTime() - start.getTime()) / 60000);
                        const top = (startMinutes / 60) * HOUR_HEIGHT_PX;
                        const height = (durationMinutes / 60) * HOUR_HEIGHT_PX;

                        return (
                          <div
                            key={appointment.id}
                            className={`agenda-appt-block ${STATUS_CLASS[appointment.status]}`}
                            style={{ top, height }}
                            onClick={(event) => {
                              event.stopPropagation();
                              openDetailFrom(appointment);
                            }}
                          >
                            <strong>{start.toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit", hour12: false })}</strong>
                            <span>{patientName(patients, appointment.patientId)}</span>
                          </div>
                        );
                      })}
                    </div>
                  );
                })}


              </div>
            </div>
          </>
        )}

        {viewMode === "board" && (
          <>
            <div className="agenda-list-day-nav" style={{ display: "flex", alignItems: "center", gap: "12px", flexWrap: "wrap", marginBottom: "16px" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                <button className="icon-btn" type="button" aria-label="Semana anterior" onClick={() => setWeekStart((prev) => addDays(prev, -7))}>
                  <IconChevronLeft size={18} />
                </button>
                <button className="btn ghost" type="button" onClick={() => setWeekStart(startOfWeek(new Date()))}>
                  Hoy
                </button>
                <button className="icon-btn" type="button" aria-label="Semana siguiente" onClick={() => setWeekStart((prev) => addDays(prev, 7))}>
                  <IconChevronRight size={18} />
                </button>
              </div>

              <span style={{ fontSize: "15px", fontWeight: 700, color: "var(--color-text-1, #1e293b)" }}>
                {`${formatLongDate(weekDays[0])} – ${formatLongDate(weekDays[6])}`}
              </span>

              <div style={{ display: "flex", alignItems: "center", gap: "6px", marginLeft: "auto" }}>
                <span style={{ fontSize: "12px", color: "var(--color-text-3, #64748b)", fontWeight: 600 }}>
                  Ir a fecha:
                </span>
                <input
                  type="date"
                  value={toIsoDateInput(weekDays[0] || new Date())}
                  onChange={(e) => {
                    if (e.target.value) {
                      const [y, m, d] = e.target.value.split("-").map(Number);
                      const chosen = new Date(y, m - 1, d);
                      setListDate(chosen);
                      setWeekStart(startOfWeek(chosen));
                    }
                  }}
                  style={{
                    height: "34px",
                    padding: "0 8px",
                    borderRadius: "6px",
                    border: "1px solid #bae6fd",
                    backgroundColor: "rgba(2, 132, 199, 0.08)",
                    color: "#0369a1",
                    fontWeight: 600,
                    fontSize: "13px",
                    cursor: "pointer",
                    outline: "none"
                  }}
                />
              </div>
            </div>

            <div className="agenda-week-board">
              {weekDays.map((day) => {
                const dayAppointments = visibleAppointments
                  .filter((appointment) => isSameDay(new Date(appointment.scheduledStart), day))
                  .sort((a, b) => new Date(a.scheduledStart).getTime() - new Date(b.scheduledStart).getTime());
                const isToday = isSameDay(day, new Date());

                return (
                  <div className="agenda-board-column" key={day.toISOString()}>
                    <div className={`agenda-board-day-header ${isToday ? "is-today" : ""}`}>
                      <strong>{DAY_LABELS[(day.getDay() + 6) % 7]}</strong>
                      <span>{day.getDate()}</span>
                    </div>
                    <div className="agenda-board-cards">
                      {dayAppointments.map((appointment) => {
                        const start = new Date(appointment.scheduledStart);
                        const end = new Date(appointment.scheduledEnd);
                        return (
                          <div
                            key={appointment.id}
                            className={`agenda-board-card ${STATUS_CLASS[appointment.status]}`}
                            onClick={() => openDetailFrom(appointment)}
                          >
                            <strong>
                              {start.toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit", hour12: false })} -{" "}
                              {end.toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit", hour12: false })}
                            </strong>
                            <span>{patientName(patients, appointment.patientId)}</span>
                            {onStartCare && (
                              isAppointmentReadyToStart(appointment) ? (
                                <button
                                  type="button"
                                  className="btn primary"
                                  style={{
                                    fontSize: "11px",
                                    padding: "2px 6px",
                                    marginTop: "6px",
                                    backgroundColor: "#0284c7",
                                    borderColor: "#0284c7",
                                    display: "inline-flex",
                                    alignItems: "center",
                                    gap: "4px"
                                  }}
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    const pat = patients.find((p) => p.id === appointment.patientId);
                                    if (pat) {
                                      onStartCare(pat, appointment);
                                    } else {
                                      onStartCare(
                                        {
                                          id: appointment.patientId || "",
                                          clinicId: clinicId || "",
                                          firstName: "Paciente",
                                          lastNamePaterno: "Nuevo"
                                        } as PatientResponse,
                                        appointment
                                      );
                                    }
                                  }}
                                >
                                  <IconHeartHandshake size={12} /> Iniciar Consulta
                                </button>
                              ) : null
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                );
              })}


            </div>
          </>
        )}
      </article>

      {creatingAt && (
        <AppointmentFormModal
          clinicId={clinicId}
          patients={patients}
          doctors={doctors}
          rooms={rooms}
          appointments={appointments}
          defaultStart={creatingAt}
          onClose={() => setCreatingAt(null)}
          onSaved={() => {
            setCreatingAt(null);
            loadAppointments();
            loadTodayStats();
          }}
        />
      )}

      {viewingAppointment && (
        <AppointmentDetailModal
          clinicId={clinicId}
          appointment={viewingAppointment}
          patients={patients}
          doctors={doctors}
          rooms={rooms}
          onClose={() => setViewingAppointment(null)}
          onStartCare={onStartCare}
          onChanged={(updated) => {
            setAppointments((prev) => prev.map((a) => (a.id === updated.id ? updated : a)));
            setTodayAppointments((prev) => prev.map((a) => (a.id === updated.id ? updated : a)));
            setViewingAppointment(updated);
          }}
          onDeleted={(deletedId) => {
            setAppointments((prev) => prev.filter((a) => a.id !== deletedId));
            setTodayAppointments((prev) => prev.filter((a) => a.id !== deletedId));
            setInboxAppointments((prev) => prev.filter((a) => a.id !== deletedId));
            setViewingAppointment(null);
          }}
        />
      )}

      {showScheduleModal && (
        <ClinicScheduleModal clinicId={clinicId} onClose={() => setShowScheduleModal(false)} />
      )}

      {showRoomsModal && (
        <ClinicRoomsModal
          clinicId={clinicId}
          onClose={() => setShowRoomsModal(false)}
          onUpdated={() => {
            loadRooms();
          }}
        />
      )}

      {showRoomBlocksModal && (
        <RoomBlocksModal
          clinicId={clinicId}
          rooms={rooms}
          onClose={() => setShowRoomBlocksModal(false)}
        />
      )}

      {showWaitingListModal && (
        <WaitingListModal
          clinicId={clinicId}
          patients={patients}
          doctors={doctors}
          rooms={rooms}
          onClose={() => setShowWaitingListModal(false)}
        />
      )}

      {isInboxOpen && (
        <InboxModal
          clinicId={clinicId}
          patients={patients}
          doctors={doctors}
          appointments={inboxAppointments}
          onClose={() => setIsInboxOpen(false)}
          onChanged={() => {
            agendaApi.listWithoutPatient(clinicId).then(setInboxAppointments).catch(() => setInboxAppointments([]));
            loadAppointments();
          }}
        />
      )}
    </section>
  );
}

function InboxModal({
  clinicId,
  patients,
  doctors,
  appointments,
  onClose,
  onChanged
}: {
  clinicId: string;
  patients: PatientResponse[];
  doctors: DoctorResponse[];
  appointments: AppointmentResponse[];
  onClose: () => void;
  onChanged: () => void;
}) {
  const [busy, setBusy] = useState<Record<string, boolean>>({});
  const [selectedPatients, setSelectedPatients] = useState<Record<string, string>>({});
  const [error, setError] = useState("");
  const [linkingAll, setLinkingAll] = useState(false);

  const handleLink = async (apptId: string) => {
    const patId = selectedPatients[apptId];
    setError("");
    setBusy((prev) => ({ ...prev, [apptId]: true }));
    try {
      await agendaApi.assignPatient(clinicId, apptId, patId || undefined);
      onChanged();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy((prev) => ({ ...prev, [apptId]: false }));
    }
  };

  const handleLinkAll = async () => {
    if (appointments.length === 0) return;
    if (!confirm(`¿Procesar y vincular las ${appointments.length} citas de la lista?`)) {
      return;
    }
    setError("");
    setLinkingAll(true);
    setBusy((prev) => {
      const next = { ...prev };
      appointments.forEach((a) => (next[a.id] = true));
      return next;
    });
    const results = await Promise.allSettled(
      appointments.map((appt) => agendaApi.assignPatient(clinicId, appt.id, selectedPatients[appt.id] || undefined))
    );
    const failed = results.filter((r) => r.status === "rejected").length;
    if (failed > 0) {
      setError(`${failed} de ${appointments.length} citas no se pudieron procesar. Revisa e intenta de nuevo.`);
    }
    setLinkingAll(false);
    onChanged();
  };

  const handleCancel = async (apptId: string) => {
    if (!confirm("¿Estás seguro de que deseas descartar este bloque de horario? Se cancelará la cita libre en tu agenda.")) return;
    setError("");
    setBusy((prev) => ({ ...prev, [apptId]: true }));
    try {
      await agendaApi.transitionStatus(clinicId, apptId, "CANCELLED");
      onChanged();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy((prev) => ({ ...prev, [apptId]: false }));
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card modal-card-wide" onClick={(e) => e.stopPropagation()} style={{ maxWidth: "800px" }}>
        <div className="panel-heading">
          <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
            <IconInbox size={20} />
            <h2>Inbox de Citas sin Paciente</h2>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
            <button
              className="btn secondary"
              type="button"
              disabled={linkingAll || appointments.length === 0}
              onClick={handleLinkAll}
            >
              Vincular seleccionados
            </button>
            <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
              <IconX size={18} />
            </button>
          </div>
        </div>
        
        {error && <p className="alert error">{error}</p>}

        <div className="table-wrapper" style={{ maxHeight: "400px", overflowY: "auto" }}>
          <table className="data-table no-row-click">
            <thead>
              <tr>
                <th>Fecha y Hora</th>
                <th>Doctor</th>
                <th>Detalles de Google</th>
                <th style={{ width: "260px" }}>Asignar Paciente</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {appointments.map((appt) => (
                <tr key={appt.id}>
                  <td style={{ fontSize: "13px" }}>
                    {new Date(appt.scheduledStart).toLocaleDateString("es-MX", { day: "numeric", month: "short" })}
                    <br />
                    <small style={{ color: "var(--text-muted)" }}>
                      {new Date(appt.scheduledStart).toLocaleTimeString("es-MX", { hour: "2-digit", minute: "2-digit" })}
                    </small>
                  </td>
                  <td style={{ fontSize: "13px" }}>{doctorName(doctors, appt.doctorStaffId)}</td>
                  <td style={{ fontSize: "13px" }}>
                    <strong>{appt.reason || "(Sin motivo)"}</strong>
                    {appt.notes && <p style={{ margin: "2px 0 0 0", fontSize: "11px", color: "var(--text-muted)" }}>{appt.notes}</p>}
                  </td>
                  <td>
                    <PatientSearchSelect
                      patients={patients}
                      value={selectedPatients[appt.id] || ""}
                      onChange={(val) => setSelectedPatients((prev) => ({ ...prev, [appt.id]: val }))}
                      unassignedLabel="Sin asignar u motivo indistinto"
                    />
                  </td>
                  <td>
                    <div style={{ display: "flex", gap: "6px" }}>
                      <button
                        className="btn primary"
                        type="button"
                        style={{ padding: "4px 8px", fontSize: "12px" }}
                        disabled={busy[appt.id]}
                        onClick={() => handleLink(appt.id)}
                      >
                        Vincular
                      </button>
                      <button
                        className="btn destructive ghost"
                        type="button"
                        style={{ padding: "4px 8px", fontSize: "12px" }}
                        disabled={busy[appt.id]}
                        onClick={() => handleCancel(appt.id)}
                      >
                        Descartar
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {appointments.length === 0 && (
                <tr>
                  <td colSpan={5} style={{ textAlign: "center", padding: "24px", color: "var(--text-muted)" }}>
                    No hay citas pendientes de asignar paciente en este momento.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
