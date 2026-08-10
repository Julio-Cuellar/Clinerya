import { useEffect, useRef, useState } from "react";
import { IconCalendar, IconChevronLeft, IconChevronRight, IconClock, IconX } from "@tabler/icons-react";

interface CustomDateTimePickerProps {
  value: string; // Format: "YYYY-MM-DDTHH:mm"
  onChange: (newValue: string) => void;
  onOpenPicker?: () => void;
  required?: boolean;
  disabled?: boolean;
}

const MONTH_NAMES = [
  "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
  "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
];

const DAY_NAMES = ["Do", "Lu", "Ma", "Mi", "Ju", "Vi", "Sá"];

export function CustomDateTimePicker({
  value,
  onChange,
  onOpenPicker,
  disabled
}: CustomDateTimePickerProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const handleTriggerClick = () => {
    if (disabled) return;
    if (onOpenPicker) {
      onOpenPicker();
    } else {
      setIsOpen(!isOpen);
    }
  };

  // Parse initial date
  const parsedDate = value ? new Date(value) : new Date();
  const validDate = isNaN(parsedDate.getTime()) ? new Date() : parsedDate;

  const [viewYear, setViewYear] = useState(validDate.getFullYear());
  const [viewMonth, setViewMonth] = useState(validDate.getMonth()); // 0-11

  const [selectedDay, setSelectedDay] = useState(validDate.getDate());
  const [selectedMonth, setSelectedMonth] = useState(validDate.getMonth());
  const [selectedYear, setSelectedYear] = useState(validDate.getFullYear());

  const rawHour = validDate.getHours();
  const isPm = rawHour >= 12;
  const hour12 = rawHour % 12 === 0 ? 12 : rawHour % 12;

  const [selectedHour, setSelectedHour] = useState(hour12);
  const [selectedMinute, setSelectedMinute] = useState(validDate.getMinutes());
  const [selectedAmPm, setSelectedAmPm] = useState<"am" | "pm">(isPm ? "pm" : "am");

  // Synchronize when value changes externally
  useEffect(() => {
    if (!value) return;
    const d = new Date(value);
    if (!isNaN(d.getTime())) {
      setSelectedYear(d.getFullYear());
      setSelectedMonth(d.getMonth());
      setSelectedDay(d.getDate());
      setViewYear(d.getFullYear());
      setViewMonth(d.getMonth());
      const h = d.getHours();
      setSelectedAmPm(h >= 12 ? "pm" : "am");
      setSelectedHour(h % 12 === 0 ? 12 : h % 12);
      setSelectedMinute(d.getMinutes());
    }
  }, [value]);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const emitChange = (y: number, m: number, d: number, h12: number, min: number, ampm: "am" | "pm") => {
    let h24 = h12;
    if (ampm === "pm" && h12 < 12) h24 += 12;
    if (ampm === "am" && h12 === 12) h24 = 0;

    const pad = (n: number) => String(n).padStart(2, "0");
    const formatted = `${y}-${pad(m + 1)}-${pad(d)}T${pad(h24)}:${pad(min)}`;
    onChange(formatted);
  };

  const handleDayClick = (dayNum: number) => {
    setSelectedYear(viewYear);
    setSelectedMonth(viewMonth);
    setSelectedDay(dayNum);
    emitChange(viewYear, viewMonth, dayNum, selectedHour, selectedMinute, selectedAmPm);
  };

  const handleHourClick = (h: number) => {
    setSelectedHour(h);
    emitChange(selectedYear, selectedMonth, selectedDay, h, selectedMinute, selectedAmPm);
  };

  const handleMinuteClick = (m: number) => {
    setSelectedMinute(m);
    emitChange(selectedYear, selectedMonth, selectedDay, selectedHour, m, selectedAmPm);
  };

  const handleAmPmClick = (ampm: "am" | "pm") => {
    setSelectedAmPm(ampm);
    emitChange(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, ampm);
  };

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

  // Calendar math
  const firstDayOfMonth = new Date(viewYear, viewMonth, 1).getDay(); // 0 is Sunday
  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
  const prevMonthDays = new Date(viewYear, viewMonth, 0).getDate();

  const pad = (n: number) => String(n).padStart(2, "0");
  const displayFormatted = `${selectedDay}/${pad(selectedMonth + 1)}/${selectedYear} ${pad(selectedHour)}:${pad(selectedMinute)} ${selectedAmPm.toUpperCase()}`;

  return (
    <div className="custom-datetime-picker" ref={containerRef} style={{ position: "relative", width: "100%" }}>
      <div style={{ display: "flex", alignItems: "center", position: "relative" }}>
        <input
          type="text"
          readOnly
          value={displayFormatted}
          onClick={handleTriggerClick}
          disabled={disabled}
          style={{
            width: "100%",
            cursor: disabled ? "not-allowed" : "pointer",
            paddingRight: "36px",
            fontWeight: 500
          }}
        />
        <button
          type="button"
          className="icon-btn"
          style={{
            position: "absolute",
            right: "6px",
            background: "none",
            border: "none",
            cursor: disabled ? "not-allowed" : "pointer",
            color: "var(--color-primary, #0284c7)"
          }}
          onClick={handleTriggerClick}
        >
          <IconCalendar size={18} />
        </button>
      </div>

      {isOpen && (
        <div
          className="datetime-popup"
          style={{
            position: "absolute",
            top: "100%",
            left: 0,
            zIndex: 1000,
            marginTop: "6px",
            backgroundColor: "var(--color-card, #ffffff)",
            border: "1px solid var(--color-border, #cbd5e1)",
            borderRadius: "12px",
            boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.15), 0 8px 10px -6px rgba(0, 0, 0, 0.1)",
            padding: "16px",
            display: "flex",
            gap: "16px",
            width: "auto",
            minWidth: "380px"
          }}
        >
          {/* Calendar Section */}
          <div style={{ flex: 1 }}>
            {/* Header: Month / Year */}
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "12px" }}>
              <button
                type="button"
                className="icon-btn"
                onClick={handlePrevMonth}
                style={{ padding: "4px", borderRadius: "6px", border: "1px solid var(--color-border, #e2e8f0)", background: "none", cursor: "pointer" }}
              >
                <IconChevronLeft size={16} />
              </button>
              <strong style={{ fontSize: "14px", color: "var(--color-text-1, #1e293b)" }}>
                {MONTH_NAMES[viewMonth]} {viewYear}
              </strong>
              <button
                type="button"
                className="icon-btn"
                onClick={handleNextMonth}
                style={{ padding: "4px", borderRadius: "6px", border: "1px solid var(--color-border, #e2e8f0)", background: "none", cursor: "pointer" }}
              >
                <IconChevronRight size={16} />
              </button>
            </div>

            {/* Days Header */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(7, 1fr)", textTransform: "lowercase", fontSize: "11px", color: "var(--color-text-3, #94a3b8)", textAlign: "center", marginBottom: "6px", fontWeight: 600 }}>
              {DAY_NAMES.map((d) => (
                <div key={d}>{d}.</div>
              ))}
            </div>

            {/* Days Grid */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(7, 1fr)", gap: "2px", textAlign: "center" }}>
              {/* Prev month padding */}
              {Array.from({ length: firstDayOfMonth }).map((_, idx) => (
                <div key={`prev-${idx}`} style={{ padding: "6px", fontSize: "12px", color: "var(--color-text-3, #cbd5e1)", opacity: 0.4 }}>
                  {prevMonthDays - firstDayOfMonth + idx + 1}
                </div>
              ))}

              {/* Current month days */}
              {Array.from({ length: daysInMonth }).map((_, idx) => {
                const dayNum = idx + 1;
                const isSelected = dayNum === selectedDay && viewMonth === selectedMonth && viewYear === selectedYear;
                const isToday = dayNum === new Date().getDate() && viewMonth === new Date().getMonth() && viewYear === new Date().getFullYear();

                return (
                  <button
                    key={`day-${dayNum}`}
                    type="button"
                    onClick={() => handleDayClick(dayNum)}
                    style={{
                      padding: "6px 0",
                      fontSize: "12px",
                      borderRadius: "6px",
                      border: isToday && !isSelected ? "1px solid #0284c7" : "none",
                      backgroundColor: isSelected ? "#0284c7" : "transparent",
                      color: isSelected ? "#ffffff" : "var(--color-text-1, #1e293b)",
                      fontWeight: isSelected || isToday ? 600 : 400,
                      cursor: "pointer",
                      transition: "all 0.15s ease"
                    }}
                  >
                    {dayNum}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Time Picker Section */}
          <div style={{ borderLeft: "1px solid var(--color-border, #e2e8f0)", paddingLeft: "16px", display: "flex", flexDirection: "column", gap: "12px", width: "130px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "12px", fontWeight: 600, color: "var(--color-text-2, #64748b)" }}>
              <IconClock size={14} /> Hora
            </div>

            {/* AM / PM Toggle */}
            <div style={{ display: "flex", gap: "4px", backgroundColor: "var(--color-surface, #f1f5f9)", padding: "2px", borderRadius: "6px" }}>
              <button
                type="button"
                onClick={() => handleAmPmClick("am")}
                style={{
                  flex: 1,
                  padding: "4px",
                  fontSize: "11px",
                  fontWeight: 600,
                  borderRadius: "4px",
                  border: "none",
                  backgroundColor: selectedAmPm === "am" ? "#0284c7" : "transparent",
                  color: selectedAmPm === "am" ? "#ffffff" : "var(--color-text-2, #64748b)",
                  cursor: "pointer"
                }}
              >
                a. m.
              </button>
              <button
                type="button"
                onClick={() => handleAmPmClick("pm")}
                style={{
                  flex: 1,
                  padding: "4px",
                  fontSize: "11px",
                  fontWeight: 600,
                  borderRadius: "4px",
                  border: "none",
                  backgroundColor: selectedAmPm === "pm" ? "#0284c7" : "transparent",
                  color: selectedAmPm === "pm" ? "#ffffff" : "var(--color-text-2, #64748b)",
                  cursor: "pointer"
                }}
              >
                p. m.
              </button>
            </div>

            {/* Hours Grid */}
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "4px", maxHeight: "120px", overflowY: "auto" }}>
              {[12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11].map((h) => (
                <button
                  key={h}
                  type="button"
                  onClick={() => handleHourClick(h)}
                  style={{
                    padding: "4px",
                    fontSize: "11px",
                    borderRadius: "4px",
                    border: "none",
                    backgroundColor: selectedHour === h ? "rgba(2, 132, 199, 0.15)" : "transparent",
                    color: selectedHour === h ? "#0284c7" : "var(--color-text-1, #334155)",
                    fontWeight: selectedHour === h ? 600 : 400,
                    cursor: "pointer"
                  }}
                >
                  {pad(h)}
                </button>
              ))}
            </div>

            {/* Minutes Grid */}
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "4px" }}>
              {[0, 15, 30, 45].map((m) => (
                <button
                  key={m}
                  type="button"
                  onClick={() => handleMinuteClick(m)}
                  style={{
                    padding: "4px",
                    fontSize: "11px",
                    borderRadius: "4px",
                    border: "none",
                    backgroundColor: selectedMinute === m ? "#0284c7" : "var(--color-surface, #f1f5f9)",
                    color: selectedMinute === m ? "#ffffff" : "var(--color-text-1, #334155)",
                    fontWeight: selectedMinute === m ? 600 : 400,
                    cursor: "pointer"
                  }}
                >
                  :{pad(m)}
                </button>
              ))}
            </div>

            <button
              type="button"
              className="btn primary"
              style={{ fontSize: "12px", padding: "6px", marginTop: "auto" }}
              onClick={() => setIsOpen(false)}
            >
              Listo
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
