import { useEffect, useState } from "react";
import { IconX, IconTrash, IconRefresh } from "@tabler/icons-react";
import { collaborationApi, getFriendlyError } from "@shared/api/api";
import { SHARE_SECTIONS, type TemporaryShareView } from "@modules/collaboration/types";

function formatDateTime(value?: string) {
  if (!value) return "—";
  return new Date(value).toLocaleString("es-MX", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
}

export function ManageTemporarySharesModal({
  clinicId,
  patientId,
  patientLabel,
  onClose
}: {
  clinicId: string;
  patientId: string;
  patientLabel: string;
  onClose: () => void;
}) {
  const [shares, setShares] = useState<TemporaryShareView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [revokingId, setRevokingId] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setError("");
    collaborationApi
      .listTemporaryShares(clinicId, patientId)
      .then((list) => setShares(list))
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId, patientId]);

  const revoke = async (shareId: string) => {
    if (!window.confirm("¿Revocar este enlace temporal? El especialista dejará de tener acceso de inmediato.")) {
      return;
    }
    setRevokingId(shareId);
    setError("");
    try {
      await collaborationApi.revokeTemporaryShare(clinicId, patientId, shareId);
      setShares((current) => current.filter((share) => share.id !== shareId));
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setRevokingId(null);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card modal-card-wide" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Enlaces temporales de expediente</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>

        <div className="modal-body" style={{ maxHeight: "70vh", overflowY: "auto", padding: "1.5rem" }}>
          <div className="access-log-summary">
            <p>
              Enlaces públicos activos para el expediente de: <strong>{patientLabel}</strong>
            </p>
            <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
              {!loading && shares.length > 0 && <span className="badge neutral">{shares.length} activos</span>}
              <button className="btn ghost" type="button" onClick={load} disabled={loading}>
                <IconRefresh size={16} aria-hidden="true" />
                Actualizar
              </button>
            </div>
          </div>

          {loading && <p>Cargando enlaces...</p>}

          {error && <p className="alert error">{error}</p>}

          {!loading && !error && shares.length === 0 && (
            <p style={{ textAlign: "center", opacity: 0.6, margin: "2rem 0" }}>
              No hay enlaces temporales activos para este expediente.
            </p>
          )}

          {!loading && shares.length > 0 && (
            <div className="table-wrapper">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Correo destinatario</th>
                    <th>Secciones</th>
                    <th>Creado</th>
                    <th>Expira</th>
                    <th>Último acceso</th>
                    <th style={{ textAlign: "center" }}>Accesos</th>
                    <th style={{ textAlign: "right" }}>Acción</th>
                  </tr>
                </thead>
                <tbody>
                  {shares.map((share) => {
                    const expired = new Date(share.expiresAt).getTime() < Date.now();
                    return (
                      <tr key={share.id}>
                        <td><strong>{share.email}</strong></td>
                        <td>
                          <span style={{ display: "flex", flexWrap: "wrap", gap: "4px" }}>
                            {(share.sections ?? []).map((key) => (
                              <span key={key} className="badge neutral" style={{ fontSize: "0.7rem" }}>
                                {SHARE_SECTIONS[key] ?? key}
                              </span>
                            ))}
                          </span>
                        </td>
                        <td><span style={{ fontSize: "0.85rem" }}>{formatDateTime(share.createdAt)}</span></td>
                        <td>
                          <span style={{ fontSize: "0.85rem" }}>{formatDateTime(share.expiresAt)}</span>
                          {expired && <span className="badge neutral" style={{ marginLeft: "0.5rem" }}>Expirado</span>}
                        </td>
                        <td><span style={{ fontSize: "0.85rem" }}>{formatDateTime(share.lastAccessedAt)}</span></td>
                        <td style={{ textAlign: "center" }}>{share.accessCount}</td>
                        <td style={{ textAlign: "right" }}>
                          <button
                            className="btn danger"
                            type="button"
                            disabled={revokingId === share.id}
                            onClick={() => void revoke(share.id)}
                          >
                            <IconTrash size={16} aria-hidden="true" />
                            {revokingId === share.id ? "Revocando..." : "Revocar"}
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
