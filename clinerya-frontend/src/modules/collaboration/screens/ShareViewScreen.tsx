import { useEffect, useState } from "react";
import { IconPrinter, IconAlertTriangle, IconFileText } from "@tabler/icons-react";
import { collaborationApi, getFriendlyError } from "@shared/api/api";

interface PublicSharedRecord {
  patientId: string;
  patientFullName: string;
  patientCurp?: string;
  patientPhone?: string;
  patientEmail?: string;
  clinicName: string;
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

export function ShareViewScreen() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [record, setRecord] = useState<PublicSharedRecord | null>(null);

  const token = new URLSearchParams(window.location.search).get("token") || "";

  useEffect(() => {
    if (!token) {
      setError("Token de consulta no proporcionado en el enlace.");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError("");

    collaborationApi.getSharedRecord(token)
      .then((data) => {
        setRecord(data);
      })
      .catch((caught) => {
        setError(getFriendlyError(caught));
      })
      .finally(() => {
        setLoading(false);
      });
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

      <footer style={{ marginTop: "50px", textAlign: "center", borderTop: "1px solid var(--color-border)", paddingTop: "20px", fontSize: "12px", color: "var(--color-text-3)" }}>
        Este documento es confidencial y ha sido compartido para consulta clínica temporal.
      </footer>
    </div>
  );
}
