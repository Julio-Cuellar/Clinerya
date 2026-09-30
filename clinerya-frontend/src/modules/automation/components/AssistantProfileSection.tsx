import { useEffect, useState } from "react";
import { assistantProfileApi, clinicsApi, getFriendlyError, treatmentCatalogApi } from "@shared/api/api";
import type { TreatmentCatalogItemResponse } from "@modules/treatments/types";
import { REMINDER_HOURS, catalogReview, greetingPreview, reminderPreview, reminderTemplateError } from "../logic/assistantProfile";

const MAX_NAME = 60;
const MAX_FAQ = 4000;

/**
 * Ajustes del asistente (plan v2, S5): como se presenta, que preguntas frecuentes responde y si comparte
 * precios del catalogo, con una revision del catalogo para saber que puede ofrecer.
 */
export function AssistantProfileSection({ clinicId, canManage }: { clinicId: string; canManage: boolean }) {
  const [name, setName] = useState("");
  const [faq, setFaq] = useState("");
  const [showPrices, setShowPrices] = useState(true);
  const [remindersEnabled, setRemindersEnabled] = useState(true);
  const [reminderHours, setReminderHours] = useState(24);
  const [reminderTemplate, setReminderTemplate] = useState("");
  const [clinicName, setClinicName] = useState("tu clínica");
  const [services, setServices] = useState<TreatmentCatalogItemResponse[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      assistantProfileApi.get(clinicId),
      clinicsApi.get(clinicId).catch(() => null),
      treatmentCatalogApi.list(clinicId, false).catch(() => [] as TreatmentCatalogItemResponse[])
    ])
      .then(([profile, clinic, catalog]) => {
        if (cancelled) return;
        setName(profile.assistantName ?? "");
        setFaq(profile.faq ?? "");
        setShowPrices(profile.showPrices);
        setRemindersEnabled(profile.remindersEnabled);
        setReminderHours(profile.reminderHoursBefore);
        setReminderTemplate(profile.reminderTemplateName ?? "");
        if (clinic?.name) setClinicName(clinic.name);
        setServices(catalog);
        setLoaded(true);
      })
      .catch((caught) => !cancelled && setError(getFriendlyError(caught)));
    return () => {
      cancelled = true;
    };
  }, [clinicId]);

  const templateError = reminderTemplateError(reminderTemplate);

  const save = async () => {
    if (templateError) {
      setError(templateError);
      return;
    }
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const saved = await assistantProfileApi.update(clinicId, {
        assistantName: name, faq, showPrices, remindersEnabled, reminderHoursBefore: reminderHours,
        reminderTemplateName: reminderTemplate.trim() || null
      });
      setName(saved.assistantName ?? "");
      setFaq(saved.faq ?? "");
      setShowPrices(saved.showPrices);
      setRemindersEnabled(saved.remindersEnabled);
      setReminderHours(saved.reminderHoursBefore);
      setReminderTemplate(saved.reminderTemplateName ?? "");
      setNotice("Ajustes del asistente guardados.");
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy(false);
    }
  };

  if (!loaded) {
    return error ? <p className="alert error">{error}</p> : <p className="wa-help">Cargando ajustes del asistente…</p>;
  }

  const readOnly = !canManage || busy;
  const review = catalogReview(services);
  const reminder = reminderPreview(clinicName);
  const hourOptions = REMINDER_HOURS.includes(reminderHours as (typeof REMINDER_HOURS)[number])
    ? [...REMINDER_HOURS] : [...REMINDER_HOURS, reminderHours].sort((a, b) => a - b);

  return (
    <div className="wa-profile" aria-labelledby="wa-profile-title">
      <h4 id="wa-profile-title">Cómo atiende el asistente</h4>
      {error && <p className="alert error">{error}</p>}
      {notice && <p className="alert success">{notice}</p>}

      <label className="field">
        <span>Nombre del asistente (opcional)</span>
        <input value={name} maxLength={MAX_NAME} disabled={readOnly} placeholder="Ej. Sofi"
          onChange={(event) => setName(event.target.value)} />
      </label>
      <p className="wa-help">Vacío: habla a nombre de la clínica. Si le preguntan en serio si es un bot, dice que es el asistente virtual.</p>
      <div className="wa-profile-preview"><span className="wa-profile-bubble">{greetingPreview(name, clinicName)}</span></div>

      <label className="field">
        <span>Preguntas frecuentes</span>
        <textarea value={faq} maxLength={MAX_FAQ} disabled={readOnly} rows={5}
          placeholder="Formas de pago, estacionamiento, seguros, facturación, qué traer a la primera cita…"
          onChange={(event) => setFaq(event.target.value)} />
      </label>
      <p className="wa-help">Solo responde con esto y con los datos de la clínica; lo que no esté aquí lo pasa a una persona. {faq.length} / {MAX_FAQ}</p>

      <label className="wa-row" style={{ gap: 10 }}>
        <input type="checkbox" role="switch" checked={showPrices} disabled={readOnly} onChange={(event) => setShowPrices(event.target.checked)} />
        <strong>Compartir precios del catálogo</strong>
      </label>
      <p className="wa-help">
        Precio fijo: dice cuánto cuesta. Varía por paciente: dice "desde" y que el médico da el precio en la valoración. Apagado: no da cifras.
        Nunca ofrece descuentos ni promociones.
      </p>

      <div className="wa-profile-review" role="status">
        <strong>Revisa tu catálogo</strong>
        <ul>
          <li>{review.available} {review.available === 1 ? "servicio disponible" : "servicios disponibles"} en el asistente.</li>
          {review.incomplete > 0 && (
            <li>{review.incomplete} {review.incomplete === 1 ? "servicio no se ofrece" : "servicios no se ofrecen"}: les falta descripción o duración (complétalos en Tratamientos).</li>
          )}
          <li>
            {review.consultation
              ? `"${review.consultation}" está disponible: el asistente podrá decir su precio.`
              : "No hay una consulta disponible en el asistente: si preguntan por la consulta, no podrá dar un precio."}
          </li>
        </ul>
      </div>

      <h4 id="wa-reminder-title">Recordatorio de citas</h4>
      <label className="wa-row" style={{ gap: 10 }}>
        <input type="checkbox" role="switch" checked={remindersEnabled} disabled={readOnly}
          onChange={(event) => setRemindersEnabled(event.target.checked)} />
        <strong>Enviar un recordatorio antes de cada cita</strong>
      </label>
      <p className="wa-help">Solo a pacientes que autorizaron WhatsApp. Confirma, cancela o pide reprogramar desde el mismo mensaje.</p>
      <div className="wa-reminder-grid">
        <label className="field">
          <span>Enviar</span>
          <select value={reminderHours} disabled={readOnly || !remindersEnabled}
            onChange={(event) => setReminderHours(Number(event.target.value))}>
            {hourOptions.map((hours) => <option key={hours} value={hours}>{hours} horas antes</option>)}
          </select>
        </label>
        <label className="field">
          <span>Plantilla aprobada en Meta</span>
          <input value={reminderTemplate} maxLength={512} disabled={readOnly || !remindersEnabled} placeholder="recordatorio_cita"
            aria-invalid={templateError ? true : undefined} onChange={(event) => setReminderTemplate(event.target.value)} />
        </label>
      </div>
      {templateError && <p className="alert error">{templateError}</p>}
      <div className="wa-profile-preview">
        <span className="wa-profile-bubble">
          {reminder.text}
          <span className="wa-reminder-buttons">{reminder.buttons.map((label) => <span key={label}>{label}</span>)}</span>
        </span>
      </div>
      <p className="wa-help">
        Confirmo marca la cita como confirmada en la agenda. Reprogramar sigue necesitando la aprobación del médico. Un solo
        recordatorio por cita. Si pasaron más de 24 h desde el último mensaje del paciente, WhatsApp exige esta plantilla: créala
        en Meta con cuatro variables (nombre, clínica, fecha y hora, médico) y esos tres botones de respuesta rápida, en ese orden.
      </p>

      {canManage && (
        <div className="wa-row end">
          <button className="btn primary" type="button" disabled={busy} onClick={() => void save()}>
            {busy ? "Guardando…" : "Guardar ajustes del asistente"}
          </button>
        </div>
      )}
    </div>
  );
}
