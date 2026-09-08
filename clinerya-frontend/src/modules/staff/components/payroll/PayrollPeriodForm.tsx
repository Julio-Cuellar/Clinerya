import { useRef, useState } from "react";
import { IconArrowLeft, IconReceipt } from "@tabler/icons-react";
import {
  payrollPeriodPreset,
  todayIsoDate,
  type PayrollPeriodPreset
} from "@modules/staff/lib/payroll";

const presetButtons: { preset: PayrollPeriodPreset; label: string }[] = [
  { preset: "fortnight", label: "Quincena actual" },
  { preset: "month", label: "Mes actual" },
  { preset: "week", label: "Semana actual" }
];

/** Standalone page to create a payroll period; reached from the Periodos view. */
export function PayrollPeriodForm({
  onCreatePeriod,
  onBack
}: {
  onCreatePeriod: (input: { name: string; periodStart: string; periodEnd: string }) => Promise<void>;
  onBack: () => void;
}) {
  const formRef = useRef<HTMLFormElement>(null);
  const [creating, setCreating] = useState(false);

  const applyPreset = (preset: PayrollPeriodPreset) => {
    const form = formRef.current;
    if (!form) return;
    const { start, end, name } = payrollPeriodPreset(preset);
    const startInput = form.elements.namedItem("periodStart") as HTMLInputElement | null;
    const endInput = form.elements.namedItem("periodEnd") as HTMLInputElement | null;
    const nameInput = form.elements.namedItem("name") as HTMLInputElement | null;
    if (startInput) startInput.value = start;
    if (endInput) endInput.value = end;
    if (nameInput && !nameInput.value.trim()) nameInput.value = name;
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setCreating(true);
    try {
      await onCreatePeriod({
        name: String(form.get("name") ?? ""),
        periodStart: String(form.get("periodStart") ?? ""),
        periodEnd: String(form.get("periodEnd") ?? "")
      });
      formRef.current?.reset();
    } catch {
      // Feedback is surfaced by the panel; keep the form values for a retry.
    } finally {
      setCreating(false);
    }
  };

  return (
    <article className="panel full">
      <div className="panel-heading">
        <div>
          <h2>Nuevo periodo</h2>
          <span className="panel-subtitle">Un acceso rapido rellena fechas y nombre; ajusta lo que necesites</span>
        </div>
        <button className="btn ghost" type="button" onClick={onBack}>
          <IconArrowLeft size={16} aria-hidden="true" />
          Volver a periodos
        </button>
      </div>
      <form className="profile-form" ref={formRef} onSubmit={handleSubmit}>
        <div className="field field-full">
          <span>Periodo rapido</span>
          <div className="clinic-row-actions">
            {presetButtons.map(({ preset, label }) => (
              <button key={preset} className="btn ghost" type="button" onClick={() => applyPreset(preset)}>
                {label}
              </button>
            ))}
          </div>
        </div>
        <label className="field">
          <span>Nombre del periodo</span>
          <input name="name" placeholder="Nomina agosto 2026" required />
        </label>
        <label className="field">
          <span>Inicio</span>
          <input name="periodStart" type="date" defaultValue={todayIsoDate()} required />
        </label>
        <label className="field">
          <span>Fin</span>
          <input name="periodEnd" type="date" defaultValue={todayIsoDate()} required />
        </label>
        <div className="form-actions">
          <button className="btn primary" type="submit" disabled={creating}>
            <IconReceipt size={16} aria-hidden="true" />
            {creating ? "Creando..." : "Crear periodo"}
          </button>
        </div>
      </form>
    </article>
  );
}
