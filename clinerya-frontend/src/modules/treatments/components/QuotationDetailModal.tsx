import { useEffect, useState } from "react";
import { IconFileTypePdf, IconPencil, IconX } from "@tabler/icons-react";
import { getFriendlyError, quotationsApi, visitsApi } from "@shared/api/api";
import { clinicToHeaderInfo, exportQuotationToPdf } from "@modules/treatments/lib/quotationPdfExport";
import {
  ITEM_PROGRESS_STATUS_LABELS,
  QUOTATION_STATUS_BADGES,
  QUOTATION_STATUS_LABELS,
  type ItemProgressStatus,
  type QuotationResponse,
  type QuotationStatus,
  type VisitResponse
} from "@modules/treatments/quotationTypes";
import type { ClinicResponse } from "@modules/clinics/types";
import type { PatientResponse } from "@modules/patients/types";
import { getClinicProfileFor } from "@shared/utils/clinicProfile";
import { RegisterVisitModal } from "@modules/treatments/components/RegisterVisitModal";
import { VisitDetailsModal } from "@modules/treatments/components/VisitDetailsModal";

const currencyFormatter = new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" });
const numberFormatter = new Intl.NumberFormat("es-MX", { maximumFractionDigits: 4 });

export function QuotationDetailModal({
  patientId,
  clinicId,
  patient,
  clinic,
  quotation,
  onClose,
  onChanged,
  onEdit
}: {
  patientId: string;
  clinicId: string;
  patient: PatientResponse;
  clinic?: ClinicResponse;
  quotation: QuotationResponse;
  onClose: () => void;
  onChanged: (updated: QuotationResponse) => void;
  onEdit: () => void;
}) {
  const profile = getClinicProfileFor(clinic);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [visits, setVisits] = useState<VisitResponse[]>([]);
  const [loadingVisits, setLoadingVisits] = useState(false);
  const [showRegisterVisit, setShowRegisterVisit] = useState(false);
  const [selectedVisit, setSelectedVisit] = useState<VisitResponse | null>(null);

  useEffect(() => {
    if (quotation.status === "ACCEPTED") {
      setLoadingVisits(true);
      visitsApi.listByQuotation(patientId, quotation.id, clinicId)
        .then(setVisits)
        .catch(console.error)
        .finally(() => setLoadingVisits(false));
    }
  }, [patientId, quotation.id, clinicId, quotation.status]);

  const transition = async (targetStatus: QuotationStatus) => {
    setBusy(true);
    setError("");
    try {
      const updated = await quotationsApi.transitionStatus(patientId, quotation.id, clinicId, targetStatus);
      onChanged(updated);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy(false);
    }
  };

  const changeItemProgress = async (itemId: string, progressStatus: ItemProgressStatus) => {
    setBusy(true);
    setError("");
    try {
      const updated = await quotationsApi.updateItemProgress(patientId, quotation.id, itemId, clinicId, progressStatus);
      onChanged(updated);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy(false);
    }
  };

  const handleExport = async () => {
    setBusy(true);
    setError("");
    try {
      await exportQuotationToPdf({
        quotation,
        patient,
        clinicInfo: clinic ? clinicToHeaderInfo(clinic) : undefined,
        profile
      });
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card modal-card-wide" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Cotización — {quotation.quotationDate}</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>

        <div className="modal-scroll-body">
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Estado</strong>
            <span className={`badge ${QUOTATION_STATUS_BADGES[quotation.status]}`}>
              {QUOTATION_STATUS_LABELS[quotation.status]}
            </span>
          </div>
          {quotation.validUntil && (
            <div className="clinic-row">
              <strong>Vigente hasta</strong>
              <span>{quotation.validUntil}</span>
            </div>
          )}
          {quotation.notes && (
            <div className="clinic-row">
              <strong>Notas</strong>
              <span>{quotation.notes}</span>
            </div>
          )}
        </div>

        <div className="table-wrapper">
          <table className="data-table no-row-click">
            <thead>
              <tr>
                <th>Descripción</th>
                {profile.showsClinicalLocator && <th>{profile.locatorColumnLabel}</th>}
                <th>{profile.laborLabel}</th>
                <th>Materiales</th>
                <th>Desc. %</th>
                <th>Progreso</th>
                <th>Subtotal</th>
              </tr>
            </thead>
            <tbody>
              {quotation.items.map((item) => (
                <tr key={item.id}>
                  <td>{item.description}</td>
                  {profile.showsClinicalLocator && <td>{item.toothNumber ?? "—"}</td>}
                  <td>{currencyFormatter.format(item.laborCharge)}</td>
                  <td>
                    {item.materials.length === 0 ? (
                      "—"
                    ) : (
                      <>
                        <strong>{currencyFormatter.format(item.materialsTotal)}</strong>
                        <ul className="quotation-materials-summary">
                          {item.materials.map((material) => (
                            <li key={material.id}>
                              {material.materialName} · {numberFormatter.format(material.estimatedQuantity)} ×{" "}
                              {currencyFormatter.format(material.unitCostAtQuote)}
                            </li>
                          ))}
                        </ul>
                      </>
                    )}
                  </td>
                  <td>{item.discountPercentage ?? 0}%</td>
                  <td>
                    <select
                      value={item.progressStatus}
                      disabled={busy}
                      onChange={(event) => changeItemProgress(item.id, event.target.value as ItemProgressStatus)}
                    >
                      {Object.entries(ITEM_PROGRESS_STATUS_LABELS).map(([value, label]) => (
                        <option key={value} value={value}>
                          {label}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>{currencyFormatter.format(item.subtotal)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td colSpan={6} style={{ textAlign: "right" }}>
                  <strong>Total general</strong>
                </td>
                <td>
                  <strong>{currencyFormatter.format(quotation.grandTotal)}</strong>
                </td>
              </tr>
            </tfoot>
          </table>
        </div>

        {quotation.status === "ACCEPTED" && (
          <div style={{ marginTop: 'var(--space-5)', borderTop: '1px solid var(--color-border)', paddingTop: 'var(--space-4)', paddingLeft: 'var(--space-5)', paddingRight: 'var(--space-5)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-3)' }}>
              <h3 style={{ fontSize: '15px', fontWeight: 600 }}>Historial de Sesiones de Tratamiento</h3>
              <button 
                type="button" 
                className="btn primary" 
                onClick={() => setShowRegisterVisit(true)}
                disabled={busy}
                style={{ padding: '6px 12px', fontSize: '13px' }}
              >
                Registrar Sesión
              </button>
            </div>

            {loadingVisits ? (
              <p style={{ fontSize: '13px', color: 'var(--color-text-3)' }}>Cargando sesiones...</p>
            ) : visits.length === 0 ? (
              <p style={{ fontSize: '13px', color: 'var(--color-text-3)', fontStyle: 'italic' }}>
                No se han registrado sesiones de tratamiento para esta cotización aún.
              </p>
            ) : (
              <div className="table-wrapper" style={{ maxHeight: '200px', overflowY: 'auto' }}>
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Fecha de Sesión</th>
                      <th>Notas / Observaciones</th>
                      <th style={{ width: '120px', textAlign: 'center' }}>Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {visits.map((visit) => (
                      <tr key={visit.id} onClick={() => setSelectedVisit(visit)} style={{ cursor: 'pointer' }}>
                        <td>{new Date(visit.visitDate + 'T00:00:00').toLocaleDateString()}</td>
                        <td style={{ maxWidth: '400px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {visit.notes || "—"}
                        </td>
                        <td style={{ textAlign: 'center' }}>
                          <button 
                            type="button" 
                            className="btn ghost" 
                            onClick={(e) => {
                              e.stopPropagation();
                              setSelectedVisit(visit);
                            }}
                            style={{ padding: '4px 8px', fontSize: '12px' }}
                          >
                            Ver detalle
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {error && <p className="alert error" style={{ margin: 'var(--space-4)' }}>{error}</p>}

        <div className="form-actions">
          {quotation.status === "DRAFT" && (
            <>
              <button className="btn ghost" type="button" disabled={busy} onClick={onEdit}>
                <IconPencil size={16} aria-hidden="true" />
                Editar
              </button>
              <button className="btn secondary" type="button" disabled={busy} onClick={() => transition("SENT")}>
                Enviar
              </button>
            </>
          )}
          {quotation.status === "SENT" && (
            <>
              <button className="btn secondary" type="button" disabled={busy} onClick={() => transition("REJECTED")}>
                Marcar rechazada
              </button>
              <button className="btn secondary" type="button" disabled={busy} onClick={() => transition("EXPIRED")}>
                Marcar vencida
              </button>
              <button className="btn primary" type="button" disabled={busy} onClick={() => transition("ACCEPTED")}>
                Marcar aceptada
              </button>
            </>
          )}
          <button className="btn primary" type="button" disabled={busy} onClick={handleExport}>
            <IconFileTypePdf size={16} aria-hidden="true" />
            Exportar PDF
          </button>
        </div>
        </div>
      </div>

      {showRegisterVisit && (
        <RegisterVisitModal
          patientId={patientId}
          clinicId={clinicId}
          quotation={quotation}
          profile={profile}
          onClose={() => setShowRegisterVisit(false)}
          onSaved={() => {
            setShowRegisterVisit(false);
            setLoadingVisits(true);
            visitsApi.listByQuotation(patientId, quotation.id, clinicId)
              .then(setVisits)
              .catch(console.error)
              .finally(() => setLoadingVisits(false));
          }}
        />
      )}

      {selectedVisit && (
        <VisitDetailsModal
          quotation={quotation}
          visit={selectedVisit}
          profile={profile}
          onClose={() => setSelectedVisit(null)}
        />
      )}
    </div>
  );
}
