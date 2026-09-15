import { useCallback, useEffect, useState } from "react";
import { IconAlertTriangle, IconCalendarEvent, IconCash, IconCircleCheck, IconEdit } from "@tabler/icons-react";
import { agendaApi, clinicalSummaryApi, getFriendlyError, quotationsApi, ticketsApi } from "@shared/api/api";
import { BLOOD_TYPE_LABELS, type PatientClinicalSummaryResponse } from "@modules/records/types";
import type { AppointmentResponse } from "@modules/agenda/types";
import { ClinicalDataEditor } from "@modules/records/components/ClinicalDataEditor";

const currencyFormatter = new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" });

function formatAppointment(value: string) {
  return new Date(value).toLocaleString("es-MX", { dateStyle: "medium", timeStyle: "short" });
}

/**
 * Panel fijo con lo que un clinico necesita ver sin importar en que pestana
 * del expediente esta: alertas, diagnosticos activos, medicacion, proxima
 * cita y saldo. Sustituye al banner horizontal en la vista de expediente.
 */
export function PatientSummarySidebar({
  clinicId,
  patientId,
  canEdit = true
}: {
  clinicId: string;
  patientId: string;
  canEdit?: boolean;
}) {
  const [summary, setSummary] = useState<PatientClinicalSummaryResponse | null>(null);
  const [summaryError, setSummaryError] = useState("");
  const [nextAppointment, setNextAppointment] = useState<AppointmentResponse | null>(null);
  const [appointmentError, setAppointmentError] = useState(false);
  const [balance, setBalance] = useState<number | null>(null);
  const [balanceError, setBalanceError] = useState(false);
  const [editing, setEditing] = useState(false);

  const loadSummary = useCallback(() => {
    if (!clinicId || !patientId) return;
    clinicalSummaryApi
      .get(patientId, clinicId)
      .then(setSummary)
      .catch((caught) => setSummaryError(getFriendlyError(caught)));
  }, [clinicId, patientId]);

  useEffect(() => {
    loadSummary();
  }, [loadSummary]);

  useEffect(() => {
    if (!clinicId || !patientId) return;
    setAppointmentError(false);
    agendaApi
      .listByPatient(clinicId, patientId)
      .then((appointments) => {
        const now = Date.now();
        const upcoming = appointments
          .filter(
            (appointment) =>
              (appointment.status === "SCHEDULED" || appointment.status === "CONFIRMED") &&
              new Date(appointment.scheduledStart).getTime() >= now
          )
          .sort((a, b) => new Date(a.scheduledStart).getTime() - new Date(b.scheduledStart).getTime());
        setNextAppointment(upcoming[0] ?? null);
      })
      .catch(() => setAppointmentError(true));
  }, [clinicId, patientId]);

  useEffect(() => {
    if (!clinicId || !patientId) return;
    setBalanceError(false);
    quotationsApi
      .listByPatient(patientId, clinicId)
      .then(async (quotations) => {
        const accepted = quotations.filter((quotation) => quotation.status === "ACCEPTED");
        const balances = await Promise.all(
          accepted.map((quotation) =>
            ticketsApi.getQuotationBalance(clinicId, quotation.id, patientId).catch(() => null)
          )
        );
        const total = balances.reduce((sum, item) => sum + (item?.remainingBalance ?? 0), 0);
        setBalance(total);
      })
      .catch(() => setBalanceError(true));
  }, [clinicId, patientId]);

  if (summaryError) {
    return (
      <aside className="expediente-summary-sidebar">
        <p className="expediente-summary-message">No se pudo cargar el resumen clinico.</p>
      </aside>
    );
  }

  if (!summary) {
    return (
      <aside className="expediente-summary-sidebar">
        <p className="expediente-summary-message">Cargando resumen...</p>
      </aside>
    );
  }

  const activeConditions = summary.conditions.filter((condition) => condition.status === "ACTIVE");
  const medicationList = summary.activeMedications.map((medication) => medication.medicationName).join(", ");
  const bloodType = summary.bloodType ? BLOOD_TYPE_LABELS[summary.bloodType] ?? summary.bloodType : null;
  const lastNote = summary.lastNoteAt ? new Date(summary.lastNoteAt).toLocaleDateString("es-MX") : null;

  return (
    <>
      <aside className="expediente-summary-sidebar">
        <div className="expediente-summary-heading">
          <h3>Resumen</h3>
          {canEdit && (
            <button
              className="icon-btn"
              type="button"
              onClick={() => setEditing(true)}
              aria-label="Gestionar datos clinicos"
            >
              <IconEdit size={15} aria-hidden="true" />
            </button>
          )}
        </div>

        <section className="expediente-summary-section">
          <span className="expediente-summary-label">Alertas</span>
          {summary.allergies.length > 0 ? (
            <div className="expediente-summary-chip-list">
              {summary.allergies.map((allergy) => (
                <span key={allergy.id} className="expediente-summary-chip is-alert">
                  <IconAlertTriangle size={12} aria-hidden="true" />
                  {allergy.substance}
                </span>
              ))}
            </div>
          ) : summary.allergiesReview.noneReported ? (
            <span className="expediente-summary-value expediente-summary-ok">
              <IconCircleCheck size={13} aria-hidden="true" />
              Preguntadas y negadas
            </span>
          ) : (
            <span className="expediente-summary-value expediente-summary-muted">Sin registrar</span>
          )}
        </section>

        <section className="expediente-summary-section">
          <span className="expediente-summary-label">Diagnosticos activos</span>
          {activeConditions.length > 0 ? (
            <div className="expediente-summary-diagnosis-list">
              {activeConditions.map((condition) => (
                <div key={condition.id} className="expediente-summary-diagnosis">
                  {condition.icd10Code ? <strong>{condition.icd10Code}</strong> : null}
                  <span>{condition.name}</span>
                </div>
              ))}
            </div>
          ) : (
            <span className="expediente-summary-value expediente-summary-muted">Sin diagnosticos activos</span>
          )}
        </section>

        <section className="expediente-summary-section">
          <span className="expediente-summary-label">Medicacion activa</span>
          <span className="expediente-summary-value">{medicationList || "Sin medicacion activa"}</span>
        </section>

        <section className="expediente-summary-section">
          <span className="expediente-summary-label">
            <IconCalendarEvent size={13} aria-hidden="true" />
            Proxima cita
          </span>
          <span className="expediente-summary-value">
            {appointmentError
              ? "No disponible"
              : nextAppointment
                ? formatAppointment(nextAppointment.scheduledStart)
                : "Sin citas programadas"}
          </span>
        </section>

        <section className="expediente-summary-section">
          <span className="expediente-summary-label">
            <IconCash size={13} aria-hidden="true" />
            Saldo
          </span>
          <span className={`expediente-summary-value ${balance ? "expediente-summary-alert-text" : ""}`}>
            {balanceError ? "No disponible" : balance === null ? "Calculando..." : currencyFormatter.format(balance)}
          </span>
        </section>

        <footer className="expediente-summary-footer">
          <span>{bloodType ? `Sangre: ${bloodType}` : "Sangre sin registrar"}</span>
          <span>{lastNote ? `Ultima nota: ${lastNote}` : "Sin notas registradas"}</span>
        </footer>
      </aside>

      {editing && (
        <ClinicalDataEditor
          clinicId={clinicId}
          patientId={patientId}
          summary={summary}
          onClose={() => setEditing(false)}
          onChanged={loadSummary}
        />
      )}
    </>
  );
}
