import { useEffect, useState } from "react";
import { IconChevronDown, IconX } from "@tabler/icons-react";
import { getFriendlyError, recordAccessLogsApi } from "@shared/api/api";
import type { RecordAccessLogResponse } from "@modules/records/types";

const PAGE_SIZE = 50;

export function RecordAccessLogsModal({
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
  const [logs, setLogs] = useState<RecordAccessLogResponse[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    setLogs([]);
    setNextCursor(null);
    setHasMore(false);
    recordAccessLogsApi
      .listByPatient(patientId, clinicId, { limit: PAGE_SIZE })
      .then((page) => {
        if (!active) return;
        setLogs(page.items);
        setNextCursor(page.nextCursor);
        setHasMore(page.hasMore);
      })
      .catch((caught) => {
        if (active) setError(getFriendlyError(caught));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [patientId, clinicId]);

  const loadMore = async () => {
    if (!nextCursor || loadingMore) return;
    setLoadingMore(true);
    setError("");
    try {
      const page = await recordAccessLogsApi.listByPatient(patientId, clinicId, {
        cursor: nextCursor,
        limit: PAGE_SIZE
      });
      setLogs((current) => [...current, ...page.items]);
      setNextCursor(page.nextCursor);
      setHasMore(page.hasMore);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoadingMore(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card modal-card-wide" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Bitácora de Accesos y Modificaciones</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>

        <div className="modal-body" style={{ maxHeight: "70vh", overflowY: "auto", padding: "1.5rem" }}>
          <div className="access-log-summary">
            <p>
              Historial de auditoría para el expediente de: <strong>{patientLabel}</strong>
            </p>
            {!loading && logs.length > 0 && <span className="badge neutral">{logs.length} registros</span>}
          </div>

          {loading && <p>Cargando bitácora...</p>}

          {error && <p className="alert error">{error}</p>}

          {!loading && !error && logs.length === 0 && (
            <p style={{ textAlign: "center", opacity: 0.6, margin: "2rem 0" }}>
              No se han registrado accesos ni modificaciones para este expediente.
            </p>
          )}

          {!loading && logs.length > 0 && (
            <>
              <div className="table-wrapper">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Usuario</th>
                      <th>Acción</th>
                      <th>Recurso</th>
                      <th>Fecha y Hora</th>
                      <th>Dirección IP</th>
                      <th>Navegador / Sistema</th>
                    </tr>
                  </thead>
                  <tbody>
                    {logs.map((log) => (
                      <tr key={log.id}>
                        <td><strong>{log.userName}</strong></td>
                        <td>
                          <span className={`badge ${log.actionType === "WRITE" ? "success" : "neutral"}`}>
                            {log.actionType === "WRITE" ? "Modificación" : "Lectura"}
                          </span>
                        </td>
                        <td>
                          <code style={{ fontSize: "0.85rem" }}>
                            {log.resourceType === "MEDICAL_HISTORY" ? "Historia Clínica" : "Nota Clínica"}
                          </code>
                        </td>
                        <td><span style={{ fontSize: "0.85rem" }}>{new Date(log.createdAt).toLocaleString()}</span></td>
                        <td><span style={{ fontSize: "0.85rem", fontFamily: "monospace" }}>{log.ipAddress || "Desconocida"}</span></td>
                        <td className="access-log-user-agent">
                          <span title={log.userAgent ?? ""}>{log.userAgent || "Desconocido"}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {hasMore && (
                <div className="access-log-load-more">
                  <button className="btn secondary" type="button" disabled={loadingMore} onClick={() => void loadMore()}>
                    <IconChevronDown size={17} aria-hidden="true" />
                    {loadingMore ? "Cargando..." : "Cargar más"}
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
