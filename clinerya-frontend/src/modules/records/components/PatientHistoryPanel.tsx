import { useEffect, useState } from "react";
import { IconArrowLeft, IconUserShare, IconHistory, IconAlertTriangle, IconShieldCheck } from "@tabler/icons-react";
import { getFriendlyError, historyTemplatesApi, medicalHistoryApi, clinicsApi, privacyConsentApi } from "@shared/api/api";
import type { HistoryTemplateResponse, MedicalHistoryResponse, PrivacyConsentResponse } from "@modules/records/types";
import type { PatientResponse } from "@modules/patients/types";
import type { ClinicResponse } from "@modules/clinics/types";
import { ClinicalNotesSection } from "@modules/records/components/ClinicalNotesSection";
import { VitalSignsChart } from "@modules/records/components/VitalSignsChart";
import { PatientClinicalHeader } from "@modules/records/components/PatientClinicalHeader";
import { HistoryFormModal } from "@modules/records/components/HistoryFormModal";
import { InviteExternalAccessModal } from "@modules/collaboration/components/InviteExternalAccessModal";
import { RecordAccessLogsModal } from "@modules/records/components/RecordAccessLogsModal";
import { PrivacyConsentModal } from "@modules/records/components/PrivacyConsentModal";

export function PatientHistoryPanel({
  clinicId,
  patient,
  onChangePatient,
  allowSharing = true,
  historyReadOnly = false,
  canWriteNotes = true,
  defaultDoctorId,
  showClinicalHeader = true
}: {
  clinicId: string;
  patient: PatientResponse;
  onChangePatient: () => void;
  allowSharing?: boolean;
  historyReadOnly?: boolean;
  canWriteNotes?: boolean;
  defaultDoctorId?: string;
  showClinicalHeader?: boolean;
}) {
  const [templates, setTemplates] = useState<HistoryTemplateResponse[]>([]);
  const [histories, setHistories] = useState<MedicalHistoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [activeTemplate, setActiveTemplate] = useState<HistoryTemplateResponse | null>(null);
  const [inviteOpen, setInviteOpen] = useState(false);
  const [accessLogsOpen, setAccessLogsOpen] = useState(false);
  const [status, setStatus] = useState("");
  const [clinic, setClinic] = useState<ClinicResponse | null>(null);
  const [privacyConsent, setPrivacyConsent] = useState<PrivacyConsentResponse | null>(null);
  const [privacyConsentLoading, setPrivacyConsentLoading] = useState(true);
  const [privacyConsentOpen, setPrivacyConsentOpen] = useState(false);
  const historiesByTemplateId = new Map(histories.map((history) => [history.templateId, history]));
  const currentTemplates = templates
    .filter((template) => historiesByTemplateId.has(template.id))
    .sort((a, b) => {
      const historyA = historiesByTemplateId.get(a.id);
      const historyB = historiesByTemplateId.get(b.id);
      return new Date(historyB?.updatedAt ?? b.updatedAt).getTime() - new Date(historyA?.updatedAt ?? a.updatedAt).getTime();
    });
  const newTemplates = templates.filter((template) => !historiesByTemplateId.has(template.id));

  const load = () => {
    setLoading(true);
    setPrivacyConsentLoading(true);
    setError("");
    Promise.all([
      historyTemplatesApi.list(clinicId, patient.id),
      medicalHistoryApi.listByPatient(patient.id, clinicId),
      clinicsApi.get(clinicId),
      privacyConsentApi.get(patient.id, clinicId).catch(() => null)
    ])
      .then(([templateList, historyList, clinicData, consentData]) => {
        setTemplates(templateList.filter((template) => template.active));
        setHistories(historyList);
        setClinic(clinicData);
        setPrivacyConsent(consentData);
      })
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => {
        setLoading(false);
        setPrivacyConsentLoading(false);
      });
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId, patient.id]);

  return (
    <article className="panel full">
      <div className="panel-heading">
        <div>
          <h2>
            {patient.firstName} {patient.lastNamePaterno} {patient.lastNameMaterno}
          </h2>
          <span>Historia clínica y formularios</span>
        </div>
        <div className="clinic-row-actions">
          <button className="btn secondary" type="button" onClick={() => setAccessLogsOpen(true)}>
            <IconHistory size={16} aria-hidden="true" />
            Bitácora de Accesos
          </button>
          {allowSharing && (
            <button className="btn secondary" type="button" onClick={() => setInviteOpen(true)}>
              <IconUserShare size={16} aria-hidden="true" />
              Compartir con especialista
            </button>
          )}
          <button className="btn secondary" type="button" onClick={onChangePatient}>
            <IconArrowLeft size={16} aria-hidden="true" />
            Cambiar paciente
          </button>
        </div>
      </div>

      {showClinicalHeader && (
        <PatientClinicalHeader clinicId={clinicId} patientId={patient.id} canEdit={!historyReadOnly} />
      )}

      {!privacyConsentLoading && clinic && (
        <div style={{
          padding: "1rem 1.5rem",
          background: privacyConsent ? "rgba(34, 197, 94, 0.08)" : "rgba(239, 68, 68, 0.08)",
          borderBottom: "1px solid var(--color-border)",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          gap: "1rem"
        }}>
          <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", fontSize: "0.9rem" }}>
            {privacyConsent ? (
              <>
                <IconShieldCheck size={20} color="var(--color-success, #22c55e)" />
                <span>
                  Consentimiento expreso de Aviso de Privacidad firmado por <strong>{privacyConsent.signerName}</strong> el {new Date(privacyConsent.signedAt).toLocaleDateString()}.
                </span>
              </>
            ) : (
              <>
                <IconAlertTriangle size={20} color="var(--color-error, #ef4444)" />
                <span style={{ fontWeight: 500 }}>
                  ⚠️ Pendiente firma de consentimiento del aviso de privacidad (Obligatorio LFPDPPP para expedientes clínicos de salud).
                </span>
              </>
            )}
          </div>
          <button
            className={`btn ${privacyConsent ? "secondary" : "primary"}`}
            type="button"
            onClick={() => setPrivacyConsentOpen(true)}
            style={{ padding: "0.35rem 0.75rem", fontSize: "0.8rem", whiteSpace: "nowrap" }}
          >
            {privacyConsent ? "Ver Firmado" : "Firmar Consentimiento"}
          </button>
        </div>
      )}

      {status && <p className="alert success">{status}</p>}

      {loading && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Cargando plantillas...</strong>
          </div>
        </div>
      )}

      {!loading && templates.length === 0 && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>No hay plantillas activas</strong>
            <span>Crea una plantilla en la pestaña "Plantillas" para poder llenar la historia clínica</span>
          </div>
        </div>
      )}

      {!loading && templates.length > 0 && (
        <div className="history-template-sections">
          <section className="history-template-section">
            <div className="history-template-section-heading">
              <h3>Historias clínicas actuales</h3>
              <span className="badge neutral">{currentTemplates.length}</span>
            </div>
            {currentTemplates.length > 0 ? (
              <div className="clinic-list">
                {currentTemplates.map((template) => {
                  const history = historiesByTemplateId.get(template.id);
                  return (
                    <div className="clinic-row clinic-row-actionable" key={template.id} onClick={() => setActiveTemplate(template)}>
                      <div>
                        <strong>{template.name}</strong>
                        <span>{history ? `Última actualización: ${new Date(history.updatedAt).toLocaleString()}` : "Historia iniciada"}</span>
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
                <span>Selecciona una plantilla en Nueva historia clínica para comenzar.</span>
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
                {newTemplates.map((template) => (
                  <div className="clinic-row clinic-row-actionable" key={template.id} onClick={() => setActiveTemplate(template)}>
                    <div>
                      <strong>{template.name}</strong>
                      <span>{template.description || "Plantilla disponible para iniciar con este paciente"}</span>
                    </div>
                    <div className="clinic-row-actions">
                      <span className="badge neutral">Iniciar</span>
                    </div>
                  </div>
                ))}
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

      <VitalSignsChart patientId={patient.id} clinicId={clinicId} />

      <ClinicalNotesSection
        patientId={patient.id}
        clinicId={clinicId}
        canWriteNotes={canWriteNotes}
        defaultDoctorId={defaultDoctorId}
      />

      {activeTemplate && (
        <HistoryFormModal
          patient={patient}
          clinicId={clinicId}
          template={activeTemplate}
          onClose={() => setActiveTemplate(null)}
          onSaved={() => {
            setActiveTemplate(null);
            load();
          }}
          readOnly={historyReadOnly}
        />
      )}

      {inviteOpen && (
        <InviteExternalAccessModal
          clinicId={clinicId}
          patientId={patient.id}
          patientLabel={`${patient.firstName} ${patient.lastNamePaterno} ${patient.lastNameMaterno ?? ""}`.trim()}
          onClose={() => setInviteOpen(false)}
          onInvited={() => {
            setInviteOpen(false);
            setStatus("Invitación enviada. El especialista deberá aceptarla desde su cuenta.");
          }}
        />
      )}

      {accessLogsOpen && (
        <RecordAccessLogsModal
          clinicId={clinicId}
          patientId={patient.id}
          patientLabel={`${patient.firstName} ${patient.lastNamePaterno} ${patient.lastNameMaterno ?? ""}`.trim()}
          onClose={() => setAccessLogsOpen(false)}
        />
      )}

      {privacyConsentOpen && clinic && (
        <PrivacyConsentModal
          clinic={clinic}
          patient={patient}
          existingConsent={privacyConsent}
          onClose={() => setPrivacyConsentOpen(false)}
          onSigned={(consent) => setPrivacyConsent(consent)}
        />
      )}
    </article>
  );
}
