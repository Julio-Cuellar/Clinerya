import { FormEvent, ReactNode, useState } from "react";
import { IconTrash, IconX } from "@tabler/icons-react";
import { clinicalSummaryApi, getFriendlyError } from "@shared/api/api";
import { ConfirmDialog } from "@modules/records/components/ConfirmDialog";
import {
  ALLERGY_CATEGORY_LABELS,
  ALLERGY_SEVERITY_LABELS,
  CONDITION_STATUS_LABELS,
  type AllergyCategory,
  type AllergySeverity,
  type ClinicalReviewDto,
  type ConditionStatus,
  type PatientClinicalSummaryResponse
} from "@modules/records/types";

function fmtDate(value?: string | null) {
  return value ? new Date(value).toLocaleDateString("es-MX") : null;
}

interface PendingChange {
  title: string;
  detail: ReactNode;
  tone?: "primary" | "danger";
  confirmLabel?: string;
  action: () => Promise<unknown>;
}

/**
 * Alta y baja de los datos clínicos tipados del paciente (alergias, padecimientos
 * y medicación activa). Cada cambio se confirma en un modal con los datos
 * capturados antes de guardarse. Sin lógica de decisión clínica.
 */
export function ClinicalDataEditor({
  clinicId,
  patientId,
  summary,
  onClose,
  onChanged
}: {
  clinicId: string;
  patientId: string;
  summary: PatientClinicalSummaryResponse;
  onClose: () => void;
  onChanged: () => void;
}) {
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [pending, setPending] = useState<PendingChange | null>(null);

  const confirmPending = async () => {
    if (!pending) return;
    setBusy(true);
    setError("");
    try {
      await pending.action();
      setPending(null);
      onChanged();
    } catch (caught) {
      setError(getFriendlyError(caught));
      setPending(null);
    } finally {
      setBusy(false);
    }
  };

  const reviewNote = (review: ClinicalReviewDto, label: string) =>
    review.noneReported && review.reviewedAt
      ? `${label} · ${fmtDate(review.reviewedAt)}${review.reviewedByUserName ? ` por ${review.reviewedByUserName}` : ""}`
      : null;

  const askReview = (
    kind: "allergies" | "conditions" | "medications",
    checked: boolean,
    onLabel: string,
    offLabel: string
  ) =>
    setPending({
      title: checked ? onLabel : offLabel,
      detail: (
        <p>
          {checked
            ? "Quedará registrado que se preguntó y el paciente lo negó."
            : "Se quitará esa marca del expediente."}
        </p>
      ),
      action: () => clinicalSummaryApi.setReview(patientId, clinicId, kind, checked)
    });

  return (
    <>
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card modal-card-wide" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Datos clínicos del paciente</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>

        <p className="description">
          Cada dato se confirma antes de guardarse. Si el paciente no tiene alergias, padecimientos o
          medicación, márcalo explícitamente con la casilla de cada sección.
        </p>

        {error && <p className="alert error">{error}</p>}

        {/* ---- Alergias ---- */}
        <section className="clinical-editor-section">
          <h3>Alergias</h3>
          <label className="clinical-editor-check">
            <input
              type="checkbox"
              checked={summary.allergiesReview.noneReported}
              disabled={busy || summary.allergies.length > 0}
              onChange={(e) =>
                askReview(
                  "allergies",
                  e.target.checked,
                  "Marcar alergias como preguntadas y negadas",
                  "Quitar la marca de alergias preguntadas y negadas"
                )
              }
            />
            <span>Preguntadas y negadas (sin alergias conocidas)</span>
          </label>
          {summary.allergies.length > 0 && summary.allergiesReview.noneReported && (
            <p className="description">Hay alergias registradas; quítalas para marcar "preguntadas y negadas".</p>
          )}
          {reviewNote(summary.allergiesReview, "Preguntadas y negadas") && (
            <p className="description">{reviewNote(summary.allergiesReview, "Preguntadas y negadas")}</p>
          )}

          <ul className="clinical-editor-list">
            {summary.allergies.length === 0 && !summary.allergiesReview.noneReported && (
              <li className="description">Sin alergias registradas.</li>
            )}
            {summary.allergies.map((allergy) => (
              <li key={allergy.id}>
                <span>
                  <strong>{allergy.substance}</strong>
                  {allergy.reaction ? ` — ${allergy.reaction}` : ""}{" "}
                  <span className="description">
                    ({ALLERGY_CATEGORY_LABELS[allergy.category]} · {ALLERGY_SEVERITY_LABELS[allergy.severity]})
                  </span>
                </span>
                <button
                  className="icon-btn"
                  type="button"
                  aria-label="Eliminar alergia"
                  disabled={busy}
                  onClick={() =>
                    setPending({
                      title: "Eliminar alergia",
                      tone: "danger",
                      confirmLabel: "Eliminar",
                      detail: (
                        <p>
                          Se eliminará <strong>{allergy.substance}</strong> del expediente.
                        </p>
                      ),
                      action: () => clinicalSummaryApi.removeAllergy(patientId, allergy.id, clinicId)
                    })
                  }
                >
                  <IconTrash size={15} />
                </button>
              </li>
            ))}
          </ul>
          <AllergyForm
            busy={busy}
            onSubmit={(input, label) =>
              setPending({
                title: "Registrar alergia",
                detail: <p>{label}</p>,
                action: () => clinicalSummaryApi.addAllergy(patientId, clinicId, input)
              })
            }
          />
        </section>

        {/* ---- Padecimientos ---- */}
        <section className="clinical-editor-section">
          <h3>Padecimientos</h3>
          <label className="clinical-editor-check">
            <input
              type="checkbox"
              checked={summary.conditionsReview.noneReported}
              disabled={busy || summary.conditions.length > 0}
              onChange={(e) =>
                askReview(
                  "conditions",
                  e.target.checked,
                  "Marcar que no hay padecimientos reportados",
                  "Quitar la marca de sin padecimientos reportados"
                )
              }
            />
            <span>Sin padecimientos reportados</span>
          </label>
          {summary.conditions.length > 0 && summary.conditionsReview.noneReported && (
            <p className="description">Hay padecimientos registrados; quítalos para marcar "sin padecimientos".</p>
          )}
          {reviewNote(summary.conditionsReview, "Sin padecimientos reportados") && (
            <p className="description">{reviewNote(summary.conditionsReview, "Sin padecimientos reportados")}</p>
          )}

          <ul className="clinical-editor-list">
            {summary.conditions.length === 0 && !summary.conditionsReview.noneReported && (
              <li className="description">Sin padecimientos registrados.</li>
            )}
            {summary.conditions.map((condition) => (
              <li key={condition.id}>
                <span>
                  <strong>{condition.name}</strong>
                  {condition.icd10Code ? ` · ${condition.icd10Code}` : ""}{" "}
                  <span className="description">
                    ({CONDITION_STATUS_LABELS[condition.status]}
                    {fmtDate(condition.onsetDate) ? ` · desde ${fmtDate(condition.onsetDate)}` : ""})
                  </span>
                </span>
                <button
                  className="icon-btn"
                  type="button"
                  aria-label="Eliminar padecimiento"
                  disabled={busy}
                  onClick={() =>
                    setPending({
                      title: "Eliminar padecimiento",
                      tone: "danger",
                      confirmLabel: "Eliminar",
                      detail: (
                        <p>
                          Se eliminará <strong>{condition.name}</strong> del expediente.
                        </p>
                      ),
                      action: () => clinicalSummaryApi.removeCondition(patientId, condition.id, clinicId)
                    })
                  }
                >
                  <IconTrash size={15} />
                </button>
              </li>
            ))}
          </ul>
          <ConditionForm
            busy={busy}
            onSubmit={(input, label) =>
              setPending({
                title: "Registrar padecimiento",
                detail: <p>{label}</p>,
                action: () => clinicalSummaryApi.addCondition(patientId, clinicId, input)
              })
            }
          />
        </section>

        {/* ---- Medicación ---- */}
        <section className="clinical-editor-section">
          <h3>Medicación activa</h3>
          <label className="clinical-editor-check">
            <input
              type="checkbox"
              checked={summary.medicationsReview.noneReported}
              disabled={busy || summary.activeMedications.length > 0}
              onChange={(e) =>
                askReview(
                  "medications",
                  e.target.checked,
                  "Marcar que no hay medicación activa",
                  "Quitar la marca de sin medicación activa"
                )
              }
            />
            <span>Sin medicación activa</span>
          </label>
          {summary.activeMedications.length > 0 && summary.medicationsReview.noneReported && (
            <p className="description">Hay medicación registrada; quítala para marcar "sin medicación activa".</p>
          )}
          {reviewNote(summary.medicationsReview, "Sin medicación activa") && (
            <p className="description">{reviewNote(summary.medicationsReview, "Sin medicación activa")}</p>
          )}

          <ul className="clinical-editor-list">
            {summary.activeMedications.length === 0 && !summary.medicationsReview.noneReported && (
              <li className="description">Sin medicación activa registrada.</li>
            )}
            {summary.activeMedications.map((medication) => (
              <li key={medication.id}>
                <span>
                  <strong>{medication.medicationName}</strong>
                  {medication.dose ? ` · ${medication.dose}` : ""}
                  {medication.schedule ? ` · ${medication.schedule}` : ""}{" "}
                  <span className="description">
                    ({fmtDate(medication.startedOn) ?? "inicio s/f"} → {fmtDate(medication.stoppedOn) ?? "en curso"})
                  </span>
                </span>
                <button
                  className="icon-btn"
                  type="button"
                  aria-label="Eliminar medicación"
                  disabled={busy}
                  onClick={() =>
                    setPending({
                      title: "Eliminar medicación",
                      tone: "danger",
                      confirmLabel: "Eliminar",
                      detail: (
                        <p>
                          Se eliminará <strong>{medication.medicationName}</strong> del expediente.
                        </p>
                      ),
                      action: () => clinicalSummaryApi.removeMedication(patientId, medication.id, clinicId)
                    })
                  }
                >
                  <IconTrash size={15} />
                </button>
              </li>
            ))}
          </ul>
          <MedicationForm
            busy={busy}
            onSubmit={(input, label) =>
              setPending({
                title: "Registrar medicamento",
                detail: <p>{label}</p>,
                action: () => clinicalSummaryApi.addMedication(patientId, clinicId, input)
              })
            }
          />
        </section>
      </div>
    </div>

    {pending && (
      <ConfirmDialog
        title={pending.title}
        tone={pending.tone}
        confirmLabel={pending.confirmLabel}
        busy={busy}
        onConfirm={confirmPending}
        onCancel={() => setPending(null)}
      >
        {pending.detail}
      </ConfirmDialog>
    )}
    </>
  );
}

const SEVERITIES: AllergySeverity[] = ["UNKNOWN", "MILD", "MODERATE", "SEVERE"];
const CATEGORIES: AllergyCategory[] = ["DRUG", "FOOD", "ENVIRONMENTAL", "OTHER"];
const STATUSES: ConditionStatus[] = ["ACTIVE", "RESOLVED"];

function AllergyForm({
  busy,
  onSubmit
}: {
  busy: boolean;
  onSubmit: (
    input: { substance: string; reaction: string | null; severity: AllergySeverity; category: AllergyCategory },
    label: string
  ) => void;
}) {
  const [substance, setSubstance] = useState("");
  const [reaction, setReaction] = useState("");
  const [severity, setSeverity] = useState<AllergySeverity>("UNKNOWN");
  const [category, setCategory] = useState<AllergyCategory>("DRUG");

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!substance.trim()) return;
    const s = substance.trim();
    const r = reaction.trim() || null;
    const label = `${s}${r ? ` — ${r}` : ""} (${ALLERGY_CATEGORY_LABELS[category]} · ${ALLERGY_SEVERITY_LABELS[severity]})`;
    onSubmit({ substance: s, reaction: r, severity, category }, label);
    setSubstance("");
    setReaction("");
    setSeverity("UNKNOWN");
    setCategory("DRUG");
  };

  return (
    <form className="clinical-editor-form" onSubmit={submit}>
      <label>
        <span>Sustancia *</span>
        <input placeholder="Sustancia" value={substance} onChange={(e) => setSubstance(e.target.value)} />
      </label>
      <label>
        <span>Reacción</span>
        <input placeholder="Reacción" value={reaction} onChange={(e) => setReaction(e.target.value)} />
      </label>
      <label>
        <span>Tipo</span>
        <select value={category} onChange={(e) => setCategory(e.target.value as AllergyCategory)}>
          {CATEGORIES.map((c) => (
            <option key={c} value={c}>{ALLERGY_CATEGORY_LABELS[c]}</option>
          ))}
        </select>
      </label>
      <label>
        <span>Severidad</span>
        <select value={severity} onChange={(e) => setSeverity(e.target.value as AllergySeverity)}>
          {SEVERITIES.map((s) => (
            <option key={s} value={s}>{ALLERGY_SEVERITY_LABELS[s]}</option>
          ))}
        </select>
      </label>
      <button className="btn secondary" type="submit" disabled={busy || !substance.trim()}>Agregar</button>
    </form>
  );
}

function ConditionForm({
  busy,
  onSubmit
}: {
  busy: boolean;
  onSubmit: (
    input: { name: string; icd10Code: string | null; status: ConditionStatus; onsetDate: string | null },
    label: string
  ) => void;
}) {
  const [name, setName] = useState("");
  const [icd10Code, setIcd10Code] = useState("");
  const [status, setStatus] = useState<ConditionStatus>("ACTIVE");
  const [onsetDate, setOnsetDate] = useState("");

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!name.trim()) return;
    const n = name.trim();
    const code = icd10Code.trim() || null;
    const label = `${n}${code ? ` · ${code}` : ""} (${CONDITION_STATUS_LABELS[status]}${onsetDate ? ` · desde ${onsetDate}` : ""})`;
    onSubmit({ name: n, icd10Code: code, status, onsetDate: onsetDate || null }, label);
    setName("");
    setIcd10Code("");
    setStatus("ACTIVE");
    setOnsetDate("");
  };

  return (
    <form className="clinical-editor-form" onSubmit={submit}>
      <label>
        <span>Padecimiento *</span>
        <input placeholder="Padecimiento" value={name} onChange={(e) => setName(e.target.value)} />
      </label>
      <label>
        <span>CIE-10</span>
        <input placeholder="CIE-10" value={icd10Code} onChange={(e) => setIcd10Code(e.target.value.toUpperCase())} />
      </label>
      <label>
        <span>Estado</span>
        <select value={status} onChange={(e) => setStatus(e.target.value as ConditionStatus)}>
          {STATUSES.map((s) => (
            <option key={s} value={s}>{CONDITION_STATUS_LABELS[s]}</option>
          ))}
        </select>
      </label>
      <label>
        <span>Desde</span>
        <input type="date" value={onsetDate} onChange={(e) => setOnsetDate(e.target.value)} />
      </label>
      <button className="btn secondary" type="submit" disabled={busy || !name.trim()}>Agregar</button>
    </form>
  );
}

function MedicationForm({
  busy,
  onSubmit
}: {
  busy: boolean;
  onSubmit: (
    input: {
      medicationName: string;
      dose: string | null;
      schedule: string | null;
      active: boolean;
      startedOn: string | null;
      stoppedOn: string | null;
    },
    label: string
  ) => void;
}) {
  const [medicationName, setMedicationName] = useState("");
  const [dose, setDose] = useState("");
  const [schedule, setSchedule] = useState("");
  const [startedOn, setStartedOn] = useState("");
  const [stoppedOn, setStoppedOn] = useState("");

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!medicationName.trim()) return;
    const n = medicationName.trim();
    const d = dose.trim() || null;
    const sc = schedule.trim() || null;
    const label = `${n}${d ? ` · ${d}` : ""}${sc ? ` · ${sc}` : ""} (${startedOn || "inicio s/f"} → ${stoppedOn || "en curso"})`;
    onSubmit(
      {
        medicationName: n,
        dose: d,
        schedule: sc,
        active: true,
        startedOn: startedOn || null,
        stoppedOn: stoppedOn || null
      },
      label
    );
    setMedicationName("");
    setDose("");
    setSchedule("");
    setStartedOn("");
    setStoppedOn("");
  };

  return (
    <form className="clinical-editor-form" onSubmit={submit}>
      <label>
        <span>Medicamento *</span>
        <input placeholder="Medicamento" value={medicationName} onChange={(e) => setMedicationName(e.target.value)} />
      </label>
      <label>
        <span>Dosis</span>
        <input placeholder="Dosis" value={dose} onChange={(e) => setDose(e.target.value)} />
      </label>
      <label>
        <span>Frecuencia</span>
        <input placeholder="Frecuencia" value={schedule} onChange={(e) => setSchedule(e.target.value)} />
      </label>
      <label>
        <span>Inicio</span>
        <input type="date" value={startedOn} onChange={(e) => setStartedOn(e.target.value)} />
      </label>
      <label>
        <span>Fin</span>
        <input type="date" value={stoppedOn} onChange={(e) => setStoppedOn(e.target.value)} />
      </label>
      <button className="btn secondary" type="submit" disabled={busy || !medicationName.trim()}>Agregar</button>
    </form>
  );
}
