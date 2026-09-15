import { useEffect, useRef, useState } from "react";
import {
  IconArrowLeft,
  IconChevronLeft,
  IconChevronRight,
  IconDeviceFloppy,
  IconFileTypePdf,
  IconX,
  IconHistory,
  IconGitCompare
} from "@tabler/icons-react";
import { CANVAS_WIDTH, CONSENT_FIELD_IDS, DEFAULT_CONSENT_TEXT, NOM_SECTIONS, ensureConsentPage } from "@modules/records/constants/nomHistoryTemplate";
import { APP_INFO_CONSENT_FIELD_IDS, DEFAULT_APP_INFO_CONSENT_TEXT, ODONTOLOGY_FIELD_IDS, ODONTOLOGY_TEMPLATE_DEFAULT_ANSWERS } from "@modules/records/constants/nomOdontologyTemplate";
import { exportPagesToPdf, type PdfExportRange } from "@modules/records/lib/pdfExport";
import { ApiClientError, clinicsApi, getFriendlyError, medicalHistoryApi } from "@shared/api/api";
import { bloodTypeLabel, genderLabel, maritalStatusLabel } from "@modules/patients/constants/patientOptions";
import { getAge } from "@shared/utils/getAge";
import type { Address, PatientResponse } from "@modules/patients/types";
import type { ClinicResponse } from "@modules/clinics/types";
import { ClinicHeaderPreview } from "@modules/clinics/components/ClinicHeaderPreview";
import { SignaturePad } from "@shared/ui/SignaturePad";
import { HistoryVersionsModal } from "@modules/records/components/HistoryVersionsModal";
import {
  DEFAULT_FONT_FAMILY,
  DEFAULT_FONT_SIZE,
  DEFAULT_TEXT_ALIGN,
  fontCssStack,
  parseAttachments,
  parseAnswers,
  parseSchema,
  parseTableRows,
  serializeAnswers,
  serializeTableRows,
  type HistoryTemplateResponse,
  type TemplateElement,
  type MedicalHistoryVersionResponse
} from "@modules/records/types";
import { ConfirmDiscardModal, ConfirmSaveChangesModal } from "@shared/ui/ConfirmDiscardModal";
import { ExportPdfModal } from "@shared/ui/ExportPdfModal";
import { FileFieldEditor } from "@shared/ui/FileFieldEditor";
import { OdontogramField } from "@modules/records/components/OdontogramField";
import { TableFieldEditor } from "@shared/ui/TableFieldEditor";
import { formatClinicAddress } from "@modules/clinics/components/ClinicHeaderPreview";
import { calculateCpodIndex } from "@modules/records/constants/odontogram";

function groupElements(elements: TemplateElement[], enforceBands: boolean) {
  const byPosition = (a: TemplateElement, b: TemplateElement) => a.y - b.y || a.x - b.x;

  if (!enforceBands) {
    return [{ sectionId: undefined, title: undefined, elements: [...elements].sort(byPosition) }];
  }

  return [...NOM_SECTIONS]
    .sort((a, b) => a.order - b.order)
    .map((section) => ({
      sectionId: section.id,
      title: section.title,
      elements: elements.filter((element) => element.sectionId === section.id).sort(byPosition)
    }))
    .filter((group) => group.elements.length > 0);
}

function formatAddress(address?: Address): string {
  if (!address) return "";
  const streetLine = [address.street, address.outdoorNumber].filter(Boolean).join(" ");
  const parts = [
    streetLine || undefined,
    address.indoorNumber ? `Int. ${address.indoorNumber}` : undefined,
    address.colonia,
    address.municipality,
    address.state,
    address.zipCode ? `C.P. ${address.zipCode}` : undefined
  ].filter(Boolean);
  return parts.join(", ");
}

function buildPatientAutofill(patient: PatientResponse, availableFieldIds?: Set<string>): Record<string, string> {
  const autofill: Record<string, string> = {};
  const canFill = (fieldId: string) => !availableFieldIds || availableFieldIds.has(fieldId);

  Object.entries(ODONTOLOGY_TEMPLATE_DEFAULT_ANSWERS).forEach(([fieldId, value]) => {
    if (canFill(fieldId)) autofill[fieldId] = value;
  });

  const fullName = [patient.firstName, patient.lastNamePaterno, patient.lastNameMaterno].filter(Boolean).join(" ");
  if (fullName && canFill("nombre")) autofill.nombre = fullName;
  if (fullName && canFill(CONSENT_FIELD_IDS.signerName)) autofill[CONSENT_FIELD_IDS.signerName] = fullName;
  if (fullName && canFill(APP_INFO_CONSENT_FIELD_IDS.signerName)) autofill[APP_INFO_CONSENT_FIELD_IDS.signerName] = fullName;
  if (fullName && canFill(ODONTOLOGY_FIELD_IDS.treatmentConsentPatient)) autofill[ODONTOLOGY_FIELD_IDS.treatmentConsentPatient] = fullName;
  if (fullName && canFill(ODONTOLOGY_FIELD_IDS.contractPatient)) autofill[ODONTOLOGY_FIELD_IDS.contractPatient] = fullName;

  const gender = genderLabel(patient.gender);
  if ((gender === "Masculino" || gender === "Femenino") && canFill("sexo")) autofill.sexo = gender;

  const age = getAge(patient.dateOfBirth);
  if (age !== undefined && canFill("edad")) autofill.edad = String(age);
  if (age !== undefined && canFill(ODONTOLOGY_FIELD_IDS.treatmentConsentAge)) {
    autofill[ODONTOLOGY_FIELD_IDS.treatmentConsentAge] = String(age);
  }

  const address = formatAddress(patient.address);
  if (address && canFill("domicilio")) autofill.domicilio = address;

  if (patient.dateOfBirth && canFill("fechaNacimiento")) autofill.fechaNacimiento = patient.dateOfBirth.slice(0, 10);
  if (patient.phone && canFill("telefono")) autofill.telefono = patient.phone;

  const maritalStatus = maritalStatusLabel(patient.maritalStatus);
  if (maritalStatus && canFill("estadoCivil")) autofill.estadoCivil = maritalStatus;

  if (patient.occupation && canFill("ocupacion")) autofill.ocupacion = patient.occupation;

  const bloodType = bloodTypeLabel(patient.bloodType);
  if (bloodType && canFill("tipoSangreRh")) autofill.tipoSangreRh = bloodType;

  const now = new Date();
  const localDate = new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  if (canFill("fechaHistoriaDental")) autofill.fechaHistoriaDental = localDate;
  if (canFill(CONSENT_FIELD_IDS.text)) autofill[CONSENT_FIELD_IDS.text] = DEFAULT_CONSENT_TEXT;
  if (canFill(CONSENT_FIELD_IDS.date)) autofill[CONSENT_FIELD_IDS.date] = localDate;
  if (canFill(APP_INFO_CONSENT_FIELD_IDS.text)) autofill[APP_INFO_CONSENT_FIELD_IDS.text] = DEFAULT_APP_INFO_CONSENT_TEXT;
  if (canFill(APP_INFO_CONSENT_FIELD_IDS.date)) autofill[APP_INFO_CONSENT_FIELD_IDS.date] = localDate;
  if (canFill(APP_INFO_CONSENT_FIELD_IDS.noticeVersion)) {
    autofill[APP_INFO_CONSENT_FIELD_IDS.noticeVersion] = "Aviso de privacidad vigente de la clinica";
  }
  if (canFill(ODONTOLOGY_FIELD_IDS.declarationDate)) autofill[ODONTOLOGY_FIELD_IDS.declarationDate] = localDate;
  if (canFill(ODONTOLOGY_FIELD_IDS.treatmentConsentDate)) autofill[ODONTOLOGY_FIELD_IDS.treatmentConsentDate] = localDate;
  if (canFill(ODONTOLOGY_FIELD_IDS.contractDate)) autofill[ODONTOLOGY_FIELD_IDS.contractDate] = localDate;

  return autofill;
}

function normalizeCpodLabel(label: string) {
  return label.trim().toLowerCase();
}

function buildCpodTableAnswer(odontogramValue: string | undefined, previousCpodValue: string | undefined) {
  const cpod = calculateCpodIndex(odontogramValue);
  const previousRows = parseTableRows(previousCpodValue, 3);
  const finalByLabel = new Map(previousRows.map((row) => [normalizeCpodLabel(row[0] ?? ""), row[2] ?? ""]));
  const finalByIndex = previousRows.map((row) => row[2] ?? "");
  const finalFor = (label: string, index: number) => finalByLabel.get(normalizeCpodLabel(label)) ?? finalByIndex[index] ?? "";

  return serializeTableRows([
    ["Cariados", String(cpod.decayed), finalFor("Cariados", 0)],
    ["Perdidos", String(cpod.missing), finalFor("Perdidos", 1)],
    ["Obturados", String(cpod.filled), finalFor("Obturados", 2)],
    ["Total CPOD", String(cpod.total), finalFor("Total CPOD", 3)]
  ]);
}

function syncComputedCpod(
  nextAnswers: Record<string, string>,
  availableFieldIds: Set<string>
): Record<string, string> {
  const hasAutomaticCpod =
    availableFieldIds.has(ODONTOLOGY_FIELD_IDS.odontogram) && availableFieldIds.has(ODONTOLOGY_FIELD_IDS.cpod);
  if (!hasAutomaticCpod) return nextAnswers;

  const computedCpod = buildCpodTableAnswer(nextAnswers[ODONTOLOGY_FIELD_IDS.odontogram], nextAnswers[ODONTOLOGY_FIELD_IDS.cpod]);
  if (nextAnswers[ODONTOLOGY_FIELD_IDS.cpod] === computedCpod) return nextAnswers;
  return { ...nextAnswers, [ODONTOLOGY_FIELD_IDS.cpod]: computedCpod };
}

function formatCompareValue(element: TemplateElement, raw: string): string {
  if (element.type === "file") {
    const attachments = parseAttachments(raw);
    return attachments.map((attachment) => attachment.originalFilename || attachment.id).join("\n");
  }

  if (element.type === "table") {
    const columns = element.columns ?? [];
    const rows = parseTableRows(raw, columns.length).filter((row) => row.some((cell) => String(cell ?? "").trim()));
    return rows
      .map((row, rowIndex) => {
        const cells = row
          .map((cell, colIndex) => {
            const column = columns[colIndex] || `Columna ${colIndex + 1}`;
            return `${column}: ${String(cell ?? "") || "Sin valor"}`;
          })
          .join(" | ");
        return `Fila ${rowIndex + 1}: ${cells}`;
      })
      .join("\n");
  }

  if (element.type === "signature_patient" || element.type === "signature_doctor") {
    return raw ? "Firma capturada" : "";
  }

  if (element.type === "odontogram") {
    return raw ? "Odontograma capturado" : "";
  }

  return raw;
}

export function HistoryFormModal({
  patient,
  clinicId,
  template,
  onClose,
  onSaved,
  readOnly = false,
  embedded = false
}: {
  patient: PatientResponse;
  clinicId: string;
  template: HistoryTemplateResponse;
  onClose: () => void;
  onSaved: () => void;
  readOnly?: boolean;
  embedded?: boolean;
}) {
  const parsedSchema = parseSchema(template.schemaJson);
  const schema = parsedSchema.kind === "historia_clinica" ? { ...parsedSchema, pages: ensureConsentPage(parsedSchema.pages) } : parsedSchema;
  const isHistoriaClinica = schema.kind === "historia_clinica";
  const templateFieldIds = new Set(schema.pages.flatMap((page) => page.elements.map((element) => element.id)));
  const hasAutomaticCpod =
    templateFieldIds.has(ODONTOLOGY_FIELD_IDS.odontogram) && templateFieldIds.has(ODONTOLOGY_FIELD_IDS.cpod);
  const [pageIndex, setPageIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [showExport, setShowExport] = useState(false);
  const [showUnsavedPrompt, setShowUnsavedPrompt] = useState(false);
  const [showSaveConfirmPrompt, setShowSaveConfirmPrompt] = useState(false);
  const [error, setError] = useState("");
  const [clinic, setClinic] = useState<ClinicResponse | null>(null);
  const [clinicLoading, setClinicLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const initialAnswersRef = useRef<string>(serializeAnswers({}));

  const [versions, setVersions] = useState<MedicalHistoryVersionResponse[]>([]);
  const [showVersions, setShowVersions] = useState(false);
  const [compareMode, setCompareMode] = useState(false);
  const [compareVersionRange, setCompareVersionRange] = useState<{ vA: number; vB: number } | null>(null);
  const [compareAnswersA, setCompareAnswersA] = useState<Record<string, string>>({});
  const [compareAnswersB, setCompareAnswersB] = useState<Record<string, string>>({});
  const [viewVersionNumber, setViewVersionNumber] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    clinicsApi
      .get(clinicId)
      .then((response) => {
        if (!cancelled) setClinic(response);
      })
      .catch(() => {
        // si no se puede cargar la clínica, el encabezado simplemente se omite en la vista previa y el PDF
      })
      .finally(() => {
        if (!cancelled) setClinicLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [clinicId]);

  useEffect(() => {
    let cancelled = false;
    const autofill = buildPatientAutofill(patient, templateFieldIds);
    medicalHistoryApi
      .getByTemplate(patient.id, template.id, clinicId)
      .then((history) => {
        if (cancelled) return;
        const merged = syncComputedCpod({ ...autofill, ...parseAnswers(history.answersJson) }, templateFieldIds);
        setAnswers(merged);
        initialAnswersRef.current = serializeAnswers(merged);

        medicalHistoryApi
          .listVersions(patient.id, template.id, clinicId)
          .then((vList) => {
            if (!cancelled) setVersions(vList);
          })
          .catch(() => {});
      })
      .catch((caught) => {
        if (cancelled) return;
        if (caught instanceof ApiClientError && caught.status === 404) {
          const merged = syncComputedCpod(autofill, templateFieldIds);
          setAnswers(merged);
          initialAnswersRef.current = serializeAnswers(merged);
          return;
        }
        setError(getFriendlyError(caught));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [patient.id, template.id, clinicId]);

  const isDirty = () => serializeAnswers(answers) !== initialAnswersRef.current;

  const updateAnswer = (elementId: string, value: string) => {
    if (readOnly) return;
    setAnswers((prev) => {
      const next = { ...prev, [elementId]: value };
      if (elementId === ODONTOLOGY_FIELD_IDS.odontogram || elementId === ODONTOLOGY_FIELD_IDS.cpod) {
        return syncComputedCpod(next, templateFieldIds);
      }
      return next;
    });
  };

  const handleSave = async () => {
    setSaving(true);
    setError("");
    try {
      await medicalHistoryApi.save(patient.id, {
        clinicId,
        templateId: template.id,
        answersJson: serializeAnswers(answers)
      });
      onSaved();
      return true;
    } catch (caught) {
      setError(getFriendlyError(caught));
      return false;
    } finally {
      setSaving(false);
    }
  };

  const handleRequestClose = () => {
    if (isDirty()) {
      setShowUnsavedPrompt(true);
      return;
    }
    onClose();
  };

  const handleExport = async (range: PdfExportRange) => {
    setShowExport(false);
    setExporting(true);
    try {
      await exportPagesToPdf({
        title: template.name,
        pages: schema.pages,
        canvasWidthPx: CANVAS_WIDTH,
        answers,
        range,
        clinicalLayout: schema.kind === "historia_clinica",
        clinicInfo: clinic
          ? {
              name: clinic.name,
              legalName: clinic.legalName,
              addressLine: formatClinicAddress(clinic),
              phone: clinic.phone,
              email: clinic.email,
              logoUrl: clinic.logoUrl
            }
          : undefined
      });
    } finally {
      setExporting(false);
    }
  };

  const handleViewVersion = (versionAnswers: Record<string, string>, versionNumber: number) => {
    setAnswers(versionAnswers);
    setViewVersionNumber(versionNumber);
    setCompareMode(false);
  };

  const handleCompareVersions = (answersA: Record<string, string>, answersB: Record<string, string>, vA: number, vB: number) => {
    setCompareAnswersA(answersA);
    setCompareAnswersB(answersB);
    setCompareVersionRange({ vA, vB });
    setCompareMode(true);
    setViewVersionNumber(null);
  };

  const handleExitVersionMode = () => {
    setViewVersionNumber(null);
    setCompareMode(false);
    setCompareVersionRange(null);
    setLoading(true);
    const autofill = buildPatientAutofill(patient, templateFieldIds);
    medicalHistoryApi
      .getByTemplate(patient.id, template.id, clinicId)
      .then((history) => {
        const merged = syncComputedCpod({ ...autofill, ...parseAnswers(history.answersJson) }, templateFieldIds);
        setAnswers(merged);
      })
      .catch(() => {
        setAnswers(syncComputedCpod(autofill, templateFieldIds));
      })
      .finally(() => {
        setLoading(false);
      });
  };

  const isFormReadOnly = readOnly || viewVersionNumber !== null || compareMode;

  const currentPage = schema.pages[pageIndex];
  const groups = groupElements(currentPage?.elements ?? [], isHistoriaClinica && pageIndex === 0);
  const patientName = [patient.firstName, patient.lastNamePaterno, patient.lastNameMaterno].filter(Boolean).join(" ");

  const content = (
    <div className={embedded ? "panel full history-form-embedded-panel" : "modal-card modal-card-wide history-form-modal-card"} onClick={(event) => event.stopPropagation()}>
      <div className="panel-heading history-form-header">
          <div className="history-form-header-copy">
            <h2>{template.name}</h2>
            <p>
              {patientName || "Paciente"} {patient.curp ? ` - ${patient.curp}` : ""}
            </p>
          </div>
          <div className="topbar-actions">
            {versions.length > 0 && (
              <button className="btn ghost" type="button" onClick={() => setShowVersions(true)}>
                <IconHistory size={16} aria-hidden="true" />
                Historial de Cambios ({versions.length})
              </button>
            )}
            <button className="btn ghost" type="button" disabled={exporting} onClick={() => setShowExport(true)}>
              <IconFileTypePdf size={16} aria-hidden="true" />
              {exporting ? "Generando PDF..." : "Exportar PDF"}
            </button>
            {embedded ? (
              <button className="btn secondary" type="button" onClick={handleRequestClose}>
                <IconArrowLeft size={16} aria-hidden="true" />
                Volver
              </button>
            ) : (
              <button className="icon-btn" type="button" aria-label="Cerrar" onClick={handleRequestClose}>
                <IconX size={18} />
              </button>
            )}
          </div>
        </div>

        {(viewVersionNumber !== null || compareMode) && (
          <div style={{
            background: "rgba(239, 110, 0, 0.1)",
            borderBottom: "1px solid var(--color-border)",
            padding: "0.75rem 1.5rem",
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center"
          }}>
            <span style={{ fontSize: "0.9rem", fontWeight: 600 }}>
              {viewVersionNumber !== null
                ? `Viendo Versión ${viewVersionNumber} (Solo Lectura)`
                : `Comparando Cambios: Versión ${compareVersionRange?.vA} (Rojo) ↔ Versión ${compareVersionRange?.vB} (Verde)`
              }
            </span>
            <button className="btn secondary" type="button" onClick={handleExitVersionMode} style={{ padding: "0.25rem 0.75rem", fontSize: "0.8rem" }}>
              Salir de Modo Historial
            </button>
          </div>
        )}

        {schema.pages.length > 1 && (
          <div className="history-form-stepper">
            <div className="history-form-stepper-head">
              <button
                type="button"
                className="icon-btn"
                aria-label="Pagina anterior"
                disabled={pageIndex === 0}
                onClick={() => setPageIndex((index) => Math.max(0, index - 1))}
              >
                <IconChevronLeft size={16} aria-hidden="true" />
              </button>
              <span className="history-form-stepper-label">
                Pagina {pageIndex + 1} de {schema.pages.length}
              </span>
              <button
                type="button"
                className="icon-btn"
                aria-label="Pagina siguiente"
                disabled={pageIndex === schema.pages.length - 1}
                onClick={() => setPageIndex((index) => Math.min(schema.pages.length - 1, index + 1))}
              >
                <IconChevronRight size={16} aria-hidden="true" />
              </button>
            </div>
            <div className="history-form-stepper-track">
              {schema.pages.map((page, index) => (
                <span
                  key={page.id}
                  className={`history-form-stepper-segment ${index <= pageIndex ? "filled" : ""}`}
                />
              ))}
            </div>
          </div>
        )}

        <div className="history-form-shell">
          <div className="history-form-scroll">
            {loading ? (
              <p className="history-form-status">Cargando...</p>
            ) : (
              <div className="history-form">
                {groups.map((group) => (
                  <div className="history-form-group" key={group.sectionId ?? "root"}>
                    {group.title && <h4>{group.title}</h4>}
                    {group.elements.map((element) => {
                      const labelStyle: React.CSSProperties = {
                        fontFamily: fontCssStack(element.fontFamily ?? DEFAULT_FONT_FAMILY),
                        fontSize: element.fontSize ?? DEFAULT_FONT_SIZE,
                        fontWeight: element.bold ? 700 : 500,
                        textAlign: element.align ?? DEFAULT_TEXT_ALIGN,
                        color: element.color || undefined,
                        backgroundColor: element.backgroundColor || "transparent"
                      };

                      const valA = compareAnswersA[element.id] ?? "";
                      const valB = compareAnswersB[element.id] ?? "";

                      const renderDiff = (vA: string, vB: string) => {
                        const emptyValue = <span style={{ opacity: 0.55 }}>Sin valor</span>;
                        if (vA === vB) {
                          return (
                            <div style={{ padding: "0.5rem", background: "var(--color-bg-light, rgba(0,0,0,0.02))", border: "1px solid var(--color-border)", borderRadius: "4px", fontSize: "0.9rem", whiteSpace: "pre-wrap", wordBreak: "break-word" }}>
                              {vA || emptyValue}
                            </div>
                          );
                        }
                        return (
                          <div style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
                            <div style={{ padding: "0.5rem", background: "rgba(239, 68, 68, 0.1)", color: "#ef4444", border: "1px dashed #ef4444", borderRadius: "4px", textDecoration: vA ? "line-through" : "none", fontSize: "0.9rem", whiteSpace: "pre-wrap", wordBreak: "break-word" }}>
                              - {vA || emptyValue}
                            </div>
                            <div style={{ padding: "0.5rem", background: "rgba(34, 197, 94, 0.1)", color: "#22c55e", border: "1px dashed #22c55e", borderRadius: "4px", fontSize: "0.9rem", whiteSpace: "pre-wrap", wordBreak: "break-word" }}>
                              + {vB || emptyValue}
                            </div>
                          </div>
                        );
                      };

                      const renderComparePanel = (previous: React.ReactNode, current: React.ReactNode, stacked = false) => (
                        <div style={{ display: "grid", gridTemplateColumns: stacked ? "1fr" : "repeat(auto-fit, minmax(280px, 1fr))", gap: "0.75rem" }}>
                          <div style={{ border: "1px dashed #ef4444", borderRadius: "6px", background: "rgba(239, 68, 68, 0.06)", padding: "0.75rem", minWidth: 0 }}>
                            <div style={{ color: "#ef4444", fontSize: "0.78rem", fontWeight: 700, marginBottom: "0.5rem" }}>
                              Versión {compareVersionRange?.vA ?? ""} (anterior)
                            </div>
                            {previous}
                          </div>
                          <div style={{ border: "1px dashed #22c55e", borderRadius: "6px", background: "rgba(34, 197, 94, 0.06)", padding: "0.75rem", minWidth: 0 }}>
                            <div style={{ color: "#16833b", fontSize: "0.78rem", fontWeight: 700, marginBottom: "0.5rem" }}>
                              Versión {compareVersionRange?.vB ?? ""} (nueva)
                            </div>
                            {current}
                          </div>
                        </div>
                      );

                      const renderCompareContent = () => {
                        if (element.type === "clinic_header") {
                          return <ClinicHeaderPreview clinic={clinic} loading={clinicLoading} />;
                        }

                        if (element.type === "odontogram" && valA !== valB) {
                          return renderComparePanel(
                            <OdontogramField value={valA} onChange={() => undefined} readOnly />,
                            <OdontogramField value={valB} onChange={() => undefined} readOnly />,
                            true
                          );
                        }

                        if ((element.type === "signature_patient" || element.type === "signature_doctor") && valA !== valB) {
                          const signatureLabel = element.type === "signature_patient" ? "Firma del paciente" : "Firma del medico";
                          return renderComparePanel(
                            <SignaturePad
                              label={signatureLabel}
                              signerName={element.type === "signature_patient" ? compareAnswersA.nombre : undefined}
                              value={valA}
                              onChange={() => undefined}
                              storageKey={`compare:${clinicId}:${patient.id}:${template.id}:${element.id}:${compareVersionRange?.vA ?? "a"}`}
                              disabled
                            />,
                            <SignaturePad
                              label={signatureLabel}
                              signerName={element.type === "signature_patient" ? compareAnswersB.nombre : undefined}
                              value={valB}
                              onChange={() => undefined}
                              storageKey={`compare:${clinicId}:${patient.id}:${template.id}:${element.id}:${compareVersionRange?.vB ?? "b"}`}
                              disabled
                            />
                          );
                        }

                        return renderDiff(formatCompareValue(element, valA), formatCompareValue(element, valB));
                      };

                      const renderFieldContent = () => {
                        if (compareMode) {
                          return renderCompareContent();
                        }

                        if (element.type === "textarea") {
                          return (
                            <textarea
                              value={answers[element.id] ?? ""}
                              disabled={isFormReadOnly}
                              onChange={(event) => updateAnswer(element.id, event.target.value)}
                            />
                          );
                        } else if (element.type === "table") {
                          const isAutomaticCpodField = hasAutomaticCpod && element.id === ODONTOLOGY_FIELD_IDS.cpod;
                          return (
                            <TableFieldEditor
                              columns={element.columns ?? []}
                              value={answers[element.id]}
                              onChange={(next) => updateAnswer(element.id, next)}
                              readOnly={isFormReadOnly}
                              readOnlyColumns={isAutomaticCpodField ? [0, 1] : undefined}
                              disableRowActions={isAutomaticCpodField}
                            />
                          );
                        } else if (element.type === "file") {
                          return (
                            <FileFieldEditor
                              patientId={patient.id}
                              clinicId={clinicId}
                              elementId={element.id}
                              value={answers[element.id]}
                              onChange={(next) => updateAnswer(element.id, next)}
                              readOnly={isFormReadOnly}
                            />
                          );
                        } else if (element.type === "odontogram") {
                          return (
                            <OdontogramField
                              value={answers[element.id]}
                              onChange={(next) => updateAnswer(element.id, next)}
                              readOnly={isFormReadOnly}
                            />
                          );
                        } else if (element.type === "clinic_header") {
                          return <ClinicHeaderPreview clinic={clinic} loading={clinicLoading} />;
                        } else if (element.type === "signature_patient") {
                          return (
                            <SignaturePad
                              label="Firma del paciente"
                              signerName={answers.nombre}
                              value={answers[element.id]}
                              onChange={(next) => updateAnswer(element.id, next)}
                              storageKey={`${clinicId}:${patient.id}:${template.id}:${element.id}`}
                              disabled={isFormReadOnly}
                            />
                          );
                        } else if (element.type === "signature_doctor") {
                          return (
                            <SignaturePad
                              label="Firma del medico"
                              value={answers[element.id]}
                              onChange={(next) => updateAnswer(element.id, next)}
                              storageKey={`${clinicId}:${patient.id}:${template.id}:${element.id}`}
                              disabled={isFormReadOnly}
                            />
                          );
                        } else if (element.type === "select") {
                          return (
                            <select
                              value={answers[element.id] ?? ""}
                              disabled={isFormReadOnly}
                              onChange={(event) => updateAnswer(element.id, event.target.value)}
                            >
                              <option value="" disabled>
                                Selecciona una opcion
                              </option>
                              {(element.options ?? []).map((option) => (
                                <option key={option} value={option}>
                                  {option}
                                </option>
                              ))}
                            </select>
                          );
                        } else {
                          return (
                            <input
                              type={element.type === "number" ? "number" : element.type === "date" ? "date" : "text"}
                              value={answers[element.id] ?? ""}
                              disabled={isFormReadOnly}
                              onChange={(event) => updateAnswer(element.id, event.target.value)}
                            />
                          );
                        }
                      };

                      return (
                        <label className="field" key={element.id}>
                          <span style={labelStyle}>{element.label}</span>
                          {renderFieldContent()}
                        </label>
                      );
                    })}
                  </div>
                ))}
              </div>
            )}
            {error && <p className="alert error history-form-error">{error}</p>}
          </div>
          <div className="form-actions history-form-footer">
            {isFormReadOnly ? (
              <span className="badge neutral">Solo lectura: no puedes editar este apartado</span>
            ) : (
              <button className="btn primary" type="button" disabled={loading || saving} onClick={handleSave}>
                <IconDeviceFloppy size={18} aria-hidden="true" />
                {saving ? "Guardando" : "Guardar"}
              </button>
            )}
          </div>
        </div>
      </div>
    );

    const modals = (
      <>
        {showExport && (
          <ExportPdfModal
            totalPages={schema.pages.length}
            currentPage={pageIndex + 1}
            onCancel={() => setShowExport(false)}
            onConfirm={handleExport}
          />
        )}
        {showUnsavedPrompt && (
          <ConfirmDiscardModal
            saving={saving}
            onCancel={() => setShowUnsavedPrompt(false)}
            onDiscard={onClose}
            onSaveRequest={() => {
              setShowUnsavedPrompt(false);
              setShowSaveConfirmPrompt(true);
            }}
          />
        )}
        {showSaveConfirmPrompt && (
          <ConfirmSaveChangesModal
            saving={saving}
            onCancel={() => {
              if (!saving) setShowSaveConfirmPrompt(false);
            }}
            onConfirm={async () => {
              const saved = await handleSave();
              if (saved) setShowSaveConfirmPrompt(false);
            }}
          />
        )}
        {showVersions && (
          <HistoryVersionsModal
            versions={versions}
            patientId={patient.id}
            templateId={template.id}
            clinicId={clinicId}
            onClose={() => setShowVersions(false)}
            onViewVersion={handleViewVersion}
            onCompareVersions={handleCompareVersions}
          />
        )}
      </>
    );

    if (embedded) {
      return (
        <>
          {content}
          {modals}
        </>
      );
    }

    return (
      <div className="modal-overlay" onClick={handleRequestClose}>
        {content}
        {modals}
      </div>
    );
}
