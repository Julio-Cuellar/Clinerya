import { FormEvent, useEffect, useState } from "react";
import { IconDeviceFloppy, IconSignature, IconX } from "@tabler/icons-react";
import { agendaApi, clinicalNotesApi, getFriendlyError } from "@shared/api/api";
import type { ClinicalNoteFields, ClinicalNoteResponse } from "@modules/records/types";
import type { DoctorResponse } from "@modules/agenda/types";

function toNumberString(value: number | null | undefined): string {
  return value === null || value === undefined ? "" : String(value);
}

function toOptionalNumber(value: string): number | undefined {
  return value.trim() === "" ? undefined : Number(value);
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

  useEffect(() => {
    if (!showDoctorSelect) return;
    agendaApi
      .listDoctors(clinicId)
      .then(setDoctors)
      .catch(() => {
        // si no se puede cargar el listado de doctores, se deja el selector vacío
      });
  }, [clinicId, showDoctorSelect]);

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
      await clinicalNotesApi.sign(patientId, existingNote.id, clinicId);
      onSaved();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSigning(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>{existingNote ? "Nota clínica" : "Nueva nota clínica"}</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>
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
              {existingNote.signedByUserId && <span>Firmante: {existingNote.signedByUserId}</span>}
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
      </div>
    </div>
  );
}
