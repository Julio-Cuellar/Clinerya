import { useEffect, useState } from "react";
import { IconX } from "@tabler/icons-react";
import { getFriendlyError, medicalHistoryApi } from "@shared/api/api";
import { parseSchema, type HistoryTemplateResponse, type MedicalHistoryResponse } from "@modules/records/types";
import type { PatientResponse } from "@modules/patients/types";

export function HistoryTemplatePickerModal({
  clinicId,
  patient,
  templates,
  onClose,
  onSelect
}: {
  clinicId: string;
  patient: PatientResponse;
  templates: HistoryTemplateResponse[];
  onClose: () => void;
  onSelect: (template: HistoryTemplateResponse) => void;
}) {
  const [histories, setHistories] = useState<MedicalHistoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const historiesByTemplateId = new Map(histories.map((history) => [history.templateId, history]));
  const currentTemplates = templates
    .filter((template) => historiesByTemplateId.has(template.id))
    .sort((a, b) => {
      const historyA = historiesByTemplateId.get(a.id);
      const historyB = historiesByTemplateId.get(b.id);
      return new Date(historyB?.updatedAt ?? b.updatedAt).getTime() - new Date(historyA?.updatedAt ?? a.updatedAt).getTime();
    });
  const newTemplates = templates.filter((template) => !historiesByTemplateId.has(template.id));

  useEffect(() => {
    setLoading(true);
    setError("");
    medicalHistoryApi
      .listByPatient(patient.id, clinicId)
      .then(setHistories)
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  }, [clinicId, patient.id]);

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Historia clínica</h2>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>
        <p className="confirm-summary-subtitle">
          {patient.firstName} {patient.lastNamePaterno} {patient.lastNameMaterno}
        </p>

        {loading && (
          <div className="clinic-list">
            <div className="clinic-row">
              <strong>Cargando plantillas...</strong>
            </div>
          </div>
        )}

        {!loading && (
          <div className="history-template-sections">
            <section className="history-template-section">
              <div className="history-template-section-heading">
                <h3>Historias clínicas actuales</h3>
                <span className="badge neutral">{currentTemplates.length}</span>
              </div>
              {currentTemplates.length > 0 ? (
                <div className="clinic-list">
                  {currentTemplates.map((template) => {
                    const isHistoriaClinica = parseSchema(template.schemaJson).kind === "historia_clinica";
                    const history = historiesByTemplateId.get(template.id);
                    return (
                      <div className="clinic-row clinic-row-actionable" key={template.id} onClick={() => onSelect(template)}>
                        <div>
                          <strong>{template.name}</strong>
                          <span>{history ? `Última actualización: ${new Date(history.updatedAt).toLocaleString()}` : isHistoriaClinica ? "Historia clínica" : "Historia clínica personalizada"}</span>
                        </div>
                        <div className="clinic-row-actions">
                          <span className="badge success">Iniciada</span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div className="clinic-row">
                  <strong>Sin historias iniciadas</strong>
                  <span>Este paciente aún no tiene una historia clínica creada.</span>
                </div>
              )}
            </section>

            <section className="history-template-section">
              <div className="history-template-section-heading">
                <h3>Nueva historia clínica</h3>
                <span className="badge neutral">{newTemplates.length}</span>
              </div>
              {newTemplates.length > 0 ? (
                <div className="clinic-list">
                  {newTemplates.map((template) => {
                    const isHistoriaClinica = parseSchema(template.schemaJson).kind === "historia_clinica";
                    return (
                      <div className="clinic-row clinic-row-actionable" key={template.id} onClick={() => onSelect(template)}>
                        <div>
                          <strong>{template.name}</strong>
                          <span>{template.description || (isHistoriaClinica ? "Plantilla de historia clínica" : "Plantilla personalizada")}</span>
                        </div>
                        <div className="clinic-row-actions">
                          <span className="badge neutral">Iniciar</span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div className="clinic-row">
                  <strong>Sin plantillas disponibles</strong>
                  <span>Este paciente ya tiene iniciadas todas las plantillas activas.</span>
                </div>
              )}
            </section>
          </div>
        )}

        {error && <p className="alert error">{error}</p>}
      </div>
    </div>
  );
}
