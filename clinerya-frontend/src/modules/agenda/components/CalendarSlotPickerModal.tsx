import { useEffect, useMemo, useState } from "react";
import { IconCalendar, IconChevronLeft, IconChevronRight, IconClock, IconX, IconCheck } from "@tabler/icons-react";
import type { AppointmentResponse, DoctorResponse } from "@modules/agenda/types";

interface CalendarSlotPickerModalProps {
  clinicId: string;
  doctorStaffId?: string;
  doctors: DoctorResponse[];
  appointments: AppointmentResponse[];
  initialStartStr: string;
  currentDurationMins?: number;
  onSelectSlot: (newStartStr: string, newEndStr: string) => void;
  onClose: () => void;
}

const MONTH_NAMES = [
  "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
  "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
];

const DAY_NAMES = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"];

const OPERATING_START_HOUR = 8; // 08:00 AM
const OPERATING_END_HOUR = 20;  // 08:00 PM
const SLOT_DURATION_MINS = 30;

function pad(n: number): string {
  return String(n).padStart(2, "0");
}

function toIsoDateStr(y: number, m: number, d: number): string {
  return `${y}-${pad(m + 1)}-${pad(d)}`;
}

export function CalendarSlotPickerModal({
  doctorStaffId,
  doctors,
  appointments,
  initialStartStr,
  currentDurationMins = 30,
  onSelectSlot,
  onClose
}: CalendarSlotPickerModalProps) {
  const initialDate = initialStartStr ? new Date(initialStartStr) : new Date();
  const validInitialDate = isNaN(initialDate.getTime()) ? new Date() : initialDate;

  const [viewYear, setViewYear] = useState(validInitialDate.getFullYear());
  const [viewMonth, setViewMonth] = useState(validInitialDate.getMonth());

  const [selectedDateStr, setSelectedDateStr] = useState(
    toIsoDateStr(validInitialDate.getFullYear(), validInitialDate.getMonth(), validInitialDate.getDate())
  );
  const [currentTimeMs, setCurrentTimeMs] = useState(() => Date.now());

  useEffect(() => {
    const timer = window.setInterval(() => setCurrentTimeMs(Date.now()), 30_000);
    return () => window.clearInterval(timer);
  }, []);

  const selectedDoctor = doctors.find((d) => d.staffId === doctorStaffId);

  // Generate all 30-min slots for operating hours
  const daySlots = useMemo(() => {
    const slots: { startHour: number; startMin: number; label: string; timeStr: string }[] = [];
    for (let h = OPERATING_START_HOUR; h < OPERATING_END_HOUR; h++) {
      for (let m = 0; m < 60; m += SLOT_DURATION_MINS) {
        const h12 = h % 12 === 0 ? 12 : h % 12;
        const ampm = h >= 12 ? "p. m." : "a. m.";
        const endH = m + SLOT_DURATION_MINS >= 60 ? h + 1 : h;
        const endM = (m + SLOT_DURATION_MINS) % 60;
        const endH12 = endH % 12 === 0 ? 12 : endH % 12;
        const endAmPm = endH >= 12 ? "p. m." : "a. m.";

        const label = `${pad(h12)}:${pad(m)} ${ampm} — ${pad(endH12)}:${pad(endM)} ${endAmPm}`;
        const timeStr = `${pad(h)}:${pad(m)}`;
        slots.push({ startHour: h, startMin: m, label, timeStr });
      }
    }
    return slots;
  }, []);

  // Compute daily occupancy for all days in current view month
  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
  const firstDayOfWeek = (new Date(viewYear, viewMonth, 1).getDay() + 6) % 7; // Monday = 0

  const occupancyByDay = useMemo(() => {
    const map: Record<string, { total: number; occupied: number; status: "free" | "warning" | "full" }> = {};

    for (let day = 1; day <= daysInMonth; day++) {
      const dateKey = toIsoDateStr(viewYear, viewMonth, day);
      const totalSlots = daySlots.length;
      let occupiedCount = 0;

      // Filter active appointments for this day & doctor (if filtered)
      const dayAppts = appointments.filter((a) => {
        if (a.status === "CANCELLED" || a.status === "NO_SHOW") return false;
        if (doctorStaffId && a.doctorStaffId !== doctorStaffId) return false;
        const start = new Date(a.scheduledStart);
        return (
          start.getFullYear() === viewYear &&
          start.getMonth() === viewMonth &&
          start.getDate() === day
        );
      });

      // Check slot overlap
      daySlots.forEach((slot) => {
        const slotStartMs = new Date(`${dateKey}T${slot.timeStr}:00`).getTime();
        const slotEndMs = slotStartMs + SLOT_DURATION_MINS * 60000;

        const isOccupied = dayAppts.some((a) => {
          const aStart = new Date(a.scheduledStart).getTime();
          const aEnd = new Date(a.scheduledEnd).getTime();
          return aStart < slotEndMs && aEnd > slotStartMs;
        });

        if (isOccupied) occupiedCount++;
      });

      const ratio = totalSlots > 0 ? occupiedCount / totalSlots : 0;
      let status: "free" | "warning" | "full" = "free";
      if (ratio >= 0.85) status = "full";
      else if (ratio >= 0.4) status = "warning";

      map[dateKey] = { total: totalSlots, occupied: occupiedCount, status };
    }

    return map;
  }, [viewYear, viewMonth, daysInMonth, daySlots, appointments, doctorStaffId]);

  // Slots availability for currently selected date
  const selectedDaySlotsStatus = useMemo(() => {
    if (!selectedDateStr) return [];

    const dayAppts = appointments.filter((a) => {
      if (a.status === "CANCELLED" || a.status === "NO_SHOW") return false;
      if (doctorStaffId && a.doctorStaffId !== doctorStaffId) return false;
      const startIso = a.scheduledStart.slice(0, 10);
      return startIso === selectedDateStr;
    });

    return daySlots.map((slot) => {
      const slotStartMs = new Date(`${selectedDateStr}T${slot.timeStr}:00`).getTime();
      const slotEndMs = slotStartMs + SLOT_DURATION_MINS * 60000;

      const overlappingAppt = dayAppts.find((a) => {
        const aStart = new Date(a.scheduledStart).getTime();
        const aEnd = new Date(a.scheduledEnd).getTime();
        return aStart < slotEndMs && aEnd > slotStartMs;
      });

      return {
        ...slot,
        isOccupied: Boolean(overlappingAppt),
        isPast: slotStartMs < currentTimeMs,
        apptReason: overlappingAppt?.reason || "Cita ocupada"
      };
    });
  }, [selectedDateStr, daySlots, appointments, doctorStaffId, currentTimeMs]);

  const handlePrevMonth = () => {
    if (viewMonth === 0) {
      setViewMonth(11);
      setViewYear(viewYear - 1);
    } else {
      setViewMonth(viewMonth - 1);
    }
  };

  const handleNextMonth = () => {
    if (viewMonth === 11) {
      setViewMonth(0);
      setViewYear(viewYear + 1);
    } else {
      setViewMonth(viewMonth + 1);
    }
  };

  const handleSelectSlot = (slotTimeStr: string) => {
    const newStartStr = `${selectedDateStr}T${slotTimeStr}`;
    const startDt = new Date(newStartStr);
    const endDt = new Date(startDt.getTime() + currentDurationMins * 60000);

    const endYear = endDt.getFullYear();
    const endMonth = pad(endDt.getMonth() + 1);
    const endDay = pad(endDt.getDate());
    const endHour = pad(endDt.getHours());
    const endMin = pad(endDt.getMinutes());
    const newEndStr = `${endYear}-${endMonth}-${endDay}T${endHour}:${endMin}`;

    onSelectSlot(newStartStr, newEndStr);
    onClose();
  };

  const formattedSelectedDateLabel = useMemo(() => {
    if (!selectedDateStr) return "";
    const parts = selectedDateStr.split("-");
    const d = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
    return d.toLocaleDateString("es-MX", { weekday: "long", day: "numeric", month: "long", year: "numeric" });
  }, [selectedDateStr]);

  return (
    <div className="modal-overlay" onClick={onClose} style={{ zIndex: 1100 }}>
      <div
        className="modal-card modal-card-wide"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: "900px", width: "95%", maxHeight: "90vh", display: "flex", flexDirection: "column" }}
      >
        <div className="panel-heading">
          <div>
            <h2>Seleccionar Horario y Cupo Disponible</h2>
            <p className="panel-subtitle" style={{ margin: 0 }}>
              {selectedDoctor ? `Doctor: ${selectedDoctor.fullName}` : "Todos los doctores de la clínica"}
            </p>
          </div>
          <button className="icon-btn" type="button" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>

        {/* Legend bar */}
        <div
          style={{
            display: "flex",
            gap: "16px",
            alignItems: "center",
            padding: "8px 12px",
            backgroundColor: "var(--color-surface, #f8fafc)",
            borderRadius: "8px",
            fontSize: "12px",
            marginBottom: "12px",
            border: "1px solid var(--color-border, #e2e8f0)"
          }}
        >
          <strong>Nivel de Ocupación:</strong>
          <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
            <span style={{ display: "inline-block", width: "12px", height: "12px", borderRadius: "50%", border: "1px solid #cbd5e1", backgroundColor: "#ffffff" }}></span>
            <span>Libre / Disponible</span>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
            <span style={{ display: "inline-block", width: "12px", height: "12px", borderRadius: "50%", backgroundColor: "#fef08a", border: "1px solid #eab308" }}></span>
            <span>Parcialmente Ocupado (50%+)</span>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
            <span style={{ display: "inline-block", width: "12px", height: "12px", borderRadius: "50%", backgroundColor: "#fca5a5", border: "1px solid #ef4444" }}></span>
            <span>Lleno / Sin cupo (90%+)</span>
          </div>
        </div>

        {/* Main split grid */}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px", flex: 1, overflow: "hidden" }}>
          {/* Left Column: Month Calendar */}
          <div style={{ display: "flex", flexDirection: "column", borderRight: "1px solid var(--color-border, #e2e8f0)", paddingRight: "16px" }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "12px" }}>
              <button
                type="button"
                className="icon-btn"
                onClick={handlePrevMonth}
                style={{ padding: "4px 8px", borderRadius: "6px", border: "1px solid var(--color-border, #e2e8f0)" }}
              >
                <IconChevronLeft size={16} />
              </button>
              <strong style={{ fontSize: "15px" }}>
                {MONTH_NAMES[viewMonth]} {viewYear}
              </strong>
              <button
                type="button"
                className="icon-btn"
                onClick={handleNextMonth}
                style={{ padding: "4px 8px", borderRadius: "6px", border: "1px solid var(--color-border, #e2e8f0)" }}
              >
                <IconChevronRight size={16} />
              </button>
            </div>

            {/* Days header */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(7, 1fr)", textTransform: "uppercase", fontSize: "11px", color: "var(--color-text-3, #64748b)", textAlign: "center", marginBottom: "6px", fontWeight: 600 }}>
              {DAY_NAMES.map((d) => (
                <div key={d}>{d}</div>
              ))}
            </div>

            {/* Calendar Days */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(7, 1fr)", gap: "4px", textAlign: "center" }}>
              {Array.from({ length: firstDayOfWeek }).map((_, idx) => (
                <div key={`empty-${idx}`} style={{ padding: "10px 0" }} />
              ))}

              {Array.from({ length: daysInMonth }).map((_, idx) => {
                const dayNum = idx + 1;
                const dateKey = toIsoDateStr(viewYear, viewMonth, dayNum);
                const occ = occupancyByDay[dateKey];
                const isSelected = dateKey === selectedDateStr;

                let bgColor = "var(--color-card, #ffffff)";
                let borderColor = "var(--color-border, #e2e8f0)";
                let textColor = "var(--color-text-1, #1e293b)";

                if (occ?.status === "full") {
                  bgColor = "#fee2e2";
                  borderColor = "#f87171";
                  textColor = "#991b1b";
                } else if (occ?.status === "warning") {
                  bgColor = "#fef9c3";
                  borderColor = "#facc15";
                  textColor = "#854d0e";
                }

                if (isSelected) {
                  bgColor = "#0284c7";
                  borderColor = "#0284c7";
                  textColor = "#ffffff";
                }

                return (
                  <button
                    key={dateKey}
                    type="button"
                    onClick={() => setSelectedDateStr(dateKey)}
                    style={{
                      padding: "8px 0",
                      borderRadius: "8px",
                      border: `2px solid ${borderColor}`,
                      backgroundColor: bgColor,
                      color: textColor,
                      fontWeight: isSelected ? 700 : 500,
                      cursor: "pointer",
                      display: "flex",
                      flexDirection: "column",
                      alignItems: "center",
                      gap: "2px",
                      transition: "all 0.15s ease"
                    }}
                  >
                    <span style={{ fontSize: "13px" }}>{dayNum}</span>
                    {occ && occ.occupied > 0 && !isSelected && (
                      <span
                        style={{
                          fontSize: "9px",
                          fontWeight: 600,
                          opacity: 0.85
                        }}
                      >
                        {occ.occupied}/{occ.total}
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Right Column: Time Slots for Selected Day */}
          <div style={{ display: "flex", flexDirection: "column", overflow: "hidden" }}>
            <div style={{ marginBottom: "12px", borderBottom: "1px solid var(--color-border, #e2e8f0)", paddingBottom: "8px" }}>
              <span style={{ fontSize: "12px", color: "var(--color-text-3, #64748b)", textTransform: "capitalize" }}>
                Día seleccionado
              </span>
              <h3 style={{ margin: "2px 0 0 0", fontSize: "15px", textTransform: "capitalize" }}>
                {formattedSelectedDateLabel}
              </h3>
            </div>

            <div style={{ flex: 1, overflowY: "auto", display: "flex", flexDirection: "column", gap: "6px", paddingRight: "4px" }}>
              {selectedDaySlotsStatus.map((slot) => (
                <div
                  key={slot.timeStr}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    padding: "8px 12px",
                    borderRadius: "6px",
                    border: slot.isOccupied ? "1px solid #fca5a5" : "1px solid #cbd5e1",
                    backgroundColor: slot.isOccupied ? "#fef2f2" : slot.isPast ? "#f1f5f9" : "var(--color-card, #ffffff)",
                    opacity: slot.isOccupied || slot.isPast ? 0.75 : 1
                  }}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <IconClock size={16} color={slot.isOccupied ? "#ef4444" : slot.isPast ? "#64748b" : "#0284c7"} />
                    <span style={{ fontSize: "13px", fontWeight: 600 }}>{slot.label}</span>
                  </div>

                  {slot.isOccupied ? (
                    <span style={{ fontSize: "11px", color: "#991b1b", backgroundColor: "#fee2e2", padding: "2px 6px", borderRadius: "4px", fontWeight: 500 }}>
                      🔴 Ocupado
                    </span>
                  ) : slot.isPast ? (
                    <span style={{ fontSize: "11px", color: "#475569", backgroundColor: "#e2e8f0", padding: "2px 6px", borderRadius: "4px", fontWeight: 500 }}>
                      Horario pasado
                    </span>
                  ) : (
                    <button
                      type="button"
                      className="btn primary"
                      style={{ fontSize: "11px", padding: "3px 10px", backgroundColor: "#0284c7" }}
                      onClick={() => handleSelectSlot(slot.timeStr)}
                    >
                      <IconCheck size={12} /> Seleccionar cupo
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="form-actions" style={{ marginTop: "16px", paddingTop: "12px", borderTop: "1px solid var(--color-border, #e2e8f0)" }}>
          <button className="btn secondary" type="button" onClick={onClose}>
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}
