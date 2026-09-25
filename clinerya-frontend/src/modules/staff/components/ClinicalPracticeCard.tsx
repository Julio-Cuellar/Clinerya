import { FormEvent, useEffect, useState } from "react";
import { IconCheck, IconStethoscope } from "@tabler/icons-react";
import { getFriendlyError, staffApi, type ClinicalPracticeResponse, type ClinicStaffResponse } from "@shared/api/api";

const credentialStatusLabels: Record<string, string> = {
  EN_TRAMITE: "En trámite",
  ACTIVO: "Verificada",
  SUSPENDIDO: "Suspendida"
};

/**
 * Tarjeta "Práctica clínica" de un administrador: decide si además atiende pacientes, que es lo
 * único que le da agenda propia, consultorio y acceso a expedientes. Activarlo pide su cédula.
 */
export function ClinicalPracticeCard({
  clinicId,
  employee,
  canManage,
  onSaved
}: {
  clinicId: string;
  employee: ClinicStaffResponse;
  canManage: boolean;
  onSaved: (practice: ClinicalPracticeResponse) => void;
}) {
  const [practice, setPractice] = useState<ClinicalPracticeResponse | null>(null);
  const [attendsPatients, setAttendsPatients] = useState(false);
  const [cedula, setCedula] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [status, setStatus] = useState("");

  useEffect(() => {
    let cancelled = false;
    staffApi
      .getClinicalPractice(clinicId, employee.staffId)
      .then((loaded) => {
        if (cancelled) return;
        setPractice(loaded);
        setAttendsPatients(loaded.attendsPatients);
        setCedula(loaded.cedulaProfesional ?? "");
      })
      .catch((caught) => {
        if (!cancelled) setError(getFriendlyError(caught));
      });
    return () => {
      cancelled = true;
    };
  }, [clinicId, employee.staffId]);

  const save = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    setStatus("");
    try {
      const saved = await staffApi.updateClinicalPractice(clinicId, employee.staffId, {
        attendsPatients,
        cedulaProfesional: attendsPatients ? cedula.trim() : undefined
      });
      setPractice(saved);
      setCedula(saved.cedulaProfesional ?? "");
      setStatus(saved.attendsPatients ? "Ahora atiende pacientes." : "Ya no atiende pacientes.");
      onSaved(saved);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  const changed = practice !== null
    && (attendsPatients !== practice.attendsPatients || (attendsPatients && cedula.trim() !== (practice.cedulaProfesional ?? "")));

  return (
    <article className="panel staff-detail-card">
      <div className="panel-heading">
        <div>
          <h3>Práctica clínica</h3>
          <span className="panel-subtitle">Decide si además cuenta como doctor</span>
        </div>
        {practice?.attendsPatients ? (
          <span className="badge success"><IconCheck size={13} aria-hidden="true" /> Atiende pacientes</span>
        ) : (
          <IconStethoscope size={20} aria-hidden="true" />
        )}
      </div>

      {!practice && !error && <p className="description">Cargando práctica clínica...</p>}
      {practice && (
        <form className="clinical-practice-form" onSubmit={save}>
          <label className="clinical-practice-toggle">
            <span>
              <strong>Atiende pacientes</strong>
              <small>Agenda, consultorios, expediente y presupuestos</small>
            </span>
            <input
              type="checkbox"
              checked={attendsPatients}
              disabled={!canManage || saving}
              onChange={(event) => setAttendsPatients(event.target.checked)}
            />
          </label>

          {attendsPatients ? (
            <>
              <label className="field">
                <span>Cédula profesional <span className="required-mark">*</span></span>
                <input
                  value={cedula}
                  onChange={(event) => setCedula(event.target.value)}
                  readOnly={!canManage}
                  placeholder="Ej. 12345678"
                  inputMode="numeric"
                  pattern="\d{5,8}"
                  title="Solo números, entre 5 y 8 dígitos"
                  required
                />
              </label>
              {practice.credentialStatus && (
                <p className="clinical-practice-status">
                  Estado de la credencial:{" "}
                  <span className={`badge ${practice.credentialStatus === "ACTIVO" ? "success" : "warning"}`}>
                    {credentialStatusLabels[practice.credentialStatus] || practice.credentialStatus}
                  </span>
                </p>
              )}
            </>
          ) : (
            <p className="description">
              Sin acceso a expedientes, notas, recetas, presupuestos ni visitas. Conserva el catálogo de tratamientos,
              caja, inventario, contabilidad y personal.
            </p>
          )}

          {error && <p className="alert error">{error}</p>}
          {status && <p className="alert success">{status}</p>}
          {canManage && (
            <div className="form-actions">
              <button className="btn primary" type="submit" disabled={saving || !changed}>
                {saving ? "Guardando..." : "Guardar"}
              </button>
            </div>
          )}
        </form>
      )}
      {!practice && error && <p className="alert error">{error}</p>}
    </article>
  );
}
