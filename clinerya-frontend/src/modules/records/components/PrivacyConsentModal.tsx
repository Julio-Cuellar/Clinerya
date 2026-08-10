import { useState, useEffect } from "react";
import { IconX, IconFileCheck, IconSignature } from "@tabler/icons-react";
import { getFriendlyError, privacyConsentApi } from "@shared/api/api";
import { SignaturePad, getSignatureImageValue } from "@shared/ui/SignaturePad";
import type { ClinicResponse } from "@modules/clinics/types";
import type { PatientResponse } from "@modules/patients/types";
import type { PrivacyConsentResponse } from "@modules/records/types";

export function PrivacyConsentModal({
  clinic,
  patient,
  existingConsent,
  onClose,
  onSigned
}: {
  clinic: ClinicResponse;
  patient: PatientResponse;
  existingConsent: PrivacyConsentResponse | null;
  onClose: () => void;
  onSigned: (consent: PrivacyConsentResponse) => void;
}) {
  const [signerName, setSignerName] = useState("");
  const [signatureValue, setSignatureValue] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const patientName = [patient.firstName, patient.lastNamePaterno, patient.lastNameMaterno].filter(Boolean).join(" ");

  useEffect(() => {
    if (!existingConsent) {
      setSignerName(patientName);
    }
  }, [existingConsent, patientName]);

  const legalName = clinic.legalName || clinic.name;
  const address = [
    clinic.addressStreet,
    clinic.addressColonia,
    clinic.addressMunicipality,
    clinic.addressState,
    clinic.addressZip ? `C.P. ${clinic.addressZip}` : ""
  ].filter(Boolean).join(", ");

  const noticeText = `AVISO DE PRIVACIDAD DE DATOS PERSONALES SENSIBLES

1. Identidad y Domicilio del Responsable
${legalName}, con domicilio en ${address || "domicilio registrado en la clínica"}, en cumplimiento con la Ley Federal de Protección de Datos Personales en Posesión de los Particulares (LFPDPPP) de México, es responsable del tratamiento de sus datos personales.

2. Datos Personales Sensibles Tratados
Para la prestación de nuestros servicios de atención médica e integración de su expediente clínico (conforme a la NOM-004-SSA3-2012 y NOM-013-SSA2-2015), recabaremos datos de salud (antecedentes médicos, diagnósticos, alergias, tratamientos y firmas autógrafas), considerados como datos personales sensibles.

3. Finalidad del Tratamiento
Sus datos sensibles se utilizarán únicamente para:
- Brindar atención médica y dental integral.
- Integrar su expediente clínico legal y odontograma.
- Gestionar facturación y cobros de tratamientos médicos.
- Fines de auditoría clínica y cumplimiento de leyes de salud aplicables.

4. Medios para ejercer Derechos ARCO
Usted puede ejercer sus derechos de Acceso, Rectificación, Cancelación y Oposición escribiendo a: ${clinic.email || "el correo de contacto de la clínica"}.

5. Consentimiento Expreso y por Escrito (Artículo 9 de la LFPDPPP)
Consiento de manera expresa y por escrito que mis datos personales sensibles de salud sean tratados por la clínica de conformidad con los términos y condiciones informados en el presente Aviso de Privacidad.`;

  const handleSign = async () => {
    if (!signerName.trim()) {
      setError("Por favor, ingresa el nombre de la persona que firma.");
      return;
    }
    const signatureImage = getSignatureImageValue(signatureValue);
    if (!signatureImage) {
      setError("Por favor, dibuja la firma para otorgar el consentimiento.");
      return;
    }

    setLoading(true);
    setError("");
    try {
      const saved = await privacyConsentApi.save(patient.id, {
        clinicId: clinic.id,
        privacyNoticeText: noticeText,
        signerName: signerName.trim(),
        signatureImage
      });
      onSigned(saved);
      onClose();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card modal-card-wide" onClick={(event) => event.stopPropagation()} style={{ maxWidth: "650px" }}>
        <div className="panel-heading">
          <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
            <IconFileCheck size={20} />
            <h2>Consentimiento del Aviso de Privacidad</h2>
          </div>
          <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
            <IconX size={18} />
          </button>
        </div>

        <div className="modal-body" style={{ maxHeight: "75vh", overflowY: "auto", padding: "1.5rem" }}>
          {error && <p className="alert error" style={{ marginBottom: "1rem" }}>{error}</p>}

          <div
            style={{
              padding: "1rem",
              background: "var(--color-bg-light, rgba(0,0,0,0.02))",
              border: "1px solid var(--color-border)",
              borderRadius: "8px",
              maxHeight: "200px",
              overflowY: "auto",
              fontSize: "0.85rem",
              whiteSpace: "pre-wrap",
              fontFamily: "var(--font-mono, monospace)",
              marginBottom: "1.5rem",
              lineHeight: "1.4"
            }}
          >
            {existingConsent ? existingConsent.privacyNoticeText : noticeText}
          </div>

          {existingConsent ? (
            <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
              <div style={{ padding: "0.75rem", background: "rgba(34, 197, 94, 0.1)", border: "1px solid var(--color-success, #22c55e)", borderRadius: "6px", color: "var(--color-success, #22c55e)", display: "flex", alignItems: "center", gap: "0.5rem" }}>
                <IconFileCheck size={18} />
                <span style={{ fontSize: "0.9rem", fontWeight: 600 }}>Consentimiento firmado expresamente y por escrito</span>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", fontSize: "0.85rem" }}>
                <div>
                  <span style={{ opacity: 0.7, display: "block" }}>Nombre del Firmante:</span>
                  <strong>{existingConsent.signerName}</strong>
                </div>
                <div>
                  <span style={{ opacity: 0.7, display: "block" }}>Fecha y Hora:</span>
                  <strong>{new Date(existingConsent.signedAt).toLocaleString()}</strong>
                </div>
                <div>
                  <span style={{ opacity: 0.7, display: "block" }}>Dirección IP:</span>
                  <code style={{ fontSize: "0.8rem" }}>{existingConsent.ipAddress || "Local / Desconocida"}</code>
                </div>
                <div style={{ gridColumn: "span 2" }}>
                  <span style={{ opacity: 0.7, display: "block" }}>Navegador / Dispositivo:</span>
                  <span style={{ fontSize: "0.8rem", opacity: 0.8 }}>{existingConsent.userAgent || "Desconocido"}</span>
                </div>
              </div>

              <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: "0.25rem", marginTop: "0.5rem" }}>
                <span style={{ fontSize: "0.8rem", opacity: 0.7 }}>Firma Digital Autógrafa Registrada:</span>
                <div style={{ border: "1px solid var(--color-border)", borderRadius: "8px", padding: "0.5rem", background: "#fff", maxWidth: "100%" }}>
                  <img src={existingConsent.signatureImage} alt="Firma" style={{ maxHeight: "120px", display: "block" }} />
                </div>
                <span style={{ fontSize: "0.7rem", opacity: 0.5 }}>SHA-256: {existingConsent.signatureImageHash.substring(0, 16)}...</span>
              </div>

              <div className="form-actions" style={{ marginTop: "1rem" }}>
                <button className="btn secondary" type="button" onClick={onClose} style={{ width: "100%" }}>
                  Cerrar
                </button>
              </div>
            </div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "1.25rem" }}>
              <label className="field">
                <span>Nombre del Firmante (Paciente o Representante Legal)</span>
                <input
                  type="text"
                  value={signerName}
                  onChange={(e) => setSignerName(e.target.value)}
                  placeholder="Nombre completo"
                  disabled={loading}
                />
              </label>

              <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
                <span style={{ fontSize: "0.9rem", fontWeight: 500 }}>Firma Autógrafa Digital</span>
                <div style={{ display: "flex", justifyContent: "center" }}>
                  <SignaturePad
                    label="Firma en el recuadro"
                    signerName={signerName}
                    value={signatureValue}
                    onChange={setSignatureValue}
                    disabled={loading}
                  />
                </div>
              </div>

              <div className="form-actions" style={{ marginTop: "1rem", display: "flex", gap: "1rem" }}>
                <button className="btn secondary" type="button" onClick={onClose} disabled={loading} style={{ flex: 1 }}>
                  Cancelar
                </button>
                <button className="btn primary" type="button" onClick={handleSign} disabled={loading} style={{ flex: 1 }}>
                  <IconSignature size={18} style={{ marginRight: "0.5rem" }} />
                  {loading ? "Firmando..." : "Firmar Consentimiento"}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
