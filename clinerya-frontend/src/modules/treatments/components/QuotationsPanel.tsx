import { useEffect, useState } from "react";
import { IconArrowLeft, IconPlus } from "@tabler/icons-react";
import { getFriendlyError, quotationsApi, treatmentCatalogApi } from "@shared/api/api";
import {
  QUOTATION_STATUS_BADGES,
  QUOTATION_STATUS_LABELS,
  type QuotationResponse
} from "@modules/treatments/quotationTypes";
import type { TreatmentCatalogItemResponse } from "@modules/treatments/types";
import type { ClinicResponse } from "@modules/clinics/types";
import { getClinicProfileFor } from "@shared/utils/clinicProfile";
import type { PatientResponse } from "@modules/patients/types";
import { QuotationEditorScreen } from "@modules/treatments/components/QuotationEditorScreen";
import { QuotationDetailModal } from "@modules/treatments/components/QuotationDetailModal";

const currencyFormatter = new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" });

export function QuotationsPanel({
  clinicId,
  clinic,
  patient,
  onChangePatient
}: {
  clinicId: string;
  clinic?: ClinicResponse;
  patient: PatientResponse;
  onChangePatient: () => void;
}) {
  const [quotations, setQuotations] = useState<QuotationResponse[]>([]);
  const [catalogItems, setCatalogItems] = useState<TreatmentCatalogItemResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [creating, setCreating] = useState(false);
  const [viewing, setViewing] = useState<QuotationResponse | null>(null);
  const [editing, setEditing] = useState<QuotationResponse | null>(null);

  const load = () => {
    setLoading(true);
    setError("");
    Promise.all([quotationsApi.listByPatient(patient.id, clinicId), treatmentCatalogApi.list(clinicId)])
      .then(([quotationList, catalogList]) => {
        setQuotations(quotationList);
        setCatalogItems(catalogList.filter((item) => item.active));
      })
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId, patient.id]);

  if (creating || editing) {
    return (
      <QuotationEditorScreen
        patientId={patient.id}
        clinicId={clinicId}
        patient={patient}
        quotation={editing ?? undefined}
        catalogItems={catalogItems}
        profile={getClinicProfileFor(clinic)}
        onClose={() => {
          setCreating(false);
          setEditing(null);
        }}
        onSaved={() => {
          setCreating(false);
          setEditing(null);
          load();
        }}
      />
    );
  }

  return (
    <article className="panel full">
      <div className="panel-heading">
        <div>
          <h2>
            {patient.firstName} {patient.lastNamePaterno} {patient.lastNameMaterno}
          </h2>
          <span>Cotizaciones de tratamiento</span>
        </div>
        <div className="topbar-actions">
          <button className="btn primary" type="button" onClick={() => setCreating(true)}>
            <IconPlus size={16} aria-hidden="true" />
            Nueva cotización
          </button>
          <button className="btn secondary" type="button" onClick={onChangePatient}>
            <IconArrowLeft size={16} aria-hidden="true" />
            Cambiar paciente
          </button>
        </div>
      </div>

      {loading && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Cargando cotizaciones...</strong>
          </div>
        </div>
      )}

      {!loading && quotations.length === 0 && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Sin cotizaciones registradas</strong>
            <span>Crea la primera cotización de tratamiento para este paciente</span>
          </div>
        </div>
      )}

      {!loading && quotations.length > 0 && (
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Estado</th>
                <th>Total</th>
                <th aria-label="Acciones" />
              </tr>
            </thead>
            <tbody>
              {quotations.map((quotation) => {
                const statusBadge = QUOTATION_STATUS_BADGES[quotation.status];
                return (
                  <tr key={quotation.id} onClick={() => setViewing(quotation)}>
                    <td>{quotation.quotationDate}</td>
                    <td>
                      <span className={`badge ${statusBadge}`}>{QUOTATION_STATUS_LABELS[quotation.status]}</span>
                    </td>
                    <td>{currencyFormatter.format(quotation.grandTotal)}</td>
                    <td className="table-actions">
                      {quotation.status === "DRAFT" && (
                        <button
                          className="btn ghost"
                          type="button"
                          onClick={(event) => {
                            event.stopPropagation();
                            setEditing(quotation);
                          }}
                        >
                          Editar
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {error && <p className="alert error">{error}</p>}

      {viewing && (
        <QuotationDetailModal
          patientId={patient.id}
          clinicId={clinicId}
          patient={patient}
          clinic={clinic}
          quotation={viewing}
          onClose={() => setViewing(null)}
          onChanged={(updated) => {
            setQuotations((prev) => prev.map((item) => (item.id === updated.id ? updated : item)));
            setViewing(updated);
          }}
          onEdit={() => {
            setEditing(viewing);
            setViewing(null);
          }}
        />
      )}
    </article>
  );
}
