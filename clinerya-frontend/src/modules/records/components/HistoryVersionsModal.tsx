import { useState } from "react";
import { IconX, IconGitCompare, IconEye } from "@tabler/icons-react";
import { medicalHistoryApi } from "@shared/api/api";
import { parseAnswers, type MedicalHistoryVersionResponse } from "@modules/records/types";

export function HistoryVersionsModal({
  versions,
  patientId,
  templateId,
  clinicId,
  onClose,
  onViewVersion,
  onCompareVersions
}: {
  versions: MedicalHistoryVersionResponse[];
  patientId: string;
  templateId: string;
  clinicId: string;
  onClose: () => void;
  onViewVersion: (answers: Record<string, string>, versionNumber: number) => void;
  onCompareVersions: (answersA: Record<string, string>, answersB: Record<string, string>, vA: number, vB: number) => void;
}) {
  const [selectedV1, setSelectedV1] = useState<number | "">("");
  const [selectedV2, setSelectedV2] = useState<number | "">("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleCompare = async () => {
    if (selectedV1 === "" || selectedV2 === "") {
      setError("Por favor, selecciona dos versiones para comparar.");
      return;
    }
    if (selectedV1 === selectedV2) {
      setError("Debes seleccionar dos versiones diferentes.");
      return;
    }

    setLoading(true);
    setError("");
    try {
      // Ordenar las versiones de menor a mayor
      const vMin = Math.min(Number(selectedV1), Number(selectedV2));
      const vMax = Math.max(Number(selectedV1), Number(selectedV2));

      const [resMin, resMax] = await Promise.all([
        medicalHistoryApi.getVersion(patientId, templateId, vMin, clinicId),
        medicalHistoryApi.getVersion(patientId, templateId, vMax, clinicId)
      ]);

      const answersA = parseAnswers(resMin.answersJson);
      const answersB = parseAnswers(resMax.answersJson);

      onCompareVersions(answersA, answersB, vMin, vMax);
      onClose();
    } catch {
      setError("Ocurrió un error al cargar las versiones para comparar.");
    } finally {
      setLoading(false);
    }
  };

  const handleView = async (versionNumber: number) => {
    setLoading(true);
    setError("");
    try {
      const res = await medicalHistoryApi.getVersion(patientId, templateId, versionNumber, clinicId);
      onViewVersion(parseAnswers(res.answersJson), versionNumber);
      onClose();
    } catch {
      setError("Ocurrió un error al cargar la versión seleccionada.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()} style={{ maxWidth: "550px" }}>
        <div className="panel-heading">
          <h2>Historial de Cambios (Versiones)</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>

        <div className="modal-body" style={{ padding: "1.5rem" }}>
          {error && <p className="alert error">{error}</p>}
          {loading && <p>Cargando datos de versión...</p>}

          <div style={{ marginBottom: "1.5rem", borderBottom: "1px solid var(--color-border)", paddingBottom: "1.5rem" }}>
            <h4 style={{ marginBottom: "0.75rem", fontSize: "0.95rem" }}>Comparar versiones</h4>
            <div style={{ display: "flex", gap: "1rem", alignItems: "center", marginBottom: "1rem" }}>
              <label style={{ display: "flex", flexDirection: "column", gap: "0.25rem", flex: 1 }}>
                <span style={{ fontSize: "0.8rem", opacity: 0.8 }}>Versión Base (Anterior)</span>
                <select value={selectedV1} onChange={(event) => setSelectedV1(event.target.value ? Number(event.target.value) : "")}>
                  <option value="">Selecciona...</option>
                  {versions.map((v) => (
                    <option key={v.version} value={v.version}>
                      Versión {v.version} ({v.changedByUserName})
                    </option>
                  ))}
                </select>
              </label>

              <label style={{ display: "flex", flexDirection: "column", gap: "0.25rem", flex: 1 }}>
                <span style={{ fontSize: "0.8rem", opacity: 0.8 }}>Versión Comparada (Posterior)</span>
                <select value={selectedV2} onChange={(event) => setSelectedV2(event.target.value ? Number(event.target.value) : "")}>
                  <option value="">Selecciona...</option>
                  {versions.map((v) => (
                    <option key={v.version} value={v.version}>
                      Versión {v.version} ({v.changedByUserName})
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <button className="btn primary" type="button" disabled={loading} onClick={handleCompare} style={{ width: "100%" }}>
              <IconGitCompare size={16} style={{ marginRight: "0.5rem" }} />
              Comparar Cambios
            </button>
          </div>

          <div>
            <h4 style={{ marginBottom: "0.75rem", fontSize: "0.95rem" }}>Historial Completo</h4>
            <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem", maxHeight: "250px", overflowY: "auto" }}>
              {versions.map((v) => (
                <div
                  key={v.id}
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    padding: "0.75rem",
                    borderRadius: "6px",
                    backgroundColor: "var(--color-bg-light, rgba(0,0,0,0.02))",
                    border: "1px solid var(--color-border)"
                  }}
                >
                  <div style={{ display: "flex", flexDirection: "column", gap: "0.15rem" }}>
                    <span style={{ fontWeight: 600, fontSize: "0.9rem" }}>Versión {v.version}</span>
                    <span style={{ fontSize: "0.8rem", opacity: 0.8 }}>
                      Por: <strong>{v.changedByUserName}</strong>
                    </span>
                    <span style={{ fontSize: "0.75rem", opacity: 0.6 }}>
                      {new Date(v.createdAt).toLocaleString()} | IP: {v.ipAddress || "Local"}
                    </span>
                  </div>
                  <button className="btn secondary" type="button" disabled={loading} onClick={() => handleView(v.version)} style={{ padding: "0.25rem 0.5rem", fontSize: "0.8rem" }}>
                    <IconEye size={14} style={{ marginRight: "0.25rem" }} />
                    Ver
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
