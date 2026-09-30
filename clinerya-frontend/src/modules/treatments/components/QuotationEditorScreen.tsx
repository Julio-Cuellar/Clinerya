import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  IconArrowLeft,
  IconChevronDown,
  IconChevronUp,
  IconDeviceFloppy,
  IconPlus,
  IconTrash,
  IconFileTypePdf,
  IconPhoto
} from "@tabler/icons-react";
import {
  attachmentsApi,
  getFriendlyError,
  historyTemplatesApi,
  materialsApi,
  medicalHistoryApi,
  quotationsApi,
  sessionStore,
  clinicsApi
} from "@shared/api/api";
import {
  parseAnswers,
  parseSchema,
  serializeAttachments,
  parseAttachments,
  parseTableRows,
  DEFAULT_FONT_FAMILY,
  DEFAULT_FONT_SIZE,
  DEFAULT_TEXT_ALIGN,
  fontCssStack,
  type HistoryTemplateResponse,
  type MedicalHistoryResponse,
  type TemplateElement
} from "@modules/records/types";
import type { MaterialLineRequest, QuotationItemRequest, QuotationResponse } from "@modules/treatments/quotationTypes";
import type { TreatmentCatalogItemResponse } from "@modules/treatments/types";
import type { MaterialResponse } from "@modules/inventory/types";
import type { PatientResponse } from "@modules/patients/types";
import { DEFAULT_CLINIC_PROFILE, type ClinicProfile } from "@shared/utils/clinicProfile";
import type { ClinicResponse } from "@modules/clinics/types";
import { FileFieldEditor } from "@shared/ui/FileFieldEditor";
import { TableFieldEditor } from "@shared/ui/TableFieldEditor";
import { OdontogramField } from "@modules/records/components/OdontogramField";
import { ClinicHeaderPreview } from "@modules/clinics/components/ClinicHeaderPreview";
import { SignaturePad } from "@shared/ui/SignaturePad";
import { NOM_SECTIONS } from "@modules/records/constants/nomHistoryTemplate";

const STUDIES_ELEMENT_ID = "patient_studies";
const SIMPLE_FIELD_TYPES = new Set(["text", "textarea", "number", "date", "select"]);
const SIDE_PANEL_MIN_WIDTH = 300;
const SIDE_PANEL_MAX_WIDTH = 760;
const SIDE_PANEL_MAIN_MIN_WIDTH = 520;
const SIDE_PANEL_MAX_RIGHT_OFFSET = 240;
const HISTORY_PANEL_MIN_HEIGHT = 320;
const HISTORY_PANEL_MAX_HEIGHT = 820;
const STUDIES_PANEL_MIN_HEIGHT = 180;
const STUDIES_PANEL_MAX_HEIGHT = 560;

type QuotationSidePanelKey = "history" | "studies";
type QuotationResizeDirection = "left" | "right";

interface QuotationSidePanelSize {
  width: number;
  height: number;
  offsetX: number;
}

interface EditableMaterialLine {
  key: string;
  materialId: string;
  materialName: string;
  estimatedQuantity: string;
  manualUnitCost: string;
}

interface EditableItem {
  key: string;
  catalogItemId: string;
  description: string;
  toothNumber: string;
  laborCharge: string;
  discountPercentage: string;
  materials: EditableMaterialLine[];
}

const currencyFormatter = new Intl.NumberFormat("es-MX", { style: "currency", currency: "MXN" });

function clampNumber(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

function emptyMaterialLine(): EditableMaterialLine {
  return {
    key: `mat_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    materialId: "",
    materialName: "",
    estimatedQuantity: "1",
    manualUnitCost: "0"
  };
}

function emptyItem(): EditableItem {
  return {
    key: `item_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    catalogItemId: "",
    description: "",
    toothNumber: "",
    laborCharge: "0",
    discountPercentage: "",
    materials: []
  };
}

function itemsFromQuotation(quotation?: QuotationResponse): EditableItem[] {
  if (!quotation || quotation.items.length === 0) return [emptyItem()];
  return quotation.items.map((item) => ({
    key: item.id,
    catalogItemId: item.catalogItemId ?? "",
    description: item.description,
    toothNumber: item.toothNumber ? String(item.toothNumber) : "",
    laborCharge: String(item.laborCharge),
    discountPercentage: item.discountPercentage ? String(item.discountPercentage) : "",
    materials: item.materials.map((material) => ({
      key: material.id,
      materialId: material.materialId ?? "",
      materialName: material.materialId ? "" : material.materialName,
      estimatedQuantity: String(material.estimatedQuantity),
      manualUnitCost: material.materialId ? "0" : String(material.unitCostAtQuote)
    }))
  }));
}

function materialCost(line: EditableMaterialLine, materialsById: Map<string, MaterialResponse>): number {
  const quantity = Number(line.estimatedQuantity) || 0;
  if (line.materialId) {
    const material = materialsById.get(line.materialId);
    return quantity * (material?.unitCost ?? 0);
  }
  return quantity * (Number(line.manualUnitCost) || 0);
}

function materialsTotalFor(item: EditableItem, materialsById: Map<string, MaterialResponse>): number {
  return item.materials.reduce((sum, line) => sum + materialCost(line, materialsById), 0);
}

function subtotalFor(item: EditableItem, materialsById: Map<string, MaterialResponse>): number {
  const labor = Number(item.laborCharge) || 0;
  const discount = Number(item.discountPercentage) || 0;
  return (labor + materialsTotalFor(item, materialsById)) * (1 - discount / 100);
}

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

function HistoryPanel({
  patientId,
  clinicId,
  size,
  onResizeStart
}: {
  patientId: string;
  clinicId: string;
  size: QuotationSidePanelSize;
  onResizeStart: (event: React.PointerEvent<HTMLButtonElement>, direction: QuotationResizeDirection) => void;
}) {
  const [templates, setTemplates] = useState<HistoryTemplateResponse[]>([]);
  const [histories, setHistories] = useState<MedicalHistoryResponse[]>([]);
  const [selectedTemplateId, setSelectedTemplateId] = useState("");
  const [pageIndex, setPageIndex] = useState(0);
  const [collapsed, setCollapsed] = useState(false);
  const [clinic, setClinic] = useState<ClinicResponse | null>(null);
  const [clinicLoading, setClinicLoading] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    clinicsApi
      .get(clinicId)
      .then((response) => {
        if (!cancelled) setClinic(response);
      })
      .catch(() => {})
      .finally(() => {
        if (!cancelled) setClinicLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [clinicId]);

  useEffect(() => {
    setLoading(true);
    setError("");
    Promise.all([historyTemplatesApi.list(clinicId), medicalHistoryApi.listByPatient(patientId, clinicId)])
      .then(([templateList, historyList]) => {
        setTemplates(templateList);
        setHistories(historyList);
        if (historyList.length > 0) {
          setSelectedTemplateId(historyList[0].templateId);
        }
      })
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  }, [patientId, clinicId]);

  const availableHistories = histories.filter((history) => templates.some((template) => template.id === history.templateId));
  const selectedHistory = availableHistories.find((history) => history.templateId === selectedTemplateId);
  const selectedTemplate = templates.find((template) => template.id === selectedTemplateId);

  // Reset pageIndex when template changes
  useEffect(() => {
    setPageIndex(0);
  }, [selectedTemplateId]);

  const schema = useMemo(() => {
    if (!selectedTemplate) return null;
    return parseSchema(selectedTemplate.schemaJson);
  }, [selectedTemplate]);

  const answers = useMemo(() => {
    if (!selectedHistory) return {};
    return parseAnswers(selectedHistory.answersJson);
  }, [selectedHistory]);

  const currentPage = schema?.pages?.[pageIndex];
  const isHistoriaClinica = schema?.kind === "historia_clinica";
  const groups = useMemo(() => {
    return groupElements(currentPage?.elements ?? [], isHistoriaClinica && pageIndex === 0);
  }, [currentPage, isHistoriaClinica, pageIndex]);

  const handleViewFile = async (attachmentId: string) => {
    try {
      const blob = await attachmentsApi.downloadBlob(patientId, attachmentId);
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank", "noopener,noreferrer");
    } catch (caught) {
      console.error(caught);
    }
  };

  return (
    <article
      className={`panel quotation-side-panel quotation-resizable-panel quotation-history-panel ${collapsed ? "collapsed" : ""}`}
      style={{
        width: `${size.width}px`,
        height: collapsed ? undefined : `${size.height}px`,
        marginRight: collapsed ? undefined : `-${size.offsetX}px`
      }}
    >
      <div className="panel-heading quotation-side-panel-heading">
        <div className="quotation-side-panel-title">
          <h2 style={{ fontSize: '15px' }}>Historia clínica</h2>
          <button
            type="button"
            className="quotation-panel-toggle"
            onClick={() => setCollapsed((value) => !value)}
            aria-expanded={!collapsed}
            title={collapsed ? "Expandir historia clínica" : "Contraer historia clínica"}
          >
            {collapsed ? <IconChevronDown size={16} aria-hidden="true" /> : <IconChevronUp size={16} aria-hidden="true" />}
          </button>
        </div>
        {!collapsed && !loading && availableHistories.length > 0 && (
          <select 
            value={selectedTemplateId} 
            onChange={(event) => setSelectedTemplateId(event.target.value)}
            style={{ width: '100%', padding: '6px', fontSize: '13px', borderRadius: '4px' }}
          >
            {availableHistories.map((history) => {
              const template = templates.find((item) => item.id === history.templateId);
              return (
                <option key={history.id} value={history.templateId}>
                  {template?.name ?? "Historia clínica"}
                </option>
              );
            })}
          </select>
        )}
      </div>

      {!collapsed && loading && <p className="panel-subtitle">Cargando...</p>}
      {!collapsed && !loading && error && <p className="alert error">{error}</p>}
      {!collapsed && !loading && !error && availableHistories.length === 0 && (
        <p className="panel-subtitle">Este paciente no tiene historias clínicas registradas.</p>
      )}

      {!collapsed && !loading && selectedHistory && schema && (
        <div className="history-panel-body" style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)', minHeight: 0, flex: 1 }}>
          {/* Tabs para paginación */}
          {schema.pages.length > 1 && (
            <div className="canvas-page-tabs" style={{ padding: '0 0 var(--space-2) 0', borderBottom: '1px solid var(--color-border)' }}>
              {schema.pages.map((page, index) => (
                <div key={page.id} className={`canvas-page-tab ${index === pageIndex ? "active" : ""}`} style={{ padding: '2px 8px' }}>
                  <button type="button" onClick={() => setPageIndex(index)} style={{ padding: '2px 0' }}>
                    Pág {index + 1}
                  </button>
                </div>
              ))}
            </div>
          )}

          {/* Renderizado de elementos de la página actual */}
          <div className="history-form" style={{ overflowY: 'auto', paddingRight: '4px', minHeight: 0, flex: 1 }}>
            {groups.map((group) => (
              <div className="history-form-group" key={group.sectionId ?? "root"} style={{ marginBottom: 'var(--space-4)' }}>
                {group.title && <h4 style={{ fontSize: '13px', borderBottom: '1px solid var(--color-border)', paddingBottom: '4px', marginBottom: '8px' }}>{group.title}</h4>}
                {group.elements.map((element) => {
                  const labelStyle: React.CSSProperties = {
                    fontFamily: fontCssStack(element.fontFamily ?? DEFAULT_FONT_FAMILY),
                    fontSize: element.fontSize ?? DEFAULT_FONT_SIZE,
                    fontWeight: element.bold ? 700 : 500,
                    textAlign: element.align ?? DEFAULT_TEXT_ALIGN,
                    color: element.color || undefined,
                    backgroundColor: element.backgroundColor || "transparent",
                    display: 'block',
                    marginBottom: '4px'
                  };

                  return (
                    <div className="field" key={element.id} style={{ marginBottom: 'var(--space-3)' }}>
                      <span style={labelStyle}>{element.label}</span>
                      
                      {element.type === "textarea" ? (
                        <textarea
                          value={answers[element.id] ?? ""}
                          disabled
                          style={{ minHeight: '60px', resize: 'none', fontSize: '13px' }}
                        />
                      ) : element.type === "table" ? (
                        (() => {
                          const tableRows = parseTableRows(answers[element.id], element.columns?.length ?? 0);
                          return (
                            <div className="table-wrapper" style={{ marginTop: '4px', overflowX: 'auto' }}>
                              <table className="data-table no-row-click" style={{ fontSize: '12px' }}>
                                <thead>
                                  <tr>
                                    {(element.columns ?? []).map((col) => (
                                      <th key={col} style={{ padding: '4px 8px' }}>{col}</th>
                                    ))}
                                  </tr>
                                </thead>
                                <tbody>
                                  {tableRows.map((row, rIdx) => (
                                    <tr key={rIdx}>
                                      {row.map((cell, cIdx) => (
                                        <td key={cIdx} style={{ padding: '4px 8px' }}>{cell || "—"}</td>
                                      ))}
                                    </tr>
                                  ))}
                                  {tableRows.length === 0 && (
                                    <tr>
                                      <td colSpan={element.columns?.length ?? 1} style={{ textAlign: 'center', padding: '4px' }}>
                                        Sin datos
                                      </td>
                                    </tr>
                                  )}
                                </tbody>
                              </table>
                            </div>
                          );
                        })()
                      ) : element.type === "file" ? (
                        (() => {
                          const files = parseAttachments(answers[element.id]);
                          return (
                            <div className="file-field-readonly" style={{ marginTop: '4px' }}>
                              {files.length === 0 ? (
                                <span style={{ fontSize: '12px', color: 'var(--color-text-3)' }}>Sin archivos adjuntos</span>
                              ) : (
                                <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                  {files.map((file) => (
                                    <li key={file.id} style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '12px' }}>
                                      {file.contentType === "application/pdf" ? (
                                        <IconFileTypePdf size={14} aria-hidden="true" />
                                      ) : (
                                        <IconPhoto size={14} aria-hidden="true" />
                                      )}
                                      <button 
                                        type="button" 
                                        onClick={() => handleViewFile(file.id)}
                                        style={{ background: 'none', border: 'none', padding: 0, color: 'var(--color-primary)', textDecoration: 'underline', cursor: 'pointer', textAlign: 'left' }}
                                      >
                                        {file.originalFilename}
                                      </button>
                                    </li>
                                  ))}
                                </ul>
                              )}
                            </div>
                          );
                        })()
                      ) : element.type === "odontogram" ? (
                        <div style={{ pointerEvents: 'none', opacity: 0.95, zoom: 0.8 }}>
                          <OdontogramField
                            value={answers[element.id]}
                            onChange={() => {}}
                          />
                        </div>
                      ) : element.type === "clinic_header" ? (
                        <ClinicHeaderPreview clinic={clinic} loading={clinicLoading} />
                      ) : element.type === "signature_patient" ? (
                        <div style={{ pointerEvents: 'none', opacity: 0.95 }}>
                          <SignaturePad
                            label="Firma del paciente"
                            signerName={answers.nombre}
                            value={answers[element.id]}
                            onChange={() => {}}
                          />
                        </div>
                      ) : element.type === "signature_doctor" ? (
                        <div style={{ pointerEvents: 'none', opacity: 0.95 }}>
                          <SignaturePad
                            label="Firma del médico"
                            value={answers[element.id]}
                            onChange={() => {}}
                          />
                        </div>
                      ) : element.type === "select" ? (
                        <select value={answers[element.id] ?? ""} disabled style={{ fontSize: '13px', padding: '6px' }}>
                          <option value="">—</option>
                          {(element.options ?? []).map((option) => (
                            <option key={option} value={option}>
                              {option}
                            </option>
                          ))}
                        </select>
                      ) : (
                        <input
                          type={element.type === "number" ? "number" : element.type === "date" ? "date" : "text"}
                          value={answers[element.id] ?? ""}
                          disabled
                          style={{ fontSize: '13px', padding: '6px' }}
                        />
                      )}
                    </div>
                  );
                })}
              </div>
            ))}
          </div>
        </div>
      )}
      {!collapsed && (
        <button
          type="button"
          className="quotation-panel-resize-handle left"
          aria-label="Ajustar tamaño de historia clínica"
          title="Ajustar tamaño"
          onPointerDown={(event) => onResizeStart(event, "left")}
        />
      )}
    </article>
  );
}

function StudiesPanel({
  patientId,
  clinicId,
  size,
  onResizeStart
}: {
  patientId: string;
  clinicId: string;
  size: QuotationSidePanelSize;
  onResizeStart: (event: React.PointerEvent<HTMLButtonElement>, direction: QuotationResizeDirection) => void;
}) {
  const [value, setValue] = useState<string>("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    setLoading(true);
    setError("");
    attachmentsApi
      .list(patientId, clinicId, STUDIES_ELEMENT_ID)
      .then((list) => setValue(serializeAttachments(list)))
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  }, [patientId, clinicId]);

  return (
    <article
      className={`panel quotation-side-panel quotation-resizable-panel quotation-studies-panel ${collapsed ? "collapsed" : ""}`}
      style={{
        width: `${size.width}px`,
        height: collapsed ? undefined : `${size.height}px`,
        marginRight: collapsed ? undefined : `-${size.offsetX}px`
      }}
    >
      <div className="panel-heading quotation-side-panel-heading">
        <div className="quotation-side-panel-title">
          <h2>Estudios</h2>
          <button
            type="button"
            className="quotation-panel-toggle"
            onClick={() => setCollapsed((value) => !value)}
            aria-expanded={!collapsed}
            title={collapsed ? "Expandir estudios" : "Contraer estudios"}
          >
            {collapsed ? <IconChevronDown size={16} aria-hidden="true" /> : <IconChevronUp size={16} aria-hidden="true" />}
          </button>
        </div>
      </div>
      {!collapsed && (
        <div className="quotation-studies-content">
          <p className="panel-subtitle">Estudios resguardados en el expediente del paciente.</p>
          {loading && <p className="panel-subtitle">Cargando...</p>}
          {error && <p className="alert error">{error}</p>}
          {!loading && (
            <FileFieldEditor
              patientId={patientId}
              clinicId={clinicId}
              elementId={STUDIES_ELEMENT_ID}
              value={value}
              onChange={setValue}
            />
          )}
        </div>
      )}
      {!collapsed && (
        <button
          type="button"
          className="quotation-panel-resize-handle left"
          aria-label="Ajustar tamaño de estudios"
          title="Ajustar tamaño"
          onPointerDown={(event) => onResizeStart(event, "left")}
        />
      )}
    </article>
  );
}

export function QuotationEditorScreen({
  patientId,
  clinicId,
  patient,
  quotation,
  catalogItems,
  profile = DEFAULT_CLINIC_PROFILE,
  onClose,
  onSaved
}: {
  patientId: string;
  clinicId: string;
  patient: PatientResponse;
  quotation?: QuotationResponse;
  catalogItems: TreatmentCatalogItemResponse[];
  profile?: ClinicProfile;
  onClose: () => void;
  onSaved: (quotation: QuotationResponse) => void;
}) {
  const [notes, setNotes] = useState(quotation?.notes ?? "");
  const [validUntil, setValidUntil] = useState(quotation?.validUntil ?? "");
  const [quotationDate, setQuotationDate] = useState(quotation?.quotationDate ?? new Date().toISOString().slice(0, 10));
  const [items, setItems] = useState<EditableItem[]>(() => itemsFromQuotation(quotation));
  const [materials, setMaterials] = useState<MaterialResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [historyPanelSize, setHistoryPanelSize] = useState<QuotationSidePanelSize>({ width: 360, height: 520, offsetX: 0 });
  const [studiesPanelSize, setStudiesPanelSize] = useState<QuotationSidePanelSize>({ width: 360, height: 260, offsetX: 0 });

  useEffect(() => {
    materialsApi
      .list(clinicId, false)
      .then(setMaterials)
      .catch((caught) => setError(getFriendlyError(caught)));
  }, [clinicId]);

  const materialsById = new Map(materials.map((material) => [material.id, material]));
  const grandTotal = items.reduce((sum, item) => sum + subtotalFor(item, materialsById), 0);
  const quotationSideWidth = Math.max(
    SIDE_PANEL_MIN_WIDTH,
    historyPanelSize.width - historyPanelSize.offsetX,
    studiesPanelSize.width - studiesPanelSize.offsetX
  );

  const startSidePanelResize = (
    panel: QuotationSidePanelKey,
    event: React.PointerEvent<HTMLButtonElement>,
    direction: QuotationResizeDirection
  ) => {
    if (event.button !== 0) return;
    event.preventDefault();
    event.stopPropagation();

    const initialSize = panel === "history" ? historyPanelSize : studiesPanelSize;
    const setPanelSize = panel === "history" ? setHistoryPanelSize : setStudiesPanelSize;
    const leftMaxWidth = Math.max(
      SIDE_PANEL_MIN_WIDTH,
      Math.min(SIDE_PANEL_MAX_WIDTH, window.innerWidth - SIDE_PANEL_MAIN_MIN_WIDTH)
    );
    const maxWidth =
      direction === "right"
        ? Math.max(
            SIDE_PANEL_MIN_WIDTH,
            Math.min(SIDE_PANEL_MAX_WIDTH, initialSize.width + (SIDE_PANEL_MAX_RIGHT_OFFSET - initialSize.offsetX))
          )
        : leftMaxWidth;
    const startX = event.clientX;

    document.body.classList.add("resizing-quotation-panel");

    const handlePointerMove = (moveEvent: PointerEvent) => {
      const horizontalDelta = direction === "left" ? startX - moveEvent.clientX : moveEvent.clientX - startX;
      const nextWidth = clampNumber(initialSize.width + horizontalDelta, SIDE_PANEL_MIN_WIDTH, maxWidth);
      const nextOffsetX =
        direction === "right"
          ? clampNumber(initialSize.offsetX + (nextWidth - initialSize.width), 0, SIDE_PANEL_MAX_RIGHT_OFFSET)
          : initialSize.offsetX;
      setPanelSize({ width: nextWidth, height: initialSize.height, offsetX: nextOffsetX });
    };

    const handlePointerUp = () => {
      document.body.classList.remove("resizing-quotation-panel");
      window.removeEventListener("pointermove", handlePointerMove);
      window.removeEventListener("pointerup", handlePointerUp);
      window.removeEventListener("pointercancel", handlePointerUp);
    };

    window.addEventListener("pointermove", handlePointerMove);
    window.addEventListener("pointerup", handlePointerUp);
    window.addEventListener("pointercancel", handlePointerUp);
  };

  const updateItem = (key: string, patch: Partial<EditableItem>) => {
    setItems((prev) => prev.map((item) => (item.key === key ? { ...item, ...patch } : item)));
  };

  const handleCatalogSelect = (key: string, catalogItemId: string) => {
    const catalogItem = catalogItems.find((item) => item.id === catalogItemId);
    if (!catalogItem) {
      updateItem(key, { catalogItemId: "" });
      return;
    }
    updateItem(key, {
      catalogItemId,
      description: catalogItem.name,
      laborCharge: String(catalogItem.defaultPrice ?? 0),
      materials: catalogItem.materials.map((material) => ({
        key: `mat_${material.id}_${Math.random().toString(36).slice(2, 6)}`,
        materialId: material.materialId,
        materialName: "",
        estimatedQuantity: material.typicalQuantity ? String(material.typicalQuantity) : "1",
        manualUnitCost: "0"
      }))
    });
  };

  const removeItem = (key: string) => {
    setItems((prev) => (prev.length > 1 ? prev.filter((item) => item.key !== key) : prev));
  };

  const addMaterialLine = (itemKey: string) => {
    setItems((prev) =>
      prev.map((item) => (item.key === itemKey ? { ...item, materials: [...item.materials, emptyMaterialLine()] } : item))
    );
  };

  const updateMaterialLine = (itemKey: string, lineKey: string, patch: Partial<EditableMaterialLine>) => {
    setItems((prev) =>
      prev.map((item) =>
        item.key === itemKey
          ? {
              ...item,
              materials: item.materials.map((line) => (line.key === lineKey ? { ...line, ...patch } : line))
            }
          : item
      )
    );
  };

  const removeMaterialLine = (itemKey: string, lineKey: string) => {
    setItems((prev) =>
      prev.map((item) =>
        item.key === itemKey ? { ...item, materials: item.materials.filter((line) => line.key !== lineKey) } : item
      )
    );
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);
    setError("");

    const requestItems: QuotationItemRequest[] = items.map((item) => {
      const materialLines: MaterialLineRequest[] = item.materials
        .filter((line) => line.materialId || line.materialName.trim())
        .map((line) => ({
          materialId: line.materialId || undefined,
          materialName: line.materialId ? undefined : line.materialName.trim(),
          estimatedQuantity: Number(line.estimatedQuantity) || 0,
          manualUnitCost: line.materialId ? undefined : Number(line.manualUnitCost) || 0
        }));

      return {
        catalogItemId: item.catalogItemId || undefined,
        description: item.description,
        // Un perfil sin localizador clínico no manda diente aunque la partida lo traiga de antes:
        // reenviarlo dejaría la cotización fuera de lo que el perfil de la clínica acepta.
        toothNumber: profile.showsClinicalLocator && item.toothNumber ? Number(item.toothNumber) : undefined,
        laborCharge: Number(item.laborCharge) || 0,
        materials: materialLines,
        discountPercentage: item.discountPercentage ? Number(item.discountPercentage) : undefined
      };
    });

    try {
      if (quotation) {
        await quotationsApi.updateHeader(patientId, quotation.id, {
          clinicId,
          notes: notes || undefined,
          validUntil: validUntil || undefined,
          quotationDate
        });
        const updated = await quotationsApi.replaceItems(patientId, quotation.id, { clinicId, items: requestItems });
        onSaved(updated);
      } else {
        const created = await quotationsApi.create(patientId, {
          clinicId,
          quotationDate,
          notes: notes || undefined,
          validUntil: validUntil || undefined,
          items: requestItems
        });
        onSaved(created);
      }
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  };

  return (
    <section
      className="dashboard-grid quotation-editor-screen"
      style={{ "--quotation-side-width": `${quotationSideWidth}px` } as React.CSSProperties}
    >
      <article className="panel quotation-editor-main">
        <div className="panel-heading">
          <div className="inventory-heading-main">
            <button className="btn secondary" type="button" onClick={onClose}>
              <IconArrowLeft size={16} aria-hidden="true" />
              Volver
            </button>
            <div>
              <h2>{quotation ? "Editar cotización" : "Nueva cotización"}</h2>
              <p className="panel-subtitle">
                {patient.firstName} {patient.lastNamePaterno} {patient.lastNameMaterno}
              </p>
            </div>
          </div>
        </div>

        <form className="profile-form" onSubmit={submit}>
          <div className="quotation-modal-row">
            <label className="field">
              <span>Fecha de la cotización</span>
              <input type="date" value={quotationDate} onChange={(event) => setQuotationDate(event.target.value)} required />
            </label>
            <label className="field">
              <span>Vigente hasta</span>
              <input type="date" value={validUntil} onChange={(event) => setValidUntil(event.target.value)} />
            </label>
          </div>
          <label className="field field-full">
            <span>Notas</span>
            <textarea value={notes} onChange={(event) => setNotes(event.target.value)} />
          </label>

          <div className="quotation-items-editor">
            {items.map((item) => (
              <div className="quotation-item-block" key={item.key}>
                <div className="quotation-item-block-header">
                  <strong>Partida</strong>
                  <button
                    className="icon-btn"
                    type="button"
                    aria-label="Quitar partida"
                    disabled={items.length === 1}
                    onClick={() => removeItem(item.key)}
                  >
                    <IconTrash size={16} />
                  </button>
                </div>

                <div className="quotation-modal-row">
                  <label className="field">
                    <span>Servicio del catálogo</span>
                    <select value={item.catalogItemId} onChange={(event) => handleCatalogSelect(item.key, event.target.value)}>
                      <option value="">Personalizado</option>
                      {catalogItems.map((catalogItem) => (
                        <option key={catalogItem.id} value={catalogItem.id}>
                          {catalogItem.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  {profile.showsClinicalLocator && (
                    <label className="field">
                      <span>{profile.locatorFieldLabel}</span>
                      <input
                        type="number"
                        value={item.toothNumber}
                        onChange={(event) => updateItem(item.key, { toothNumber: event.target.value })}
                      />
                    </label>
                  )}
                </div>

                <label className="field field-full">
                  <span>Descripción</span>
                  <input
                    type="text"
                    value={item.description}
                    onChange={(event) => updateItem(item.key, { description: event.target.value })}
                    required
                  />
                </label>

                <div className="quotation-modal-row">
                  <label className="field">
                    <span>{profile.laborLabel}</span>
                    <input
                      type="number"
                      min={0}
                      value={item.laborCharge}
                      onChange={(event) => updateItem(item.key, { laborCharge: event.target.value })}
                    />
                  </label>
                  <label className="field">
                    <span>Descuento %</span>
                    <input
                      type="number"
                      min={0}
                      max={100}
                      value={item.discountPercentage}
                      onChange={(event) => updateItem(item.key, { discountPercentage: event.target.value })}
                    />
                  </label>
                </div>

                <div className="quotation-item-materials">
                  <span>Materiales estimados</span>
                  <div className="table-wrapper">
                    <table className="data-table no-row-click">
                      <thead>
                        <tr>
                          <th>Material</th>
                          <th>Cantidad est.</th>
                          <th>Costo unitario</th>
                          <th>Costo estimado</th>
                          <th aria-label="Quitar" />
                        </tr>
                      </thead>
                      <tbody>
                        {item.materials.map((line) => (
                          <tr key={line.key}>
                            <td>
                              <select
                                value={line.materialId}
                                onChange={(event) =>
                                  updateMaterialLine(item.key, line.key, { materialId: event.target.value })
                                }
                              >
                                <option value="">Material personalizado</option>
                                {materials.map((material) => (
                                  <option key={material.id} value={material.id}>
                                    {material.name}
                                  </option>
                                ))}
                              </select>
                              {!line.materialId && (
                                <input
                                  type="text"
                                  placeholder="Nombre del material"
                                  value={line.materialName}
                                  onChange={(event) =>
                                    updateMaterialLine(item.key, line.key, { materialName: event.target.value })
                                  }
                                />
                              )}
                            </td>
                            <td>
                              <input
                                type="number"
                                min="0.0001"
                                step="0.0001"
                                value={line.estimatedQuantity}
                                onChange={(event) =>
                                  updateMaterialLine(item.key, line.key, { estimatedQuantity: event.target.value })
                                }
                              />
                            </td>
                            <td>
                              {line.materialId ? (
                                currencyFormatter.format(materialsById.get(line.materialId)?.unitCost ?? 0)
                              ) : (
                                <input
                                  type="number"
                                  min={0}
                                  step="0.0001"
                                  value={line.manualUnitCost}
                                  onChange={(event) =>
                                    updateMaterialLine(item.key, line.key, { manualUnitCost: event.target.value })
                                  }
                                />
                              )}
                            </td>
                            <td>{currencyFormatter.format(materialCost(line, materialsById))}</td>
                            <td>
                              <button
                                className="icon-btn"
                                type="button"
                                aria-label="Quitar material"
                                onClick={() => removeMaterialLine(item.key, line.key)}
                              >
                                <IconTrash size={16} />
                              </button>
                            </td>
                          </tr>
                        ))}
                        {item.materials.length === 0 && (
                          <tr>
                            <td colSpan={5}>
                              <div className="empty-table-state">Sin materiales estimados en esta partida.</div>
                            </td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                  <button className="btn ghost" type="button" onClick={() => addMaterialLine(item.key)}>
                    <IconPlus size={16} aria-hidden="true" />
                    Agregar material
                  </button>
                </div>

                <div className="quotation-item-subtotal">
                  <span>Subtotal de la partida</span>
                  <span>{currencyFormatter.format(subtotalFor(item, materialsById))}</span>
                </div>
              </div>
            ))}

            <button className="btn ghost" type="button" onClick={() => setItems((prev) => [...prev, emptyItem()])}>
              <IconPlus size={16} aria-hidden="true" />
              Agregar partida
            </button>

            <div className="quotation-item-subtotal">
              <strong>Total general</strong>
              <strong>{currencyFormatter.format(grandTotal)}</strong>
            </div>
          </div>

          {error && <p className="alert error">{error}</p>}
          <div className="form-actions">
            <button className="btn primary" disabled={loading} type="submit">
              <IconDeviceFloppy size={18} aria-hidden="true" />
              {loading ? "Guardando" : "Guardar"}
            </button>
          </div>
        </form>
      </article>

      <div className="quotation-editor-side">
        <HistoryPanel
          patientId={patientId}
          clinicId={clinicId}
          size={historyPanelSize}
          onResizeStart={(event, direction) => startSidePanelResize("history", event, direction)}
        />
        <StudiesPanel
          patientId={patientId}
          clinicId={clinicId}
          size={studiesPanelSize}
          onResizeStart={(event, direction) => startSidePanelResize("studies", event, direction)}
        />
      </div>
    </section>
  );
}
