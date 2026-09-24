import { IconX } from "@tabler/icons-react";
import type { QuotationResponse, VisitResponse } from "@modules/treatments/quotationTypes";
import { DEFAULT_CLINIC_PROFILE, type ClinicProfile } from "@shared/utils/clinicProfile";

interface VisitDetailsModalProps {
  quotation: QuotationResponse;
  visit: VisitResponse;
  profile?: ClinicProfile;
  onClose: () => void;
}

export function VisitDetailsModal({
  quotation,
  visit,
  profile = DEFAULT_CLINIC_PROFILE,
  onClose
}: VisitDetailsModalProps) {
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card modal-card-wide" onClick={(event) => event.stopPropagation()} style={{ maxWidth: '700px' }}>
        <div className="panel-heading" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2>Detalle de Sesión de Tratamiento</h2>
          <button type="button" className="icon-btn" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>

        <div className="panel-body" style={{ maxHeight: '65vh', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-4)' }}>
            <div>
              <h4 style={{ fontSize: '11px', color: 'var(--color-text-3)', textTransform: 'uppercase', margin: '0 0 4px 0' }}>Fecha de la sesión</h4>
              <p style={{ fontSize: '14px', fontWeight: 500, margin: 0 }}>
                {new Date(visit.visitDate + 'T00:00:00').toLocaleDateString()}
              </p>
            </div>
          </div>

          <div>
            <h4 style={{ fontSize: '11px', color: 'var(--color-text-3)', textTransform: 'uppercase', margin: '0 0 4px 0' }}>Notas clínicas de la sesión</h4>
            <p 
              style={{
                fontSize: '13px',
                margin: 0,
                padding: 'var(--space-3)',
                background: 'var(--color-bg-1)',
                borderRadius: '6px',
                whiteSpace: 'pre-wrap',
                fontStyle: visit.notes ? 'normal' : 'italic',
                color: visit.notes ? 'var(--color-text-1)' : 'var(--color-text-3)'
              }}
            >
              {visit.notes || "Sin observaciones adicionales."}
            </p>
          </div>

          <div>
            <h3 style={{ fontSize: '14px', marginBottom: 'var(--space-3)', borderBottom: '1px solid var(--color-border)', paddingBottom: '4px' }}>
              Tratamientos realizados y comparación de consumo
            </h3>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
              {visit.items.map((item) => {
                const qItem = quotation.items.find((qi) => qi.id === item.quotationItemId);

                return (
                  <div 
                    key={item.id} 
                    style={{
                      border: '1px solid var(--color-border)',
                      borderRadius: '6px',
                      padding: 'var(--space-3)',
                      backgroundColor: 'var(--color-surface)'
                    }}
                  >
                    <h4 style={{ fontWeight: 600, fontSize: '13px', margin: '0 0 10px 0' }}>
                      {qItem?.description || "Tratamiento"}{' '}
                      {profile.showsClinicalLocator && qItem?.toothNumber
                        ? `(${profile.locatorItemPrefix} ${qItem.toothNumber})`
                        : ''}
                    </h4>

                    <table className="data-table no-row-click" style={{ fontSize: '12px' }}>
                      <thead>
                        <tr>
                          <th>Material</th>
                          <th style={{ textAnchor: 'middle', textAlign: 'center' }}>Presupuestado</th>
                          <th style={{ textAnchor: 'middle', textAlign: 'center' }}>Consumido real</th>
                          <th style={{ textAnchor: 'middle', textAlign: 'center' }}>Desviación</th>
                        </tr>
                      </thead>
                      <tbody>
                        {item.materialsUsed.map((m) => {
                          const originalMat = qItem?.materials.find((om) => om.materialId === m.materialId);
                          const estimated = originalMat ? originalMat.estimatedQuantity : 0;
                          const actual = m.actualQuantity;
                          const diff = actual - estimated;

                          let diffStyle = { color: 'var(--color-text-1)' };
                          let diffText = String(diff);

                          if (diff > 0) {
                            diffStyle = { color: '#D64545' };
                            diffText = `+${diff.toFixed(2)}`;
                          } else if (diff < 0) {
                            diffStyle = { color: '#2E7D32' };
                            diffText = `${diff.toFixed(2)}`;
                          } else {
                            diffText = "0.00 (Ok)";
                          }

                          return (
                            <tr key={m.id}>
                              <td>{m.materialName}</td>
                              <td style={{ textAlign: 'center' }}>{originalMat ? originalMat.estimatedQuantity.toFixed(2) : "0.00"}</td>
                              <td style={{ textAlign: 'center', fontWeight: 600 }}>{actual.toFixed(2)}</td>
                              <td style={{ textAlign: 'center', fontWeight: 600, ...diffStyle }}>
                                {diffText}
                              </td>
                            </tr>
                          );
                        })}
                        {item.materialsUsed.length === 0 && (
                          <tr>
                            <td colSpan={4} style={{ textAlign: 'center', padding: 'var(--space-2)' }}>
                              No se registraron materiales para este tratamiento en esta sesión.
                            </td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        <div className="form-actions" style={{ display: 'flex', justifyContent: 'flex-end', padding: 'var(--space-4)' }}>
          <button type="button" className="btn primary" onClick={onClose}>
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}
