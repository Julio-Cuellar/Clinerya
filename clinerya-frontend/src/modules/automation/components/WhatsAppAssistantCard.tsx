import { useEffect, useState, type ReactNode } from "react";
import { IconBrandWhatsapp, IconCheck, IconCopy, IconX } from "@tabler/icons-react";
import { ApiClientError, getFriendlyError, whatsAppSettingsApi } from "@shared/api/api";
import type { ChannelSettingsView, ConnectionTestResult, PromptMode, SecretKind } from "../types";
import { assistantStatus, setupSteps } from "../logic/assistant";
import { DoctorChannelsSection } from "./DoctorChannelsSection";

const MAX_PROMPT = 4000;
const MIN_RETENTION = 1;
const MAX_RETENTION = 60;

function Step({ number, done, title, help, children }: { number: number; done: boolean; title: string; help: string; children: ReactNode }) {
  return (
    <div className="wa-step">
      <div className={`wa-step-num${done ? " done" : ""}`} aria-label={done ? `Paso ${number} completo` : `Paso ${number} pendiente`}>{number}</div>
      <div className="wa-step-body">
        <h4>{title}</h4>
        <p className="wa-help">{help}</p>
        {children}
      </div>
    </div>
  );
}

function SecretField({ label, hint, value, onChange, onRemove, disabled }: {
  label: string; hint: string | null; value: string; onChange: (value: string) => void; onRemove: () => void; disabled: boolean;
}) {
  return (
    <div className="wa-row" style={{ alignItems: "flex-end" }}>
      <label className="field">
        <span>{label}</span>
        <input
          type="password"
          autoComplete="off"
          value={value}
          disabled={disabled}
          placeholder={hint ? `Guardado ${hint} · escribe uno nuevo para reemplazarlo` : "Sin guardar"}
          onChange={(event) => onChange(event.target.value)}
        />
      </label>
      {hint && !disabled && <button className="btn ghost" type="button" onClick={onRemove}>Quitar</button>}
    </div>
  );
}

/**
 * Tarjeta "Asistente de WhatsApp" en Clinica e Integraciones. Los secretos nunca vuelven completos:
 * se muestran como pista y se reemplazan escribiendo uno nuevo. Cambiar credenciales apaga el asistente
 * y pide volver a probar la conexion (lo decide el backend).
 */
export function WhatsAppAssistantCard({ clinicId, canManage }: { clinicId: string; canManage: boolean }) {
  const [view, setView] = useState<ChannelSettingsView | null>(null);
  const [hidden, setHidden] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [test, setTest] = useState<ConnectionTestResult | null>(null);
  const [whatsApp, setWhatsApp] = useState({ phoneNumberId: "", businessAccountId: "", accessToken: "", appSecret: "" });
  const [gemini, setGemini] = useState({ apiKey: "", model: "" });
  const [templates, setTemplates] = useState({ patientTemplateName: "", doctorTemplateName: "", languageCode: "es_MX" });
  const [tone, setTone] = useState<{ promptMode: PromptMode; customPrompt: string; chatRetentionMonths: number }>({
    promptMode: "DEFAULT", customPrompt: "", chatRetentionMonths: 12
  });

  const adopt = (next: ChannelSettingsView) => {
    setView(next);
    setWhatsApp({
      phoneNumberId: next.whatsappPhoneNumberId ?? "",
      businessAccountId: next.whatsappBusinessAccountId ?? "",
      accessToken: "",
      appSecret: ""
    });
    setGemini({ apiKey: "", model: next.geminiModel });
    setTemplates({
      patientTemplateName: next.patientTemplateName ?? "",
      doctorTemplateName: next.doctorTemplateName ?? "",
      languageCode: next.templateLanguage
    });
    setTone({ promptMode: next.promptMode, customPrompt: next.customPrompt ?? "", chatRetentionMonths: next.chatRetentionMonths });
  };

  useEffect(() => {
    whatsAppSettingsApi
      .get(clinicId)
      .then(adopt)
      .catch((caught) => {
        if (caught instanceof ApiClientError && caught.status === 403) {
          setHidden(true);
        } else {
          setError(getFriendlyError(caught));
        }
      });
  }, [clinicId]);

  const run = async (action: () => Promise<ChannelSettingsView>, done: string) => {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      adopt(await action());
      setNotice(done);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy(false);
    }
  };

  const removeSecret = (kind: SecretKind, what: string) =>
    run(() => whatsAppSettingsApi.removeSecret(clinicId, kind), `${what} eliminado. El asistente quedó apagado.`);

  const testConnection = async () => {
    setBusy(true);
    setError("");
    try {
      setTest(await whatsAppSettingsApi.testConnection(clinicId));
      adopt(await whatsAppSettingsApi.get(clinicId));
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setBusy(false);
    }
  };

  const copy = async (text: string, what: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setNotice(`${what} copiado.`);
    } catch {
      setError("No se pudo copiar; selecciona el texto y cópialo a mano.");
    }
  };

  if (hidden) return null;
  if (!view) {
    return <div className="wa-card wa-sub">{error || "Cargando el asistente de WhatsApp…"}</div>;
  }

  const status = assistantStatus(view);
  const steps = setupSteps(view);
  const readOnly = !canManage || busy;
  const webhookUrl = `${window.location.origin}${view.webhookPath}`;

  return (
    <section className="wa-card" aria-labelledby="wa-assistant-title">
      <div className="wa-card-head">
        <div className="wa-row" style={{ alignItems: "flex-start", gap: 14 }}>
          <IconBrandWhatsapp size={32} stroke={1.6} style={{ color: "var(--color-success)", flex: "none" }} aria-hidden="true" />
          <div>
            <h3 id="wa-assistant-title">Asistente de WhatsApp</h3>
            <p className="wa-help" style={{ maxWidth: 560 }}>
              Los pacientes piden cita escribiendo al WhatsApp de la clínica. El asistente ofrece horarios reales de la agenda y el médico aprueba desde Clinerya.
            </p>
          </div>
        </div>
        <span className={`wa-pill ${status.tone}`}>{status.label}</span>
      </div>
      {!canManage && <p className="wa-help">Solo quien administra las integraciones puede cambiar esta configuración.</p>}
      {error && <p className="alert error" style={{ marginTop: 12 }}>{error}</p>}
      {notice && <p className="alert success" style={{ marginTop: 12 }}>{notice}</p>}

      <Step number={1} done={steps.whatsapp} title="Cuenta de WhatsApp Business (Meta)"
        help="Datos de tu app en Meta for Developers. Los secretos se guardan cifrados y nunca se vuelven a mostrar completos.">
        <div className="wa-grid">
          <label className="field"><span>Phone Number ID</span>
            <input value={whatsApp.phoneNumberId} disabled={readOnly} inputMode="numeric"
              onChange={(e) => setWhatsApp({ ...whatsApp, phoneNumberId: e.target.value })} /></label>
          <label className="field"><span>WhatsApp Business Account ID</span>
            <input value={whatsApp.businessAccountId} disabled={readOnly} inputMode="numeric"
              onChange={(e) => setWhatsApp({ ...whatsApp, businessAccountId: e.target.value })} /></label>
          <SecretField label="Token de acceso" hint={view.accessTokenHint} value={whatsApp.accessToken} disabled={readOnly}
            onChange={(value) => setWhatsApp({ ...whatsApp, accessToken: value })}
            onRemove={() => removeSecret("WHATSAPP_ACCESS_TOKEN", "Token de acceso")} />
          <SecretField label="App Secret (firma del webhook)" hint={view.appSecretHint} value={whatsApp.appSecret} disabled={readOnly}
            onChange={(value) => setWhatsApp({ ...whatsApp, appSecret: value })}
            onRemove={() => removeSecret("WHATSAPP_APP_SECRET", "App Secret")} />
        </div>
        {canManage && (
          <div className="wa-row end">
            <button className="btn primary" type="button" disabled={busy}
              onClick={() => run(() => whatsAppSettingsApi.updateWhatsApp(clinicId, whatsApp), "Datos de WhatsApp guardados. Vuelve a probar la conexión.")}>
              Guardar WhatsApp
            </button>
          </div>
        )}
      </Step>

      <Step number={2} done={steps.whatsapp} title="Conecta el webhook en Meta"
        help="En Meta › WhatsApp › Configuración, pega esta URL y el token de verificación, y suscríbete al campo messages.">
        <div className="wa-grid" style={{ alignItems: "start" }}>
          <div className="wa-row" style={{ alignItems: "flex-end" }}>
            <label className="field"><span>URL de devolución de llamada</span><input className="wa-mono" readOnly value={webhookUrl} /></label>
            <button className="btn" type="button" onClick={() => copy(webhookUrl, "URL")}><IconCopy size={15} aria-hidden="true" /> Copiar</button>
          </div>
          {view.verifyToken && (
            <div>
              <div className="wa-row" style={{ alignItems: "flex-end" }}>
                <label className="field"><span>Token de verificación</span><input className="wa-mono" readOnly value={view.verifyToken} /></label>
                <button className="btn" type="button" onClick={() => copy(view.verifyToken ?? "", "Token")}><IconCopy size={15} aria-hidden="true" /> Copiar</button>
              </div>
              {canManage && (
                <button className="btn ghost" type="button" disabled={busy} style={{ marginTop: 4 }}
                  onClick={() => run(() => whatsAppSettingsApi.regenerateVerifyToken(clinicId), "Token nuevo generado. Actualízalo en Meta.")}>
                  Generar otro token
                </button>
              )}
            </div>
          )}
        </div>
      </Step>

      <Step number={3} done={steps.gemini} title="Gemini (entiende lo que escribe el paciente)"
        help="Sin clave de Gemini no se puede activar el asistente. La clave es de la clínica y se guarda cifrada.">
        <div className="wa-grid">
          <SecretField label="Clave de API de Gemini" hint={view.geminiApiKeyHint} value={gemini.apiKey} disabled={readOnly}
            onChange={(value) => setGemini({ ...gemini, apiKey: value })}
            onRemove={() => removeSecret("GEMINI_API_KEY", "Clave de Gemini")} />
          <label className="field"><span>Modelo</span>
            <input value={gemini.model} disabled={readOnly} placeholder="gemini-2.5-flash"
              onChange={(e) => setGemini({ ...gemini, model: e.target.value })} /></label>
        </div>
        {canManage && (
          <div className="wa-row end">
            <button className="btn primary" type="button" disabled={busy}
              onClick={() => run(() => whatsAppSettingsApi.updateGemini(clinicId, gemini), "Gemini guardado. Vuelve a probar la conexión.")}>
              Guardar Gemini
            </button>
          </div>
        )}
      </Step>

      <Step number={4} done={steps.tested} title="Prueba la conexión"
        help="Consulta de solo lectura a Meta y a Gemini. Hay que repetirla cada vez que cambias una credencial.">
        {canManage && (
          <div className="wa-row" style={{ marginTop: 10 }}>
            <button className="btn primary" type="button" disabled={busy} onClick={testConnection}>Probar conexión</button>
          </div>
        )}
        {test && (
          <div className="wa-grid">
            {[
              { ok: test.whatsappOk, title: test.whatsappOk ? "WhatsApp conectado" : "WhatsApp no respondió bien", detail: test.whatsappDetail },
              { ok: test.geminiOk, title: test.geminiOk ? "Gemini conectado" : "Gemini rechazó la conexión", detail: test.geminiDetail }
            ].map((item) => (
              <div key={item.title} className={`wa-result${item.ok ? "" : " bad"}`} role="status">
                {item.ok ? <IconCheck size={18} color="var(--color-success)" aria-hidden="true" /> : <IconX size={18} color="var(--color-error)" aria-hidden="true" />}
                <div><strong>{item.title}</strong><span className="wa-sub">{item.detail}</span></div>
              </div>
            ))}
          </div>
        )}
      </Step>

      <Step number={5} done={steps.templates} title="Plantillas aprobadas en Meta"
        help="Dentro de las 24 h desde el último mensaje el asistente escribe con texto libre. Pasado ese plazo WhatsApp solo permite una plantilla aprobada.">
        <div className="wa-grid three">
          <label className="field"><span>Plantilla para pacientes</span>
            <input value={templates.patientTemplateName} disabled={readOnly}
              onChange={(e) => setTemplates({ ...templates, patientTemplateName: e.target.value })} /></label>
          <label className="field"><span>Plantilla para médicos</span>
            <input value={templates.doctorTemplateName} disabled={readOnly}
              onChange={(e) => setTemplates({ ...templates, doctorTemplateName: e.target.value })} /></label>
          <label className="field"><span>Idioma</span>
            <input value={templates.languageCode} disabled={readOnly}
              onChange={(e) => setTemplates({ ...templates, languageCode: e.target.value })} /></label>
        </div>
        <p className="wa-help">La plantilla de médicos lleva dos variables: la 1 es el resumen (fecha y hora) y la 2 el enlace a la bandeja. Nunca incluye datos del paciente.</p>
        {canManage && (
          <div className="wa-row end">
            <button className="btn primary" type="button" disabled={busy}
              onClick={() => run(() => whatsAppSettingsApi.updateTemplates(clinicId, templates), "Plantillas guardadas.")}>
              Guardar plantillas
            </button>
          </div>
        )}
      </Step>

      <Step number={6} done title="Tono del asistente e historial"
        help="Cómo se dirige a los pacientes. Las reglas de seguridad (no inventar horarios, no dar diagnósticos) siempre se aplican.">
        <div className="wa-row" style={{ marginTop: 12, alignItems: "stretch" }}>
          {(["DEFAULT", "CUSTOM"] as PromptMode[]).map((mode) => (
            <label key={mode} className={`wa-radio${tone.promptMode === mode ? " on" : ""}`}>
              <input type="radio" name="wa-tone" checked={tone.promptMode === mode} disabled={readOnly}
                onChange={() => setTone({ ...tone, promptMode: mode })} />
              <span><strong>{mode === "DEFAULT" ? "Predeterminado" : "Personalizado"}</strong><br />
                <span className="wa-sub">{mode === "DEFAULT" ? view.defaultPrompt : "Escribe tus propias indicaciones."}</span></span>
            </label>
          ))}
        </div>
        {tone.promptMode === "CUSTOM" && (
          <label className="field" style={{ marginTop: 12 }}>
            <span>Indicaciones para el asistente</span>
            <textarea value={tone.customPrompt} maxLength={MAX_PROMPT} disabled={readOnly}
              onChange={(e) => setTone({ ...tone, customPrompt: e.target.value })} />
            <span className="wa-sub">{tone.customPrompt.length} / {MAX_PROMPT} · por ahora solo se guarda: responder dudas con esta información llega en la siguiente entrega.</span>
          </label>
        )}
        <label className="field" style={{ marginTop: 12, maxWidth: 280 }}>
          <span>Guardar el historial de chats (meses, de {MIN_RETENTION} a {MAX_RETENTION})</span>
          <input type="number" min={MIN_RETENTION} max={MAX_RETENTION} value={tone.chatRetentionMonths} disabled={readOnly}
            onChange={(e) => setTone({ ...tone, chatRetentionMonths: Number(e.target.value) })} />
        </label>
        {canManage && (
          <div className="wa-row end">
            <button className="btn primary" type="button" disabled={busy}
              onClick={() => run(() => whatsAppSettingsApi.updateAssistant(clinicId, tone), "Tono e historial guardados.")}>
              Guardar tono e historial
            </button>
          </div>
        )}
      </Step>

      <Step number={7} done title="Médicos que reciben avisos por WhatsApp"
        help="Cada médico recibe un aviso cuando le llega una solicitud y responde desde Clinerya. Sin su consentimiento el aviso no se activa; también pueden configurarlo desde su Perfil.">
        {canManage ? (
          <DoctorChannelsSection clinicId={clinicId} canManage={canManage} />
        ) : (
          <p className="wa-sub" style={{ marginTop: 8 }}>Cada médico configura su celular en Configuración › Perfil.</p>
        )}
      </Step>

      <div className="wa-missing">
        <div>
          {view.enabled ? (
            <strong>El asistente está respondiendo a los pacientes.</strong>
          ) : view.missingToEnable.length > 0 ? (
            <>
              <strong>Para activar falta:</strong>
              <ul>{view.missingToEnable.map((item) => <li key={item}>{item.charAt(0).toUpperCase() + item.slice(1)}</li>)}</ul>
            </>
          ) : (
            <strong>Todo listo para activar el asistente.</strong>
          )}
        </div>
        {canManage && (
          <label className="wa-check" style={{ fontWeight: 600, alignItems: "center" }}>
            <input type="checkbox" checked={view.enabled} disabled={busy || (!view.enabled && view.missingToEnable.length > 0)}
              onChange={(e) => run(() => whatsAppSettingsApi.setEnabled(clinicId, e.target.checked),
                e.target.checked ? "Asistente activado." : "Asistente apagado.")} />
            Activar asistente
          </label>
        )}
      </div>
    </section>
  );
}
