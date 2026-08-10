import { useEffect, useState } from "react";
import { IconPlus, IconPrinter, IconTrash } from "@tabler/icons-react";
import type { PatientResponse } from "@modules/patients/types";
import type { IssuePrescriptionRequest, Prescription, PrescriptionItem } from "@modules/records/prescriptionTypes";
import { getFriendlyError, prescriptionsApi } from "@shared/api/api";

export function PrescriptionSection({
  clinicId,
  patient,
  appointmentId,
  doctorId
}: {
  clinicId: string;
  patient: PatientResponse;
  appointmentId?: string;
  doctorId?: string;
}) {
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notes, setNotes] = useState("");
  const [selectedPrescription, setSelectedPrescription] = useState<Prescription | null>(null);
  const [items, setItems] = useState<Omit<PrescriptionItem, "id">[]>([
    { medicationName: "", dosage: "", frequency: "", duration: "", instructions: "" }
  ]);

  useEffect(() => {
    if (!clinicId || !patient.id) return;
    setLoading(true);
    prescriptionsApi
      .listByPatient(clinicId, patient.id)
      .then((data) => {
        setPrescriptions(data);
        if (data.length > 0) {
          setSelectedPrescription(data[0]);
        }
      })
      .catch((err) => setError(getFriendlyError(err)))
      .finally(() => setLoading(false));
  }, [clinicId, patient.id]);

  const handleAddItem = () => {
    setItems((prev) => [...prev, { medicationName: "", dosage: "", frequency: "", duration: "", instructions: "" }]);
  };

  const handleRemoveItem = (index: number) => {
    setItems((prev) => prev.filter((_, i) => i !== index));
  };

  const handleItemChange = (index: number, field: keyof Omit<PrescriptionItem, "id">, value: string) => {
    setItems((prev) =>
      prev.map((item, i) => (i === index ? { ...item, [field]: value } : item))
    );
  };

  const handleIssue = async () => {
    setError("");
    const validItems = items.filter((i) => i.medicationName.trim().length > 0);
    if (validItems.length === 0) {
      setError("Ingresa al menos un medicamento válido para la receta.");
      return;
    }

    try {
      setSaving(true);
      const req: IssuePrescriptionRequest = {
        patientId: patient.id,
        doctorId,
        appointmentId,
        notes,
        items: validItems
      };
      const created = await prescriptionsApi.issue(clinicId, req);
      setPrescriptions((prev) => [created, ...prev]);
      setSelectedPrescription(created);
      setNotes("");
      setItems([{ medicationName: "", dosage: "", frequency: "", duration: "", instructions: "" }]);
    } catch (err) {
      setError(getFriendlyError(err));
    } finally {
      setSaving(false);
    }
  };

  const handlePrint = (p: Prescription) => {
    const patientName = [patient.firstName, patient.lastNamePaterno, patient.lastNameMaterno].filter(Boolean).join(" ");
    const printWindow = window.open("", "_blank");
    if (!printWindow) return;

    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>Receta Médica - ${patientName}</title>
          <style>
            body { font-family: sans-serif; padding: 40px; color: #1e293b; }
            .header { border-bottom: 2px solid #0284c7; padding-bottom: 12px; margin-bottom: 24px; }
            .header h1 { margin: 0; color: #0284c7; font-size: 24px; }
            .meta { margin-bottom: 20px; font-size: 14px; }
            .rx-title { font-size: 20px; font-weight: bold; margin-top: 24px; color: #0369a1; }
            table { width: 100%; border-collapse: collapse; margin-top: 12px; }
            th, td { border-bottom: 1px solid #e2e8f0; text-align: left; padding: 10px 8px; font-size: 14px; }
            th { background-color: #f8fafc; }
            .notes { margin-top: 24px; font-style: italic; font-size: 13px; color: #475569; }
            .footer { margin-top: 60px; text-align: center; border-top: 1px dashed #cbd5e1; padding-top: 12px; font-size: 12px; color: #64748b; }
          </style>
        </head>
        <body>
          <div class="header">
            <h1>Clinerya - Receta Médica</h1>
          </div>
          <div class="meta">
            <strong>Paciente:</strong> ${patientName}<br/>
            <strong>Fecha:</strong> ${new Date(p.createdAt).toLocaleDateString("es-MX")}<br/>
            <strong>Folio:</strong> ${p.id.slice(0, 8).toUpperCase()}
          </div>
          <div class="rx-title">Rp / Prescripción Médica</div>
          <table>
            <thead>
              <tr>
                <th>Medicamento</th>
                <th>Dosis</th>
                <th>Frecuencia</th>
                <th>Duración</th>
                <th>Indicaciones</th>
              </tr>
            </thead>
            <tbody>
              ${p.items
                .map(
                  (item) => `
                <tr>
                  <td><strong>${item.medicationName}</strong></td>
                  <td>${item.dosage || "-"}</td>
                  <td>${item.frequency || "-"}</td>
                  <td>${item.duration || "-"}</td>
                  <td>${item.instructions || "-"}</td>
                </tr>
              `
                )
                .join("")}
            </tbody>
          </table>
          ${p.notes ? `<div class="notes"><strong>Notas adicionales:</strong> ${p.notes}</div>` : ""}
          <div class="footer">
            <p>Firma y Sello del Médico Evaluador</p>
          </div>
          <script>window.print();</script>
        </body>
      </html>
    `);
    printWindow.document.close();
  };

  return (
    <div style={{ display: "grid", gridTemplateColumns: "1fr 340px", gap: "20px", marginTop: "16px" }}>
      <div className="panel full">
        <div className="panel-heading">
          <h3>Emitir Nueva Receta Médica</h3>
        </div>

        {error && <div className="banner danger">{error}</div>}

        <div style={{ display: "flex", flexDirection: "column", gap: "12px", padding: "16px" }}>
          <div>
            <label style={{ fontSize: "13px", fontWeight: 600 }}>Indicaciones / Diagnóstico de soporte:</label>
            <input
              type="text"
              placeholder="Ej. Tratamiento post-operatorio analgésico y antibiótico"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              style={{ width: "100%", padding: "8px", marginTop: "4px" }}
            />
          </div>

          <h4 style={{ margin: "12px 0 4px 0", color: "#334155" }}>Medicamentos Prescritos</h4>

          {items.map((item, index) => (
            <div
              key={index}
              style={{
                display: "grid",
                gridTemplateColumns: "2fr 1fr 1fr 1fr 2fr auto",
                gap: "8px",
                alignItems: "center",
                backgroundColor: "#f8fafc",
                padding: "8px",
                borderRadius: "6px",
                border: "1px solid #e2e8f0"
              }}
            >
              <input
                type="text"
                placeholder="Nombre del Medicamento *"
                value={item.medicationName}
                onChange={(e) => handleItemChange(index, "medicationName", e.target.value)}
              />
              <input
                type="text"
                placeholder="Dosis (Ej. 500mg)"
                value={item.dosage}
                onChange={(e) => handleItemChange(index, "dosage", e.target.value)}
              />
              <input
                type="text"
                placeholder="Frecuencia (c/8h)"
                value={item.frequency}
                onChange={(e) => handleItemChange(index, "frequency", e.target.value)}
              />
              <input
                type="text"
                placeholder="Duración (7 días)"
                value={item.duration}
                onChange={(e) => handleItemChange(index, "duration", e.target.value)}
              />
              <input
                type="text"
                placeholder="Indicaciones (Tomar con alimentos)"
                value={item.instructions}
                onChange={(e) => handleItemChange(index, "instructions", e.target.value)}
              />
              {items.length > 1 && (
                <button
                  type="button"
                  className="button secondary icon-only"
                  onClick={() => handleRemoveItem(index)}
                  title="Eliminar medicamento"
                >
                  <IconTrash size={16} />
                </button>
              )}
            </div>
          ))}

          <div style={{ display: "flex", justifyContent: "space-between", marginTop: "12px" }}>
            <button type="button" className="button secondary" onClick={handleAddItem}>
              <IconPlus size={16} /> Agregar otro medicamento
            </button>
            <button type="button" className="button primary" onClick={handleIssue} disabled={saving}>
              {saving ? "Emitiendo..." : "Emitir Receta Médica"}
            </button>
          </div>
        </div>
      </div>

      <div className="panel full">
        <div className="panel-heading">
          <h3>Historial de Recetas</h3>
        </div>
        <div style={{ padding: "16px" }}>
          {loading && <p style={{ fontSize: "13px", color: "#64748b" }}>Cargando recetas...</p>}
          {!loading && prescriptions.length === 0 && (
            <p style={{ fontSize: "13px", color: "#64748b" }}>Este paciente no tiene recetas previas.</p>
          )}
          {prescriptions.map((p) => (
            <div
              key={p.id}
              onClick={() => setSelectedPrescription(p)}
              style={{
                padding: "10px",
                borderRadius: "6px",
                border: selectedPrescription?.id === p.id ? "2px solid #0284c7" : "1px solid #e2e8f0",
                marginBottom: "8px",
                cursor: "pointer",
                backgroundColor: selectedPrescription?.id === p.id ? "#f0f9ff" : "#fff"
              }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", fontWeight: 600, fontSize: "13px" }}>
                <span>Folio {p.id.slice(0, 6)}</span>
                <span>{new Date(p.createdAt).toLocaleDateString("es-MX")}</span>
              </div>
              <p style={{ fontSize: "12px", color: "#475569", margin: "4px 0" }}>
                {p.items.length} medicamento(s): {p.items.map((i) => i.medicationName).join(", ")}
              </p>
              <button
                type="button"
                className="button secondary"
                style={{ width: "100%", marginTop: "6px", fontSize: "12px" }}
                onClick={(e) => {
                  e.stopPropagation();
                  handlePrint(p);
                }}
              >
                <IconPrinter size={14} /> Imprimir Receta
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
