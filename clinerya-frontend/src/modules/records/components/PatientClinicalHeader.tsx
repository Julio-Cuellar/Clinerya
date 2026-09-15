import { useCallback, useEffect, useState } from "react";
import { IconDroplet, IconEdit, IconAlertTriangle, IconCircleCheck } from "@tabler/icons-react";
import { clinicalSummaryApi, getFriendlyError } from "@shared/api/api";
import { BLOOD_TYPE_LABELS, type PatientClinicalSummaryResponse } from "@modules/records/types";
import { ClinicalDataEditor } from "@modules/records/components/ClinicalDataEditor";

/**
 * Banda de datos clínicos críticos del paciente (alergias · crónicos ·
 * medicación activa · tipo de sangre · última consulta). Display de lo que
 * está registrado; el botón "Gestionar" abre la captura.
 */
export function PatientClinicalHeader({
  clinicId,
  patientId,
  canEdit = true
}: {
  clinicId: string;
  patientId: string;
  canEdit?: boolean;
}) {
  const [summary, setSummary] = useState<PatientClinicalSummaryResponse | null>(null);
  const [error, setError] = useState("");
  const [editing, setEditing] = useState(false);

  const load = useCallback(() => {
    if (!clinicId || !patientId) return;
    clinicalSummaryApi
      .get(patientId, clinicId)
      .then(setSummary)
      .catch((caught) => setError(getFriendlyError(caught)));
  }, [clinicId, patientId]);

  useEffect(() => {
    load();
  }, [load]);

  if (error) {
    return <div className="clinical-header clinical-header--message">No se pudo cargar el resumen clínico.</div>;
  }

  if (!summary) {
    return <div className="clinical-header clinical-header--message">Cargando resumen clínico...</div>;
  }

  const allergyList = summary.allergies.map((a) => a.substance).join(", ");
  const conditionList = summary.conditions
    .filter((c) => c.status === "ACTIVE")
    .map((c) => c.name)
    .join(", ");
  const medicationList = summary.activeMedications.map((m) => m.medicationName).join(", ");
  const bloodType = summary.bloodType ? BLOOD_TYPE_LABELS[summary.bloodType] ?? summary.bloodType : null;
  const lastNote = summary.lastNoteAt ? new Date(summary.lastNoteAt).toLocaleDateString("es-MX") : null;

  const resolve = (list: string, review: { noneReported: boolean }, deniedText: string) =>
    list ? list : review.noneReported ? deniedText : "Sin registrar";

  const allergyHasList = summary.allergies.length > 0;

  return (
    <>
      <div className="clinical-header">
        <div className={`clinical-header-cell ${allergyHasList ? "is-alert" : ""}`}>
          <span className="clinical-header-label">
            {allergyHasList ? <IconAlertTriangle size={13} aria-hidden="true" /> : null}
            {!allergyHasList && summary.allergiesReview.noneReported ? (
              <IconCircleCheck size={13} aria-hidden="true" />
            ) : null}
            Alergias
          </span>
          <span className="clinical-header-value">
            {resolve(allergyList, summary.allergiesReview, "Preguntadas y negadas")}
          </span>
        </div>

        <div className="clinical-header-cell">
          <span className="clinical-header-label">Crónicos</span>
          <span className="clinical-header-value">
            {resolve(conditionList, summary.conditionsReview, "Sin padecimientos reportados")}
          </span>
        </div>

        <div className="clinical-header-cell">
          <span className="clinical-header-label">Medicación activa</span>
          <span className="clinical-header-value">
            {resolve(medicationList, summary.medicationsReview, "Sin medicación activa")}
          </span>
        </div>

        <div className="clinical-header-cell">
          <span className="clinical-header-label">
            <IconDroplet size={13} aria-hidden="true" />
            Sangre
          </span>
          <span className="clinical-header-value">{bloodType || "Sin registrar"}</span>
        </div>

        <div className="clinical-header-cell">
          <span className="clinical-header-label">Última consulta</span>
          <span className="clinical-header-value">{lastNote || "Sin notas"}</span>
        </div>

        {canEdit && (
          <button className="btn ghost clinical-header-manage" type="button" onClick={() => setEditing(true)}>
            <IconEdit size={15} aria-hidden="true" />
            Gestionar
          </button>
        )}
      </div>

      {editing && (
        <ClinicalDataEditor
          clinicId={clinicId}
          patientId={patientId}
          summary={summary}
          onClose={() => setEditing(false)}
          onChanged={load}
        />
      )}
    </>
  );
}
