import { FormEvent, useEffect, useMemo, useState } from "react";
import { IconCopy, IconDeviceFloppy, IconSignature, IconX } from "@tabler/icons-react";
import { agendaApi, clinicalNotesApi, getFriendlyError } from "@shared/api/api";
import type {
  ClinicalNoteAddendumResponse,
  ClinicalNoteDiagnosisResponse,
  ClinicalNoteFields,
  ClinicalNoteResponse,
  DiagnosisEntryInput
} from "@modules/records/types";
import type { DoctorResponse } from "@modules/agenda/types";
import { Icd10Autocomplete } from "@modules/records/components/Icd10Autocomplete";

function toNumberString(value: number | null | undefined): string {
  return value === null || value === undefined ? "" : String(value);
}

function toOptionalNumber(value: string): number | undefined {
  return value.trim() === "" ? undefined : Number(value);
}

interface DraftState {
  subjective: string;
  objective: string;
  temperature: string;
  bloodPressure: string;
  heartRate: string;
  respiratoryRate: string;
  weight: string;
  height: string;
  oxygenSaturation: string;
  assessment: string;
  plan: string;
}

function readDraft(key: string): DraftState | null {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as DraftState) : null;
  } catch {
    return null;
  }
}

function writeDraft(key: string, draft: DraftState) {
  try {
    localStorage.setItem(key, JSON.stringify(draft));
  } catch {
    // almacenamiento no disponible (modo privado, cuota); el autoguardado es best-effort
  }
}

function clearDraft(key: string) {
  try {
    localStorage.removeItem(key);
  } catch {
    // ídem
  }
}

export function ClinicalNoteModal({
  patientId,
  clinicId,
  existingNote,
  defaultDoctorId,
  onClose,
  onSaved,
  readOnly = false
}: {
  patientId: string;
  clinicId: string;
  existingNote?: ClinicalNoteResponse;
  defaultDoctorId?: string;
  onClose: () => void;
  onSaved: () => void;
  readOnly?: boolean;
}) {
  const locked = existingNote?.status === "SIGNED" || readOnly;
  const showDoctorSelect = !existingNote && !defaultDoctorId;
  const draftKey = `clinical-note-draft:${patientId}:${existingNote?.id ?? "new"}`;

  const [doctors, setDoctors] = useState<DoctorResponse[]>([]);
  const [doctorId, setDoctorId] = useState(existingNote?.doctorId ?? defaultDoctorId ?? "");
  const [subjective, setSubjective] = useState(existingNote?.subjective ?? "");
  const [objective, setObjective] = useState(existingNote?.objective ?? "");
  const [temperature, setTemperature] = useState(toNumberString(existingNote?.vitalSigns?.temperature));
  const [bloodPressure, setBloodPressure] = useState(existingNote?.vitalSigns?.bloodPressure ?? "");
  const [heartRate, setHeartRate] = useState(toNumberString(existingNote?.vitalSigns?.heartRate));
  const [respiratoryRate, setRespiratoryRate] = useState(toNumberString(existingNote?.vitalSigns?.respiratoryRate));
  const [weight, setWeight] = useState(toNumberString(existingNote?.vitalSigns?.weight));
  const [height, setHeight] = useState(toNumberString(existingNote?.vitalSigns?.height));
  const [oxygenSaturation, setOxygenSaturation] = useState(toNumberString(existingNote?.vitalSigns?.oxygenSaturation));
  const [assessment, setAssessment] = useState(existingNote?.assessment ?? "");
  const [plan, setPlan] = useState(existingNote?.plan ?? "");
  const [saving, setSaving] = useState(false);
  const [signing, setSigning] = useState(false);
  const [error, setError] = useState("");
  const [draftRestored, setDraftRestored] = useState(false);
  const [copiedFromLast, setCopiedFromLast] = useState(false);

  const [addenda, setAddenda] = useState<ClinicalNoteAddendumResponse[]>([]);
  const [addendumText, setAddendumText] = useState("");
  const [addingAddendum, setAddingAddendum] = useState(false);

  const [diagnoses, setDiagnoses] = useState<DiagnosisEntryInput[]>([]);
  const [savedDiagnoses, setSavedDiagnoses] = useState<ClinicalNoteDiagnosisResponse[]>([]);

  const currentDraft = useMemo<DraftState>(
    () => ({
      subjective,
      objective,
      temperature,
      bloodPressure,
      heartRate,
      respiratoryRate,
      weight,
      height,
      oxygenSaturation,
      assessment,
      plan
    }),
    [subjective, objective, temperature, bloodPressure, heartRate, respiratoryRate, weight, height, oxygenSaturation, assessment, plan]
  );

  const applyDraft = (draft: DraftState) => {
    setSubjective(draft.subjective ?? "");
    setObjective(draft.objective ?? "");
    setTemperature(draft.temperature ?? "");
    setBloodPressure(draft.bloodPressure ?? "");
    setHeartRate(draft.heartRate ?? "");
    setRespiratoryRate(draft.respiratoryRate ?? "");
    setWeight(draft.weight ?? "");
    setHeight(draft.height ?? "");
    setOxygenSaturation(draft.oxygenSaturation ?? "");
    setAssessment(draft.assessment ?? "");
    setPlan(draft.plan ?? "");
  };

  useEffect(() => {
    if (!showDoctorSelect) return;
    agendaApi
      .listDoctors(clinicId)
      .then(setDoctors)
      .catch(() => {
        // si no se puede cargar el listado de doctores, se deja el selector vacío
      });
  }, [clinicId, showDoctorSelect]);

  // Hidratar un borrador local si existe y la nota aún es editable.
  useEffect(() => {
    if (locked) return;
    const draft = readDraft(draftKey);
    if (draft) {
      applyDraft(draft);
      setDraftRestored(true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Autoguardado local mientras se edita (best-effort).
  useEffect(() => {
    if (locked) return;
    writeDraft(draftKey, currentDraft);
  }, [locked, draftKey, currentDraft]);

  // Addendums de una nota firmada.
  useEffect(() => {
    if (existingNote?.status !== "SIGNED") return;
    clinicalNotesApi
      .listAddenda(patientId, existingNote.id, clinicId)
      .then(setAddenda)
      .catch(() => {
        // no se pudieron cargar los addendums; no bloquea la vista de la nota
      });
  }, [patientId, clinicId, existingNote?.id, existingNote?.status]);

  // Diagnosticos CIE-10 de una nota ya firmada (se capturan al firmar, ver handleSign).
  useEffect(() => {
    if (existingNote?.status !== "SIGNED") return;
    clinicalNotesApi
      .listDiagnoses(patientId, existingNote.id, clinicId)
      .then(setSavedDiagnoses)
      .catch(() => {
        // no se pudieron cargar los diagnosticos; no bloquea la vista de la nota
      });
  }, [patientId, clinicId, existingNote?.id, existingNote?.status]);

  const buildFields = (): ClinicalNoteFields => ({
    subjective: subjective || undefined,
    objective: objective || undefined,
    temperature: toOptionalNumber(temperature),
    bloodPressure: bloodPressure || undefined,
    heartRate: toOptionalNumber(heartRate),
    respiratoryRate: toOptionalNumber(respiratoryRate),
    weight: toOptionalNumber(weight),
    height: toOptionalNumber(height),
    oxygenSaturation: toOptionalNumber(oxygenSaturation),
    assessment: assessment || undefined,
    plan: plan || undefined
  });

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      if (existingNote) {
        await clinicalNotesApi.update(patientId, existingNote.id, { clinicId, ...buildFields() });
      } else {
        await clinicalNotesApi.create(patientId, { clinicId, doctorId, status: "DRAFT", ...buildFields() });
      }
      clearDraft(draftKey);
      onSaved();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  const handleSign = async () => {
    if (!existingNote) return;
    if (!window.confirm("Firmar esta nota la deja bloqueada de forma permanente. ¿Confirmas?")) return;
    setSigning(true);
    setError("");
    try {
      await clinicalNotesApi.sign(patientId, existingNote.id, clinicId, diagnoses);
      clearDraft(draftKey);
      onSaved();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSigning(false);
    }
  };

  const handleCopyFromLast = async () => {
    setError("");
    try {
      const all = await clinicalNotesApi.listByPatient(patientId, clinicId);
      const last = all
        .filter((note) => note.id !== existingNote?.id)
        .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())[0];
      if (!last) {
        setError("No hay una nota previa para copiar.");
        return;
      }
      applyDraft({
        subjective: last.subjective ?? "",
        objective: last.objective ?? "",
        temperature: toNumberString(last.vitalSigns?.temperature),
        bloodPressure: last.vitalSigns?.bloodPressure ?? "",
        heartRate: toNumberString(last.vitalSigns?.heartRate),
        respiratoryRate: toNumberString(last.vitalSigns?.respiratoryRate),
        weight: toNumberString(last.vitalSigns?.weight),
        height: toNumberString(last.vitalSigns?.height),
        oxygenSaturation: toNumberString(last.vitalSigns?.oxygenSaturation),
        assessment: last.assessment ?? "",
        plan: last.plan ?? ""
      });
      setCopiedFromLast(true);
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  const handleAddAddendum = async () => {
    if (!existingNote || !addendumText.trim()) return;
    setAddingAddendum(true);
    setError("");
    try {
      const created = await clinicalNotesApi.addAddendum(patientId, existingNote.id, {
        clinicId,
        content: addendumText.trim()
      });
      setAddenda((prev) => [...prev, created]);
      setAddendumText("");
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setAddingAddendum(false);
    }
  };

  const signerName = existingNote?.signedByName || existingNote?.signedByUserId;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>{existingNote ? "Nota clínica" : "Nueva nota clínica"}</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>

        {existingNote?.doctorName && (
          <p className="note-modal-author">
            Autor: <strong>{existingNote.doctorName}</strong>
          </p>
        )}

        {!locked && (draftRestored || copiedFromLast) && (
          <p className="note-modal-hint">
            {copiedFromLast ? "Contenido copiado de la última nota. Revísalo antes de guardar." : "Se recuperó un borrador sin guardar."}
          </p>
        )}

        <form className="profile-form" onSubmit={handleSubmit}>
          {showDoctorSelect && (
            <label className="field field-full">
              <span>Doctor responsable</span>
              <select value={doctorId} required disabled={locked} onChange={(event) => setDoctorId(event.target.value)}>
                <option value="" disabled>
                  Selecciona un doctor
                </option>
                {doctors.map((doctor) => (
                  <option key={doctor.staffId} value={doctor.staffId}>
                    {doctor.fullName}
                  </option>
                ))}
              </select>
            </label>
          )}

          {!locked && (
            <div className="field-full">
              <button className="btn ghost" type="button" onClick={handleCopyFromLast}>
                <IconCopy size={16} aria-hidden="true" />
                Copiar de la última nota
              </button>
            </div>
          )}

          <label className="field field-full">
            <span>Subjetivo</span>
            <textarea value={subjective} disabled={locked} onChange={(event) => setSubjective(event.target.value)} />
          </label>

          <label className="field field-full">
            <span>Objetivo</span>
            <textarea value={objective} disabled={locked} onChange={(event) => setObjective(event.target.value)} />
          </label>

          <label className="field">
            <span>Temperatura (°C)</span>
            <input type="number" step="0.1" value={temperature} disabled={locked} onChange={(event) => setTemperature(event.target.value)} />
          </label>
          <label className="field">
            <span>Presión arterial</span>
            <input
              type="text"
              placeholder="120/80"
              value={bloodPressure}
              disabled={locked}
              onChange={(event) => setBloodPressure(event.target.value)}
            />
          </label>
          <label className="field">
            <span>Frecuencia cardiaca (lpm)</span>
            <input type="number" value={heartRate} disabled={locked} onChange={(event) => setHeartRate(event.target.value)} />
          </label>
          <label className="field">
            <span>Frecuencia respiratoria (rpm)</span>
            <input
              type="number"
              value={respiratoryRate}
              disabled={locked}
              onChange={(event) => setRespiratoryRate(event.target.value)}
            />
          </label>
          <label className="field">
            <span>Peso (kg)</span>
            <input type="number" step="0.1" value={weight} disabled={locked} onChange={(event) => setWeight(event.target.value)} />
          </label>
          <label className="field">
            <span>Talla (m)</span>
            <input type="number" step="0.01" value={height} disabled={locked} onChange={(event) => setHeight(event.target.value)} />
          </label>
          <label className="field">
            <span>Saturación de oxígeno (%)</span>
            <input
              type="number"
              value={oxygenSaturation}
              disabled={locked}
              onChange={(event) => setOxygenSaturation(event.target.value)}
            />
          </label>

          <label className="field field-full">
            <span>Evaluación</span>
            <textarea value={assessment} disabled={locked} onChange={(event) => setAssessment(event.target.value)} />
          </label>

          <label className="field field-full">
            <span>Plan</span>
            <textarea value={plan} disabled={locked} onChange={(event) => setPlan(event.target.value)} />
          </label>

          {existingNote && existingNote.status === "DRAFT" && !readOnly && (
            <div className="field field-full">
              <span>Diagnóstico (CIE-10)</span>
              <Icd10Autocomplete value={diagnoses} onChange={setDiagnoses} />
            </div>
          )}

          {existingNote?.status === "SIGNED" && savedDiagnoses.length > 0 && (
            <div className="field field-full">
              <span>Diagnóstico (CIE-10)</span>
              <Icd10Autocomplete
                value={savedDiagnoses.map((d) => ({ icd10Code: d.icd10Code, kind: d.kind }))}
                onChange={() => {}}
                disabled
              />
            </div>
          )}

          {locked && (
            <p className="field-full">
              {existingNote?.status === "SIGNED"
                ? "Esta nota está firmada y ya no se puede editar."
                : "No tienes permiso para editar esta nota."}
            </p>
          )}
          {existingNote?.status === "SIGNED" && (
            <div className="signature-evidence field-full">
              <strong>Firma médica registrada</strong>
              <span>
                Fecha: {existingNote.signedAt ? new Date(existingNote.signedAt).toLocaleString("es-MX") : "No disponible"}
              </span>
              {signerName && <span>Firmante: {signerName}</span>}
              {existingNote.documentHash && (
                <span>
                  Hash SHA-256: <code>{existingNote.documentHash}</code>
                </span>
              )}
            </div>
          )}
          {error && <p className="alert error field-full">{error}</p>}

          {!locked && (
            <div className="form-actions field-full">
              <button className="btn primary" type="submit" disabled={saving || signing}>
                <IconDeviceFloppy size={18} aria-hidden="true" />
                {saving ? "Guardando..." : "Guardar borrador"}
              </button>
              {existingNote && existingNote.status === "DRAFT" && (
                <button className="btn secondary" type="button" disabled={saving || signing} onClick={handleSign}>
                  <IconSignature size={18} aria-hidden="true" />
                  {signing ? "Firmando..." : "Firmar"}
                </button>
              )}
            </div>
          )}
        </form>

        {existingNote?.status === "SIGNED" && (
          <div className="note-addenda">
            <h3>Addendums</h3>
            <p className="note-addenda-help">
              La nota firmada no se modifica. Toda corrección posterior se registra como un addendum con su propia firma.
            </p>

            {addenda.length === 0 && <p className="note-addenda-empty">Sin addendums.</p>}

            {addenda.map((addendum) => (
              <div className="note-addendum" key={addendum.id}>
                <div className="note-addendum-head">
                  <strong>{addendum.createdByUserName}</strong>
                  <span>{new Date(addendum.createdAt).toLocaleString("es-MX", { dateStyle: "medium", timeStyle: "short" })}</span>
                </div>
                <p className="note-addendum-body">{addendum.content}</p>
              </div>
            ))}

            {!readOnly && (
              <div className="note-addendum-form">
                <textarea
                  value={addendumText}
                  placeholder="Escribe la corrección o aclaración…"
                  onChange={(event) => setAddendumText(event.target.value)}
                />
                <button
                  className="btn secondary"
                  type="button"
                  disabled={addingAddendum || !addendumText.trim()}
                  onClick={handleAddAddendum}
                >
                  {addingAddendum ? "Agregando..." : "Agregar addendum"}
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
