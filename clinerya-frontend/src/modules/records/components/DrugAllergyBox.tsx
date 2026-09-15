import { useEffect, useState } from "react";
import { IconAlertHexagon } from "@tabler/icons-react";
import { clinicalSummaryApi, getFriendlyError } from "@shared/api/api";
import { ALLERGY_SEVERITY_LABELS, type PatientClinicalSummaryResponse } from "@modules/records/types";

/**
 * Recuadro pasivo de alergias a fármacos registradas. Solo lectura: lista lo que
 * el clínico capturó, sin cruzarlo con el medicamento recetado ni emitir avisos.
 */
export function DrugAllergyBox({ clinicId, patientId }: { clinicId: string; patientId: string }) {
  const [summary, setSummary] = useState<PatientClinicalSummaryResponse | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!clinicId || !patientId) return;
    let active = true;
    clinicalSummaryApi
      .get(patientId, clinicId)
      .then((data) => {
        if (active) setSummary(data);
      })
      .catch((caught) => {
        if (active) setError(getFriendlyError(caught));
      });
    return () => {
      active = false;
    };
  }, [clinicId, patientId]);

  const drugAllergies = summary?.allergies.filter((a) => a.category === "DRUG") ?? [];

  const provenance = (() => {
    if (drugAllergies.length === 0) return null;
    const latest = [...drugAllergies].sort(
      (a, b) => new Date(b.notedAt).getTime() - new Date(a.notedAt).getTime()
    )[0];
    const who = latest.notedByUserName ? ` por ${latest.notedByUserName}` : "";
    return `actualizado ${new Date(latest.notedAt).toLocaleDateString("es-MX")}${who}`;
  })();

  return (
    <div className="drug-allergy-box">
      <div className="drug-allergy-box-head">
        <IconAlertHexagon size={16} aria-hidden="true" />
        <strong>Alergias a fármacos registradas</strong>
        {provenance && <span className="drug-allergy-box-meta">· {provenance}</span>}
      </div>

      {error && <p className="description">No se pudieron cargar las alergias registradas.</p>}

      {!error && summary === null && <p className="description">Cargando alergias...</p>}

      {!error && summary !== null && drugAllergies.length === 0 && (
        <p className="description">
          {summary.allergiesReview.noneReported
            ? "Preguntadas y negadas: sin alergias a fármacos conocidas."
            : "Sin alergias a fármacos registradas."}
        </p>
      )}

      {!error && drugAllergies.length > 0 && (
        <ul className="drug-allergy-box-list">
          {drugAllergies.map((allergy) => (
            <li key={allergy.id}>
              <strong>{allergy.substance}</strong>
              {allergy.reaction ? ` — ${allergy.reaction}` : ""}
              <span className="drug-allergy-box-sev"> ({ALLERGY_SEVERITY_LABELS[allergy.severity]})</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
