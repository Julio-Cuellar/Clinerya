import { useEffect, useState } from "react";
import { IconPrinter, IconAlertTriangle, IconFileText, IconPill, IconClipboardText, IconDownload, IconFolderOpen, IconMailCheck } from "@tabler/icons-react";
import { ApiClientError, collaborationApi, getFriendlyError } from "@shared/api/api";
import { parseSchema, parseAnswers, parseTableRows, type TemplateElement } from "@modules/records/types";
import { TableFieldEditor } from "@shared/ui/TableFieldEditor";
import { OdontogramField } from "@modules/records/components/OdontogramField";

interface SharedMedicalHistory {
  templateName: string;
  schemaJson?: string;
  answersJson?: string;
  updatedAt?: string;
}

interface SharedPrescription {
  createdAt?: string;
  notes?: string;
  items: Array<{
    medicationName?: string;
    dosage?: string;
    frequency?: string;
    duration?: string;
    instructions?: string;
  }>;
}

interface SharedStudy {
  id: string;
  filename: string;
  contentType?: string;
  sizeBytes?: number;
  createdAt?: string;
}

interface PublicSharedRecord {
  patientId: string;
  patientFullName: string;
  patientCurp?: string;
  patientPhone?: string;
  patientEmail?: string;
  clinicName: string;
  sections?: string[];
  medicalHistories?: SharedMedicalHistory[];
  prescriptions?: SharedPrescription[];
  studies?: SharedStudy[];
  clinicalNotes: Array<{
    id: string;
    doctorId: string;
    doctorName?: string;
    subjective?: string;
    objective?: string;
    assessment?: string;
    plan?: string;
    diagnoses?: string[];
    createdAt: string;
    vitalSigns?: {
      temperature?: number;
      bloodPressure?: string;
      heartRate?: number;
      respiratoryRate?: number;
      weight?: number;
      height?: number;
      oxygenSaturation?: number;
    };
  }>;
}

/** Campos con respuesta que aportan contenido al lector externo. */
const RENDERABLE_FIELD_TYPES = new Set([
  "text", "textarea", "number", "date", "select", "table", "odontogram"
]);

interface RenderedAnswer {
  element: TemplateElement;
  raw: string;
}

function tableHasContent(raw: string, columnCount: number): boolean {
  return parseTableRows(raw, columnCount).some((row) => row.some((cell) => String(cell ?? "").trim() !== ""));
}

/** Selecciona los campos del formulario que tienen respuesta útil, en orden. */
function collectHistoryAnswers(schemaJson?: string, answersJson?: string): RenderedAnswer[] {
  const schema = parseSchema(schemaJson);
  const answers = parseAnswers(answersJson);
  const rows: RenderedAnswer[] = [];
  for (const page of schema.pages) {
    for (const element of page.elements) {
      if (!RENDERABLE_FIELD_TYPES.has(element.type)) continue;
      const raw = answers[element.id];
      if (raw === undefined || raw === null || String(raw).trim() === "") continue;
      if (element.type === "table" && !tableHasContent(raw, (element.columns ?? []).length)) continue;
      rows.push({ element, raw: String(raw) });
    }
  }
  return rows;
}

function HistoryAnswerRow({ element, raw }: RenderedAnswer) {
  if (element.type === "table") {
    return (
      <div style={{ margin: "4px 0 12px 0" }}>
        <span style={{ fontSize: "12px", color: "var(--color-text-3)", textTransform: "uppercase", display: "block", marginBottom: "4px" }}>
          {element.label || "Tabla"}
        </span>
        <div style={{ overflowX: "auto" }}>
          <TableFieldEditor columns={element.columns ?? []} value={raw} onChange={() => undefined} readOnly />
        </div>
      </div>
    );
  }
  if (element.type === "odontogram") {
    return (
      <div style={{ margin: "4px 0 12px 0" }}>
        <span style={{ fontSize: "12px", color: "var(--color-text-3)", textTransform: "uppercase", display: "block", marginBottom: "4px" }}>
          {element.label || "Odontograma"}
        </span>
        <OdontogramField value={raw} onChange={() => undefined} readOnly />
      </div>
    );
  }
  return (
    <div style={{ display: "grid", gridTemplateColumns: "minmax(140px, 200px) 1fr", gap: "4px 16px", margin: "4px 0" }}>
      <span style={{ fontSize: "12px", color: "var(--color-text-3)", textTransform: "uppercase" }}>{element.label || "Campo"}</span>
      <span style={{ fontSize: "14px", whiteSpace: "pre-wrap" }}>{raw}</span>
    </div>
  );
}

function formatBytes(bytes?: number): string {
  if (!bytes || bytes <= 0) return "";
  const units = ["B", "KB", "MB"];
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit += 1;
  }
  return `${value.toFixed(unit === 0 ? 0 : 1)} ${units[unit]}`;
}

export function ShareViewScreen() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [record, setRecord] = useState<PublicSharedRecord | null>(null);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const [studyError, setStudyError] = useState("");

  const [needsVerification, setNeedsVerification] = useState(false);
  const [codeSent, setCodeSent] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [verificationCode, setVerificationCode] = useState("");
  const [verificationError, setVerificationError] = useState("");

  const token = new URLSearchParams(window.location.search).get("token") || "";

  const loadRecord = () => {
    setLoading(true);
    setError("");

    return collaborationApi.getSharedRecord(token)
      .then((data) => {
        setNeedsVerification(false);
        setRecord(data);
      })
      .catch((caught) => {
        if (caught instanceof ApiClientError && caught.status === 428) {
          setNeedsVerification(true);
        } else {
          setError(getFriendlyError(caught));
        }
      })
      .finally(() => {
        setLoading(false);
      });
  };

  const sendVerificationCode = async () => {
    setSendingCode(true);
    setVerificationError("");
    try {
      await collaborationApi.requestShareVerification(token);
      setCodeSent(true);
    } catch (caught) {
      setVerificationError(getFriendlyError(caught));
    } finally {
      setSendingCode(false);
    }
  };

  const confirmVerificationCode = async () => {
    if (!verificationCode.trim()) return;
    setVerifying(true);
    setVerificationError("");
    try {
      await collaborationApi.confirmShareVerification(token, verificationCode.trim());
      await loadRecord();
    } catch (caught) {
      setVerificationError(getFriendlyError(caught));
    } finally {
      setVerifying(false);
    }
  };

  const downloadStudy = async (study: SharedStudy) => {
    setDownloadingId(study.id);
    setStudyError("");
    try {
      const blob = await collaborationApi.getSharedStudyContent(token, study.id);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = study.filename || "estudio";
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (caught) {
      setStudyError(getFriendlyError(caught));
    } finally {
      setDownloadingId(null);
    }
  };

  useEffect(() => {
    if (!token) {
      setError("Token de consulta no proporcionado en el enlace.");
      setLoading(false);
      return;
    }

    void loadRecord();
  }, [token]);

  const handlePrint = () => {
    window.print();
  };

  if (loading) {
    return (
      <div className="auth-layout" style={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: "100vh" }}>
        <p style={{ fontSize: "16px", color: "var(--color-text-2)" }}>Cargando expediente clínico compartido...</p>
      </div>
    );
  }

  if (needsVerification) {
    return (
      <div className="auth-layout" style={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: "100vh" }}>
        <section className="auth-panel" style={{ maxWidth: "450px", textAlign: "center" }}>
          <div style={{ color: "var(--color-primary)", marginBottom: "15px" }}>
            <IconMailCheck size={48} style={{ margin: "0 auto" }} />
          </div>
          <h2>Verifica tu correo</h2>
          <p style={{ fontSize: "14px", color: "var(--color-text-2)", marginTop: "10px", marginBottom: "20px" }}>
            Por seguridad, antes de mostrar el expediente enviamos un código de un solo uso al correo al que se compartió este enlace.
          </p>

          {verificationError && <p className="alert error" style={{ marginBottom: "15px" }}>{verificationError}</p>}

          {!codeSent ? (
            <button className="btn primary" type="button" disabled={sendingCode} onClick={() => void sendVerificationCode()}>
              {sendingCode ? "Enviando..." : "Enviar código a mi correo"}
            </button>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              <input
                type="text"
                placeholder="Código de 6 caracteres"
                value={verificationCode}
                maxLength={6}
                onChange={(event) => setVerificationCode(event.target.value.toUpperCase())}
                style={{ textAlign: "center", fontSize: "18px", letterSpacing: "4px", textTransform: "uppercase" }}
              />
              <button
                className="btn primary"
                type="button"
                disabled={verifying || !verificationCode.trim()}
                onClick={() => void confirmVerificationCode()}
              >
                {verifying ? "Verificando..." : "Verificar y continuar"}
              </button>
              <button
                className="btn secondary"
                type="button"
                disabled={sendingCode}
                onClick={() => void sendVerificationCode()}
              >
                {sendingCode ? "Enviando..." : "Reenviar código"}
              </button>
            </div>
          )}
        </section>
      </div>
    );
  }

  if (error || !record) {
    return (
      <div className="auth-layout" style={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: "100vh" }}>
        <section className="auth-panel" style={{ maxWidth: "450px", textAlign: "center" }}>
          <div style={{ color: "var(--color-error)", marginBottom: "15px" }}>
            <IconAlertTriangle size={48} style={{ margin: "0 auto" }} />
          </div>
          <h2>Enlace de Consulta Inválido</h2>
          <p style={{ fontSize: "14px", color: "var(--color-text-2)", marginTop: "10px", marginBottom: "20px" }}>
            {error || "El acceso temporal ha expirado o el enlace es incorrecto."}
          </p>
          <p style={{ fontSize: "12px", color: "var(--color-text-3)" }}>
            Por razones de seguridad, los enlaces para especialistas externos tienen un tiempo de validez limitado. Solicita un nuevo enlace al médico emisor si es necesario.
          </p>
        </section>
      </div>
    );
  }

  return (
    <div className="share-view-container" style={{ padding: "40px 20px", maxWidth: "900px", margin: "0 auto" }}>
      {/* Botón de impresión y cabecera */}
      <header style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "30px", borderBottom: "2px solid var(--color-border)", paddingBottom: "15px" }} className="no-print">
        <div>
          <span style={{ fontSize: "12px", textTransform: "uppercase", fontWeight: "bold", letterSpacing: "1px", color: "var(--color-primary)" }}>
            Consulta Temporal Externa
          </span>
          <h1 style={{ fontSize: "24px", margin: "5px 0 0 0" }}>{record.clinicName}</h1>
        </div>
        <button className="btn secondary" onClick={handlePrint} type="button">
          <IconPrinter size={18} style={{ marginRight: "6px" }} />
          Imprimir / Guardar PDF
        </button>
      </header>

      {/* Cabecera para cuando se imprime */}
      <div className="print-only" style={{ display: "none", marginBottom: "30px", borderBottom: "2px solid #000", paddingBottom: "15px" }}>
        <h1 style={{ fontSize: "28px", margin: 0 }}>{record.clinicName}</h1>
        <p style={{ fontSize: "14px", margin: "5px 0 0 0", color: "#666" }}>Expediente Clínico Compartido - Consulta Temporal Externa</p>
      </div>

      {/* Datos del Paciente */}
      <section style={{ background: "var(--color-bg-2)", borderRadius: "8px", padding: "20px", marginBottom: "30px", border: "1px solid var(--color-border)" }}>
        <h2 style={{ fontSize: "16px", textTransform: "uppercase", letterSpacing: "0.5px", color: "var(--color-text-2)", marginBottom: "15px", display: "flex", alignItems: "center", gap: "6px" }}>
          <IconFileText size={18} />
          Datos del Paciente
        </h2>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "15px" }}>
          <div>
            <span style={{ fontSize: "11px", color: "var(--color-text-3)", display: "block" }}>Nombre Completo</span>
            <strong style={{ fontSize: "15px" }}>{record.patientFullName}</strong>
          </div>
          {record.patientCurp && (
            <div>
              <span style={{ fontSize: "11px", color: "var(--color-text-3)", display: "block" }}>CURP</span>
              <strong style={{ fontSize: "15px" }}>{record.patientCurp}</strong>
            </div>
          )}
          {record.patientPhone && (
            <div>
              <span style={{ fontSize: "11px", color: "var(--color-text-3)", display: "block" }}>Teléfono</span>
              <span style={{ fontSize: "15px" }}>{record.patientPhone}</span>
            </div>
          )}
          {record.patientEmail && (
            <div>
              <span style={{ fontSize: "11px", color: "var(--color-text-3)", display: "block" }}>Correo Electrónico</span>
              <span style={{ fontSize: "15px" }}>{record.patientEmail}</span>
            </div>
          )}
        </div>
      </section>

      {/* Historia Clínica (formularios) */}
      {(record.medicalHistories?.length ?? 0) > 0 && (
        <section style={{ marginBottom: "30px" }}>
          <h2 style={{ fontSize: "18px", marginBottom: "20px", display: "flex", alignItems: "center", gap: "6px" }}>
            <IconClipboardText size={20} />
            Historia Clínica
          </h2>
          <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            {record.medicalHistories!.map((history, index) => {
              const rows = collectHistoryAnswers(history.schemaJson, history.answersJson);
              return (
                <article key={index} style={{ background: "var(--color-bg-1)", border: "1px solid var(--color-border)", borderRadius: "8px", padding: "20px" }}>
                  <h3 style={{ fontSize: "15px", margin: "0 0 12px 0" }}>{history.templateName}</h3>
                  {rows.length === 0 ? (
                    <p style={{ color: "var(--color-text-2)", fontStyle: "italic", margin: 0 }}>Sin respuestas registradas.</p>
                  ) : (
                    <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                      {rows.map((row) => (
                        <HistoryAnswerRow key={row.element.id} element={row.element} raw={row.raw} />
                      ))}
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        </section>
      )}

      {/* Historial de Notas Clínicas */}
      <section>
        <h2 style={{ fontSize: "18px", marginBottom: "20px" }}>Notas de Evolución e Historia Clínica</h2>

        {record.clinicalNotes.length === 0 ? (
          <p style={{ color: "var(--color-text-2)", fontStyle: "italic" }}>No hay notas clínicas registradas en el expediente.</p>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "25px" }}>
            {record.clinicalNotes.map((note) => (
              <article key={note.id} style={{ background: "var(--color-bg-1)", border: "1px solid var(--color-border)", borderRadius: "8px", padding: "20px" }}>
                <header style={{ display: "flex", justifyContent: "space-between", alignItems: "center", borderBottom: "1px solid var(--color-border)", paddingBottom: "10px", marginBottom: "15px" }}>
                  <div>
                    <strong style={{ fontSize: "15px", display: "block" }}>
                      Médico: {note.doctorName || "Especialista"}
                    </strong>
                    <span style={{ fontSize: "12px", color: "var(--color-text-3)" }}>
                      Fecha: {new Date(note.createdAt).toLocaleDateString("es-MX", { day: "2-digit", month: "long", year: "numeric", hour: "2-digit", minute: "2-digit" })}
                    </span>
                  </div>
                </header>

                {/* Signos Vitales */}
                {note.vitalSigns && (
                  <div style={{ background: "var(--color-bg-2)", padding: "10px 15px", borderRadius: "6px", display: "flex", flexWrap: "wrap", gap: "15px", marginBottom: "15px", fontSize: "12px" }}>
                    {note.vitalSigns.temperature && <div><strong>Temp:</strong> {note.vitalSigns.temperature} °C</div>}
                    {note.vitalSigns.bloodPressure && <div><strong>P.A.:</strong> {note.vitalSigns.bloodPressure} mmHg</div>}
                    {note.vitalSigns.heartRate && <div><strong>F.C.:</strong> {note.vitalSigns.heartRate} lpm</div>}
                    {note.vitalSigns.respiratoryRate && <div><strong>F.R.:</strong> {note.vitalSigns.respiratoryRate} rpm</div>}
                    {note.vitalSigns.weight && <div><strong>Peso:</strong> {note.vitalSigns.weight} kg</div>}
                    {note.vitalSigns.height && <div><strong>Talla:</strong> {note.vitalSigns.height} cm</div>}
                    {note.vitalSigns.oxygenSaturation && <div><strong>Sat. O2:</strong> {note.vitalSigns.oxygenSaturation} %</div>}
                  </div>
                )}

                {/* Secciones SOAP */}
                <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                  {note.subjective && (
                    <div>
                      <h4 style={{ fontSize: "12px", textTransform: "uppercase", color: "var(--color-primary)", margin: "0 0 4px 0" }}>Subjetivo</h4>
                      <p style={{ margin: 0, fontSize: "14px", lineHeight: "1.5", whiteSpace: "pre-wrap" }}>{note.subjective}</p>
                    </div>
                  )}
                  {note.objective && (
                    <div>
                      <h4 style={{ fontSize: "12px", textTransform: "uppercase", color: "var(--color-primary)", margin: "0 0 4px 0" }}>Objetivo</h4>
                      <p style={{ margin: 0, fontSize: "14px", lineHeight: "1.5", whiteSpace: "pre-wrap" }}>{note.objective}</p>
                    </div>
                  )}
                  {note.assessment && (
                    <div>
                      <h4 style={{ fontSize: "12px", textTransform: "uppercase", color: "var(--color-primary)", margin: "0 0 4px 0" }}>Diagnóstico y Evaluación</h4>
                      <p style={{ margin: 0, fontSize: "14px", lineHeight: "1.5", whiteSpace: "pre-wrap" }}>{note.assessment}</p>
                    </div>
                  )}
                  {note.plan && (
                    <div>
                      <h4 style={{ fontSize: "12px", textTransform: "uppercase", color: "var(--color-primary)", margin: "0 0 4px 0" }}>Plan de Tratamiento</h4>
                      <p style={{ margin: 0, fontSize: "14px", lineHeight: "1.5", whiteSpace: "pre-wrap" }}>{note.plan}</p>
                    </div>
                  )}
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      {/* Prescripciones */}
      {(record.prescriptions?.length ?? 0) > 0 && (
        <section style={{ marginTop: "30px" }}>
          <h2 style={{ fontSize: "18px", marginBottom: "20px", display: "flex", alignItems: "center", gap: "6px" }}>
            <IconPill size={20} />
            Prescripciones
          </h2>
          <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
            {record.prescriptions!.map((prescription, index) => (
              <article key={index} style={{ background: "var(--color-bg-1)", border: "1px solid var(--color-border)", borderRadius: "8px", padding: "16px 20px" }}>
                <span style={{ fontSize: "12px", color: "var(--color-text-3)" }}>
                  {prescription.createdAt
                    ? new Date(prescription.createdAt).toLocaleDateString("es-MX", { day: "2-digit", month: "long", year: "numeric" })
                    : ""}
                </span>
                <ul style={{ margin: "8px 0 0 0", paddingLeft: "18px", display: "flex", flexDirection: "column", gap: "6px" }}>
                  {prescription.items.map((item, itemIndex) => (
                    <li key={itemIndex} style={{ fontSize: "14px" }}>
                      <strong>{item.medicationName || "Medicamento"}</strong>
                      {[item.dosage, item.frequency, item.duration].filter(Boolean).length > 0 && (
                        <span> — {[item.dosage, item.frequency, item.duration].filter(Boolean).join(", ")}</span>
                      )}
                      {item.instructions && (
                        <span style={{ display: "block", color: "var(--color-text-2)", fontSize: "13px" }}>{item.instructions}</span>
                      )}
                    </li>
                  ))}
                </ul>
                {prescription.notes && (
                  <p style={{ margin: "10px 0 0 0", fontSize: "13px", color: "var(--color-text-2)", whiteSpace: "pre-wrap" }}>{prescription.notes}</p>
                )}
              </article>
            ))}
          </div>
        </section>
      )}

      {/* Estudios */}
      {(record.studies?.length ?? 0) > 0 && (
        <section style={{ marginTop: "30px" }} className="no-print">
          <h2 style={{ fontSize: "18px", marginBottom: "20px", display: "flex", alignItems: "center", gap: "6px" }}>
            <IconFolderOpen size={20} />
            Estudios
          </h2>
          {studyError && <p className="alert error">{studyError}</p>}
          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            {record.studies!.map((study) => (
              <div
                key={study.id}
                style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "12px", background: "var(--color-bg-1)", border: "1px solid var(--color-border)", borderRadius: "8px", padding: "12px 16px" }}
              >
                <div style={{ minWidth: 0 }}>
                  <strong style={{ fontSize: "14px", wordBreak: "break-word" }}>{study.filename}</strong>
                  <span style={{ display: "block", fontSize: "12px", color: "var(--color-text-3)" }}>
                    {[study.createdAt ? new Date(study.createdAt).toLocaleDateString("es-MX", { day: "2-digit", month: "short", year: "numeric" }) : "", formatBytes(study.sizeBytes)].filter(Boolean).join(" · ")}
                  </span>
                </div>
                <button
                  className="btn secondary"
                  type="button"
                  disabled={downloadingId === study.id}
                  onClick={() => void downloadStudy(study)}
                >
                  <IconDownload size={16} style={{ marginRight: "6px" }} />
                  {downloadingId === study.id ? "Descargando..." : "Descargar"}
                </button>
              </div>
            ))}
          </div>
        </section>
      )}

      <footer style={{ marginTop: "50px", textAlign: "center", borderTop: "1px solid var(--color-border)", paddingTop: "20px", fontSize: "12px", color: "var(--color-text-3)" }}>
        Este documento es confidencial y ha sido compartido para consulta clínica temporal.
      </footer>
    </div>
  );
}
