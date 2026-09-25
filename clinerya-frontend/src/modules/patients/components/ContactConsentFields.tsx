import { useEffect, useState } from "react";
import { getFriendlyError, patientsApi } from "@shared/api/api";
import type { ContactConsentTextResponse } from "@modules/patients/types";

/**
 * Bloque del alta de paciente: el personal lee al paciente el texto oficial (lo sirve el backend con
 * su version) y marca la casilla si autoriza. Sin marcar se registra como un "no" explicito.
 * Expone el campo "contactConsentGranted" y la version leida en "contactConsentTextVersion".
 */
export function ContactConsentFields() {
  const [consentText, setConsentText] = useState<ContactConsentTextResponse | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    patientsApi
      .contactConsentText()
      .then(setConsentText)
      .catch((caught) => setError(getFriendlyError(caught)));
  }, []);

  return (
    <div className="field field-full consent-box">
      <strong>Autorización de contacto</strong>
      {error && <p className="alert error">{error}</p>}
      {consentText && (
        <>
          <p className="consent-instruction">Léale al paciente:</p>
          <blockquote className="consent-text">“{consentText.text}”</blockquote>
          <label className="checkbox-field">
            <input type="checkbox" name="contactConsentGranted" />
            <span>El paciente autoriza el contacto por WhatsApp y correo</span>
          </label>
          <input type="hidden" name="contactConsentTextVersion" value={consentText.version} />
          <small className="consent-meta">
            Texto v{consentText.version} · quedará registrado quién lo marcó
          </small>
        </>
      )}
    </div>
  );
}
